# Flyway V5 Reconciliation Guide

## Problem

`V5__login_rate_limit.sql` was deleted and replaced with `V5__add_password_changed_at.sql`.  
The original V5 created `failed_login_attempts` and `locked_until` on the `users` table.  
Those columns are still referenced by `User.java` but no longer created by any migration in the repo.

This causes a hard boot failure in two scenarios:

1. **Fresh database** — Flyway runs V1→V6, the columns are never created, and Hibernate `validate` mode fails at startup.
2. **Existing database** — Flyway sees a checksum/script mismatch for version 5 against `flyway_schema_history` and refuses to start.

## Fix Applied

A new migration was added:

- `V7__add_login_lockout_columns.sql` — adds `failed_login_attempts` and `locked_until` back to `users`. This migration is **idempotent**: it uses `IF NOT EXISTS`, so it is safe on both fresh databases and existing databases where the old V5 already created these columns.

This resolves the **fresh database** case automatically.

**Important:** After any code change, run `./mvnw clean spring-boot:run` to ensure updated SQL migrations are compiled into `target/classes`. A plain `./mvnw spring-boot:run` may use stale cached resources.

## Existing Database Reconciliation

For environments where the old V5 already ran and is recorded in `flyway_schema_history`, you must reconcile the version 5 entry before the application can boot.

### Option A — Flyway repair (preferred)

If you have access to the database and the old V5 SQL content is still available in your deployment artifact history:

1. Restore the original `V5__login_rate_limit.sql` content temporarily.
2. Run the Flyway Maven plugin directly:
   ```bash
   ./mvnw flyway:repair
   ```
   Or use the Flyway CLI:
   ```bash
   flyway repair
   ```
3. Remove the temporary V5 file again.

`repair` updates the checksum in `flyway_schema_history` to match the currently deployed V5 script without re-running it.

### Option B — Manual history update

If the original V5 content is not available:

```sql
UPDATE flyway_schema_history
SET checksum = <checksum_of_current_V5__add_password_changed_at.sql>
WHERE version = '5';
```

To get the checksum, run:

```bash
./mvnw flyway:info
```

or compute it from the current `V5__add_password_changed_at.sql` content.

### Option C — Baseline past V5

If the database is already at a later version and you want to skip reconciliation, set the baseline version in `application.yml` temporarily:

```yaml
spring:
  flyway:
    baseline-version: 6
```

Or pass it as an environment variable:

```bash
SPRING_FLYWAY_BASELINE-VERSION=6 ./mvnw spring-boot:run
```

This tells Flyway to treat version 6 as the baseline and ignore earlier history.  
Use this only if you are certain the schema already contains all columns through V6.  
Remember to remove the temporary configuration after the app starts successfully.

## Verification

After reconciliation, restart the application and confirm:

- Flyway reports success for versions 1–7.
- Hibernate `validate` passes.
- `users` table contains:
  - `failed_login_attempts`
  - `locked_until`
  - `password_changed_at`

**Note:** If you see `Access denied for user 'root'@'localhost' (using password: NO)`, this is a database configuration issue, not a migration issue. It means MySQL is not configured with the expected credentials. The application defaults to `root` with no password. Either:
- Configure MySQL with a root user that has no password, or
- Set environment variables: `MYSQLUSER=your_user MYSQLPASSWORD=your_password`, or
- Use the test profile: `./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=test"` (uses H2 with Flyway disabled)

## Prevention

Never delete or renumber an already-shipped migration. Always add a new migration with the next available version number.

The test profile (`application-test.yml`) disables Flyway and uses `ddl-auto: create-drop`, so integration tests will not catch this class of bug. Consider adding a Testcontainers-based integration test that runs the real MySQL + Flyway migration chain.