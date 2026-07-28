# Flyway Clean Command Security Fix

## Summary
Fixed a critical security vulnerability where Flyway's `clean` command was enabled in the production configuration, potentially allowing accidental or malicious database wipes.

## Changes Made

### 1. `src/main/resources/application.yml`
**Changed:** `flyway.clean-disabled: false` → `flyway.clean-disabled: true`

This is the default configuration used in production and all non-local environments. The `clean` command is now disabled by default, preventing accidental database wipes.

### 2. `src/main/resources/application-local.yml`
**Kept:** `flyway.clean-disabled: false`

The local development profile retains the ability to use Flyway's clean command for development and testing purposes. This profile is only activated when explicitly running with `-Dspring.profiles.active=local`.

### 3. `src/main/resources/application-test.yml` (NEW)
**Created:** New test profile configuration

The test profile uses H2 in-memory database with `ddl-auto: create-drop` and has Flyway completely disabled (`enabled: false`). This is appropriate because:
- Tests need a clean database for each run
- H2's `create-drop` handles schema management automatically
- No migration files need to be applied during tests
- Tests are isolated and don't affect production data

## Security Impact

### Before
- `flyway.clean-disabled: false` in production configuration
- Risk: Accidental database wipe if:
  - Wrong profile activated in CI/CD
  - CLI run against production credentials
  - Misconfigured deployment

### After
- `flyway.clean-disabled: true` in production configuration
- Protection: Clean command blocked by default
- Only local development profile can use clean command
- Test profile doesn't need Flyway at all

## Profile Behavior

| Profile | clean-disabled | Use Case |
|---------|---------------|----------|
| **default** (production) | `true` | Production deployments - clean command blocked |
| **local** | `false` | Local development - clean command allowed |
| **test** | N/A (Flyway disabled) | Automated tests - uses H2 with create-drop |

## Verification

The configuration has been validated:
- ✅ Maven validate passes
- ✅ YAML syntax is correct
- ✅ Profile-specific configurations are properly structured
- ✅ No code references to `flyway.clean` or `clean()` method found
- ✅ No CI/CD scripts that invoke Flyway clean
- ✅ Dockerfile does not execute Flyway commands

## Recommendations

1. **Never activate the local profile in production** - This would re-enable the clean command
2. **Use environment variables** to explicitly set `SPRING_PROFILES_ACTIVE` in production deployments
3. **Monitor database access logs** for any Flyway clean attempts
4. **Consider database-level protections** such as read replicas for production

## Related Files
- `src/main/resources/application.yml` - Main configuration
- `src/main/resources/application-local.yml` - Local development profile
- `src/main/resources/application-test.yml` - Test profile (new)
- `src/test/java/com/domuspacis/AbstractIntegrationTest.java` - Uses test profile