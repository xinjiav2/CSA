# Study Tracker REST API (Option A)

This is the stateless Spring Boot backend for the separate GitHub Pages frontend.

## Run

Requirements: Java 21+ and Maven 3.9+.

```bash
cd spring-backend
mvn spring-boot:run
```

The SQLite database is created at `data/study-tracker.db`. To use the assignment's deployment path, set `SQLITE_URL=jdbc:sqlite:/volumes/sqlite.db`. This prototype uses `ddl-auto=create-drop`, so the schema is recreated when the application restarts.

Open `http://localhost:8585/api/study-records`. The Postman collection in `postman/` exercises all five CRUD routes.
