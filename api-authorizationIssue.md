---
title: API Issue Documentation
author: Zhengji

---

# API authorization audit — issue #13


Source: https://github.com/CSA-Admin-OCS/spring/issues/13
Base: origin/updated, e5a7650b.

The updated branch already has a role-protected API default. The remaining
anonymous exceptions nevertheless expose important operations:

- POST /api/grades and /api/grades/: anonymous grade writes.
- POST /api/submissions/**: anonymous submissions and any other matching POST,
  including grader assignment; trusting a payload student ID is not authentication.
- POST /api/assignment-submissions/upload: anonymous file uploads.
- POST /api/face/register/public: anonymous biometric enrollment; now follows the
  existing teacher/admin face policy. Self-service uses /api/face/register.
- GET /api/person/uid/**: anonymous person lookups.
- /api/make/**, /api/pausemenu/**, /api/leaderboard/**: every HTTP method was public,
  allowing future writes under those paths to inherit anonymous access.
- GET /api/grades/read-gist/**: anonymous GitHub proxy reads.
- The informational apiEndpointRolePolicy bean advertised broader signup access
  than the actual filter chain and listed public exceptions without method limits.

## Resulting policy

Only POST /authenticate, POST /api/person/create, and API/authentication OPTIONS
requests remain anonymous. All feature APIs require authentication and the
existing role restrictions. The final anyRequest rule protects every request
selected by the API chain, including /api and unsupported authentication methods.
Existing admin, teacher, and assignment-sync role boundaries remain in place.
The role policy metadata and Gist method annotation now reflect this behavior.
Anonymous API requests return JSON 401; authenticated callers without the
required authority receive JSON 403. CORS and stateless cookie JWT authentication
remain enabled.

Clients that previously submitted grades, uploaded files, enrolled faces, read
Gists, or displayed game data anonymously must now log in and send the JWT cookie
(with credentials included for cross-origin fetches). An anonymous /api/jokes/
smoke request is expected to return 401, despite the older AGENTS.md smoke note.

## Scope and validation

ApiAuthorizationTest exercises the actual Spring Security chain in a small web
context without Spring Boot or database startup. It checks formerly public paths,
public login/signup/preflight, valid and invalid JWT cookies, JSON error statuses,
statelessness, admin/teacher restrictions, and the assignment-sync exception.

This change closes anonymous filter-chain access. It does not constitute a full
controller ownership audit: authenticated object access, payload student IDs,
CSRF with cross-site cookies, MVC /run/**, and websocket message authorization
require separate review. No database schema or seed data changes are needed.

Validation on this branch: `./mvnw clean compile` passed; `./mvnw test`
passed (182 tests, including 24 API authorization checks). Java 21 was provisioned
under /tmp and Maven used a temporary dependency cache. The application startup
smoke test was attempted but failed because this checkout has no .env; the
localhost request could not connect. Live POST /authenticate cookie verification
therefore remains unverified. No local database was initialized or reset.

