#!/bin/bash

BASE_URL="http://localhost:8080/api"

echo "1. Create User (Author)"
curl -s -X POST $BASE_URL/users \
  -H 'Content-Type: application/json' \
  -d '{"username": "author_curl", "email": "author_curl@example.com", "role": "AUTHOR"}'
echo -e "\n"

echo "2. Create User (Moderator)"
curl -s -X POST $BASE_URL/users \
  -H 'Content-Type: application/json' \
  -d '{"username": "moderator_curl", "email": "mod_curl@example.com", "role": "MODERATOR"}'
echo -e "\n"

echo "3. Create Channel"
curl -s -X POST $BASE_URL/channels \
  -H 'Content-Type: application/json' \
  -d '{"userId": 1, "name": "Bash Script Channel", "description": "Just testing"}'
echo -e "\n"

echo "4. Upload Video"
curl -s -X POST $BASE_URL/channels/1/videos \
  -H 'Content-Type: application/json' \
  -d '{"title": "Terminal Tricks", "description": "How to use cURL"}'
echo -e "\n"

echo "5. Process Video"
curl -s -X POST $BASE_URL/system/videos/1/process
echo -e "\n"

echo "6. Apply for Monetization"
curl -s -X POST $BASE_URL/videos/1/monetization-requests
echo -e "\n"

echo "7. Approve Monetization"
curl -s -X POST $BASE_URL/moderation/monetization-requests/1/approve \
  -H 'Content-Type: application/json' \
  -d '{"moderatorId": 2}'
echo -e "\n"

echo "8. Publish Video"
curl -s -X POST $BASE_URL/videos/1/publish
echo -e "\n"

echo "9. Add 5 Views"
for i in {1..5}; do
  curl -s -X POST $BASE_URL/videos/1/views
done
echo -e "5 views added\n"

echo "10. Check Balance of User 1"
curl -s -X GET $BASE_URL/users/1
echo -e "\n"
