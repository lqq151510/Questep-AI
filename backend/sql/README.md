# SQL Initialization

## Files

- `init.sql`: MySQL 8 initialization script for core business tables and constraints.

## Execution Order

1. Start infrastructure services: MySQL, Redis, Milvus.
2. Connect to MySQL 8 with a privileged account.
3. Execute `init.sql`.

## Example

```bash
mysql -h 127.0.0.1 -P 3306 -u root -p < backend/sql/init.sql
```

## Notes

- The script creates and uses database `interview_ai`.
- DDL uses `CREATE TABLE IF NOT EXISTS` for safer repeat execution.
- Foreign keys are defined by dependency order and restored at the end.
