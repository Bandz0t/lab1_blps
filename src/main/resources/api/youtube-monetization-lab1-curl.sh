#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
AUTHOR_ID=${AUTHOR_ID:-1}
AUTHOR_USERNAME=${AUTHOR_USERNAME:-demo_author}
AUTHOR_PASSWORD=${AUTHOR_PASSWORD:-demo12345}
MODERATOR_USERNAME=${MODERATOR_USERNAME:-moderator}
MODERATOR_PASSWORD=${MODERATOR_PASSWORD:-moderator12345}

printf '\n[0] Get author JWT\n'
AUTHOR_TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/token" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${AUTHOR_USERNAME}\",\"password\":\"${AUTHOR_PASSWORD}\"}" | jq -r '.token')

printf '\n[1] Create video and queue async Kafka processing\n'
VIDEO_JSON=$(curl -sS -X POST "$BASE_URL/api/videos" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d "{\"authorId\":${AUTHOR_ID},\"title\":\"My first vlog\",\"description\":\"Travel video\",\"filePath\":\"/uploads/video1.mp4\",\"format\":\"mp4\",\"sizeBytes\":104857600,\"durationSeconds\":600}")
echo "$VIDEO_JSON" | jq .
VIDEO_ID=$(echo "$VIDEO_JSON" | jq -r '.id')

printf '\n[2] Poll async video status\n'
for _ in 1 2 3 4 5; do
  curl -sS "$BASE_URL/api/videos/${VIDEO_ID}/status" -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .
  sleep 2
done

printf '\n[3] Get moderator JWT\n'
MODERATOR_TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/token" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"${MODERATOR_USERNAME}\",\"password\":\"${MODERATOR_PASSWORD}\"}" | jq -r '.token')

printf '\n[4] Manual copyright clear if async recognizer left video pending\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/copyright-check" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"hasViolation":false}' | jq .

printf '\n[5] Moderator APPROVE decision, moderation.decision event goes through Kafka outbox\n'
curl -sS -X POST "$BASE_URL/api/moderation/videos/${VIDEO_ID}/decision" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"decision":"APPROVE","reason":"All checks passed"}' | jq .

printf '\n[6] Select monetization\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/monetization" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"monetizationType":"ALL_FORMATS"}' | jq .

printf '\n[7] Queue monthly payout process asynchronously\n'
ADMIN_TOKEN=$(curl -sS -X POST "$BASE_URL/api/auth/token" \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin12345"}' | jq -r '.token')
curl -sS -X POST "$BASE_URL/api/payouts/process-monthly/async" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}' | jq .

printf '\n[8] Check payouts after Kafka consumer and Payment EIS/JCA registration\n'
sleep 5
curl -sS "$BASE_URL/api/users/${AUTHOR_ID}/payouts" -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .
