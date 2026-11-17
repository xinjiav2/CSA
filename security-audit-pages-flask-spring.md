# Security Audit: `spring`, `flask`, and `pages`

**Date:** 2026-11-17  
**Scope:** Source code review of `/home/jade/dev/spring`, `/home/jade/dev/flask`, and `/home/jade/dev/pages`.

---

## Executive Summary

| Codebase | Critical | High | Medium | Key Themes |
| :--- | :---: | :---: | :---: | :--- |
| **`spring`** | 1 | 4 | 2 | Unauthenticated RCE (`/run/java`), hardcoded JWT/SMTP/API secrets, broken access control / IDOR, CSRF disabled with `SameSite=None` cookies, SSRF redirect/IPv6 gaps |
| **`flask`** | 1 | 4 | 3 | Unauthenticated RCE (`/run/python`, `/run/javascript`), hardcoded `SECRET_KEY` & weak default passwords, dev-mode auth bypasses, leaked API keys/JWTs, `debug=True`, wildcard CORS |
| **`pages`** | 0 | 2 | 3 | Hardcoded client-side API keys & unauthenticated AWS token endpoint, unauthenticated Socket.IO with wildcard CORS, DOM XSS via `innerHTML`, unsandboxed `eval`/`new Function` |

---

## 1. Spring Backend (`/home/jade/dev/spring`)

### 1.1 [Critical] Unauthenticated Remote Code Execution (RCE)
- **Locations:**
  - `src/main/java/com/open/spring/security/MvcSecurityConfig.java` (`mvcSecurityFilterChain`, line 146)
  - `src/main/java/com/open/spring/mvc/javarunner/JavaRunnerApiController.java` (`runJava`, lines 35–44)
  - `src/main/java/com/open/spring/mvc/javarunner/LocalJavaRunner.java` (`run`, lines 19–70)
- **Details & Impact:**  
  `MvcSecurityConfig` configures `.requestMatchers("/run/**").permitAll()`, making `POST /run/java` publicly accessible without authentication. When `IS_PRODUCTION` is not `"true"` (the default in `JavaRunnerApiController` line 30 is `"false"`), `runJava()` calls `LocalJavaRunner.run()`, which writes user-supplied Java code to a temporary directory and executes `javac` and `java` directly on the host with no sandboxing, `SecurityManager`, or network/filesystem isolation (only a 3-second timeout).
- **Remediation:**
  1. Require authentication (and rate limiting) on `/run/**` instead of `permitAll()`.
  2. Never execute untrusted user code on the main application host; always route execution to a hardened, network-disabled, read-only container with strict cgroup CPU/memory/PID limits.

### 1.2 [High] Hardcoded Secrets & Weak Default Credentials
- **Locations:**
  - `src/main/resources/application.properties`:
    - `jwt.secret` (line 32): Hardcoded Base64-encoded HMAC signing key (`bmlnaHRoYXdrY29kaW5nc29jaWV0eWFwY29tcHV0ZXJzY2llbmNlYQ==`).
    - `spring.mail.username` / `spring.mail.password` (lines 107–108): Hardcoded Gmail SMTP credentials.
    - `google.api.key` (line 126): Hardcoded Google API key (`AIzaSy...`).
    - `spring.datasource.username` / `spring.datasource.password` (lines 57–58): Weak fallback credentials (`admin` / `admin`).
    - `person.default-password` (line 64): Weak fallback (`DefaultPassword123!`) used for seeded accounts and password resets.
- **Details & Impact:**  
  Anyone with repository access can forge valid JWTs for any user/role, send emails through the Gmail account, abuse the Google API key quota, or log into accounts using the default password.
- **Remediation:**
  1. Rotate the JWT signing key, Gmail app password, Google API key, and DB credentials immediately.
  2. Remove all literal secrets from `application.properties` and require them via environment variables (fail startup if missing in production).

### 1.3 [High] Broken Access Control & IDOR
- **Locations:**
  - `src/main/java/com/open/spring/security/SecurityConfig.java` (line 123) & `src/main/java/com/open/spring/mvc/person/FaceApiController.java` (`registerPublicFace`, lines 54–79):  
    `POST /api/face/register/public` is `permitAll()` and accepts an arbitrary `sid` or `uid` alongside `faceData`, allowing any unauthenticated caller to overwrite any student's biometric face data.
  - `src/main/java/com/open/spring/mvc/person/PersonApiController.java` (`bulkExtractPersons`, lines 616–638):  
    `GET /api/people/bulk/extract` lacks `@PreAuthorize("hasAuthority('ROLE_ADMIN')")` and falls under the general `/api/**` authenticated rules. Any authenticated user can dump all users' emails, UIDs, SIDs, and password hashes (`personDto.setPassword(person.getPassword())`).
  - `src/main/java/com/open/spring/mvc/S3uploads/S3FileApiController.java` (`uploadFile`, `downloadFile`, `downloadFileByQuery`, `deleteUserFiles`, lines 37–139):  
    `/api/files/upload/{uid}`, `/api/files/download/{uid}/{filename}`, `/api/files/download?uid=...`, and `/api/files/delete/{uid}` accept `{uid}` as a user-supplied parameter without verifying that `{uid}` matches the authenticated principal (or that the caller is an admin). Any authenticated user can read, overwrite, or delete another user's S3 files.
  - `src/main/java/com/open/spring/security/SecurityConfig.java` (line 117) & `src/main/java/com/open/spring/mvc/Make_debugger/MakeDebugController.java` (`analyzeLog`, `analyzeFile`, lines 19–33):  
    `/api/make/**` is `permitAll()`, allowing unauthenticated users to invoke AI log analysis using the server's API key.
- **Remediation:**
  1. Remove `/api/face/register/public` or require authenticated ownership/admin verification.
  2. Restrict `GET /api/people/bulk/extract` to `ROLE_ADMIN` and remove `password` from `PersonDto`.
  3. Enforce `uid.equals(authentication.getName())` (or `ROLE_ADMIN`) in `S3FileApiController`.
  4. Require authentication on `/api/make/**`.

### 1.4 [High] CSRF Disabled with `SameSite=None` Cookies
- **Locations:**
  - `src/main/java/com/open/spring/security/MvcSecurityConfig.java` (line 90): `.csrf(csrf -> csrf.disable())`
  - `src/main/java/com/open/spring/security/SecurityConfig.java` (line 84): `.csrf(csrf -> csrf.disable())`
  - `src/main/resources/application.properties` (lines 35, 51): `jwt.cookie.same-site=None` and `server.servlet.session.cookie.same-site=none`
- **Details & Impact:**  
  Because both session and JWT cookies are sent automatically on cross-site requests (`SameSite=None`), disabling CSRF allows malicious third-party sites to trigger authenticated state-changing requests on behalf of logged-in users.
- **Remediation:**
  - Enable CSRF protection for cookie-authenticated endpoints, or switch API authentication to `Authorization: Bearer <token>` headers rather than cookies.

### 1.5 [Medium] Password Reset Sets Static Default Password
- **Location:** `src/main/java/com/open/spring/mvc/person/PersonViewController.java` (`resetPasswordCheck`, lines 472–475)
- **Details & Impact:**  
  Completing a password reset sets `personToReset.setPassword(defaultPassword)` (which defaults to `DefaultPassword123!`) instead of letting the user supply a new password, leaving the account briefly or permanently protected by a publicly known password.
- **Remediation:**
  - Require the user to submit a new password meeting complexity rules during reset completion, or generate a high-entropy random temporary password and force an immediate change on next login.

### 1.6 [Medium] SSRF Bypass Potential in Scraper URL Validator
- **Location:** `src/main/java/com/open/spring/scraper/security/UrlSafetyValidator.java` (`validateTargetUrl`, lines 29–63)
- **Details & Impact:**  
  `validateTargetUrl` validates the initial URL and resolves its DNS before `Jsoup` connects, but does not prevent HTTP 3xx redirects to internal/metadata IPs or DNS rebinding between validation and connection.
- **Remediation:**
  - Disable automatic redirects (`followRedirects(false)`) and re-validate each redirect target, or pin the resolved IP at the HTTP client socket layer.

---

## 2. Flask Backend (`/home/jade/dev/flask`)

### 2.1 [Critical] Unauthenticated Remote Code Execution (RCE)
- **Locations:**
  - `api/python_exec_api.py` (`PythonExec.post`, `_execute_local`, lines 14–54)
  - `api/javascript_exec_api.py` (`JavaScriptExec.post`, `_execute_local`, lines 13–53)
- **Details & Impact:**  
  Neither `/run/python` nor `/run/javascript` has `@token_required` or any authentication check. When `IS_PRODUCTION` is not `'true'` (default `'false'`), `_execute_local` runs user-submitted code directly on the host via `subprocess.run(["python3", ...])` or `subprocess.run(["node", ...])` with `cwd="/tmp"`. Attackers can read host files, inspect environment variables/secrets, or make outbound network requests.
- **Remediation:**
  - Require authentication and rate limiting on `/run/python` and `/run/javascript`, and always delegate execution to an isolated sandboxed container.

### 2.2 [High] Hardcoded `SECRET_KEY`, Weak Default Passwords & Committed JWT Cookie
- **Locations:**
  - `__init__.py` (line 91): `SECRET_KEY = os.environ.get('SECRET_KEY') or 'SECRET_KEY'`
  - `__init__.py` (lines 59, 64, 69, 72, 85): `ADMIN_PASSWORD`, `TEACHER_PASSWORD`, `USER_PASSWORD`, `DEFAULT_PASSWORD`, and `MY_PASSWORD` all fall back to `'123Qwerty!'`.
  - `cookies.txt` (line 5): Committed Netscape cookie file containing a signed `jwt_python_flask` token for `_uid: "admin"`.
- **Details & Impact:**  
  If `SECRET_KEY` is unset, anyone can forge Flask session cookies and HS256 JWTs for any user (including `admin`). The committed `cookies.txt` also exposes a valid admin JWT signed with the default key.
- **Remediation:**
  1. Remove `'SECRET_KEY'` and `'123Qwerty!'` fallbacks; require strong secrets via environment variables.
  2. Delete `cookies.txt` from the repository and git history if needed, and rotate `SECRET_KEY`.

### 2.3 [High] Development-Mode Auth Bypasses
- **Location:** `api/gemini_api.py` (`require_auth_if_production`, `require_admin_if_production`, lines 78–99)
- **Details & Impact:**  
  Both decorators return the unwrapped view function `f` whenever `IS_PRODUCTION` is not `'true'`, leaving Gemini AI endpoints and admin/teacher summarization endpoints completely unauthenticated in non-production or misconfigured environments.
- **Remediation:**
  - Always enforce `@token_required()` / `@token_required(roles=["Admin", "Teacher"])` regardless of `IS_PRODUCTION`.

### 2.4 [High] API Key & JWT Token Exposure
- **Locations:**
  - `api/gemini_api.py` (`GeminiAPI._Debug.post`, lines 796–797): Sets `debug_info['endpoint'] = f"{server}?key={api_key}"` and returns `debug_info` in the HTTP JSON response, leaking `GEMINI_API_KEY` to callers.
  - `api/user.py` (`UserAPI._Security.post`, lines 554–563): In non-production (`IS_PRODUCTION != 'true'`), sets the JWT cookie with `httponly=False` and `secure=False` (exposing it to `document.cookie` / XSS) and logs the raw token with `print(f"Token set: {token}")`.
- **Remediation:**
  - Never include `api_key` in debug responses or logs.
  - Set `httponly=True` on JWT cookies and remove `print(f"Token set: {token}")`.

### 2.5 [High] Missing Auth / IDOR on User & Study Endpoints
- **Locations:**
  - `main.py` (`update_user`, lines 264–268): `/update_user/<string:uid>` (`PUT`) is missing `@login_required`, causing unauthenticated requests to call `current_user.is_admin()` on `AnonymousUserMixin` (raising an unhandled `AttributeError` / 500).
  - `api/study.py` (`add_study_record`, `get_study_records`, `get_study_stats`, lines 12–128): `/api/study` (`POST`/`GET`) and `/api/study/stats` (`GET`) have no auth decorator; passing `?all=true` exposes all users' study records and stats unauthenticated.
  - `api/leaderboard.py` (`ElementaryLeaderboardItemAPI.delete`, lines 91–101): `DELETE /api/events/ELEMENTARY_LEADERBOARD/<int:event_id>` checks `@token_required()` but does not verify record ownership or Admin role (IDOR).
- **Remediation:**
  - Add `@login_required` to `update_user`, require auth on `/api/study*` (and restrict `?all=true` to admins/teachers), and enforce ownership/admin checks on leaderboard deletion.

### 2.6 [Medium] Hardcoded `debug=True`, Overly Permissive CORS & Unauthenticated Host Info
- **Locations:**
  - `main.py` (line 311): `app.run(debug=True, host="0.0.0.0", ...)` exposes the Werkzeug interactive debugger if run directly.
  - `app.py` (line 7): `CORS(app, supports_credentials=True, origins='*')`.
  - `host.py` (`HostAPI.get`, lines 32–56): Allows `"null"` origin with `supports_credentials=True` and exposes an unauthenticated `/api/host` endpoint that runs shell commands (`subprocess.run(..., shell=True)`).
- **Remediation:**
  - Drive `debug` from an env var defaulting to `False`, replace `origins='*'` and `"null"` with explicit trusted origins, and restrict `/api/host`.

---

## 3. Pages Frontend & Utilities (`/home/jade/dev/pages`)

### 3.1 [High] Hardcoded Client-Side API Keys & Unauthenticated AWS Secret Endpoint
- **Locations:**
  - `_layouts/chatbot.html` (line 321): Hardcoded `API_KEY = 'AIzaSy...'` in client-side `<script>` (and similar hardcoded keys across other layouts/posts).
  - `scripts/github_api_funcs.py` (`get_token_aws`, lines 14–28): Hardcodes `https://7vybv54v24.execute-api.us-east-2.amazonaws.com/GithubSecret` and fetches `response.json().get('token')` via an unauthenticated `POST` request.
- **Details & Impact:**  
  Anyone viewing page source can extract the Google/Gemini API keys for quota/billing abuse. Anyone with the AWS API Gateway URL can request the GitHub token if that endpoint is still active.
- **Remediation:**
  - Revoke/rotate exposed API keys, route AI requests through an authenticated backend proxy, and secure or decommission the AWS secret endpoint.

### 3.2 [High] Unauthenticated Socket.IO Server with Wildcard CORS
- **Location:** `node_backend/index.js` (lines 4–12)
- **Details & Impact:**  
  `new Server({ cors: { origin: "*" } })` accepts connections from any origin without authentication, and its `"update"` handler immediately rebroadcasts any client-supplied `data` to all connected clients via `io.emit("stateUpdate", data)`.
- **Remediation:**
  - Restrict `cors.origin` to trusted origins, authenticate socket connections (e.g., via JWT), and validate/sanitize incoming `"update"` payloads.

### 3.3 [Medium] DOM XSS via `innerHTML`
- **Locations:**
  - `assets/js/api/login.js` (lines 23–30): Interpolates `data.name` and `role.name` directly into `loginArea.innerHTML` without HTML escaping.
  - `_includes/grading-feedback.html` (lines 472–504): Interpolates `reply.author`, `reply.content`, `thread.studentName`, `thread.gradeReceived`, `thread.additionalComments`, `thread.id`, and `window.user.name` directly into `threadDiv.innerHTML` (including inside inline `onclick="..."` handlers).
- **Details & Impact:**  
  If a user profile name, role name, or feedback/reply field contains HTML/JS, it executes in the browser of anyone viewing the page (including teachers/admins).
- **Remediation:**
  - Build DOM nodes with `document.createElement` + `textContent` and `addEventListener`, or pass all untrusted strings through a strict HTML-escaping helper before rendering.

### 3.4 [Medium] Unsafe Dynamic Code Execution (`eval` / `new Function`)
- **Locations:**
  - `_includes/tailwind/lesson.html` (`runExample`, lines 92–102): Executes `eval(code)` on textarea input and writes the result to `output.innerHTML`.
  - `assets/js/bigsix/frontend/javascript-playground.js` (lines 207–208): Executes user code via `new Function(code)()`.
- **Details & Impact:**  
  Despite the comment claiming `new Function()` creates a sandboxed scope, it runs in the main page origin with full access to `document`, `localStorage`, and authenticated cookies/requests.
- **Remediation:**
  - Execute user-supplied JS inside a sandboxed `<iframe sandbox="allow-scripts">` (without `allow-same-origin`) and communicate via `postMessage`.

---

## Prioritized Action Checklist

1. **Immediate (Critical):** Gate or disable `/run/java` (`spring`) and `/run/python`, `/run/javascript` (`flask`) so unauthenticated users cannot execute arbitrary code on the host.
2. **Immediate (Secrets):** Rotate all committed secrets (`jwt.secret`, `SECRET_KEY`, Gmail SMTP password, Google/Gemini API keys, AWS GitHub secret endpoint) and remove `cookies.txt` and hardcoded fallback passwords.
3. **High (Access Control):** Lock down `/api/face/register/public`, `/api/people/bulk/extract`, `/api/files/**`, `/api/make/**`, `api/gemini_api.py` dev-mode bypasses, `/update_user/<uid>`, `/api/study*`, and leaderboard deletion.
4. **High (Transport/Session):** Fix `SameSite=None` + disabled CSRF in `spring`, set `httponly=True` on `flask` JWT cookies, and tighten CORS in `flask` and `node_backend/index.js`.
5. **Medium (Frontend):** Escape all dynamic values before `innerHTML` in `pages` and sandbox JS playground execution in an isolated `iframe`.
