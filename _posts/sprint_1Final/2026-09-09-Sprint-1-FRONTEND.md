---
title: Full-Stack MVC Application
layout: post
description: Phase 4 Thymeleaf list, detail, and edit views for my admin directory
permalink: /spt1/frontend
author: Jade
showReadingTime: true
---

# My full-stack architecture

I chose Option B in [my ideating post]({{ site.baseurl }}/spt1). My frontend is a set of Thymeleaf templates served by Spring. GitHub Pages hosts the write-up, while Spring hosts the interactive directory.

```text
Browser request
    -> Spring Security: session, administrator access, CSRF for POST
    -> DirectoryAccountController
    -> DirectoryAccountRepository
    -> JPA / Hibernate -> SQLite directory_accounts
    -> Controller adds records to Model
    -> Thymeleaf renders HTML -> Browser
```

The [entity]({{ site.baseurl }}/spt1/persistence) defines the data and the [controller]({{ site.baseurl }}/spt1/backend) defines the workflows. The templates below complete the implementation example. They have not been installed or browser-tested in the Spring backend, so this post does not claim a deployed application or completed screenshot evidence.

# List view

Create `src/main/resources/templates/directory/list.html` in the Spring repository:

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Account directory</title>
</head>
<body>
<main>
  <h1>Account directory</h1>
  <p th:if="${message}" th:text="${message}" role="status"></p>
  <p><a th:href="@{/mvc/directory/new}">Add account</a></p>
  <p th:if="${#lists.isEmpty(accounts)}">No accounts yet. Add the first account.</p>
  <table th:unless="${#lists.isEmpty(accounts)}">
    <caption>Students and guests</caption>
    <thead><tr>
      <th scope="col">Name</th><th scope="col">School</th>
      <th scope="col">Student ID</th><th scope="col">GitHub</th>
      <th scope="col">Type</th><th scope="col">Joined (UTC)</th>
      <th scope="col">Actions</th>
    </tr></thead>
    <tbody><tr th:each="account : ${accounts}">
      <td th:text="${account.name}"></td>
      <td th:text="${account.school}"></td>
      <td th:text="${account.studentID}"></td>
      <td th:text="${account.githubUsername}"></td>
      <td th:text="${account.accountType}"></td>
      <td th:text="${account.createdAt}"></td>
      <td>
        <a th:href="@{/mvc/directory/{id}(id=${account.id})}">View</a>
        <a th:href="@{/mvc/directory/{id}/edit(id=${account.id})}">Edit</a>
      </td>
    </tr></tbody>
  </table>
</main>
</body>
</html>
```

`th:each` repeats a row for every account supplied by the controller. `th:text` escapes displayed values, so a name containing HTML is displayed as text. `th:if` and `th:unless` select the success message and empty/list states. Account type is shown in text so distinguishing students from guests does not depend on color.

# Detail view

Create `src/main/resources/templates/directory/detail.html`:

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Account details</title>
</head>
<body>
<main>
  <h1 th:text="${account.name}">Account details</h1>
  <p th:if="${message}" th:text="${message}" role="status"></p>
  <dl>
    <dt>ID</dt><dd th:text="${account.id}"></dd>
    <dt>Name</dt><dd th:text="${account.name}"></dd>
    <dt>Email</dt><dd th:text="${account.email}"></dd>
    <dt>School</dt><dd th:text="${account.school}"></dd>
    <dt>Student ID</dt><dd th:text="${account.studentID}"></dd>
    <dt>GitHub username</dt><dd th:text="${account.githubUsername}"></dd>
    <dt>Account type</dt><dd th:text="${account.accountType}"></dd>
    <dt>Joined (UTC)</dt><dd th:text="${account.createdAt}"></dd>
  </dl>
  <p>
    <a th:href="@{/mvc/directory}">Back to directory</a>
    <a th:href="@{/mvc/directory/{id}/edit(id=${account.id})}">Edit account</a>
  </p>
  <form method="post"
        th:action="@{/mvc/directory/{id}/delete(id=${account.id})}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
    <p>Deleting removes this directory entry. It does not delete a login account.</p>
    <button type="submit">Delete directory entry</button>
  </form>
</main>
</body>
</html>
```

Deleting uses POST rather than a link that changes data with GET. The detail view shows all eight fields, while the list keeps email off the overview. Both views still require administrator authorization.

# Creation and edit form

Create `src/main/resources/templates/directory/form.html`:

```html
<!DOCTYPE html>
<html lang="en" xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Edit directory account</title>
</head>
<body>
<main>
  <h1 th:text="${editingId == null ? 'Add account' : 'Edit account'}"></h1>
  <form method="post" th:object="${account}"
        th:action="${editingId == null} ? @{/mvc/directory} : @{/mvc/directory/{id}(id=${editingId})}">
    <input type="hidden" th:name="${_csrf.parameterName}" th:value="${_csrf.token}">
    <div th:if="${#fields.hasErrors('*')}" role="alert">
      <p>Please correct these fields:</p>
      <ul><li th:each="error : ${#fields.errors('*')}" th:text="${error}"></li></ul>
    </div>
    <p>
      <label for="name">Name (required)</label>
      <input id="name" th:field="*{name}" required maxlength="100" autocomplete="name">
    </p>
    <p>
      <label for="email">Email (required)</label>
      <input id="email" type="email" th:field="*{email}" required
             maxlength="254" autocomplete="email">
    </p>
    <p>
      <label for="school">School (optional)</label>
      <input id="school" th:field="*{school}" maxlength="120">
    </p>
    <p>
      <label for="studentID">Student ID (optional)</label>
      <input id="studentID" th:field="*{studentID}" maxlength="40">
    </p>
    <p>
      <label for="githubUsername">GitHub username (optional)</label>
      <input id="githubUsername" th:field="*{githubUsername}" maxlength="39">
    </p>
    <p>
      <label for="accountType">Account type</label>
      <select id="accountType" th:field="*{accountType}" required>
        <option th:each="type : ${accountTypes}" th:value="${type}" th:text="${type}"></option>
      </select>
    </p>
    <button type="submit">Save account</button>
    <a th:href="@{/mvc/directory}">Cancel</a>
  </form>
</main>
</body>
</html>
```

`th:object` selects the form's model object. `th:field` binds each input's name and current value, including rejected values after validation. `th:value` supplies each dropdown option's value. The controller supplies `accountTypes` for both new forms and forms returned with errors.

For minimal typing, editing preloads the saved values and account type uses a dropdown. ID and join date are generated, so they are not inputs. Names and identifiers still require text because their possible values are not known in advance. These templates use semantic HTML with no inline styling; visual integration should reuse the admin panel's existing layout and SCSS pipeline.

# Click → save → display

1. The administrator clicks Edit. The browser sends `GET /mvc/directory/1/edit`.
2. The controller loads record 1 and puts it into the model as `account`.
3. Thymeleaf renders the existing values in the form.
4. Save submits URL-encoded form fields and the CSRF token to `POST /mvc/directory/1`.
5. Spring validates the fields, loads record 1, copies editable values, and saves it through JPA.
6. The controller redirects to `GET /mvc/directory/1` with a one-time success message.
7. Thymeleaf renders the updated row. Refresh now repeats the GET rather than the mutation.

This is POST-Redirect-GET. Invalid submissions return the form directly with errors instead of redirecting and losing validation feedback.

# CSRF and model binding

Session cookies are automatically sent by the browser, which is why a malicious website could otherwise attempt a form submission using a logged-in administrator's session. Spring Security validates the form's CSRF token against the expected token. The templates explicitly include `_csrf.parameterName` and `_csrf.token`; a correctly configured Spring Security/Thymeleaf integration can also add a hidden token automatically.

The `_csrf` attribute must exist. Do not hide a missing token by conditionally omitting it; fix the security integration. CSRF protection requires enabled server-side validation, and it does not replace administrator authorization. The controller's allowlist also prevents form binding from accepting an attacker-supplied primary key or creation timestamp.

# MVC compared with REST

| Concern | My MVC choice | REST alternative |
|---|---|---|
| Controller | `@Controller` returns template names | `@RestController` returns response data |
| Rendering | Thymeleaf runs on Spring | JavaScript builds the page in the browser |
| Updates | HTML form POST and redirect | fetch/axios POST, PUT, DELETE |
| Authentication design | Existing session login and CSRF checks | Depends on API security; cookies can still require CSRF protection |
| Origins | Forms and backend share an origin | GitHub Pages needs backend CORS configuration |
| Deployment | UI and backend deployed together | Frontend and backend deployed separately |

MVC suits the server-hosted admin panel I described in Phase 1. A separate REST frontend would be useful for additional clients, but it is not required for this choice. There is no `config.js` or browser `fetch()` call in this workflow; Thymeleaf's `@{...}` URLs target the Spring application and respect its context path.

# Screenshots and completion evidence

After backend integration, visit [the local directory](http://localhost:8585/mvc/directory) while signed in with the appropriate administrator account. This address is a local test target, not a published demo.

| Required screenshot | What it should demonstrate | Current status |
|---|---|---|
| List view | Synthetic student and guest rows with school, IDs, and join dates | Pending runtime capture |
| Detail view | One account's complete stored values | Pending runtime capture |
| Edit form | Prefilled fields and account-type dropdown | Pending runtime capture |
| Success message | Updated record after redirect | Pending runtime capture |
| Validation | Invalid input stays on the form with a useful error | Pending runtime capture |
| DevTools form data | CSRF field present; redact its value in shared evidence | Pending runtime capture |

Run the [Phase 3 test matrix]({{ site.baseurl }}/spt1/backend) and add observed results alongside the screenshots. The documentation and implementation examples are provided; compiling, security integration, live schema validation, and browser evidence remain to be completed in the Spring environment.
