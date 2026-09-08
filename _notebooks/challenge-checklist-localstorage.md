---
title: Challenge Checklist Local Storage
description: Notes on making the challenge checklist interactive and persistent.
---

# Challenge checklist local storage

The six items in the **1. Understand the Challenge** section of `challenge.md`
are now interactive checkboxes. The original rendered HTML marked every checkbox
as `disabled`, so the browser did not allow a student to check an item.

## What changed

- Added the unique ID `understand-challenge-checklist` to the checklist.
- Removed the `disabled` attribute from its six checkbox inputs.
- Added a `DOMContentLoaded` handler that restores each checkbox state when the
  page opens.
- Added a `change` handler that saves an item's state immediately after it is
  checked or unchecked.

## Storage format

The browser stores one JSON object under the local storage key
`challenge-understanding-checklist`. Each property is the checkbox's position in
the list, and each value is a boolean. For example:

```json
{"0":true,"1":false,"2":true}
```

This data stays in the current browser and origin after a reload. It does not
sync across browsers or devices. Both reading and writing are wrapped in
`try...catch`, so a blocked or malformed local storage value will not prevent the
rest of the page from working.

## How to verify

1. Open the challenge page in a browser.
2. Check and uncheck several items under **1. Understand the Challenge**.
3. Reload the page and confirm the same selections remain.
4. In browser developer tools, open **Application > Local Storage** and inspect
   `challenge-understanding-checklist`.
