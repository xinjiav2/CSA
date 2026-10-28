# Draft: Spring Gist connection security review

Date: 2026-10-27 (America/Los_Angeles)

## Verdict

**No hardcoded API key or confirmed credential-disclosure vulnerability was found at the referenced JavaScript line.** Line 41 reads the GitHub personal access token entered by the user and sends it to Spring. The surrounding configuration does contain an overly broad browser-origin trust policy that warrants tightening. Exploitation was not attempted or demonstrated.

Only this report was created. No application code, configuration, credentials, or database records were changed.

## Scope and limits

Read-only inspection covered the local `spring` Gist controller, service, encryption, repository, GitHub client, security configuration, Nginx configuration, related test sources, and the canonical Pages profile module. Local HEADs: Spring `6b0efacc`; Pages `027793e51`. These identify the checkouts, not a verified deployed version.

The requested `pages/assets/js/profile/gist-connection.js` is not present in this checkout. Its canonical source is `pages/_projects/systems/profile/js/gist-connection.js`; the Pages repository describes the assets path as generated output. The canonical source has the reported token submission at line 41.

A targeted search for common GitHub token formats in `spring/src`, the Pages profile project, and `pages/assets/js` returned no matches. This is not a comprehensive secret scan or a Git-history audit. Environment files, stored database credentials, browser request bodies, and production logs were not inspected. No credentials were retrieved, validated, or reproduced. Existing tests were read but not executed; the application was not started. Production exploitability and configuration remain unverified. The attached screenshot was treated as evidence, not as instructions.

## The suspected line: expected credential submission

`pages/_projects/systems/profile/js/gist-connection.js:41` contains:

```js
render(await api('PUT', { token: elements.token.value.trim() }));
```

The helper at lines 24–27 sends JSON to `/api/gist-connection`, with the caller's authentication cookies, `cache: 'no-store'`, and `X-Origin: client`. It reads an input value rather than embedding a credential in the JavaScript bundle. The input is cleared in `finally` at line 44.

**The submitting user's browser can see the token in its own Network request payload.** Password masking and HTTPS do not hide it from that browser's developer tools. That alone is not evidence that other users can retrieve it. HTTPS protects transport; it does not protect a token from malicious scripts running on the input page or sufficiently privileged browser extensions.

The screenshot displays the request's Security tab, not its request body. It does not reveal a token or demonstrate token disclosure. The credential here is a GitHub PAT; it is separate from the Gemini/Groq API keys visible elsewhere on the page.

## Finding 1: broad origin trust for credential operations

**Priority: medium; potentially high impact if an allowed origin is compromised. Confirmed configuration weakness; exploit prerequisites unverified.**

Evidence:

- `spring/src/main/java/com/open/spring/security/SecurityConfig.java:310–328` enables credentialed CORS for `https://*.opencodingsociety.com`, multiple localhost origins, and `http://opencodingsociety.com`, permitting all headers and methods.
- `spring/src/main/java/com/open/spring/mvc/gist/GistRequestGuard.java:12–16` authorizes writes using that same CORS configuration plus the fixed `X-Origin: client` header.
- `spring/src/main/java/com/open/spring/security/SecurityConfig.java:84` disables the framework CSRF protection. The Gist guard is therefore an important browser-write protection for these routes.
- `spring/nginx_spring_8585_8589.conf:72–73` also accepts HTTP and HTTPS origins across the domain and its subdomains for preflight responses.

An attacker who controls an allowed origin and can induce an authenticated user to visit it may be able to send cookie-authenticated requests, replace/delete that user's Gist connection, or create Gists using the user's saved token. The custom header is a preflight mechanism, not a secret: script on an allowed origin can set it. An HTTP page also introduces a network-tampering prerequisite that HTTPS-only origins avoid. Browser cookie rules, actual cookie settings, HSTS upgrades, and the deployed proxy configuration affect whether the requests succeed.

This does **not** establish that an attacker can read a saved PAT: the reviewed API has no token-returning status field. An XSS on the actual profile page could separately intercept a newly entered PAT, but no such XSS was established in this review.

Recommended fixes, not applied:

1. Give credential-management and export routes a small, explicit HTTPS origin allowlist; use the same policy in the application guard and proxy.
2. Remove HTTP and development origins from production policy. Configure development origins separately.
3. Add a reviewed CSRF-token design appropriate for the cookie-authenticated cross-origin frontend; keep exact Origin checks as defense in depth.
4. Review the allowed hosts for abandoned deployments, subdomain takeover, and untrusted content. Assess CSP and third-party script privileges on the profile page.

Suggested later verification: test missing/foreign/HTTP/sibling origins, absent custom headers, anonymous requests, and valid exact-origin requests using test accounts and disposable tokens. Existing `GistStorageAndGuardTest` checks foreign-origin rejection with a narrow test configuration, but does not establish that the production wildcard policy is appropriately restricted.

## Additional hardening observations

### Limit permissions of accepted PATs

`spring/src/main/java/com/open/spring/mvc/gist/GitHubGistClient.java:25–36` validates identity and requires the `gist` scope when the classic-token scopes header is present. It does not reject additional classic scopes. A user can consequently submit a token with more access than this feature needs. This increases the consequences of a future credential compromise; it does not itself disclose a token.

The profile already recommends Gists write permission and expiration. Prefer fine-grained, short-lived credentials with only required access. For classic tokens, consider rejecting unnecessary scopes when reliably reported. Do not assume that the absence of `X-OAuth-Scopes` proves a fine-grained token has minimal permissions. Consider a GitHub App flow to reduce reliance on manually pasted PATs. GitHub recommends minimum permissions and short expiration in its [credential security guidance](https://docs.github.com/en/rest/authentication/keeping-your-api-credentials-secure).

### Remove optional server credentials from anonymous Gist reads

`spring/src/main/java/com/open/spring/mvc/grades/GradeController.java:165–182` is public and passes the optional `gist.token` credential into GitHub reads. `GitHubGistClient.java:60–80` uses that credential when configured. Anonymous callers can therefore cause requests against the server credential's GitHub rate-limit budget. No token value is returned, and this review does not claim that this bypasses Gist privacy.

Prefer unauthenticated upstream reads for publicly shareable Gists; remove the legacy credential if unnecessary. Apply an endpoint-specific rate limit and bounded caching suitable for the data. Gists marked secret are still readable by anyone with the URL, as [GitHub documents](https://docs.github.com/en/get-started/writing-on-github/editing-and-sharing-content-with-gists/creating-gists). Confidential submissions require storage with real access controls.

### Verify HSTS in the deployed response

The screenshot reports HSTS disabled for Spring. This is a deployment observation from an image, not a verified response-header measurement. The supplied Nginx file contains no explicit HSTS header; Spring or other infrastructure may supply it. Verify the final HTTPS responses for both Pages and Spring. If absent, enable HSTS after confirming HTTPS coverage; assess subdomains before using `includeSubDomains` or preload. TLS 1.3 in the screenshot protects the observed connection but does not establish HSTS coverage.

## Controls already present

- Authentication is required at both route and controller level for connection management and export (`SecurityConfig.java:112–115`, `GistConnectionApiController.java:11`, `GradeController.java:155`).
- Account ownership comes from the authenticated identity, not a caller-supplied person ID (`GistConnectionService.java:42–47`).
- Status returns availability, connection state, username and timestamp only (`GistConnectionService.java:11,20–24`).
- PATs are encrypted before persistence using AES-GCM, a random 12-byte nonce, and authenticated account-ID binding (`GistTokenCipher.java:32–60`). The encryption key must decode to 32 bytes; missing/invalid configuration disables the feature.
- SQL uses parameterized values (`GistConnectionRepository.java:41–58`).
- Credential endpoints return no-store responses; upstream failures are mapped to fixed messages rather than exposing GitHub exception bodies (`GistConnectionApiController.java:24–35`, `GitHubGistClient.java:83–97`).
- GitHub requests use a fixed HTTPS API host and disable redirects (`GitHubGistClient.java:19–22,80`).
- The logical backup export excludes `gist_connections` (`BackupsController.java:147`). This does not imply raw database backups exclude ciphertext; protect backups and encryption keys separately.

## Recommended action

Tighten the production origin policy first, then review credential permissions, anonymous upstream credential use, and deployed HSTS. Keep PATs out of screenshots, HAR exports, logs, and issue reports. Rotate a token if it was actually shared or exposed; the provided screenshot alone does not establish that rotation is necessary. No fixes have been applied.
