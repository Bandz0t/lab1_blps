#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
AUTHOR_ID=${AUTHOR_ID:-1}
VIDEO_ID=${VIDEO_ID:-1}
AUTHOR_USERNAME=${AUTHOR_USERNAME:-demo_author}
AUTHOR_PASSWORD=${AUTHOR_PASSWORD:-demo12345}
MODERATOR_USERNAME=${MODERATOR_USERNAME:-moderator}
MODERATOR_PASSWORD=${MODERATOR_PASSWORD:-moderator12345}

printf '\n[0] Получение JWT автора\n'
AUTHOR_TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/token" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${AUTHOR_USERNAME}\",\"password\":\"${AUTHOR_PASSWORD}\"}" | jq -r '.token')

printf '\n[1] Создание видео\n'
curl -sS -X POST "$BASE_URL/api/videos" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"authorId\":${AUTHOR_ID},\"title\":\"My first vlog\",\"description\":\"Travel video\",\"filePath\":\"/uploads/video1.mp4\",\"format\":\"mp4\",\"sizeBytes\":104857600,\"durationSeconds\":600}" | jq .

printf '\n[2] Проверка статуса видео\n'
curl -sS "$BASE_URL/api/videos/${VIDEO_ID}/status" -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .

printf '\n[3] Выбор монетизации\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/monetization" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"monetizationType":"ALL_FORMATS"}' | jq .

printf '\n[4] Доходы пользователя\n'
curl -sS "$BASE_URL/api/users/${AUTHOR_ID}/revenues" -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .

printf '\n[5] Выплаты пользователя\n'
curl -sS "$BASE_URL/api/users/${AUTHOR_ID}/payouts" -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .

printf '\n[6] Получение JWT модератора\n'
MODERATOR_TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/token" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${MODERATOR_USERNAME}\",\"password\":\"${MODERATOR_PASSWORD}\"}" | jq -r '.token')

printf '\n[7] Проверка авторских прав без нарушений\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/copyright-check" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"hasViolation":false}' | jq .

printf '\n[8] Решение модератора APPROVE\n'
curl -sS -X POST "$BASE_URL/api/moderation/videos/${VIDEO_ID}/decision" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVE","reason":"All checks passed"}' | jq .

printf '\n[9] Решение модератора REJECT\n'
curl -sS -X POST "$BASE_URL/api/moderation/videos/${VIDEO_ID}/decision" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"decision":"REJECT","reason":"Policy violation"}' | jq .

printf '\n[10] Запуск месячного расчёта администратором\n'
ADMIN_TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/token" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' | jq -r '.token')
curl -sS -X POST "$BASE_URL/api/payouts/process-monthly" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}' | jq .
