---
title: Database Schema & Persistence
layout: post
description: Phase 2 of my student and guest account directory using JPA and SQLite
permalink: /spt1/persistence
author: Jade
showReadingTime: true
---

# My data object

In [Phase 1]({{ site.baseurl }}/spt1), I chose Option B: an admin directory built with Spring MVC and Thymeleaf. The directory shows names, schools, GitHub usernames, student IDs, account types, and join dates. This information helps an administrator identify an account before handling an account problem.

This post provides the implementation to add to the Spring backend. It has not been executed against that backend in this workspace. Schema screenshots and browser evidence still need to be captured after integration; the tables below describe the intended schema, not an observed database.

# From POJO to entity

I use `DirectoryAccount` as the descriptive name for my Phase 1 `POJO`. It represents directory information, not a second login system. It stores no passwords and does not implement password resets.

Place the following classes together in a `directory` package below the backend's existing Spring Boot application package. Replace the package placeholder with that actual package before compiling. These examples use Jakarta imports, so the backend must support Jakarta Persistence and Validation. Its dependency versions and security configuration must be checked during integration.

```java
package YOUR_APPLICATION_PACKAGE.directory;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "directory_accounts")
@Getter
@Setter
@NoArgsConstructor
public class DirectoryAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @NotBlank
    @Email
    @Size(max = 254)
    @Column(nullable = false, length = 254)
    private String email;

    @Size(max = 120)
    @Column(length = 120)
    private String school;

    @Size(max = 40)
    @Column(name = "student_id", length = 40)
    private String studentID;

    @Size(max = 39)
    @Pattern(regexp = "^$|[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*",
             message = "Use letters, numbers, and single internal hyphens")
    @Column(name = "github_username", length = 39)
    private String githubUsername;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false)
    private AccountType accountType = AccountType.STUDENT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void recordCreationTime() {
        createdAt = LocalDateTime.now(java.time.Clock.systemUTC());
    }
}
```

`AccountType.java`:

```java
package YOUR_APPLICATION_PACKAGE.directory;

public enum AccountType {
    STUDENT, GUEST
}
```

My original `POJO.java` uses `@Data`; the code shown in the Phase 1 post uses `@Getter` and `@Setter`. These are different: `@Data` also generates `toString`, `equals`, and `hashCode`, while getters and setters alone do not. For this entity I choose explicit getter/setter annotations to avoid automatically including personal information in `toString` and mutable fields in equality. AP CSA still requires understanding and writing these methods by hand.

# Relational schema

| Java field | Java type | SQLite storage intent | Constraints and purpose |
|---|---|---|---|
| id | Long | INTEGER primary key | Generated identity; names can repeat |
| name | String | TEXT affinity | Required; at most 100 characters in validation |
| email | String | TEXT affinity | Required; email format and length validated |
| school | String | TEXT affinity | Optional; guests may not have a school |
| studentID | String | TEXT affinity | Optional; preserves leading zeros and letters |
| githubUsername | String | TEXT affinity | Optional; username rather than a full URL |
| accountType | AccountType | TEXT affinity | Required; stored as STUDENT or GUEST |
| createdAt | LocalDateTime | Dialect-dependent timestamp representation | Required; set by the server at creation |

`@Entity` makes the class persistent. `@Table` gives it a stable table name. `@Id` identifies each row, and `@GeneratedValue` delegates ID generation to the database. `@Column` describes database mappings and nullability. `@Enumerated(EnumType.STRING)` stores readable names instead of enum ordinal numbers that could change if the enum is reordered.

SQLite has flexible typing: a declared `VARCHAR(100)` does not enforce a 100-character limit by itself. Bean Validation supplies length checks in this application. Inspect the actual DDL rather than assuming Hibernate emits the same column declarations across dialect versions.

`LocalDateTime` does not contain a time zone. Here, the creation callback consistently records UTC; the UI labels it UTC. For a larger application, an `Instant` would make the time-zone meaning more explicit.

# Relationships and design decisions

This version has one table and no foreign keys or joins. School is a label, not a separate school entity. Student ID is text so `001234` stays `001234`. Email is not a unique constraint: this directory has not established that every account must have a distinct email.

The directory does not currently reference the backend's authentication user table. Before integrating it with real accounts, identify the existing user entity and decide whether these fields belong on that entity or in a linked profile. A future mentor relationship should use a real foreign key; a mentor's display name would not be a reliable relationship key. Selecting GUEST categorizes an entry; it does not prove mentor approval or grant permissions.

# SQLite setup

The assignment supplies this disposable development configuration:

```properties
spring.datasource.url=jdbc:sqlite:/volumes/sqlite.db
spring.jpa.hibernate.ddl-auto=create-drop
```

The backend also needs its compatible SQLite JDBC driver and Hibernate SQLite dialect configured. Preserve its established dependency and startup workflow instead of substituting unverified versions. `/volumes/sqlite.db` is an absolute Linux/container path; the running backend must have a writable `/volumes` directory.

`create-drop` creates a fresh schema at startup and drops it on normal shutdown. It is suitable for this exercise but does not preserve records across restarts. For retained data, use reviewed migrations and a non-destructive schema setting.

The assignment's database-removal step is only for a disposable development database. Stop Spring first and verify the database path before removing that specific file. Do not remove a shared account database. No database was removed for this documentation.

# Validate the generated schema

1. Integrate the entity and enum, then start Spring using the backend's documented workflow.
2. In VS Code, install a SQLite viewer and open the database used by that running process.
3. Expand `directory_accounts` and inspect its columns, primary key, and nullability.
4. Run the following read-only queries.

```sql
PRAGMA table_info('directory_accounts');
PRAGMA foreign_key_list('directory_accounts');
SELECT sql FROM sqlite_master
WHERE type = 'table' AND name = 'directory_accounts';
SELECT id, name, student_id, account_type, created_at
FROM directory_accounts;
```

Expected behavior: eight columns, a primary key on `id`, required name/email/type/creation fields, and no foreign keys. After creating an entry through MVC, the last query should return that entry. Compare its values with the browser view.

**Evidence to add:** a screenshot of the actual table structure and a row created through the form. Use synthetic accounts when capturing evidence. No schema screenshot is supplied yet.

Continue to [Backend Implementation & Testing]({{ site.baseurl }}/spt1/backend).
