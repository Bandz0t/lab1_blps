#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
AUTHOR_ID=${AUTHOR_ID:-1}
VIDEO_ID=${VIDEO_ID:-1}

printf '\n[1] Создание видео\n'
curl -sS -X POST "$BASE_URL/api/videos" \
  -H 'Content-Type: application/json' \
  -d "{\"authorId\":${AUTHOR_ID},\"title\":\"My first vlog\",\"description\":\"Travel video\",\"filePath\":\"/uploads/video1.mp4\",\"format\":\"mp4\",\"sizeBytes\":104857600,\"durationSeconds\":600}" | jq .

printf '\n[2] Проверка статуса видео\n'
curl -sS "$BASE_URL/api/videos/${VIDEO_ID}/status" | jq .

printf '\n[3] Проверка авторских прав без нарушений\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/copyright-check" \
  -H 'Content-Type: application/json' \
  -d '{"hasViolation":false}' | jq .

printf '\n[4] Выбор монетизации\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/monetization" \
  -H 'Content-Type: application/json' \
  -d '{"monetizationType":"ALL_FORMATS"}' | jq .

printf '\n[5] Запуск месячного расчёта\n'
curl -sS -X POST "$BASE_URL/api/payouts/process-monthly" \
  -H 'Content-Type: application/json' \
  -d '{}' | jq .

printf '\n[6] Доходы пользователя\n'
curl -sS "$BASE_URL/api/users/${AUTHOR_ID}/revenues" | jq .

printf '\n[7] Выплаты пользователя\n'
curl -sS "$BASE_URL/api/users/${AUTHOR_ID}/payouts" | jq .

printf '\n[8] Решение модератора APPROVE\n'
curl -sS -X POST "$BASE_URL/api/moderation/videos/${VIDEO_ID}/decision" \
  -u moderator:moderator12345 \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVE","reason":"All checks passed"}' | jq .

printf '\n[9] Решение модератора REJECT\n'
curl -sS -X POST "$BASE_URL/api/moderation/videos/${VIDEO_ID}/decision" \
  -u moderator:moderator12345 \
  -H 'Content-Type: application/json' \
  -d '{"decision":"REJECT","reason":"Policy violation"}' | jq .

printf '\n[10] Решение модератора MANUAL_REVIEW\n'
curl -sS -X POST "$BASE_URL/api/moderation/videos/${VIDEO_ID}/decision" \
  -u moderator:moderator12345 \
  -H 'Content-Type: application/json' \
  -d '{"decision":"MANUAL_REVIEW","reason":"Need additional human review"}' | jq .
