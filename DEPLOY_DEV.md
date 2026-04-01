# 云端 dev 部署说明

## 交付内容
- `compose.dev-services.yml`
- `auth-service/Dockerfile`
- `user-service/Dockerfile`
- `item-service/Dockerfile`
- `trade-service/Dockerfile`
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
- Nginx 需要把上传目录映射为静态资源目录：

```nginx
location /uploads/ {
    alias /opt/campus-market/dev/uploads/;
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

这些端口是容器内服务端口，Nginx 反向代理时请使用对应服务名和端口。
