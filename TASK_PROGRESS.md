# Security Audit Fix Progress

## 🔴 1. Hardcoded admin credentials (DataInitializer.java)
- [x] Remove hardcoded password, read from ADMIN_SEED_PASSWORD env var
- [x] Only seed if env var is set, log warning otherwise

## 🔴 2. Booking flow bug — serviceAssetId type fallback (BookingService, BookingDtos, frontend)
- [x] Backend: Change serviceAssetId to UUID in CreateBookingRequest
- [x] Backend: Remove else branch and isValidUuid helper in BookingService
- [x] Backend: Remove findFirstByAssetTypeOrderByName from ServiceAssetRepository
- [x] Frontend: Auto-select first available asset UUID when type is chosen
- [x] Frontend: Update validation schema to require UUID
- [x] Frontend: Update booking page test
- [x] Backend: Update integration test

## 🟠 3. ddl-auto: update in production config
- [x] Change ddl-auto to validate in application.yml
- [x] Add ddl-auto: update in application-local.yml

## 🟠 4. Booking getById ownership check
- [x] Add ownership check in BookingService.getById (staff or owner)

## 🟡 5. Credential logging landmine
- [x] Override toString() on LoginRequest
- [x] Override toString() on RegisterRequest
- [x] Override toString() on CreateUserRequest

## Bonus: Flyway migration setup
- [x] Add Flyway dependencies to pom.xml
- [x] Create V1__initial_schema.sql with full schema
- [x] Create V2__seed_service_assets.sql with comprehensive seed data
- [x] Configure Flyway in application.yml and application-local.yml
- [x] Remove legacy data.sql (replaced by Flyway V2)
- [x] Remove legacy seed_assets.sql
- [x] Verify build passes
