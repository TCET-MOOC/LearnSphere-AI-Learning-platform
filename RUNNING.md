# Running LearnSphere Locally

This covers everything needed to run the full stack — Spring Boot backend + Angular
frontend — on a fresh machine. For the product/feature overview, see `README.md`.

## Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | **17** | The backend targets Java 17. If your machine's default JDK is newer (e.g. 25), Lombok's annotation processing silently breaks and the build fails with "cannot find symbol" errors on every entity getter/setter. See [JAVA_HOME](#java_home-if-you-have-multiple-jdks) below. |
| Node.js | 18+ | For the Angular CLI / npm |
| PostgreSQL | 14+ | Running locally on the default port 5432 |
| Maven | not required | The repo includes the Maven wrapper (`./mvnw`) |

## 1. Database setup

Create the database the backend expects (name and credentials must match
`src/main/resources/application.properties`):

```bash
psql -U postgres -c "CREATE DATABASE \"MOOC\";"
```

Default configured credentials (edit `application.properties` if yours differ):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/MOOC
spring.datasource.username=postgres
spring.datasource.password=elon3005
```

Tables are created/updated automatically on startup (`spring.jpa.hibernate.ddl-auto=update`)
— there's no manual migration step. Roles (`STUDENT`, `TEACHER`, `ADMIN`) are seeded
automatically on first boot.

## 2. Run the backend

### JAVA_HOME (if you have multiple JDKs)

Check what's installed:

```bash
/usr/libexec/java_home -V
```

If you have a JDK 17 available, point the build at it explicitly:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

(On this project's original dev machine that was
`/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home`.)

### Start it

From the `learnsphere-backend/` directory:

```bash
cd learnsphere-backend
./mvnw spring-boot:run   # on Linux/macOS
.\mvnw.cmd spring-boot:run  # on Windows
```

Or from the repository root:
```powershell
.\learnsphere-backend\mvnw.cmd -f learnsphere-backend/pom.xml spring-boot:run
```

The API comes up on **http://localhost:8080**, base path `/api`. First boot will log
Hibernate `create table` statements — that's expected.

## 3. Run the Payments Microservice

This service handles Razorpay checkouts and webhooks.

### Database setup
By default, the payments microservice uses an in-memory H2 database for local development. If you want to use PostgreSQL, update its `application.properties`.

### Start it
From the `learnsphere-payments-service` directory:

```bash
cd learnsphere-payments-service
..\mvnw.cmd spring-boot:run  # Windows
# or ./mvnw spring-boot:run  # Mac/Linux
```

The microservice comes up on **http://localhost:8081**.
*Note: To test webhooks locally, you must run ngrok (`ngrok http 8081`) and configure the webhook URL in your Razorpay Dashboard. See the microservice's `README.md` for detailed instructions.*

To just compile without running:

```bash
./mvnw compile
```

## 4. Run the frontend

```bash
cd learnSphere/frontend/learnsphere
npm install
npm start          # same as: ng serve
```

The app comes up on **http://localhost:4200** and is already configured
(`src/environments/environment.ts`) to call the backend at `http://localhost:8080/api`.

To build a production bundle: `npm run build`.

## 5. First-time use

There's no pre-seeded login — register an account for each role you want to try:

1. Go to `http://localhost:4200/register`
2. Register once as **STUDENT**, once as **TEACHER**, once as **ADMIN** (the register
   form has a role picker). Each gets routed to its own dashboard on login.
3. As the teacher: create a course (`Teacher → Courses → New`), add a lecture, then
   have an admin account approve it (`Admin → Course approvals`, or the dashboard
   widget) — courses start in `DRAFT`/`PENDING` and aren't visible to students until
   an admin sets them `LIVE`.
4. As the student: enroll in the now-live course and go through it.

## Known simplifications (by design, not bugs)

- **Password reset has no email.** No SMTP is configured, so `POST /api/auth/forgot-password`
  returns the reset token directly in the response instead of emailing it, and the
  frontend auto-fills it on the reset screen.
- **Sentiment analysis and content moderation are keyword-based heuristics**
  (`Service/support/KeywordLists.java`), not real NLP/ML. This is explicitly labeled
  in the admin UI.
- **Uploaded files** (lecture videos, resources) are stored on local disk under the
  `uploads/` directory (configurable via `app.upload.dir`), served back at
  `/api/uploads/**`. There's no cloud storage integration.

## Troubleshooting

- **"cannot find symbol" on every entity getter/setter** → you're building with the
  wrong JDK. See [JAVA_HOME](#java_home-if-you-have-multiple-jdks) above.
- **`Port 8080 was already in use`** → a previous run is still up:
  `lsof -tiTCP:8080 -sTCP:LISTEN | xargs kill -9`
- **Frontend gets CORS errors** → the backend only allows the origin configured in
  `app.cors.allowed-origins` (`application.properties`), default `http://localhost:4200`.
  Update it if you serve the frontend from a different host/port.
- **VS Code shows Java import/package errors but `./mvnw compile` succeeds** → the
  Java language server hasn't reindexed the Maven project. Run
  `Java: Clean Java Language Server Workspace` from the command palette (or reload
  the window) once the extension is active.

## Project layout

```
.
├── pom.xml, mvnw, src/main/java/...   Spring Boot main backend
├── src/main/resources/
│   └── application.properties         DB connection, JWT secret, CORS, upload dir
├── learnsphere-payments-service/      Payments Microservice (Razorpay)
└── learnSphere/frontend/learnsphere/  Angular frontend
    └── src/environments/              apiUrl configuration
```
