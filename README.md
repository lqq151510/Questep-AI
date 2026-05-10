# AI Interview Platform

## Backend quick start

```bash
cd backend
mvn clean package
JWT_SECRET=<your-secret-at-least-32-chars> mvn -pl interview-api spring-boot:run
```

> **⚠️ JWT_SECRET 是必填环境变量**，长度至少 32 字节，否则应用无法启动。生产环境请使用强随机密钥，切勿使用示例值。
> 默认 LLM 提供商为 `anthropic`。若本机存在 `~/.claude/settings.json`，系统会自动读取其中 `ANTHROPIC_AUTH_TOKEN / ANTHROPIC_BASE_URL / ANTHROPIC_MODEL` 作为默认模型配置。也可手动覆盖：`LLM_PROVIDER`、`LLM_API_KEY`、`LLM_BASE_URL`、`LLM_MODEL`。

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

# 查询当前用户 LLM 设置（支持多厂商/模型/自定义 URL）
curl -H "Authorization: Bearer $TOKEN" \
  http://127.0.0.1:8080/api/v1/llm/settings

# 更新当前用户 LLM 设置
curl -X PUT http://127.0.0.1:8080/api/v1/llm/settings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"providerName":"openai","modelName":"gpt-4o-mini","baseUrl":"https://api.openai.com/v1","apiKey":"<your-key>","enabled":true}'

# OpenAI 兼容格式（任意兼容 /chat/completions 的厂商）
curl -X PUT http://127.0.0.1:8080/api/v1/llm/settings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"providerName":"openai-compatible","modelName":"deepseek-v4-flash","baseUrl":"https://api.deepseek.com/v1","apiKey":"<your-key>","enabled":true}'
# providerName 也支持: openai_format / openai-format / compatible
```

Frontend local token:

```js
localStorage.setItem("interview_token", "<login token>");
localStorage.setItem("interview_refresh_token", "<refresh token>");
```
