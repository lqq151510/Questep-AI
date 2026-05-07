# SQL Initialization

## Files

- `../interview-api/src/main/resources/db/migration/V1__init.sql`: baseline schema + seed data.
- `../interview-api/src/main/resources/db/migration/V*.sql`: incremental schema migrations.

## Execution Order

1. Start infrastructure services: MySQL, Redis, Milvus.
2. Start `interview-api`; Flyway migrates schema automatically at boot.

## Example

```bash
cd backend
JWT_SECRET=<your-secret-at-least-32-chars> mvn -pl interview-api spring-boot:run
```

## Notes

- Flyway is the only supported schema initialization path.
- Add future changes through new `V{n}__*.sql` scripts only.
