---
title: API Authorization Fix Documentation
author: Zhengji
date: 2026-10-28
---

# API authorization fix — issue #13

Feature APIs that previously allowed anonymous access now require a valid login
and the applicable role. The changes from the original pull request were rebuilt
on current upstream so the replacement branch merges without conflicts.

| Reference | Value |
| --- | --- |
| Original issue | [CSA-Admin-OCS/spring #13](https://github.com/CSA-Admin-OCS/spring/issues/13) |
| Original pull request | [Open-Coding-Society/spring #215](https://github.com/Open-Coding-Society/spring/pull/215) |
| Upstream repository | [Open-Coding-Society/spring](https://github.com/Open-Coding-Society/spring) |
| Base branch and commit | `master` at `4fe95ce2` |
| Replacement branch | `fix/api-authorization-clean` |
| Local commit | `01ef4f0a` — Require authentication for feature APIs while preserving upstream credential flows |

## Why the pull request was rebuilt

The original pull request was closed after the maintainer reported merge
conflicts. The replacement starts directly from upstream `master` and reapplies
the authorization changes while preserving newer upstream functionality.
It contains one new commit and does not carry over the original branch's merge
commits. At verification, the local branch had not been pushed and no replacement
pull request had been opened.

## Problem and resulting behavior

The API security chain already had a role-protected default, but earlier
`permitAll()` matchers let requests bypass that default. Spring Security uses the
first matching authorization rule, so those exceptions exposed feature APIs even
when the catch-all rule required a role.

The replacement removes these anonymous exceptions:

| Endpoint | Resulting access requirement |
| --- | --- |
| `GET /api/grades/read-gist/**` | A standard application role; the controller also requires authentication |
| `/api/make/**` | A standard application role |
| `GET /api/person/uid/**` | A standard application role |
| `POST /api/face/register/public` and its trailing-slash variant | Teacher or administrator, through the existing `/api/face/**` rule |
| `/api/pausemenu/**` | A standard application role; DELETE remains administrator-only |
| `/api/leaderboard/**` | A standard application role |
| `POST /api/assignment-submissions/upload` | A standard application role |

Standard application roles are `ROLE_USER`, `ROLE_STUDENT`, `ROLE_TEACHER`, and
`ROLE_ADMIN`, subject to any more specific endpoint rules. Self-service face
enrollment remains available to these roles through `POST /api/face/register`.

Upstream already required authentication for grade creation and
`POST /api/submissions/**`. The replacement preserves those protections and adds
regression coverage for them instead of restoring the older anonymous behavior.

The final authorization rule now uses `anyRequest()` within the API chain. This
covers every request selected by that chain, including bare `/api` and unsupported
methods on `/authenticate`. It does not change the separate MVC security chain.

Unauthenticated protected requests receive JSON **401** without a login redirect.
Authenticated callers without the required role receive JSON **403**. The API
chain remains stateless, with request caching disabled.

## Public routes and upstream behavior preserved

The following requests remain reachable without a JWT cookie:

- `POST /authenticate` for login.
- `POST /api/person/create` for signup.
- `POST /api/logout` so expired or invalid cookies can still be cleared.
- OPTIONS requests under `/api/**` and on `/authenticate` for CORS preflight.
- `POST /api/cs-pathway/setup-report/*`, where the report flow validates a
  short-lived pairing code. The more specific pairing-code creation and review
  routes retain their role requirements.
- `POST /api/toolchain/pair`, `GET /api/toolchain/sessions/*/next`, and
  `POST /api/toolchain/sessions/*/results`. The Toolchain service validates pairing
  codes or scoped agent tokens because the local agent has no browser JWT cookie.

Toolchain session creation, session polling, and check requests still require
login. Administrator-only person updates/deletes, pause-menu score deletion,
teacher-only report reviews, and the narrowly scoped `ROLE_ASSIGNMENT_SYNC`
auto-create permission are preserved.

## Files changed in Spring

| File | Change |
| --- | --- |
| `src/main/java/com/open/spring/security/SecurityConfig.java` | Removes feature API public exceptions, limits anonymous login to POST, uses a complete API fallback, and updates role policy metadata |
| `src/main/java/com/open/spring/mvc/grades/GradeController.java` | Changes Gist reading from `permitAll()` to `isAuthenticated()` |
| `src/test/java/com/open/spring/security/ApiAuthorizationTest.java` | Tests the actual security filter chain in a small web context without starting Spring Boot or connecting to a database |

The informational `apiEndpointRolePolicy` bean no longer advertises the removed
public exceptions or method-independent signup access. It also records the public
logout route. No database schema or seed changes are needed.

## Client impact

Clients that read Gists, upload assignment files, look up users, use the make
APIs, or access pause-menu and leaderboard data must log in and send the
`jwt_java_spring` cookie. Cross-origin browser requests must include credentials:

```javascript
fetch(`${springBaseUrl}/api/leaderboard/`, {
  credentials: "include"
});
```

Login alone does not grant teacher or administrator permissions. A signed-in
student calling the public-named face registration route still receives 403;
self-service enrollment uses `/api/face/register`.

## Validation on October 28, 2026

- Clean compilation passed with Java 21: `./mvnw -B clean compile`.
- The full suite passed: `./mvnw -B test` — **482 tests, zero failures, zero errors,
  zero skipped**.
- The suite includes **32 API authorization tests** covering anonymous denial,
  JSON 401 responses, absent redirects, statelessness, login/signup/logout and
  preflight access, valid and invalid JWT cookies, role boundaries, protected
  Toolchain session routes, and the preserved device credential routes.
- The tests mock token validation and user lookup while exercising the real
  filter chain. Device-route checks establish that requests can reach their
  controllers; they do not validate pairing codes or agent tokens themselves.
- `git diff --check` passed. After a fresh upstream fetch, the upstream base was
  an ancestor of the replacement commit and `git merge-tree --write-tree
  origin/master HEAD` completed without conflicts. The working tree was clean.
- Application startup was attempted with a separate SQLite path under `/tmp`.
  It failed because this checkout has no `.env`. Live HTTP and
  `POST /authenticate` cookie verification remain unverified. The Maven run goal
  reported success despite the application startup exception; that is not a
  successful smoke test.

The verification used a temporary ARM64 Java 21 JDK. In this environment, the
wrapper was invoked through Bash with that JDK configured, for example:

```bash
JAVA_HOME=/tmp/spring-java21-arm64 \
PATH=/tmp/spring-java21-arm64/bin:$PATH \
bash ./mvnw -B test
```

Mergeability was verified against `4fe95ce2`; later upstream changes may require
another check. The filter-chain fix does not establish that every authenticated
caller owns every requested record. Controller ownership checks, cross-site cookie
CSRF behavior, MVC code-runner routes, and WebSocket message authorization remain
outside this change's validation scope.
