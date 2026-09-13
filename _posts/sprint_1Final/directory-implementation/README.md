# Directory implementation source bundle

These files implement the examples in the Sprint 1 Phase 2–4 posts. They are staged in this portfolio repository for copying into the existing Spring backend, not a standalone application.

## Installation mapping

| Source | Spring backend destination |
|---|---|
| `java/directory/*.java` | `src/main/java/<application package path>/directory/` |
| `templates/directory/*.html` | `src/main/resources/templates/directory/` |
| `config/application-directory-example.properties` | Review and merge into a disposable development profile only |
| `validation/inspect-directory.sql` | Run manually against the selected development database after startup |

Replace `YOUR_APPLICATION_PACKAGE` in all four Java files with the backend application's actual parent package. Do not copy the placeholder as a real package: Spring must discover these classes beneath its component/entity scanning configuration.

Confirm the existing backend supports Jakarta Persistence/Validation, Lombok, Spring MVC, Spring Data JPA, Thymeleaf, Spring Security, SQLite JDBC, and a compatible Hibernate SQLite dialect. Follow that repository's build and startup instructions. This bundle intentionally does not replace its dependency or security configuration.

Before enabling routes, integrate `/mvc/directory` and all child routes with existing administrator-only session access and CSRF validation. The templates expect `_csrf` to be supplied by Spring Security. An account's STUDENT/GUEST classification grants no authority.

The properties example uses `create-drop`, which recreates/drops tables and loses data. It is not auto-loaded and must only be used intentionally with a disposable database. `/volumes/sqlite.db` is a Linux/container path requiring a writable directory; adapt it to the actual runtime.

## Validation

After integration, open `http://localhost:8585/mvc/directory` if the backend uses the assignment's port. Follow the manual test matrix in `../2026-09-09-Sprint-1-BACKEND.md`, including invalid input, forged fields, missing IDs, authorization, and CSRF rejection. Run `validation/inspect-directory.sql` to inspect the resulting table. Capture real screenshots using synthetic account data.

Compilation and runtime behavior have not been verified in Spring. Full inventory, authorship, design decisions, and limitations are recorded in `../2026-09-09-Sprint-1-IMPLEMENTATION-RECORD.md`.
