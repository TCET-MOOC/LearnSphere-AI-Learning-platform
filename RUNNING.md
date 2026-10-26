# Running LearnSphere Locally

This covers the full stack — Spring Boot backend, payments service, PostgreSQL and the
Angular frontend. For the product/feature overview, see `README.md`.

## Prerequisites

| Tool        | Version      | Notes |
| ----------- | ------------ | ----- |
| Docker      | Desktop with Compose v2 | Required for the one-command stack and for Testcontainers |
| JDK         | **17**       | The backend targets Java 17. |
| Node.js     | **22**       | Angular 21 requires a current Node release. |
| PostgreSQL  | 16+          | Only needed for a bare-metal run; Compose provides it. |
| Maven       | not required | Each service ships its own Maven wrapper. |

## 1. Quick start with Docker Compose

```powershell
Copy-Item .env.example .env
# Edit .env and replace the local-development placeholders. Never commit .env.
docker compose up --build
```

Compose starts PostgreSQL, the backend (`http://localhost:8080`), the payments service
(`http://localhost:8081`) and the frontend (`http://localhost:4200`). All four services
have healthchecks; `docker compose ps` shows `healthy` when the stack is ready.

The default `SPRING_PROFILES_ACTIVE=demo` seeds the documented demo personas. The
password comes from `SEED_PASSWORD` in `.env`; there is no password literal in the
repository. To reproduce the same personas and ids after experimenting, run:

```powershell
# From the repository root, using Git Bash on Windows.
bash scripts/reset-demo.sh
```

The script deletes both databases and the uploads volume, then starts Compose with the
`demo` profile so Flyway migrates and the seeder runs again. It prints the persona table
when the stack is healthy.

If port 5432 is already owned by a local PostgreSQL, set `POSTGRES_PORT=5434` in `.env`
before starting. The backend, payments and frontend host ports can be changed with
`BACKEND_PORT`, `PAYMENTS_PORT` and `FRONTEND_PORT`.

Flyway applies `V1__baseline.sql` to both databases on first start; Hibernate only
validates the schema (`ddl-auto=validate`). There is no `ddl-auto=update` path. To stop
and remove the databases:

```powershell
docker compose down -v --remove-orphans
```

## 2. Bare-metal database setup

Create both databases on PostgreSQL 16+:

```bash
psql -U postgres -c 'CREATE DATABASE "MOOC";'
psql -U postgres -c 'CREATE DATABASE learnsphere_payments;'
```

Both services default to the `local` profile, which is an empty development database.
The `demo` profile adds the PostgreSQL connection from `local` and, for the backend,
the demo seed. `prod` requires the deployment to supply its own datasource and leaves
the schema without demo accounts. Configure the connection through environment
variables; no credentials are stored in the repository.

## 3. Run the backend

From `learnsphere-backend/`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "demo"
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://localhost:5432/MOOC"
$env:SPRING_DATASOURCE_USERNAME = "postgres"
$env:SPRING_DATASOURCE_PASSWORD = "<your-local-postgres-password>"
$env:JWT_SECRET = "<at-least-32-random-characters>"
$env:INTERNAL_API_KEY = "<random-internal-key>"
$env:SEED_PASSWORD = "<8-16 chars, uppercase + digit + special>"
$env:APP_CORS_ALLOWED_ORIGINS = "http://localhost:4200"
.\mvnw.cmd spring-boot:run
```

Use `local` instead of `demo` for an empty database. The `demo` profile refuses to start
unless `SEED_PASSWORD` is set.

On Linux/macOS use `./mvnw spring-boot:run`. The API comes up on
**http://localhost:8080**, base path `/api`. Flyway creates/updates the schema on boot;
there is no manual migration step.

### JAVA_HOME (if you have multiple JDKs)

If JDK 17 is not the default, point the build at it explicitly. On macOS:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

On Windows, set `JAVA_HOME` to the JDK 17 installation directory before running Maven.

## 4. Run the payments service

From `learnsphere-payments-service/`:

```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
$env:DB_URL = "jdbc:postgresql://localhost:5432/learnsphere_payments"
$env:DB_USER = "postgres"
$env:DB_PASSWORD = "<your-local-postgres-password>"
$env:RAZORPAY_KEY_ID = "<your-razorpay-test-key-id>"
$env:RAZORPAY_KEY_SECRET = "<your-razorpay-test-key-secret>"
$env:RAZORPAY_WEBHOOK_SECRET = "<your-razorpay-webhook-secret>"
$env:INTERNAL_API_KEY = "<same-internal-key-as-the-backend>"
$env:MAIN_BACKEND_URL = "http://localhost:8080"
.\mvnw.cmd spring-boot:run
```

On Linux/macOS use `./mvnw spring-boot:run`. The service comes up on
**http://localhost:8081**.

To test webhooks locally, run `ngrok http 8081` and configure the webhook URL in the
Razorpay test dashboard. Never use live Razorpay keys for local development.

To compile without running, use `.\mvnw.cmd compile` (Windows) or `./mvnw compile`.

## 5. Run the frontend

```bash
cd learnSphere/frontend/learnsphere
npm ci
npm start
```

The app comes up on **http://localhost:4200** and is configured to call the backend at
`http://localhost:8080/api`. For a production bundle, run `npm run build`.

## 6. First-time use

The `demo` profile creates the documented personas; the `local` and `prod` profiles
leave these tables empty. Every demo account uses the same `SEED_PASSWORD` value:

| Account | Email | Role | College |
| ------- | ----- | ---- | ------- |
| Admin | `admin@learnsphere.com` | ADMIN | TCET (verified) |
| Teacher | `teacher@learnsphere.com` | TEACHER | TCET (verified) |
| Student | `student@learnsphere.com` | STUDENT | TCET (verified) |
| External student | `external@learnsphere.com` | STUDENT | Sinhgad (verified) |
| Pending teacher | `pending@learnsphere.com` | TEACHER | none; pending affiliation |

The teacher owns two LIVE starter courses. The student is enrolled in `CS201`. The
seeder is idempotent and fixes the insert order, so `scripts/reset-demo.sh` reproduces
the same ids and personas. Never enable the `demo` profile on a shared host.

To try the flows: create or use a teacher account, create a course, approve it as an
admin, then enroll and go through it as a student.

## 7. API docs and errors

With `demo`, `local` or `test` active the backend publishes its contract:

| URL | What it serves |
| --- | --- |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3 document (JSON) |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |

`prod` disables both (`springdoc.api-docs.enabled=false`). CI generates
`learnsphere-backend/target/openapi.json` from the backend tests and uploads it as the
`openapi-document` artifact.

Every backend error is `application/problem+json`:

```json
{
  "type": "https://learnsphere.ai/problems/not-found",
  "title": "Not Found",
  "status": 404,
  "detail": "Course not found with id: 999999",
  "instance": "/api/courses/999999",
  "code": "NOT_FOUND",
  "traceId": "8a4f7f6e-6b1a-4f2e-9a0e-2b6f0f7c1d23"
}
```

Branch on `code`, not `detail`. The `traceId` matches the `X-Trace-Id` response header
and the JSON log line. Validation failures add `errors: [{field, message}]`. New list
endpoints return `{content, page, size, totalElements, totalPages}`; the full list of
codes and pagination rules is in
[`Docs/plan/api-catalogue.md`](Docs/plan/api-catalogue.md#error-contract-p0-09).

## Known simplifications (by design, not bugs)

- **Password reset has no email.** No SMTP is configured, so
  `POST /api/auth/forgot-password` returns the reset token directly in the response
  instead of emailing it, and the frontend auto-fills it on the reset screen. The token
  is not written to the logs.
- **Sentiment analysis and content moderation are keyword-based heuristics**
  (`Service/support/KeywordLists.java`), not real NLP/ML. This is labeled in the admin UI.
- **Uploaded files** are stored on local disk under `APP_UPLOAD_DIR` (default `uploads`),
  served back at `/api/uploads/**`. There is no cloud-storage integration.

## Troubleshooting

- **"cannot find symbol" on every entity getter/setter** — you are building with the
  wrong JDK. Use JDK 17 (see `JAVA_HOME` above).
- **`Port 8080 was already in use`** — stop the previous run, or set `BACKEND_PORT` in
  `.env` to a free port.
- **`Port 5432 was already in use`** — set `POSTGRES_PORT=5434` (or another free port)
  in `.env`.
- **Compose says a required variable is missing** — copy `.env.example` to `.env` and
  fill in `POSTGRES_PASSWORD`, `JWT_SECRET`, `INTERNAL_API_KEY`, `SEED_PASSWORD` and the
  Razorpay test values. These have no insecure fallback.
- **The backend starts with no demo accounts** — `SPRING_PROFILES_ACTIVE` is `local`
  or `prod`. Set it to `demo` and provide `SEED_PASSWORD`.
- **Frontend gets CORS errors** — set `APP_CORS_ALLOWED_ORIGINS` to the frontend origin
  (default `http://localhost:4200`).
- **VS Code shows Java import/package errors but `./mvnw compile` succeeds** — run
  `Java: Clean Java Language Server Workspace` from the command palette.

## Project layout

```
.
├── learnsphere-backend/                 Spring Boot main backend
│   ├── pom.xml, mvnw, .mvn/
│   ├── src/main/java/...
│   └── src/main/resources/
│       ├── application.properties        common config, no secrets
│       ├── application-local.properties  local PostgreSQL profile
│       ├── application-demo.properties   required SEED_PASSWORD for the demo profile
│       └── application-prod.properties   required production env profile
├── learnsphere-payments-service/        Razorpay payments microservice
│   ├── pom.xml, mvnw, .mvn/
│   └── src/main/resources/               same local/demo/test/prod profile split
├── learnSphere/frontend/learnsphere/    Angular frontend
│   └── src/environments/                 apiUrl configuration
├── infra/postgres/init/                 creates learnsphere_payments beside MOOC
├── scripts/reset-demo.sh                deterministic demo reset
├── docker-compose.yml
└── .env.example
```
