# Microblog rate limiting

Implemented in the `pages` repository on `fix/microblog-rate-limit`.
Documentation is in the separate `CSA` repository on the same branch name.

## Behavior

- Posts, edits, and replies share one 30-second cooldown per browser origin.
- The cooldown starts before a write request is sent. Failed requests also consume
  the cooldown to prevent repeated retries from flooding the API.
- Save and Reply buttons display `Saving...` and are disabled while their request
  runs. A second write is rejected while a request is pending, even after 30 seconds.
- Attempts during cooldown use the existing browser alert popup to explain how
  many seconds remain. The form stays open and keeps the entered content.
- Local storage preserves the cooldown across refreshes, navigation, and tabs.
  Web Locks prevents simultaneous admission from multiple tabs where supported.
- HTTP 429 responses extend the cooldown using `Retry-After` in seconds or HTTP
  date format. Missing/invalid headers retain the default cooldown.
- Reading posts and replies is not limited.

## Files

- `pages/assets/js/api/microblog.js`: routes all three write functions through
  the shared limiter; API paths and request bodies are unchanged.
- `pages/assets/js/api/microblog-rate-limit.js`: cooldown, persistence, pending
  request protection, cross-tab admission, and server backoff handling.
- `pages/_includes/microblog_foundation.html`: shared create/edit/reply forms,
  disabled buttons during requests, and existing error popups.
- `pages/tests/microblog_rate_limit.test.mjs`: deterministic behavioral tests.

Change `MICROBLOG_COOLDOWN_MS` in the limiter to adjust the default interval.

## Scope and limitations

This is client-side protection against UI spam, not an enforceable API quota.
Clearing storage, using another browser, or calling the API directly bypasses it.
When storage is unavailable, protection falls back to page memory and logs a
warning. Browsers without Web Locks have a potential simultaneous cross-tab race.
The shared interval also applies when switching accounts in the same browser.

The API module sends requests to the separate Flask service's `/api/microblog`
and `/api/microblog/reply` endpoints; it does not directly create Git commits.
For protection against deliberate abuse, the backend must independently enforce
an authenticated-user quota before database writes or downstream side effects.
For cross-origin server backoff, expose `Retry-After` through CORS.
No backend code or deployment is changed by this patch.

## Verification

From `CSA Folder/pages`, run:

```sh
node --test tests/microblog_rate_limit.test.mjs tests/microblog_forms.test.mjs
git diff --check
```

Manual browser checks after serving the site through the repository Makefile:

1. Open a page that includes the microblog, sign in, and save a post. During the
   request, confirm Save displays `Saving...` and cannot be clicked again.
2. Immediately try an edit or reply. Confirm the popup gives the remaining wait,
   the draft remains available, and no additional write appears in Network.
3. Reload or open another page/tab and try again within 30 seconds; it should
   remain blocked. After the interval, one new write should succeed.
4. With a slow request, confirm no additional write can start while it is pending.
5. Simulate an HTTP 429 with `Retry-After: 120`; confirm the longer wait is shown.

Automated tests cover duplicate rejection, reload persistence, cooldown expiry,
pending requests, failed requests, numeric/date server backoff, shared admission
through all three API write functions, unrestricted reads, disabled form buttons,
popup text, and preserving a rejected draft. Requests are mocked; these tests do
not create real posts or commits.

## Windows local setup (2026-10-14)

Ruby 3.3 was already installed. Installed Bundler 4.0.19 and RubyInstaller's
MSYS2/MINGW toolchain (`ridk.cmd install 1 3`) to build native gems, then installed
the bundle into `pages/vendor/bundle`. The ignored `.bundle/config` records that
path, so subsequent `bundle exec` commands find the installed gems.

Generated Courses, Blogs, and Topics layouts with the existing
`scripts/publish_project.rb` publisher. In the agent's restricted shell Ruby
`Dir.glob` returned empty results; publication needed an unrestricted shell.
The checked-in Python virtual environments refer to Linux paths and do not run
on Windows. The local, ignored `_sass/projects/_all.scss` was generated with the
same directory scan as `scripts/generate_sass_imports.py` using Node instead.
This prepares the existing checked-out Markdown for Jekyll; it does not run the
full notebook/DOCX conversion pipeline.

Cached the configured `jekyll/minima` HEAD theme in
`vendor/minima-local/minima-HEAD`. The ignored `_config.local.yml` selects that
copy and explicitly enables `jekyll-remote-theme`, `jekyll-include-cache`,
`jekyll-seo-tag`, and `jekyll-feed`. Production `_config.yml` is unchanged.

Start from `CSA Folder/pages`:

```powershell
bundle exec jekyll serve --config _config.yml,_config.local.yml --host 127.0.0.1 --port 4500 --no-watch
```

`--no-watch` matches the repository's Jekyll target; restart to rebuild edits.
The full Makefile conversion workflow still requires a working Python environment
and its dependencies. MSYS2 supplies Make, accessible through `ridk.cmd exec make`.

### Verified results

- Jekyll completed generation in 174.702 seconds and started serving on
  `http://127.0.0.1:4500/`. Existing `post_url` deprecation warnings remain.
- HTTP 200 verified for `/microblog`, `/assets/js/api/microblog.js`,
  `/assets/js/api/microblog-rate-limit.js`, and `/assets/js/api/config.js`.
- The served microblog HTML contains both forms, the disabled-button handling,
  and popup handler; the served API imports the limiter. Config Liquid values
  were rendered successfully.
- All five Node tests pass, including simulated simultaneous tabs with Web Locks.
- `git diff --check` passes.
- No browser connection was available for visual testing. Form behavior was
  tested through the actual handlers with mocked DOM/API objects. The Flask
  backend was not started, and no live microblog writes were made.
