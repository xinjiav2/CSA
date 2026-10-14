---
layout: post
permalink: /r-limit/
author: Jade
title: Rate Limiting Problem
showReadingTime: true
toc: true
---

# The Problem
On the microblog within the lessons, if you were to make a post, you can spam the button and that would create basically a million different posts.

My plan is to add a rate limiting feature by adding a new file



# Microblog submission protection

The floating Microblog panel allows users to create posts and reply to existing
posts. Submission protection prevents accidental duplicate comments caused by
repeated clicks or slow requests.

## User experience

- The Save or Reply button is disabled and displays **Sending...** while its
  submission is pending. Additional submissions from that form are ignored.
- Posts and replies share a **10-second cooldown**. Switching between the two
  forms does not bypass it.
- An attempt during the cooldown displays a browser alert showing the remaining
  wait time, rounded up to whole seconds. The draft stays in the form.
- The button returns to its original label and becomes enabled when the request
  finishes. An enabled button does not mean the cooldown has expired; submitting
  during the cooldown shows the alert.

## Implementation

| File | Responsibility |
| --- | --- |
| [`assets/js/api/microblog-submission-guard.js`](../assets/js/api/microblog-submission-guard.js) | Tracks the pending request and cooldown deadline. |
| [`assets/js/api/microblog.js`](../assets/js/api/microblog.js) | Wraps `createPost` and `createReply` in one shared guard. |
| [`_includes/microblog_foundation.html`](../_includes/microblog_foundation.html) | Hosts the panel and forms; manages button state and error alerts. |
| [`tests/microblog_submission.test.mjs`](../tests/microblog_submission.test.mjs) | Tests API submission protection and form behavior. |

The guard sets its pending flag before starting the request. While that flag is
set, another guarded call is rejected without sending a network request, even
if the first request takes longer than 10 seconds.

The cooldown starts when the operation finishes, including response JSON parsing.
It applies after success and after network, HTTP, or JSON parsing failures. A
failed response does not prove the server failed to save the comment, so immediate
retries are also blocked. The pending flag is always released in `finally`.

`createSubmissionGuard` accepts `cooldownMs` (default `10000`) and a `now`
function (default `Date.now`). Tests supply a controlled clock so they can verify
timing without real delays. Change the default duration in the guard module to
adjust the cooldown for the API's shared instance.

Editing existing posts uses `updatePost` and does not consume the shared cooldown.
The Save button still prevents repeated submissions while an edit is pending.
Reading posts and replies is unaffected.

## Scope and limitations

This is browser-side protection against accidental repeated submissions. State
is held in memory for the currently loaded page. Reloading, navigating to a new
page, or opening another tab creates a new guard. It does not coordinate tabs,
devices, or users, and direct API requests can bypass it.

Enforced abuse prevention would require rate limiting in the separate Python
backend for `POST /api/microblog` and `POST /api/microblog/reply`. That backend
change is not part of this implementation.

## Verification

Run the focused tests from the repository root:

```sh
node --test tests/microblog_submission.test.mjs
```

The seven tests cover overlapping requests, the shared post/reply cooldown,
submission at the cooldown boundary, recovery after network/HTTP/JSON failures,
and both forms' button restoration and alert behavior.

For a manual browser check, submit a post and click Save repeatedly while it is
sending. Only one creation request should be sent. Immediately attempt a reply:
the cooldown alert should appear without sending a reply request. After the
cooldown expires, submitting the reply should work normally.
