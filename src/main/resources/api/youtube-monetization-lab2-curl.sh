#!/usr/bin/env bash
set -euo pipefail

BASE_URL=${BASE_URL:-http://localhost:8080}
AUTHOR_USERNAME=${AUTHOR_USERNAME:-demo_author}
AUTHOR_PASSWORD=${AUTHOR_PASSWORD:-demo12345}
MODERATOR_USERNAME=${MODERATOR_USERNAME:-moderator}
MODERATOR_PASSWORD=${MODERATOR_PASSWORD:-moderator12345}
ADMIN_USERNAME=${ADMIN_USERNAME:-admin}
ADMIN_PASSWORD=${ADMIN_PASSWORD:-admin12345}

login() {
  local username=$1
  local password=$2
  curl -sS -X POST "$BASE_URL/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${username}\",\"password\":\"${password}\"}" | jq -r '.accessToken'
}

AUTHOR_TOKEN=$(login "$AUTHOR_USERNAME" "$AUTHOR_PASSWORD")
MODERATOR_TOKEN=$(login "$MODERATOR_USERNAME" "$MODERATOR_PASSWORD")
ADMIN_TOKEN=$(login "$ADMIN_USERNAME" "$ADMIN_PASSWORD")

printf '\n[1] Current AUTHOR privileges\n'
curl -sS "$BASE_URL/api/auth/me" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .

printf '\n[2] AUTHOR creates video metadata and starts transactional validation/copyright process\n'
VIDEO_ID=$(curl -sS -X POST "$BASE_URL/api/videos" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"authorId":1,"title":"My first vlog","description":"Travel video","filePath":"/uploads/video1.mp4","format":"mp4","sizeBytes":104857600,"durationSeconds":600}' | tee /tmp/lab2-video.json | jq -r '.id')
cat /tmp/lab2-video.json | jq .

printf '\n[3] AUTHOR reads own process state\n'
curl -sS "$BASE_URL/api/processes/${VIDEO_ID}" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .

printf '\n[4] MODERATOR performs copyright decision\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/copyright-check" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"hasViolation":false}' | jq .

printf '\n[5] AUTHOR chooses monetization\n'
curl -sS -X POST "$BASE_URL/api/videos/${VIDEO_ID}/monetization" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{"monetizationType":"ALL_FORMATS"}' | jq .

printf '\n[6] MODERATOR is forbidden to run monthly payout process (expected 403)\n'
curl -sS -o /tmp/lab2-forbidden.json -w 'HTTP %{http_code}\n' -X POST "$BASE_URL/api/payouts/process-monthly" \
  -H "Authorization: Bearer ${MODERATOR_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}'
cat /tmp/lab2-forbidden.json | jq .

printf '\n[7] ADMIN runs monthly revenue+payout JTA transaction\n'
curl -sS -X POST "$BASE_URL/api/payouts/process-monthly" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H 'Content-Type: application/json' \
  -d '{}' | jq .

printf '\n[8] AUTHOR reads own revenues and payouts\n'
curl -sS "$BASE_URL/api/users/1/revenues" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .
curl -sS "$BASE_URL/api/users/1/payouts" \
  -H "Authorization: Bearer ${AUTHOR_TOKEN}" | jq .
