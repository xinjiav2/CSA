# Microblog author spoofing fix

Updated: October 29, 2026

The fix addresses the [author spoofing review](microblog-author-spoofing.md).
Implementation is in `/home/jade/dev/flask` on branch
`fix/microblog-author-spoofing`. Changes remain uncommitted and have not been
deployed. Existing uncommitted implementation work was preserved and extended.

## Problem and resulting behavior

Previously, an authenticated user could embed fabricated replies in a post's
`data` object. The API stored and returned the supplied author identity, content,
and timestamp. The same metadata path could seed reactions attributed to other
users.

Post creation and update now allow only presentation metadata. Supplying
`data.replies`, `data.reactions`, author fields, or unknown metadata fields returns
HTTP 400, including for administrators. Non-object metadata and malformed
non-object request bodies also return HTTP 400. Supplied non-text post content
is rejected.

For example, this request body is rejected by `POST /api/microblog`:

```json
{
  "content": "A real post",
  "data": {
    "replies": [{"userId": 99, "userName": "Another person", "content": "Forged"}]
  }
}
```

A legitimate reply goes through authenticated `POST /api/microblog/reply`:

```json
{"postId": 123, "content": "My reply"}
```

The server assigns the reply ID, authenticated author's ID, and timestamp.
Submitted identity or timestamp fields cannot override those values. Reply
display names resolve from the current related user record. Reactions similarly
use the authenticated user's ID through `/api/microblog/reaction`.

## Implementation

Paths in this table are relative to `/home/jade/dev/flask`.

| File | Change |
| --- | --- |
| `api/microblog_api.py` | Validate metadata on POST and PUT; reject malformed post bodies and non-text content. |
| `model/microblog.py` | Enforce the metadata allowlist in the model; store replies and reactions in separate tables; serialize only trusted interaction records. |
| `scripts/migrate_microblog_interactions.py` | Audit legacy metadata and optionally create the interaction tables without importing legacy interactions. |
| `tests/test_microblog_security.py` | Ten isolated HTTP/SQLite regression tests covering metadata injection, authorship, legacy data, and existing authorization behavior. |
| `README.md` | Backend upgrade and test instructions. |

The allowed `data` keys are `lessonProgress`, `rating`, `hashtags`,
`helpRequested`, `difficulty`, `projectType`, `features`, `seeking`,
`standupType`, `tasks`, `blockers`, `mood`, `resourceType`, `resourceUrl`,
`subject`, and `recommendation`.

`microblog_replies` stores a generated primary key, post and author foreign keys,
content, and timestamp. `microblog_reactions` uses post ID, author ID, and
reaction type as its composite primary key. Deleting a post through the model
cascades to its interaction records. Metadata updates reassign the JSON value
so SQLAlchemy persists edits while preserving stored legacy evidence.

## Existing databases and migration

Before starting the updated backend against an existing database, run these
commands from `/home/jade/dev/flask` with the backend's dependencies and database
configuration available:

```sh
# Audit legacy metadata without changing application records.
python scripts/migrate_microblog_interactions.py

# Create missing interaction tables, then audit legacy metadata.
python scripts/migrate_microblog_interactions.py --create-tables
```

Table creation is idempotent. The script does not drop tables, rewrite legacy
JSON, or automatically import legacy replies or reactions. Its audit reports post
IDs, unsupported field names, and interaction counts, omitting reply content and
claimed display names.

Legacy JSON interactions are excluded from API responses and reaction counts.
Previously visible replies and reactions will therefore disappear from the UI
until independently verified and restored. Matching a stored name or ID to an
existing account does not establish authorship. Use independent trusted evidence
before manually restoring any interaction; there is no automatic restoration
command in this change.

New interaction tables must exist before the updated backend serves microblog
reads or writes. The migration has not been run against an application database
as part of this work.

## Verification

Run the regression suite from `/home/jade/dev/flask`:

```sh
python -m unittest discover -s tests -p 'test_microblog_security.py' -v
```

The tests use an isolated Flask app, real HTTP handlers through Flask's test
client, an in-memory SQLite database, and a minimal user schema. They are designed
to cover rejected create/update injections, malformed requests, metadata
persistence, hidden legacy interactions, server-assigned reply identity and
current display names, replies by another authenticated user, audit output and
data preservation, reaction identity/idempotency, authentication/ownership/admin
restrictions, and post deletion cascading to interactions.

Python compilation of the changed Python files and `git diff --check` passed.
The runtime suite could not start because Flask/PyJWT dependencies are missing
from the local test environment. Installing the test dependencies failed because
the environment could not resolve `pypi.org`. The ten regression tests remain
unverified; no passing runtime test result or deployed behavior is claimed.
