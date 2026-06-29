# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

OnlyFriends (趣聚平台) is a social activity platform that connects people through shared interests. Users create, discover, and join real-world activities (sports, hiking, board games, study groups, volunteering, city exploration), form interest-based teams with gamified scoreboards, and communicate via real-time WebSocket chat.

**Client surface**: WeChat miniprogram (end users) + vanilla HTML/JS admin dev console (development/testing). The design doc also references a Vue 3 + Element Plus admin web (not yet implemented in this repo).

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Backend framework | Spring Boot | 3.2.0 |
| Microservices | Spring Cloud (Gateway, OpenFeign) | 2023.0.0 |
| Service registry/config | Nacos | 2.3.x (optional, disabled for local dev) |
| API gateway | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus | 3.5.5 |
| Auth | Spring Security + JWT (jjwt) | 0.12.3 |
| Database | MySQL | 8.0 |
| Cache / session / queue | Redis | 7 |
| File storage | MinIO (or local filesystem) | — |
| IM real-time | Spring WebSocket + STOMP | — |
| Scheduled tasks | Spring Task (`@Scheduled`) | — |
| AI service (planned) | FastAPI (Python) + LLM APIs | 0.110+ |
| Frontend miniprogram | WeChat miniprogram + Vant Weapp | — |
| Infrastructure | Docker Compose | — |
| Java version | JDK 17 | — |

## Common Commands

### Infrastructure (Docker)
```bash
# From backend/ directory
docker compose up -d mysql redis           # Start MySQL 8.0 + Redis 7
docker compose --profile infra up -d        # Also start Nacos + MinIO
docker compose down                         # Stop all
docker compose down -v                      # Stop and delete data volumes
```

### Database Initialization
```bash
# From backend/ — initialize all databases and tables
Get-Content .\sql\init-all.sql -Encoding UTF8 | docker exec -i ququ-mysql mysql -uroot -pququ_root_password --default-character-set=utf8mb4

# Optional demo data
Get-Content .\sql\demo-data.sql -Encoding UTF8 | docker exec -i ququ-mysql mysql -uroot -pququ_root_password --default-character-set=utf8mb4
```

### Build & Test
```bash
# From backend/
mvn -DskipTests package          # Full build, skip tests
mvn test                         # Run all tests
mvn -pl ququ-user-service test   # Run tests for a single module (use -pl <module>)
```

### Start Backend Services (PowerShell)
```bash
# From repo root
.\scripts\start-all.ps1                    # Each service in its own window
.\scripts\start-all.ps1 -Background        # All background, logs → backend/logs/
.\scripts\start-all.ps1 -WithAi            # Include AI service on port 8001
.\scripts\stop-all.ps1                     # Stop background services
```

### Start Frontend Dev Admin
```bash
# From repo root
.\scripts\start-frontend.ps1               # Opens on http://127.0.0.1:5173
.\scripts\start-frontend.ps1 -Background
```

### Swagger UI (when services are running)
```
http://localhost:8081/swagger-ui/index.html  # user-service
http://localhost:8082/swagger-ui/index.html  # activity-service
http://localhost:8083/swagger-ui/index.html  # social-service
http://localhost:8084/swagger-ui/index.html  # im-service
http://localhost:8085/swagger-ui/index.html  # admin-service
```

### Smoke Tests (PowerShell)
```bash
# User service: register → activate → login → profile → merchant
.\backend\test-scripts\user-service\smoke-user-service.ps1
.\backend\test-scripts\user-service\smoke-user-service.ps1 -ActivationToken "token-from-logs"

# Activity service (requires user access token)
.\backend\test-scripts\activity-service\smoke-activity-service.ps1 -AccessToken "..."
```

## Architecture

### Design Rationale

The platform was split from a monolith into microservices for three reasons: (1) **team parallel development** — multiple students work on different services without Git conflicts; (2) **fault isolation** — a bug in one module doesn't bring down the whole platform; (3) **technology heterogeneity** — the AI service needs Python (LangChain, OpenAI SDK) while the rest uses Java.

Services share a **single MySQL instance with logical database separation** (one database per service). Cross-database queries are forbidden — services must use OpenFeign to fetch data from each other. This avoids distributed-transaction complexity while keeping service boundaries clean.

### Module Map

```
                    ┌─────────────────────┐
                    │   ququ-gateway      │  port 8080
                    │   (Spring Cloud     │  JWT auth, rate limit,
                    │    Gateway)         │  route by path prefix
                    └──────┬──────────────┘
                           │ routes /api/v1/*
        ┌──────────────────┼──────────────────────────────┐
        ▼                  ▼                              ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐
│ user-service │  │  activity-   │  │  social-service      │
│    :8081     │  │  service     │  │     :8083            │
│              │  │    :8082     │  │                      │
│ auth, users, │  │ activities,  │  │ follows, friends,    │
│ merchant     │  │ reviews,     │  │ teams, votes, albums,│
│ apply        │  │ check-in,    │  │ files, scoreboards   │
│              │  │ waitlist     │  │                      │
└──────┬───────┘  └──────┬───────┘  └──────────┬───────────┘
       │                 │                     │
       │    Feign        │    Feign            │    Feign
       │◄────────────────┤◄────────────────────┤
       │  /internal/users│  /internal/social   │  /internal/social
       │                 │  /internal/users    │  /internal/users
       │                 │                     │
       ▼                 ▼                     ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐
│ admin-service│  │  im-service  │  │  ai-service          │
│    :8085     │  │    :8084     │  │    :8001 (optional)  │
│              │  │              │  │                      │
│ admin auth,  │  │ private +    │  │ AI mock / planned:   │
│ user ban,    │  │ group chat,  │  │ activity review,     │
│ activity     │  │ WebSocket    │  │ planning generation, │
│ review       │  │ STOMP        │  │ image classification │
└──────────────┘  └──────────────┘  └──────────────────────┘
```

All services depend on `ququ-common` (shared DTOs, `Result<T>`, `JwtUtil`, `BizException`, `GlobalExceptionHandler`, `FileStorageService`).

### Request Flow

1. Client → **Gateway (:8080)** → `JwtAuthGlobalFilter` validates JWT (unless whitelisted path)
2. Gateway injects `X-User-Id`, `X-User-Type`, `X-User-Role`, `X-Nickname` headers
3. Gateway routes to the correct service by path prefix (see `application.yml` routes)
4. Each service has its own `SecurityConfig` + `JwtAuthenticationFilter` that reads these headers OR parses the token directly
5. Services communicate via OpenFeign clients calling `/internal/**` endpoints (blocked at gateway — returns 403 for external requests)

### Gateway Route Table

| Path Prefix | Target Service | Auth Required |
|-------------|---------------|---------------|
| `/api/v1/auth/**` | user-service :8081 | No (whitelist) |
| `/api/v1/users/**`, `/api/v1/merchant/**` | user-service :8081 | Yes |
| `/api/v1/activities/**`, `/api/v1/ai/**`, `/api/v1/notifications/**` | activity-service :8082 | Mixed (GET activities public) |
| `/api/v1/follows/**`, `/api/v1/friends/**`, `/api/v1/teams/**` | social-service :8083 | Yes |
| `/api/v1/im/**` | im-service :8084 | Yes |
| `/ws/im/**` | im-service :8084 (WebSocket) | Yes (handshake) |
| `/api/v1/admin/**` | admin-service :8085 | Yes + ROLE_ADMIN check |

The gateway blocks `/internal/**` paths entirely from external access.

### Auth & Security

- **JWT**: access tokens (default 2h expiry) and refresh tokens (default 7d). Configured via `jwt.secret` (min 32 bytes), `jwt.access-token-expire`, and `jwt.refresh-token-expire`.
- **Gateway whitelist** (`JwtAuthGlobalFilter`): registration, login, activation, refresh, admin login, Swagger/OpenAPI paths. GET requests to `/api/v1/activities`, `/api/v1/activities/*`, `/api/v1/activities/templates`, `/api/v1/activities/tags` are also public.
- **User types**: 0=normal user (ROLE_USER), 1=merchant (ROLE_MERCHANT), 9=admin (ROLE_ADMIN). The gateway derives role from userType unless the token carries an explicit `role` claim.
- **Team-level permissions** (leader/admin/member) are enforced in business logic by checking `team_member.role`, not via Spring Security.
- **Password encoding**: BCrypt(12) in both user-service and admin-service.
- **Rate limiting**: `RedisRateLimitGlobalFilter` (order -110). Disabled by default; enable with `gateway.rate-limit.enabled=true`.
- **Check-in security**: QR codes embed `{activityId, timestamp, HMAC-SHA256 signature}`. Server validates signature and time window. Optional location verification checks user is within the configured radius (default 500m) of the activity location.

### Database Design

Each service owns a separate MySQL database (~30 tables total):

| Database | Tables |
|----------|--------|
| `ququ_user` | `user`, `merchant_info`, `merchant_apply`, `user_ban_record` |
| `ququ_activity` | `activity`, `activity_template`, `activity_tag`, `activity_review_record`, `activity_registration`, `activity_waitlist`, `activity_checkin`, `activity_summary`, `activity_comment`, `activity_offline_record`, `notification` |
| `ququ_social` | `user_follow`, `friend_relation`, `friend_apply`, `team`, `team_member`, `team_join_apply`, `team_announcement`, `team_album`, `team_file`, `team_score_log`, `team_vote`, `team_vote_option`, `team_vote_record`, `team_disable_record`, `team_admin_operation_log` |
| `ququ_im` | `im_conversation`, `im_message`, `im_group_message`, `im_conversation_read` |
| `ququ_admin` | `admin_user`, `admin_operation_log` |

All tables use `utf8mb4` charset. MyBatis-Plus handles logical deletes via the `deleted` column (0=normal, 1=deleted). Enum-like status fields use TINYINT with documented values in column comments.

### Activity State Machine

```
 ┌──────┐    submit     ┌──────────┐   AI/manual approve   ┌──────────┐
 │ 草稿  │ ──────────→  │  审核中   │ ────────────────────→ │  已发布   │
 │ (0)  │              │   (1)    │                        │   (2)    │
 └──────┘              └──────────┘                        └────┬─────┘
      ↑                    ↑    ↓ reject                       │ reg opens
      │   modify           │    └───────→ ┌──────────┐         ▼
      │  resubmit          │              │ 审核驳回   │    ┌──────────┐
      └────────────────────┘              │   (8)    │    │  报名中   │
                                          └──────────┘    │   (3)    │
                                                          └────┬─────┘
      ┌──────────┐  admin offline  ┌──────────┐               │ reg closes
      │  已下架   │ ←───────────── │  报名中   │               ▼
      │   (7)    │ ──────────────→ │   (3)    │          ┌──────────┐
      └──────────┘  admin restore  └──────────┘          │ 报名截止  │
                                                          │   (4)    │
                                                          └────┬─────┘
                                                               │ activity starts
      ┌──────────┐  activity ends  ┌──────────┐               ▼
      │  已结束   │ ←────────────── │  进行中   │ ←──────────────┘
      │   (6)    │                 │   (5)    │
      └──────────┘                 └──────────┘
```

Status is advanced automatically by a `@Scheduled` Spring Task that runs every minute, comparing `start_time`, `end_time`, and `reg_deadline` against the current time.

### AI Service Architecture (Planned/Design Doc)

The AI service is designed as a Python FastAPI application on port 8001, called internally by the activity service. Three capabilities:

1. **Content Safety Review** — Three-layer pipeline: (1) keyword blacklist rule engine → immediate reject if matched; (2) LLM semantic review with structured JSON output `{result, risk_level, risk_categories, reason, confidence}`; (3) manual admin review as fallback for uncertain cases. Results cached in Redis (key = MD5 of title+description, TTL 24h). Decision matrix: `pass + confidence≥0.7 + max≤50` → auto-publish; `reject + confidence≥0.9` → auto-reject; everything else → manual review.

2. **Activity Planning Generation** — Takes user input (theme, type, city, expected_people) and returns a complete activity draft (title, description, tags, duration, fee, safety_notes, preparation_checklist). Supports SSE streaming for progressive rendering.

3. **Image Classification** — Categorizes activity photos into: 合影, 场地, 过程记录, 物资, 成果展示. Results are advisory only — the user confirms before finalizing the activity summary.

Currently the repo has `ququ-ai-service` as a Java Spring Boot module with an `AiMockController` providing stub responses. The architecture doc describes the full Python implementation plan.

### Inter-Service Communication (Feign Contracts)

All internal APIs are under `/internal/**` and blocked at the gateway. The architecture doc defines these Feign contracts:

**User Service exposes** (called by activity, social, IM, admin services):
```
GET  /internal/users/batch?ids=1,2,3        → List<UserBasicDTO>
GET  /internal/users/{id}/valid             → Boolean
GET  /internal/users/{id}/credit            → Integer
POST /internal/users/{id}/credit/deduct?amount=N → void
```

**Social Service exposes** (called by IM service, admin service):
```
GET /internal/social/friends/check?userIdA=X&userIdB=Y   → Boolean
GET /internal/social/teams/{teamId}/members/check?userId= → Boolean
GET /internal/social/users/{userId}/team-ids              → List<Long>
GET /internal/social/teams/{teamId}/member-ids            → List<Long>
```

**Activity Service calls AI Service** (Python, fixed URL `http://localhost:8001`):
```
POST /ai/review-content   → AiReviewResultDTO
POST /ai/plan-activity    → AiActivityPlanDTO
POST /ai/classify-images  → AiImageClassifyResultDTO
```

### Key Business Flows

**Activity lifecycle**: Create (manual / AI-assisted / from template / clone) → submit for review → AI auto-review or manual review → publish → registration opens → registration closes → activity starts (in-progress) → ends → summary + comments.

**Waitlist (waiting queue)**: When an activity is full, users join a Redis-backed waitlist ordered by queue position. If someone cancels, the first person in queue gets a notification with a 30-minute confirmation window. If they don't confirm in time, the slot passes to the next person. A `@Scheduled` task checks for expired confirmations every minute.

**Team scoring**: Members earn points for participation (activity check-in +10, post dynamic +2, dynamic featured +5, upload photo +1, vote +1). Leaderboard is queryable per team.

**IM flow**: Client connects WebSocket at `ws://host/ws/im?token={JWT}` → server registers online status in Redis (`user:ws:online:{userId}`, TTL 60s, heartbeat every 30s) → subscribes to `/user/{userId}/queue/messages` (private) and `/topic/team/{teamId}` (group) → messages persist to MySQL synchronously → online users get real-time push, offline users pull history via REST. Messages can be recalled within 2 minutes.

### Redis Key Conventions (Design Doc)

| Key Pattern | Service | Purpose | TTL |
|-------------|---------|---------|-----|
| `user:token:refresh:{userId}` | user | Refresh token store | 7d |
| `user:activate:{token}` | user | Email activation token | 24h |
| `activity:list:cache:{hash}` | activity | Activity list cache | 5min |
| `activity:detail:{id}` | activity | Activity detail cache | 10min |
| `activity:review:cache:{md5}` | activity | AI review result cache | 24h |
| `activity:waitlist:lock:{id}` | activity | Waitlist distributed lock | 5s |
| `user:ws:online:{userId}` | IM | User online status | 60s (heartbeat renew) |
| `im:unread:{convId}:{userId}` | IM | Unread message count | persistent |

### Unified Response Format

All APIs return `{"code": 200, "message": "success", "data": {...}, "timestamp": 1700000000000}`.

Key error codes beyond HTTP standard codes:

| Code | Meaning |
|------|---------|
| 1001 | User not found |
| 1002 | Account banned |
| 1003 | Account not activated |
| 1004 | Email already exists |
| 1005 | Nickname already taken |
| 1006 | Wrong password |
| 1007 | Token invalid/expired |
| 1101 | Merchant application pending |
| 1102 | Already a merchant |
| 2001-2004 | Activity errors (not found, full, already registered, registration closed) |
| 3001-3003 | Team errors (not found, full, already joined) |

### Key Files to Know

| File | Purpose |
|------|---------|
| `ququ-common/.../Result.java` | Unified API response `{code, message, data, timestamp}` |
| `ququ-common/.../ResultCode.java` | Standard error codes |
| `ququ-common/.../BizException.java` | Business logic exception with code |
| `ququ-common/.../GlobalExceptionHandler.java` | Maps exceptions to HTTP status codes |
| `ququ-common/.../JwtUtil.java` | JWT generation/validation shared by services |
| `ququ-gateway/.../JwtAuthGlobalFilter.java` | Gateway-level JWT enforcement (order -100) |
| `ququ-gateway/.../RedisRateLimitGlobalFilter.java` | Rate limiting (order -110, disabled by default) |
| `ququ-gateway/.../security/GatewayJwtProvider.java` | Gateway-specific JWT validation and claim extraction |
| `backend/sql/init-all.sql` | Complete DDL across all 5 databases + default admin + activity templates |
| `backend/趣聚平台系统架构与实现方案_v3.md` | Full architecture design doc (4148 lines) — service specs, API field definitions, Feign contracts, frontend-to-API mappings, deployment guide |

### Configuration

- Each service uses `application.yml` for defaults and `application-example.yml` as a template for sensitive values.
- Sensitive config (DB passwords, JWT secret, SMTP, MinIO credentials) is injected via environment variables — never hardcoded in committed YAML files.
- Nacos service discovery is optional — disabled by default for local development (`NACOS_ENABLED: false`). The gateway falls back to hardcoded `localhost` URIs.
- File storage supports local filesystem and MinIO, configured via `app.storage.type`.
- The `.gitignore` blocks `application-dev.yml` and `application-local.yml` globally, with explicit exceptions for `ququ-user-service` and `ququ-activity-service` dev configs (which use env-var placeholders, not real secrets).

### Frontend

- **Admin dev console** (`frontend/`): Static HTML/CSS/JS served by a trivial Node.js HTTP server (`server.js`). Provides admin dashboard with user management, activity review, team management, test data seeding, and raw API debugging. Connects to gateway at `http://localhost:8080/api/v1`.
- **WeChat miniprogram** (`frontend/miniprogram/`): Full user-facing app with pages covering auth (login/register), home feed with recommend/latest/nearby tabs, activity lifecycle (create/detail/map/checkin/comment/summary), social (follows/friends), teams (detail/album/files/votes/members), IM chat (conversations/chat), and user profile (edit/notifications/merchant-apply).

### Testing

- Java tests use JUnit 5 + AssertJ, annotated with `@SpringBootTest` and `@ActiveProfiles("test")`.
- Test config files (`application-test.yml`) configure in-memory H2 or test-specific properties.
- PowerShell smoke test scripts under `backend/test-scripts/` exercise full API flows against running services.
- Test coverage reports go to `test-results/`, `surefire-reports/`.

### AI Service Workflow Constraint

The AI service (`ququ-ai-service`) uses a Python virtual environment managed by the user. When working on AI service code:
- **Do NOT run Python tests or start the AI service** after writing code. The virtual environment exists only in the user's terminal.
- **Instead, tell the user the commands to run**, e.g.: `cd backend/ququ-ai-service && source venv/bin/activate && uvicorn main:app --reload --port 8001` or `pytest tests/`.
- Java services and infrastructure commands (Docker, Maven) can still be executed as usual.

### Development Workflow

1. Start Docker infrastructure: `docker compose up -d mysql redis`
2. Initialize databases: pipe `sql/init-all.sql` into the MySQL container
3. Start backend services: `.\scripts\start-all.ps1 -Background`
4. Start frontend: `.\scripts\start-frontend.ps1`
5. Open `http://127.0.0.1:5173` for the admin console
6. Register a user, activate via log token, then log in to get an access token
7. Default admin credentials: `admin` / `Admin123456`
8. Service startup order matters: user-service first (others depend on it), gateway last
