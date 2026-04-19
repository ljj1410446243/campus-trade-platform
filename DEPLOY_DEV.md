# 云端 dev 部署说明

## 交付内容
- `compose.dev-services.yml`
- `auth-service/Dockerfile`
- `user-service/Dockerfile`
- `item-service/Dockerfile`
- `trade-service/Dockerfile`
- `notice-service/Dockerfile`
- `chat-service/Dockerfile`
- `review-service/Dockerfile`
- `file-service/Dockerfile`
- `.env.example`

## 配置读取方式
- 所有服务都优先从环境变量读取配置。
- Spring Boot 标准变量：
  - `SERVER_PORT`
  - `SPRING_DATA_MONGODB_URI`
  - `SPRING_DATA_REDIS_HOST`
  - `SPRING_DATA_REDIS_PORT`
  - `SPRING_DATA_REDIS_PASSWORD`
  - `JWT_SECRET`
- 应用自定义变量：
  - `APP_UPLOAD_DIR`
  - `APP_PUBLIC_BASE_URL`
  - `APP_CORS_ALLOWED_ORIGINS`
- `auth-service` 额外支持：
  - `AUTH_COOKIE_NAME`
  - `AUTH_COOKIE_PATH`
  - `AUTH_COOKIE_DOMAIN`
  - `AUTH_COOKIE_SAME_SITE`
  - `AUTH_COOKIE_SECURE`
  - `AUTH_COOKIE_MAX_AGE`
- `item-service` 额外支持：
  - `APP_LOCATION_AMAP_BASE_URL`
  - `APP_LOCATION_AMAP_KEY`
- `user-service` 额外支持：
  - `NOTICE_SERVICE_BASE_URL`

## 上传文件与访问路径
- `file-service` 会把文件保存到 `APP_UPLOAD_DIR` 指定的目录。
- 当前 dev 推荐值：
  - `APP_UPLOAD_DIR=/opt/campus-market/dev/uploads`
- 新上传文件的公开 URL 规则：
  - `${APP_PUBLIC_BASE_URL}/uploads/items/...`
  - `${APP_PUBLIC_BASE_URL}/uploads/avatars/...`
  - `${APP_PUBLIC_BASE_URL}/uploads/chat/...`
- 业务服务只保存图片 URL，不保存本地磁盘绝对路径。

## Nginx 要求
- Nginx 需要和业务服务加入同一个 Docker 网络。
- `proxy_pass` 不要在这些前缀路由里额外带尾部 `/`，否则 Nginx 会重写 URI，把 `/auth/login` 之类的路径改成 `/login`，导致后端返回 404/405/500。
- Nginx 必须显式代理 `user-service` 的两组接口：
  - `/users/**`
  - `/reports/**`
- Nginx 必须显式代理 `trade-service` 的用户侧自提点接口：
  - `/pickup-points`
  - `/pickup-points/**`
- Nginx 必须显式代理 `notice-service` 的通知接口：
  - `/notifications`
  - `/notifications/read-all`
  - `/notifications/**`
- Nginx 必须显式代理 `item-service` 的定位接口：
  - `/locations`
  - `/locations/**`
- Nginx 不能把全部 `/admin/**` 都转给同一个服务。
- 当前至少需要拆分四组管理接口：
  - `/admin/reports*` -> `user-service:8082`
  - `/admin/users*` -> `user-service:8082`
  - `/admin/items*` -> `user-service:8082`
  - `/admin/pickup-points*` -> `trade-service:8084`
- Nginx 需要把上传目录映射为静态资源目录：

```nginx
sendfile on;
tcp_nopush on;
tcp_nodelay on;
etag on;

gzip on;
gzip_min_length 1024;
gzip_types application/json text/css application/javascript application/xml image/svg+xml;

open_file_cache max=10000 inactive=60s;
open_file_cache_valid 120s;
open_file_cache_min_uses 2;
open_file_cache_errors on;

location /uploads/ {
    alias /opt/campus-market/dev/uploads/;
    expires 30d;
    add_header Cache-Control "public, max-age=2592000, immutable";
}
```

- 推荐直接采用仓库里的路由示例文件：
  - [scripts/nginx/campus-market-dev.routes.conf.example](/E:/WorkProject/njust/campus-trade-platform/scripts/nginx/campus-market-dev.routes.conf.example)
- 至少保证下面四条路由存在，并按服务拆分：
- 同时保证 `/locations` 也被转发到 `item-service`：

```nginx
location = /reports {
    proxy_pass http://user-service:8082;
}

location /reports/ {
    proxy_pass http://user-service:8082;
}

location = /locations {
    proxy_pass http://item-service:8083;
}

location /locations/ {
    proxy_pass http://item-service:8083;
}

location = /admin/reports {
    proxy_pass http://user-service:8082;
}

location /admin/reports/ {
    proxy_pass http://user-service:8082;
}

location /admin/users/ {
    proxy_pass http://user-service:8082;
}

location /admin/items/ {
    proxy_pass http://user-service:8082;
}

location = /admin/pickup-points {
    proxy_pass http://trade-service:8084;
}

location /admin/pickup-points/ {
    proxy_pass http://trade-service:8084;
}

location = /pickup-points {
    proxy_pass http://trade-service:8084;
}

location /pickup-points/ {
    proxy_pass http://trade-service:8084;
}

location = /notifications {
    proxy_pass http://notice-service:8088;
}

location = /notifications/read-all {
    proxy_pass http://notice-service:8088;
}

location /notifications/ {
    proxy_pass http://notice-service:8088;
}
```

- `APP_PUBLIC_BASE_URL` 必须填写成前端实际访问的公网地址，例如：
  - `http://124.220.215.204`

## 启动顺序
1. 确认 `mongo`、`redis`、`nginx` 已经启动。
2. 确认它们都在同一个 Docker 网络中，例如 `campus-market-dev`。
3. 复制 `.env.example` 为 `.env` 并填写实际值。
4. 执行业务服务 overlay：

```bash
docker compose --env-file .env -f compose.dev-services.yml up -d --build
```

## 发布规则
- 纯局部逻辑改动：
  - 只发布受影响服务即可。
- 共享 `users` 文档改动：
  - 只要改动涉及 `passwordHash`、`phone`、`realName`、`campusVerified`、`studentId`、`nickname`、`avatarUrl`、`status`、`bannedAt`、`bannedReason`、`creditScore`、`creditLevel`、`reviewCount`、`averageRating`，必须统一发布：
    - `auth-service`
    - `user-service`
    - `review-service`
    - `item-service`
    - `trade-service`
    - `chat-service`
- 原因：
  - 这些服务都读取同一个 `users` 集合，不能让新旧模型混跑。

## 局部发布后的固定动作
- 只要重建过被 Nginx 代理的业务服务，发布完成后都执行：

```bash
docker exec nginx nginx -t
docker exec nginx nginx -s reload
```

- 原因：
  - Docker 重建服务后容器 IP 可能变化，Nginx 如果不 reload，可能继续使用旧 upstream IP，导致 `502 Bad Gateway`。
- 对管理员接口，reload 后再做两次最小烟雾校验：
- 对通知接口，reload 后再补一条最小烟雾校验：
- 对定位接口，再补一条最小烟雾校验：

```bash
curl -i http://127.0.0.1/admin/reports
curl -i -X POST http://127.0.0.1/admin/users/test-user-id/ban
curl -i -X PUT http://127.0.0.1/admin/items/test-item-id/off-shelf
curl -i http://127.0.0.1/admin/pickup-points
curl -i http://127.0.0.1/pickup-points
curl -i http://127.0.0.1/notifications
curl -i "http://127.0.0.1/locations/reverse-geocode?lat=32.060255&lng=118.796877"
```

- 预期：
  - 返回 `401`：说明路由已经被 Nginx 正确转发到目标服务，只是当前请求未带 token
  - 返回 `200`：对于公开接口同样说明路径已通，例如 `/pickup-points`
  - 返回 `403`：说明路径已通，但当前 token 非管理员
  - 返回 `404`：说明对应 admin 子路径仍未正确发布，或被错误转发到了其他服务
  - 定位接口返回 `200/400/500/502` 都说明 `/locations` 已被 Nginx 正确转发到 `item-service`
  - 返回 `502`：说明 Nginx upstream 还指向旧容器或目标服务未启动

## 用户相关改动的推荐命令
- 构建共享用户相关服务：

```bash
docker compose --progress=plain --env-file .env -f compose.dev-services.yml build auth-service user-service review-service item-service trade-service chat-service
```

- 重启共享用户相关服务：

```bash
docker compose --env-file .env -f compose.dev-services.yml up -d --no-deps auth-service user-service review-service item-service trade-service chat-service
```

- 重载 Nginx：

```bash
docker exec nginx nginx -t
docker exec nginx nginx -s reload
```

- 校验管理员举报路由：

```bash
curl -i http://127.0.0.1/admin/reports
curl -i -X POST http://127.0.0.1/admin/users/test-user-id/ban
curl -i -X PUT http://127.0.0.1/admin/items/test-item-id/off-shelf
curl -i http://127.0.0.1/admin/pickup-points
curl -i http://127.0.0.1/pickup-points
curl -i http://127.0.0.1/notifications
curl -i "http://127.0.0.1/locations/reverse-geocode?lat=32.060255&lng=118.796877"
```

## 需要手工填写的变量
- `JWT_SECRET`
  - 所有服务共用的 JWT 密钥，必须替换成正式随机值。
- `APP_PUBLIC_BASE_URL`
  - 前端和客户端实际访问后端/Nginx 的公网基础地址。
- `APP_UPLOAD_DIR`
  - 宿主机上传目录，当前 dev 环境建议固定为 `/opt/campus-market/dev/uploads`。
- `APP_CORS_ALLOWED_ORIGINS`
  - 逗号分隔白名单。Flutter 真机调试、Web 调试地址都要写进去。
- `APP_LOCATION_AMAP_KEY`
  - 如果保留高德位置服务，需要填写真实 key。
- `NOTICE_SERVICE_BASE_URL`
  - `user-service` 访问 `notice-service` 的内网地址，dev 推荐：
  - `http://notice-service:8088`
- `AUTH_COOKIE_DOMAIN`
  - 当前 dev 若直接使用 IP，可留空；后续改域名时再填写。
- `AUTH_COOKIE_SECURE`
  - 当前 HTTP dev 通常填 `false`；未来 HTTPS 再改为 `true`。

## 业务服务 overlay 说明
- `compose.dev-services.yml` 只负责业务服务。
- 该文件不会重复创建 `mongo`、`redis`、`nginx`。
- `file-service` 会把宿主机的 `APP_UPLOAD_DIR` 原样挂进容器，保证 Spring 和 Nginx 看到的是同一份文件。
- MongoDB 连接建议使用：
  - `mongodb://mongo:27017/campus_trade`
- Redis 连接建议使用：
  - `redis:6379`

## 当前默认端口
- `auth-service`: `8081`
- `user-service`: `8082`
- `item-service`: `8083`
- `trade-service`: `8084`
- `chat-service`: `8085`
- `review-service`: `8086`
- `file-service`: `8087`
- `notice-service`: `8088`

这些端口是容器内服务端口，Nginx 反向代理时请使用对应服务名和端口。
