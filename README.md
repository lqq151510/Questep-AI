# AI Interview Platform

## Backend quick start

```bash
cd backend
mvn clean package
mvn -pl interview-api spring-boot:run
```

## Local infrastructure

```bash
docker compose -f "../docker-compose.infrastructure.yml" up -d
docker exec -i local-mysql mysql -uroot -proot123456 < backend/sql/init.sql
```

## API examples

```bash
curl http://127.0.0.1:8080/api/health

# demo_user / demo123456
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"demo123456"}' | jq -r '.data.token')

curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/materials

curl -X POST http://127.0.0.1:8080/api/materials/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@README.md"

curl -X POST http://127.0.0.1:8080/api/quizzes/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"materialIds":[1],"questionType":"short","difficulty":3,"count":3,"interviewMode":true}'
```

Frontend local token:

```js
localStorage.setItem("interview_token", "<login token>");
```
