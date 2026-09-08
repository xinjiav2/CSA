---
layout: post
title: "Study Tracker: POJO to REST API to GitHub Pages"
date: 2026-09-02
categories: [CSA, Spring, Full-Stack]
---

## The idea and architecture

My data object is a **StudyRecord**. It solves a practical problem: a student needs one place to record which AP CSA concept they practiced, how long they worked, their progress, and their confidence. The object can later support OCS features such as class progress summaries or recommended review topics.

I chose **Option A** because a stateless REST API keeps my portfolio interface independent from the backend. The same JSON API could later serve another web page or a mobile client.

```text
GitHub Pages UI
      | fetch + JSON
      v
StudyRecordController (/api/study-records)
      |
StudyRecordRepository (JpaRepository)
      |
StudyRecord POJO/JPA Entity -> SQLite
```

The implementation is linked here: [frontend]({{ '/study-records/' | relative_url }}), [backend source](https://github.com/xinjiav2/CSA/tree/main/spring-backend), and [Postman collection](https://github.com/xinjiav2/CSA/blob/main/spring-backend/postman/Study-Tracker-Option-A.postman_collection.json).

## Phase 1: POJO design and AP CSA

The entity stores identity, learning content, progress, measurements, reflection, and audit timestamps.

```java
@Data
@NoArgsConstructor
@Entity
@Table(name = "study_records")
public class StudyRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String topic;
    private String subtopic;
    private String studentName;
    @Enumerated(EnumType.STRING)
    private StudyStatus status = StudyStatus.PLANNED;
    private int minutesStudied;
    private int confidence = 1;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

Lombok's `@Data` generates accessors such as `getTopic()`, mutators such as `setTopic(String)`, plus `toString()`, `equals(Object)`, and `hashCode()`. It also generates a required-arguments constructor; `@NoArgsConstructor` supplies the empty constructor JPA needs. These are the same class behaviors I write manually in AP CSA: private instance variables preserve encapsulation, getters provide controlled reading, setters change object state, `toString` creates a readable representation, and `equals` defines logical equality. `hashCode` keeps equal objects consistent in hash-based collections.

**Screenshot evidence to add:** the open `StudyRecord.java` file in the IDE and the IDE's *delombok* or generated-code view showing these methods.

## Phase 2: database schema and persistence

JPA maps the POJO to the SQLite table `study_records`:

| Java field | SQLite role | Decision |
|---|---|---|
| `id` (`Long`) | integer primary key | Database-generated stable identity |
| `topic`, `subtopic`, `studentName` | required text | Required and length-limited with validation |
| `status` (`StudyStatus`) | required text | Readable enum values instead of fragile numbers |
| `minutesStudied` (`int`) | required integer | Cannot be negative |
| `confidence` (`int`) | required integer | Validated from 1 through 5 |
| `notes` | optional text | Limited to 500 characters |
| `createdAt`, `updatedAt` | required timestamps | Filled automatically by lifecycle callbacks |

`@Entity` makes the class persistent, `@Id` identifies its primary key, `@GeneratedValue` delegates ID creation, and `@Column` supplies constraints. `@PrePersist` and `@PreUpdate` keep timestamps accurate. This first version intentionally has no foreign key: one well-designed object is the assignment scope, while a future `Student` entity could create a many-to-one relationship.

The connection string defaults to `jdbc:sqlite:./data/study-tracker.db`; deployment can set `SQLITE_URL=jdbc:sqlite:/volumes/sqlite.db` without changing code. This prototype follows the assignment's `ddl-auto=create-drop` setting, so restarting recreates the table and removes its rows.

**Screenshot evidence to add:** open `data/study-tracker.db` after the backend starts and capture the `study_records` columns in the SQLite extension.

## Phase 3: REST API and testing

`StudyRecordRepository extends JpaRepository<StudyRecord, Long>`, so Spring Data generates CRUD operations such as `findAll`, `findById`, `save`, and `deleteById`. Method names also generate custom queries for student and status filters.

| Method | Route | Success | Edge case |
|---|---|---:|---:|
| GET | `/api/study-records` | 200 JSON array | Empty array when none exist |
| GET | `/api/study-records/{id}` | 200 JSON object | 404 for unknown ID |
| POST | `/api/study-records` | 201 JSON object | 400 with field errors |
| PUT | `/api/study-records/{id}` | 200 JSON object | 400 invalid / 404 unknown ID |
| DELETE | `/api/study-records/{id}` | 204 empty body | 404 for unknown ID |

Example POST body:

```json
{
  "topic": "Unit 2: Using Objects",
  "subtopic": "String methods",
  "studentName": "Zhengji Li",
  "status": "IN_PROGRESS",
  "minutesStudied": 35,
  "confidence": 3,
  "notes": "Review substring indexes"
}
```

The exported Postman collection contains all five requests, captures the new ID after POST, and checks response codes. Automated controller tests also cover successful reads and creation, validation failure, and missing IDs.

**Screenshot evidence to add:** run the collection in order and capture the five green Postman results.

## Phase 4: GitHub Pages frontend integration

The dashboard loads records with `GET` on page load, renders them in a table, sends `POST` for a new form, fills the same form for editing with `PUT`, and uses `DELETE` after confirmation. `async`/`await` keeps the flow readable, and non-success responses become visible messages instead of silent failures.

The backend address lives in one file:

```javascript
window.STUDY_API_BASE_URL = "http://localhost:8585/api/study-records";
```

This matters because deployment only requires changing one value. The frontend and API have different origins, so `@CrossOrigin` explicitly allows local Jekyll and `https://xinjiav2.github.io`. The Order 1 security chain matches `/api/**`, uses stateless sessions, enables CORS, and disables CSRF because this API does not authenticate through browser session cookies.

**Screenshot evidence to add:** the dashboard with at least three records and one successful create or update message.

## Reflection and N@tM direction

The most important design choice was storing `status` as an enum and confidence as a validated number. Both prevent inconsistent data while keeping the UI mostly dropdowns and buttons. Separating the frontend also made the boundary obvious: Java owns validation and persistence; JavaScript owns interaction and presentation; JSON is their contract.

For an N@tM version, I would add authentication and associate each record with the signed-in OCS user. Aggregated minutes and confidence could then recommend a student's weakest topics. I would also replace open write access with bearer-token authorization before public deployment.

This project reinforces the same AP CSA object model at a larger scale: state lives in fields, behavior is accessed through methods, enum values constrain state, collections hold many objects, and abstraction lets the rest of the system use a repository without writing SQL.
