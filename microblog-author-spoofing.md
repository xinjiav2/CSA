# Microblog author spoofing review

Reviewed: October 26, 2026

## Finding

**Authenticated users can fabricate replies attributed to other users by submitting replies inside a new post's `data` object.** The main post's author remains the authenticated user, but the nested replies can contain arbitrary author IDs, names, content, and timestamps.

This is an integrity problem: readers can see statements that appear to come from people who never wrote them. An attacker needs an authenticated account that is permitted to post. No administrator role is required for this creation path.

## Why it happens

1. `POST /api/microblog` reads the entire client-supplied `data` object and passes it to `MicroBlog` without excluding reserved fields such as `replies`.
2. The model stores that object as `_data` without validating nested reply authors.
3. `read()` merges `_data` into the returned post. It protects core post fields, but leaves the supplied `replies` intact.
4. `get_replies()` returns the stored reply list directly, checking only that it is a list.
5. The frontend renders each reply's supplied `userName`.

| Evidence | Location in the reviewed workspace |
| --- | --- |
| Client-controlled data enters post creation | `flask/api/microblog_api.py:41`, `:61–65` |
| Model stores the supplied data | `flask/model/microblog.py:63` |
| Post serialization includes nested replies | `flask/model/microblog.py:102–107` |
| Reply retrieval returns stored objects unchanged | `flask/model/microblog.py:133–137` |
| Frontend displays the reply's supplied name | `pages/_includes/microblog_foundation.html:268–273` |

Paths above are relative to `/home/jade/dev`.

## Illustrative payload

An authenticated user can submit the following JSON to `POST /api/microblog`:

```json
{
  "content": "A post created by the signed-in user",
  "data": {
    "replies": [
      {
        "id": 1,
        "userId": 99,
        "userName": "Another person",
        "content": "A statement that person never wrote",
        "timestamp": "2026-10-26T12:00:00"
      }
    ]
  }
}
```

The post itself is attributed to the signed-in user. The embedded reply is accepted as supplied and can be displayed as coming from “Another person.” The name does not have to match the supplied user ID or an existing account.

## Related concerns

- The same unrestricted metadata object can seed fabricated `reactions`, including arbitrary user IDs. This follows from the creation path and the model's use of `_data` for reaction storage; it was not tested against a running application.
- The post update route also accepts arbitrary `data` after checking ownership or administrator status (`flask/api/microblog_api.py:130–137`). Its model method merges that data (`flask/model/microblog.py:118–123`). Persistence of changes to an existing JSON object needs separate verification because that method does not explicitly mark the JSON field as modified. The confirmed creation path does not depend on this update behavior.

## Protections already present

- New post ownership comes from `g.current_user.id`, not a submitted author ID (`flask/api/microblog_api.py:62`).
- Server-generated `userId`, `userName`, and `userUid` override matching client metadata during post serialization (`flask/model/microblog.py:89–104`).
- The dedicated reply endpoint calls `add_reply(current_user.id, reply_content)`, and the model resolves the reply name from the user record (`flask/api/microblog_api.py:203`; `flask/model/microblog.py:151–165`).
- Updating or deleting another user's post requires administrator status in the inspected CRUD routes.

These checks protect the normal author assignment paths, but arbitrary nested replies bypass the dedicated reply creation logic.

## Recommended fixes

1. Allowlist client-editable metadata fields on both post creation and update. Reject reserved fields such as `replies` and `reactions` in general-purpose metadata.
2. Create replies only through the authenticated reply endpoint, generating author identity, IDs, and timestamps on the server.
3. Prefer separate reply records with a foreign key to the author. Resolve display names from that relationship when returning replies.
4. Review existing stored reply data before treating it as trustworthy. A client-supplied name or ID alone does not establish who wrote a reply.
5. Add regression checks that reject fabricated nested replies while preserving server-assigned authorship through the dedicated endpoints.

## Verification and limits

The review inspected local API, model, authentication, and frontend code. An isolated, in-memory check executed the model's `read()` and `get_replies()` methods with fabricated metadata. It confirmed that core post author fields override client data while forged reply identities survive both methods.

No live HTTP requests were sent, no database records were written, and no application files were changed during the review. This report documents the local source behavior; deployed behavior was not tested.
