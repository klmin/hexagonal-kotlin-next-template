#!/usr/bin/env node
// 템플릿으로 만든 새 저장소에서 한 번 실행한다.
//   node scripts/init-project.mjs --name my-shop --package com.acme.shop --owner my-github-id
//
// 하는 일
//   - 패키지 com.example.template -> --package (src/*/kotlin 폴더 이동 + 코드 치환)
//   - build.gradle.kts group -> 패키지의 앞부분 (com.acme.shop -> com.acme)
//   - README 제목, compose 프로젝트 이름, 브라우저 탭 제목(layout.tsx) -> --name
//   - CODEOWNERS, LICENSE 소유자 -> --owner (사용자 ID 또는 조직의 팀 org/team)
// 끝나면 git diff로 확인하고 ./gradlew spotlessApply build 로 검증한다.
import { execFileSync } from "node:child_process";
import fs from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { parseArgs } from "node:util";

const OLD_PACKAGE = "com.example.template";
const OLD_GROUP = "com.example";
const OLD_NAME = "hexagonal-kotlin-next-template";

const { values } = parseArgs({
  options: {
    name: { type: "string" },
    package: { type: "string" },
    owner: { type: "string" },
    "dry-run": { type: "boolean", default: false },
  },
});

const usage = "사용법: node scripts/init-project.mjs --name <프로젝트명> --package <패키지> --owner <GitHub ID>";
if (!values.name || !values.package || !values.owner) {
  console.error(usage);
  process.exit(1);
}
if (!/^[a-z][a-z0-9-]*$/.test(values.name)) {
  console.error("--name 은 소문자, 숫자, - 만 쓸 수 있습니다. 예) my-shop");
  process.exit(1);
}
if (!/^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$/.test(values.package)) {
  console.error("--package 는 소문자 자바 패키지 형식이어야 합니다. 예) com.acme.shop");
  process.exit(1);
}
// 자바 예약어는 패키지 이름에 쓸 수 없다(컴파일할 때 실패). 파일을 바꾸기 전에 막는다
const JAVA_KEYWORDS = new Set(
  ("abstract assert boolean break byte case catch char class const continue default do double else enum " +
    "extends final finally float for goto if implements import instanceof int interface long native new " +
    "package private protected public return short static strictfp super switch synchronized this throw " +
    "throws transient try void volatile while true false null _")
    .split(" "),
);
const keywordSegment = values.package.split(".").find((s) => JAVA_KEYWORDS.has(s));
if (keywordSegment) {
  console.error(`--package 에 자바 예약어 "${keywordSegment}" 를 쓸 수 없습니다. 예) com.acme.shop`);
  process.exit(1);
}
// CODEOWNERS 는 사용자(@user) 또는 조직의 팀(@org/team)만 받는다. 조직 ID 하나만으로는 동작하지 않는다
if (!/^[A-Za-z0-9-]+(\/[A-Za-z0-9_.-]+)?$/.test(values.owner)) {
  console.error("--owner 는 GitHub 사용자 ID 또는 조직의 팀(org/team) 입니다. 예) my-github-id, acme/backend");
  process.exit(1);
}

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const newPackage = values.package;
const newGroup = newPackage.split(".").slice(0, -1).join(".");
const dryRun = values["dry-run"];
const changed = [];

function trackedFiles() {
  return execFileSync("git", ["ls-files"], { cwd: root, encoding: "utf8" })
    .split("\n")
    .filter(Boolean);
}

const TEXT = /\.(kt|kts|ya?ml|md|json|mjs|ts|tsx|properties|sql|alloy|xml)$|^(CODEOWNERS|LICENSE|Dockerfile)$/;
// 템플릿 저장소에서만 쓰는 파일. 새 프로젝트에서는 의미가 없어서 지운다(4번).
// template-init 워크플로는 이전 이름을 검사 패턴으로 갖고 있어, 치환하면 새 프로젝트에서 검사가 깨진다
const TEMPLATE_ONLY = [".github/workflows/template-init.yml"];
const SKIP = [
  /package-lock\.json$/,
  /schema\.d\.ts$/,
  /^openapi\//,
  /^scripts\/init-project\.mjs$/,
  ...TEMPLATE_ONLY.map((f) => new RegExp(`^${f.replaceAll(".", "\\.")}$`)),
];

function rewrite(file, replacements) {
  const p = path.join(root, file);
  const before = fs.readFileSync(p, "utf8");
  let after = before;
  for (const [from, to] of replacements) after = after.split(from).join(to);
  if (after !== before) {
    changed.push(file);
    if (!dryRun) fs.writeFileSync(p, after);
  }
}

function fail(message) {
  console.error(message);
  process.exit(1);
}

// child 가 parent 의 하위 경로인지 (같은 경로는 아님)
function isInside(child, parent) {
  const rel = path.relative(parent, child);
  return rel !== "" && !rel.startsWith("..") && !path.isAbsolute(rel);
}

// 1. 패키지 폴더 이동 계획. 파일을 고치기 전에 검증해서 중간에 실패하지 않게 한다.
if (newPackage === OLD_PACKAGE) fail(`--package 가 기존 패키지(${OLD_PACKAGE})와 같습니다.`);
const moves = [];
for (const sourceSet of ["main", "test"]) {
  const srcDir = path.join(root, "backend", "src", sourceSet, "kotlin");
  const from = path.join(srcDir, ...OLD_PACKAGE.split("."));
  const to = path.join(srcDir, ...newPackage.split("."));
  if (!fs.existsSync(from)) continue;
  if (isInside(from, to)) {
    // 새 패키지가 기존 패키지의 상위 (예: com.example). from 을 빼면 to 가 비어야 한다.
    for (let dir = from; dir !== to; dir = path.dirname(dir)) {
      if (fs.readdirSync(path.dirname(dir)).length !== 1) {
        fail(`${path.relative(root, to)} 에 다른 파일이 있어 패키지를 옮길 수 없습니다.`);
      }
    }
  } else if (!isInside(to, from) && fs.existsSync(to)) {
    // 새 패키지가 기존 패키지의 하위 (예: com.example.template.shop)면 임시 폴더를 거쳐 옮기니 괜찮다.
    fail(`${path.relative(root, to)} 가 이미 있습니다.`);
  }
  moves.push({ srcDir, from, to });
}

// 2. 내용 치환
for (const file of trackedFiles()) {
  const base = path.basename(file);
  if (!TEXT.test(base) || SKIP.some((re) => re.test(file))) continue;
  if (!fs.existsSync(path.join(root, file))) continue;

  const replacements = [
    [OLD_PACKAGE, newPackage],
    [OLD_PACKAGE.replaceAll(".", "/"), newPackage.replaceAll(".", "/")],
    [OLD_NAME, values.name],
    ["name: template-app", `name: ${values.name}`],
    ["name: template-observability", `name: ${values.name}-observability`],
  ];
  if (file === "backend/build.gradle.kts") replacements.push([`group = "${OLD_GROUP}"`, `group = "${newGroup}"`]);
  if (file === ".github/CODEOWNERS") replacements.push(["@klmin", `@${values.owner}`]);
  if (file === "LICENSE") {
    const holder = values.owner.split("/")[0];
    replacements.push(["Copyright (c) 2026 klmin", `Copyright (c) ${new Date().getFullYear()} ${holder}`]);
  }
  if (file === "frontend/src/app/layout.tsx") replacements.push(['title: "Hexagonal Template"', `title: "${values.name}"`]);
  rewrite(file, replacements);
}

// 3. 패키지 폴더 이동
// 새 패키지가 기존 패키지의 상위/하위일 수 있어서 임시 폴더로 뺐다가 옮긴다.
function removeEmptyParents(dir, stop) {
  while (dir !== stop && fs.existsSync(dir) && fs.readdirSync(dir).length === 0) {
    fs.rmdirSync(dir);
    dir = path.dirname(dir);
  }
}

for (const { srcDir, from, to } of moves) {
  console.log(`move ${path.relative(root, from)} -> ${path.relative(root, to)}`);
  if (dryRun) continue;
  const tmp = path.join(srcDir, `.init-project-${process.pid}`);
  fs.renameSync(from, tmp);
  removeEmptyParents(path.dirname(from), srcDir); // 비게 된 상위 폴더 정리 (com/example)
  fs.mkdirSync(path.dirname(to), { recursive: true });
  fs.renameSync(tmp, to);
}

// 4. 템플릿 전용 파일 삭제
for (const file of TEMPLATE_ONLY) {
  const p = path.join(root, file);
  if (!fs.existsSync(p)) continue;
  console.log(`delete ${file}`);
  if (!dryRun) fs.rmSync(p);
}

console.log(`\n${dryRun ? "[dry-run] " : ""}${changed.length}개 파일 수정`);
for (const f of changed) console.log(`  ${f}`);
console.log(`
다음 단계
  1. git diff 로 변경 확인
  2. cd backend && ./gradlew spotlessApply build   (import 정렬이 패키지 이름에 따라 바뀔 수 있어 포맷부터. openapi.json 은 그대로)
  3. 샘플 item 기능은 참고 후 삭제하거나 실제 도메인으로 교체
  4. 이 스크립트(scripts/init-project.mjs)는 지워도 된다`);
