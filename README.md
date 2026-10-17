# SuperMarket

A multi-vendor e-commerce marketplace. Customers browse and buy from many independent vendors;
vendors manage their own catalog and orders; admins approve vendors and oversee the platform.

| Layer     | Technology                                                        |
|-----------|-------------------------------------------------------------------|
| Backend   | Java 21, Spring Boot 4.1.x, Spring Data JPA, Flyway, Maven        |
| Database  | MySQL 9.7 LTS                                                     |
| Frontend  | React (JavaScript), Vite, React Router, TanStack Query, Axios     |
| Dev tools | Docker Compose (MySQL, Mailpit), ESLint, Prettier, JUnit, Vitest  |

## Repository layout

```
supermarket/
├── backend/    Spring Boot application (open in IntelliJ IDEA)
├── frontend/   React application (open in VS Code)
├── docs/       Architecture, conventions, roadmap, decisions
└── docker-compose.yml
```

## Prerequisites (macOS)

- JDK 21 (`java -version`)
- Maven 3.9+ (IntelliJ's bundled Maven is fine)
- Node.js 22 LTS or newer (`node -v`)
- Docker Desktop

## Run locally

```bash
# 1. Start MySQL (and Mailpit)
cp .env.example .env
docker compose up -d

# 2. Backend  -> http://localhost:8080
cd backend
mvn spring-boot:run          # or run SuperMarketApplication from IntelliJ

# 3. Frontend -> http://localhost:5173
cd ../frontend
npm install
npm run dev
```

Verify: open http://localhost:5173 - the home page shows **Backend is UP**.

### Backend settings (IntelliJ run configuration)

Pass secrets through the run configuration (Modify options -> Environment variables), never in files:

| Variable | Purpose |
|----------|---------|
| `DB_USERNAME`, `DB_PASSWORD` | MySQL login (dev defaults: `supermarket` / `supermarket`) |
| `JWT_SECRET` | Token signing key, 32+ characters (dev has a fallback; required in prod) |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | Creates the first admin on startup if missing (password 12+ characters) |

Do not tick "Store as project file" for that run configuration, or the values end up in the repo.
API docs (dev only): http://localhost:8080/swagger-ui.html

## Quality checks

```bash
cd backend  && mvn verify                 # compile + unit tests
cd frontend && npm run lint && npm test && npm run build
```

## Documentation

- [Architecture](docs/ARCHITECTURE.md)
- [Coding conventions](docs/CODING_STANDARDS.md)
- [Roadmap and phases](docs/ROADMAP.md)
- [Git workflow](docs/GIT_WORKFLOW.md)
- [Project context for new chat sessions](docs/PROJECT_CONTEXT.md)
- [Decision records](docs/adr/)
