#!/usr/bin/env node
// PostToolUse hook: frontend/의 .ts/.tsx 파일이 수정되면 그 파일에 prettier --write, eslint --fix를 돌린다.
// 남은 lint 오류는 exit 2 + stderr로 돌려줘서 바로 고치게 한다.
import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";

let input = "";
for await (const chunk of process.stdin) input += chunk;

let filePath;
try {
  filePath = JSON.parse(input)?.tool_input?.file_path;
} catch {
  process.exit(0);
}
if (!filePath || !/\.(ts|tsx)$/.test(filePath)) process.exit(0);

const projectDir = process.env.CLAUDE_PROJECT_DIR ?? process.cwd();
const frontendDir = path.join(projectDir, "frontend");
const rel = path.relative(frontendDir, path.resolve(projectDir, filePath));
if (rel.startsWith("..") || path.isAbsolute(rel) || rel.startsWith("node_modules")) process.exit(0);

const bin = (...p) => path.join(frontendDir, "node_modules", ...p);
const run = (script, args) =>
  spawnSync(process.execPath, [script, ...args], { cwd: frontendDir, encoding: "utf8" });

// npm ci 전이면 건너뛴다
if (existsSync(bin("prettier", "bin", "prettier.cjs"))) {
  run(bin("prettier", "bin", "prettier.cjs"), ["--write", "--ignore-unknown", rel]);
}
if (!existsSync(bin("eslint", "bin", "eslint.js"))) process.exit(0);

const result = run(bin("eslint", "bin", "eslint.js"), ["--fix", rel]);
if (result.status !== 0 && result.status !== null) {
  process.stderr.write(`eslint 오류 (${rel}):\n${result.stdout}${result.stderr}`);
  process.exit(2);
}
