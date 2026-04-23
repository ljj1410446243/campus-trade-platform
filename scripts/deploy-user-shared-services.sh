#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="${1:-/opt/campus-market/dev/backend}"

cd "$ROOT_DIR"

docker compose --progress=plain --env-file .env -f compose.dev-services.yml build \
  auth-service \
  user-service \
  notice-service \
  review-service \
  item-service \
  trade-service \
  chat-service

docker compose --env-file .env -f compose.dev-services.yml up -d --no-deps \
  auth-service \
  user-service \
  notice-service \
  review-service \
  item-service \
  trade-service \
  chat-service

docker exec nginx nginx -t
docker exec nginx nginx -s reload

curl http://127.0.0.1/auth/ping
curl http://127.0.0.1/users/ping
curl http://127.0.0.1/items/ping
curl http://127.0.0.1/trades/ping

admin_reports_status="$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/admin/reports)"
if [[ "$admin_reports_status" != "401" ]]; then
  echo "Expected /admin/reports to return 401 after deploy, got ${admin_reports_status}" >&2
  echo "Check whether Nginx publishes /admin/reports* to user-service:8082 and whether the latest user-service image is deployed." >&2
  exit 1
fi

echo "/admin/reports is reachable through Nginx (401 without token as expected)."

admin_pickup_points_status="$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/admin/pickup-points)"
if [[ "$admin_pickup_points_status" != "401" ]]; then
  echo "Expected /admin/pickup-points to return 401 after deploy, got ${admin_pickup_points_status}" >&2
  echo "Check whether Nginx publishes /admin/pickup-points* to trade-service:8084 and whether the latest trade-service image is deployed." >&2
  exit 1
fi

echo "/admin/pickup-points is reachable through Nginx (401 without token as expected)."

pickup_points_status="$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/pickup-points)"
if [[ "$pickup_points_status" != "200" ]]; then
  echo "Expected /pickup-points to return 200 after deploy, got ${pickup_points_status}" >&2
  echo "Check whether Nginx publishes /pickup-points* to trade-service:8084 and whether trade-service has enabled pickup points." >&2
  exit 1
fi

echo "/pickup-points is reachable through Nginx (200 as expected for the public pickup point list)."

notifications_status="$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1/notifications)"
if [[ "$notifications_status" != "401" ]]; then
  echo "Expected /notifications to return 401 after deploy, got ${notifications_status}" >&2
  echo "Check whether Nginx publishes /notifications* to notice-service:8088 and whether the latest notice-service image is deployed." >&2
  exit 1
fi

echo "/notifications is reachable through Nginx (401 without token as expected)."
