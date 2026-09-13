---
title: Backend Implementation & Testing
layout: post
description: Phase 3 MVC routes, repository, validation, and testing for my account directory
permalink: /spt1/backend
author: Jade
showReadingTime: true
---

# Why I chose MVC

My [original design]({{ site.baseurl }}/spt1) is an administrator-facing account directory. I chose Option B so Spring can load records and render HTML in one application. The portfolio contains this documentation; the working forms must run on the Spring server.

The following implementation examples build on the [Phase 2 entity]({{ site.baseurl }}/spt1/persistence). They require integration into the backend before they can run. The linked backend could not be retrieved from this environment, so its package name, dependency compatibility, login flow, and security filter rules have not been verified.

# Repository

`DirectoryAccountRepository.java`, in the same package as the entity:

```java
package YOUR_APPLICATION_PACKAGE.directory;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DirectoryAccountRepository
        extends JpaRepository<DirectoryAccount, Long> {
}
```

Spring Data creates the repository implementation at runtime. `findAll()` retrieves records, `findById(id)` returns an `Optional`, `save(account)` inserts or updates an entity, and `delete(account)` removes it. The interface inherits these operations, so I do not write SQL for each route. No custom query is needed for this first version.

# Routes

| Method | Route | Result |
|---|---|---|
| GET | /mvc/directory | List HTML |
| GET | /mvc/directory/{id} | Detail HTML, or 404 |
| GET | /mvc/directory/new | Empty creation form |
| POST | /mvc/directory | Validate, create, redirect |
| GET | /mvc/directory/{id}/edit | Prefilled form, or 404 |
| POST | /mvc/directory/{id} | Validate, update, redirect |
| POST | /mvc/directory/{id}/delete | Delete, redirect, or 404 |

# Controller

`DirectoryAccountController.java`:

```java
package YOUR_APPLICATION_PACKAGE.directory;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mvc/directory")
public class DirectoryAccountController {
    private final DirectoryAccountRepository repository;

    public DirectoryAccountController(DirectoryAccountRepository repository) {
        this.repository = repository;
    }

    @InitBinder("account")
    void bindEditableFields(WebDataBinder binder) {
        // A submitted form cannot choose the primary key or creation time.
        binder.setAllowedFields("name", "email", "school", "studentID",
                "githubUsername", "accountType");
    }

    @ModelAttribute("accountTypes")
    AccountType[] accountTypes() {
        return AccountType.values();
    }

    @GetMapping
    String list(Model model) {
        model.addAttribute("accounts", repository.findAll(
                org.springframework.data.domain.Sort.by("id")));
        return "directory/list";
    }

    @GetMapping("/{id}")
    String detail(@PathVariable Long id, Model model) {
        model.addAttribute("account", requireAccount(id));
        return "directory/detail";
    }

    @GetMapping("/new")
    String createForm(Model model) {
        return form(new DirectoryAccount(), null, model);
    }

    @PostMapping
    String create(@Valid @ModelAttribute("account") DirectoryAccount account,
                  BindingResult errors, Model model,
                  RedirectAttributes redirect) {
        if (errors.hasErrors()) return form(account, null, model);
        DirectoryAccount saved = repository.save(account);
        redirect.addFlashAttribute("message", "Account created.");
        return "redirect:/mvc/directory/" + saved.getId();
    }

    @GetMapping("/{id}/edit")
    String editForm(@PathVariable Long id, Model model) {
        return form(requireAccount(id), id, model);
    }

    @PostMapping("/{id}")
    String update(@PathVariable Long id,
                  @Valid @ModelAttribute("account") DirectoryAccount submitted,
                  BindingResult errors, Model model,
                  RedirectAttributes redirect) {
        DirectoryAccount existing = requireAccount(id);
        if (errors.hasErrors()) return form(submitted, id, model);
        copyEditableFields(submitted, existing);
        repository.save(existing);
        redirect.addFlashAttribute("message", "Account updated.");
        return "redirect:/mvc/directory/" + id;
    }

    @PostMapping("/{id}/delete")
    String delete(@PathVariable Long id, RedirectAttributes redirect) {
        repository.delete(requireAccount(id));
        redirect.addFlashAttribute("message", "Account deleted.");
        return "redirect:/mvc/directory";
    }

    private DirectoryAccount requireAccount(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Directory account not found"));
    }

    private String form(DirectoryAccount account, Long editingId, Model model) {
        model.addAttribute("account", account);
        model.addAttribute("editingId", editingId);
        return "directory/form";
    }

    private void copyEditableFields(DirectoryAccount source,
                                    DirectoryAccount target) {
        target.setName(source.getName());
        target.setEmail(source.getEmail());
        target.setSchool(source.getSchool());
        target.setStudentID(source.getStudentID());
        target.setGithubUsername(source.getGithubUsername());
        target.setAccountType(source.getAccountType());
    }
}
```

`@Controller` interprets a returned string such as `directory/list` as a template name. `@RestController` would write that string to the HTTP response body instead. `@PathVariable` reads the row ID from the URL. `@ModelAttribute` binds form fields, and `@Valid` applies the entity's validation rules. `BindingResult` immediately follows the validated argument so invalid values can be displayed in the form.

Constructor injection makes the repository dependency explicit. Updates load the existing row and copy only editable fields, preserving the original creation timestamp. The path ID selects the record; an injected form field named `id` cannot redirect the update to a different row.

# Session security integration

This directory contains personal information. Before enabling it, protect **all** `/mvc/directory` routes with the backend's existing administrator authority, use its session login flow, and keep CSRF enabled on these routes. Do not create a competing security filter chain or assume the backend uses the authority name `ROLE_ADMIN`; inspect its existing rules first.

The form in Phase 4 includes a CSRF token. Spring Security must expose and validate that token. Thymeleaf rendering alone does not enable CSRF protection. If the existing backend ignores CSRF globally, that configuration must be corrected for these session-based MVC routes before deployment. Same-origin MVC forms do not need `@CrossOrigin`.

# Error handling

| Case | Intended behavior |
|---|---|
| Missing record | 404 through `ResponseStatusException` |
| Blank name or malformed email | Redisplay form with validation errors; do not save |
| Unsupported account type | Binding error; do not save |
| Non-numeric ID | Spring rejects conversion with 400 |
| Forged ID or creation time field | Binder excludes the field |
| Missing/invalid CSRF token | 403 when the security configuration is correct |
| Unauthenticated/non-admin request | Existing login/access-denied policy; no directory data exposed |
| Database unavailable | Request fails; no success flash message |

Database failures are not caught and silently treated as success. Use the backend's existing error page and logging policy to show a generic failure while keeping SQL and personal data out of the response. This small version uses last-write-wins updates; concurrent edit protection would require a version field and conflict handling.

# Browser testing plan

These are expected results to verify, **not recorded passing tests**. Run the backend on its configured port; the assignment uses `http://localhost:8585`.

| Test | Action | Expected result |
|---|---|---|
| Create | Submit name `Sample Student`, email `student@example.com`, student ID `001234`, type STUDENT | Redirect to detail; generated ID and UTC join time |
| Read all | Open /mvc/directory | New row appears |
| Read one | Select View | Every saved field appears |
| Update | Change school, save | New school shown; ID and join time unchanged |
| Guest | Create GUEST with no student ID or school | Valid entry with optional fields empty |
| Validation | Submit blank name or email `invalid` | Error shown; row count unchanged |
| Type validation | Alter accountType to an unknown value in DevTools | Binding error; no save |
| Protected fields | Add id and createdAt fields to a POST | Server ID/time remain authoritative |
| Missing ID | Open an ID absent from the database | 404 |
| CSRF | Remove the token from a form POST | 403; no database change |
| Access control | Visit routes logged out, then as a non-admin | Login/access denial according to backend policy |
| Delete | Use Delete on a synthetic entry's detail page | Redirect to list; row gone; old detail URL returns 404 |
| Refresh | Refresh after successful creation | GET refresh does not repeat creation |

For each successful mutation, inspect DevTools Network: the form POST should return a redirect, followed by a GET for the destination HTML. Capture the list, detail, edit, validation, and success states. Re-run `SELECT` in SQLite to confirm browser actions changed stored rows.

**Evidence to add:** real browser screenshots and actual test outcomes. Postman JSON collections belong to Option A and are not required for this MVC choice.

Continue to [Full-Stack MVC Application]({{ site.baseurl }}/spt1/frontend).
