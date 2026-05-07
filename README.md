# AI Interview Platform

## Backend quick start

```bash
cd backend
mvn clean package
JWT_SECRET=<your-secret-at-least-32-chars> mvn -pl interview-api spring-boot:run
```

> **⚠️ JWT_SECRET 是必填环境变量**，长度至少 32 字节，否则应用无法启动。生产环境请使用强随机密钥，切勿使用示例值。
> 本地默认使用 `LLM_PROVIDER=noop`，可以无模型密钥启动；接入真实 OpenAI 兼容模型时再显式设置 `LLM_PROVIDER=openai`、`LLM_API_KEY`、`LLM_BASE_URL`、`LLM_MODEL`。

## Local infrastructure

```bash
docker compose -f "../docker-compose.infrastructure.yml" up -d
# schema is auto-migrated by Flyway when interview-api starts
```

## API examples

```bash
curl http://127.0.0.1:8080/actuator/health

# demo_user / demo123456
TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"demo123456"}' | jq -r '.data.token')

REFRESH_TOKEN=$(curl -s -X POST http://127.0.0.1:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo_user","password":"demo123456"}' | jq -r '.data.refreshToken')

curl -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/v1/materials

# v1 upload parsing supports text formats: TXT / MD / CSV / JSON
curl -X POST http://127.0.0.1:8080/api/v1/materials/upload \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@README.md"

curl -X POST http://127.0.0.1:8080/api/v1/quizzes/generate \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"materialIds":[1],"questionType":"short","difficulty":3,"count":3,"interviewMode":true}'
```

Frontend local token:

```js
localStorage.setItem("interview_token", "<login token>");
localStorage.setItem("interview_refresh_token", "<refresh token>");
```
