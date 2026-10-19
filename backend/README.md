# ⚙️ ChatWave Backend — Spring Boot

> REST APIs and real-time messaging updates for the ChatWave Angular client. See the [main README](../README.md).

## Requirements

| Tool / framework | Version |
| --- | --- |
| Java JDK | **21** |
| Maven | **3.9+** |
| Spring Boot | **3.5.16** |
| Spring Security, Spring WebSocket, Spring Data JPA | Versions managed by Spring Boot |
| JJWT | **0.12.6** |
| H2 | Included; default database |
| MySQL | Optional MySQL Server 8.x |

## Run with H2 (no MySQL setup required)

**Windows PowerShell:**

```powershell
cd backend
$env:JWT_SECRET = "replace-with-a-random-32-byte-or-longer-secret"
mvn clean spring-boot:run
```

**macOS / Linux:**

```bash
cd backend
export JWT_SECRET='replace-with-a-random-32-byte-or-longer-secret'
mvn clean spring-boot:run
```

Backend URL: **http://localhost:8080**. Persistent H2 data is written under `backend/data/` (ignored by Git).

## Optional MySQL configuration

```sql
CREATE DATABASE chatwave CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'chatwave'@'localhost' IDENTIFIED BY 'YOUR_STRONG_PASSWORD';
GRANT ALL PRIVILEGES ON chatwave.* TO 'chatwave'@'localhost';
```

Set variables in **PowerShell**, then start Maven:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/chatwave?useSSL=false&serverTimezone=UTC"
$env:DB_USERNAME = "chatwave"
$env:DB_PASSWORD = "YOUR_STRONG_PASSWORD"
$env:JWT_SECRET = "YOUR_LONG_RANDOM_JWT_SECRET"
$env:CORS_ORIGIN = "http://localhost:4200"
mvn spring-boot:run
```

## Environment variables

| Name | Default | Purpose |
| --- | --- | --- |
| `PORT` | `8080` | Port |
| `DB_URL` | H2 file JDBC URL | JDBC connection |
| `DB_USERNAME` | `sa` | DB username |
| `DB_PASSWORD` | empty | DB password |
| `JWT_SECRET` | insecure development-only value | JWT signing key; replace |
| `CORS_ORIGIN` | `http://localhost:4200` | Angular origin if using original configurable CORS bean |

## API endpoints

| Method | Route | Authentication |
| --- | --- | --- |
| POST | `/api/auth/register` | Public |
| POST | `/api/auth/login` | Public |
| GET, PUT | `/api/me` | JWT |
| GET | `/api/users?q=` | JWT |
| GET, POST | `/api/chats` | JWT |
| GET, POST | `/api/chats/{id}/messages` | JWT |
| POST | `/api/chats/{id}/read` | JWT |

WebSocket STOMP endpoint: `ws://localhost:8080/ws`; subscribe to `/user/queue/events` with `Authorization: Bearer <token>` on STOMP `CONNECT`.

## Build

```bash
cd backend
mvn clean package
```

## Security and production checklist

- Keep Spring Security CORS enabled and allow preflight requests on the public authentication APIs.
- Never commit JWT secrets or MySQL credentials.
- Use HTTPS/WSS for deployment and authenticate WebSocket connections/subscriptions.
- Use production database migrations rather than `ddl-auto: update`.
- Add tests, rate limiting, backups, metrics, and appropriate authentication hardening.
- This service is an MVP and does **not** provide end-to-end encryption.
