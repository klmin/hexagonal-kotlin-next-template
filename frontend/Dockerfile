# docker build -t frontend .
# docker run -p 3000:3000 -e BACKEND_URL=http://backend:8080 frontend
#
# vite build 로 만든 정적 파일(dist/)을 nginx 가 서빙하고, /api 는 BACKEND_URL 로 넘긴다.
# BACKEND_URL 은 컨테이너가 시작할 때 읽으므로 이미지를 다시 빌드하지 않아도 바꿀 수 있다

FROM node:26-alpine AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:26-alpine AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npm run build

# 루트가 아닌 사용자(nginx, uid 101)로 실행되는 이미지
FROM nginxinc/nginx-unprivileged:1.29-alpine AS runtime
# NGINX_ENTRYPOINT_LOCAL_RESOLVERS: 시작할 때 /etc/resolv.conf 의 DNS 서버를 NGINX_LOCAL_RESOLVERS 로 넣는다 (nginx 설정의 resolver)
ENV BACKEND_URL=http://backend:8080 \
    NGINX_ENTRYPOINT_LOCAL_RESOLVERS=1
COPY nginx/default.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 3000
