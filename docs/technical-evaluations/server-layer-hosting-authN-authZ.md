eiifcbnbckgduhlftukkjrerjclbevjlevggenvrbrru
# Server Hosting & Authentication/Authorization Strategy

## Decision Summary

| Aspect | Choice |
|--------|--------|
| Hosting | Fly.io (Docker containers) |
| Database | Fly Postgres (private network) |
| AuthN | API key in `Authorization: Bearer <key>` header |
| AuthZ | Server-side key validation against hashed allow-list |
| Secret storage | Fly.io encrypted secrets (injected as env vars) |
| Estimated cost | $0–15/month for personal use |

---

## Why Fly.io

### Alternatives considered

| Option | Monthly cost | Verdict |
|--------|-------------|---------|
| AWS EKS | ~$73+ (control plane alone) | Overkill — cluster management for a single container |
| AWS ECS Fargate + ALB | ~$26+ | Cheaper but ALB adds $16/month fixed cost |
| AWS App Runner | ~$5–15 | Good option, but less generous free tier than Fly |
| Single EC2 instance | ~$3.50–8 | Cheapest AWS, but manual ops (patching, restarts) |
| **Fly.io** | **$0–15** | **Simplest DX, scale-to-zero, built-in secrets, private networking** |

### Why Fly wins for this use case

1. **Push a Dockerfile, get a URL** — no VPC config, no ingress controllers, no kubectl
2. **Scale to zero** — no traffic = no compute cost
3. **Built-in Postgres** — private `.internal` network, not internet-exposed
4. **Encrypted secrets** — no need for HashiCorp Vault or AWS Secrets Manager
5. **Automatic TLS** — HTTPS on a custom domain with zero config
6. **Low operational overhead** — deploy with `fly deploy`, done

---

## Architecture Overview

```
┌──────────────────────────────────────────────────────────────────────┐
│                         INTERNET                                      │
└─────────────────────────────┬────────────────────────────────────────┘
                              │ HTTPS (TLS terminated by Fly edge)
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Fly.io Edge (automatic TLS)                                         │
│  - Terminates HTTPS                                                  │
│  - Forwards plaintext to container over internal encrypted channel    │
└─────────────────────────────┬────────────────────────────────────────┘
                              │
                              ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Play Framework Container (your app)                                 │
│                                                                      │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  ApiKeyFilter                                                  │  │
│  │  - Extracts Bearer token from Authorization header             │  │
│  │  - SHA-256 hashes the token                                    │  │
│  │  - Compares against VALID_API_KEY_HASHES env var               │  │
│  │  - 401 Unauthorized if no match                                │  │
│  └───────────────────────────────┬────────────────────────────────┘  │
│                                  │ (authenticated request)            │
│                                  ▼                                    │
│  ┌────────────────────────────────────────────────────────────────┐  │
│  │  Controllers → Services → Repositories                         │  │
│  └───────────────────────────────┬────────────────────────────────┘  │
│                                  │                                    │
└──────────────────────────────────┼────────────────────────────────────┘
                                   │ Private network (.internal DNS)
                                   ▼
┌──────────────────────────────────────────────────────────────────────┐
│  Fly Postgres                                                        │
│  - NOT internet-exposed                                              │
│  - Only reachable from containers in the same Fly org                │
│  - Connection string injected via Fly secret                         │
└──────────────────────────────────────────────────────────────────────┘
```

---

## Authentication & Authorization Flow

### AuthN: "Who are you?"

The client proves identity by presenting a pre-shared API key.

### AuthZ: "Are you allowed?"

The server confirms the key hash exists in its allow-list. All valid keys grant full access (single-role for now).

### Request lifecycle

```
Client (Ionic app)                         Fly.io / Play Server
      │                                           │
      │  POST /api/v1/customers                   │
      │  Authorization: Bearer <raw-api-key>      │
      │  Content-Type: application/json           │
      │──────────────────────────────────────────▶│
      │                                           │
      │                           ┌───────────────┤
      │                           │ ApiKeyFilter   │
      │                           │               │
      │                           │ 1. Extract Bearer token
      │                           │ 2. SHA-256(token)
      │                           │ 3. Lookup hash in VALID_API_KEY_HASHES
      │                           │               │
      │                           │  Match?       │
      │                           │  ├─ YES → forward to controller
      │                           │  └─ NO  → 401 Unauthorized
      │                           └───────────────┤
      │                                           │
      │  200 OK / 401 Unauthorized                │
      │◀──────────────────────────────────────────│
      │                                           │
```

### Key generation and distribution

```
Developer Machine (you)
      │
      │  1. Generate random key:
      │     $ openssl rand -base64 32
      │     → "x7Kj2mP9qR..."
      │
      │  2. Hash it:
      │     $ echo -n "x7Kj2mP9qR..." | shasum -a 256
      │     → "a1b2c3d4e5..."
      │
      │  3. Store hash in Fly:
      │     $ fly secrets set VALID_API_KEY_HASHES="a1b2c3d4e5..."
      │
      │  4. Store raw key in your app config (mobile/web):
      │     Environment variable or secure storage on device
      │
      ▼
   Raw key goes to CLIENT only
   Hash goes to SERVER only
   (server never stores the raw key)
```

---

## Secrets Management

### Where secrets live

| Secret | Stored where | Format |
|--------|-------------|--------|
| API key hashes | Fly encrypted secrets → env var `VALID_API_KEY_HASHES` | Comma-separated SHA-256 hashes |
| Database URL | Fly encrypted secrets → env var `DATABASE_URL` | `postgres://user:pass@db.internal:5432/dbname` |
| Raw API key | Client device (Keychain / secure storage) | Base64-encoded 256-bit value |

### Fly.io secret guarantees

```
┌──────────────────────────────────────────────┐
│  Fly Secret Store                            │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │ Encrypted at rest (AES-256)           │  │
│  │                                        │  │
│  │ VALID_API_KEY_HASHES = ************   │  │
│  │ DATABASE_URL          = ************   │  │
│  └──────────────────┬─────────────────────┘  │
│                     │                        │
│         Injected at container boot           │
│         (encrypted in transit)               │
│                     │                        │
│                     ▼                        │
│  ┌────────────────────────────────────────┐  │
│  │ Container runtime                      │  │
│  │ env: VALID_API_KEY_HASHES="a1b2..."   │  │
│  │ env: DATABASE_URL="postgres://..."     │  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘

Secrets are:
  ✓ Encrypted at rest
  ✓ Encrypted in transit to container
  ✓ Never baked into Docker image layers
  ✓ Redacted from `fly logs` output
  ✓ Scoped to your app only
```

---

## Implementation Reference

### Play filter (Scala 3)

```scala
import java.security.MessageDigest
import javax.inject.Inject
import play.api.libs.json.Json
import play.api.mvc._
import scala.concurrent.ExecutionContext

class ApiKeyFilter @Inject()(implicit ec: ExecutionContext) extends EssentialFilter {
  private val validKeyHashes: Set[String] =
    sys.env("VALID_API_KEY_HASHES").split(",").toSet

  override def apply(next: EssentialAction) = EssentialAction { request =>
    request.headers.get("Authorization") match {
      case Some(s"Bearer $key") if validKeyHashes.contains(sha256(key)) =>
        next(request)
      case _ =>
        Accumulator.done(Results.Unauthorized(Json.obj("error" -> "Invalid API key")))
    }
  }

  private def sha256(s: String): String =
    MessageDigest.getInstance("SHA-256")
      .digest(s.getBytes("UTF-8"))
      .map("%02x".format(_)).mkString
}
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/universal/stage/ .
EXPOSE 9000
CMD ["bin/shopping-list-app", "-Dplay.http.secret.key=${APPLICATION_SECRET}"]
```

### Fly deployment commands

```bash
# Initial setup
fly launch                          # creates fly.toml
fly postgres create                 # provisions Postgres
fly postgres attach                 # sets DATABASE_URL secret automatically

# Set secrets
fly secrets set VALID_API_KEY_HASHES="hash1,hash2"
fly secrets set APPLICATION_SECRET="$(openssl rand -base64 64)"

# Deploy
fly deploy

# Rotate a key (generate new, add hash, remove old hash)
fly secrets set VALID_API_KEY_HASHES="hash1,hash2,hash3"
```

---

## Key Rotation & Beta Tester Onboarding

```
Adding a new user/device:

  1. Generate new key:     openssl rand -base64 32 → "newKeyABC..."
  2. Hash it:              echo -n "newKeyABC..." | shasum -a 256 → "def789..."
  3. Append hash to Fly:   fly secrets set VALID_API_KEY_HASHES="existing,...,def789..."
  4. Send raw key to user: (secure channel — in person, encrypted message)
  5. User stores in app:   Capacitor SecureStorage / iOS Keychain / Android Keystore

Revoking a user:

  1. Remove their hash from VALID_API_KEY_HASHES
  2. fly secrets set VALID_API_KEY_HASHES="remaining-hashes"
  3. Fly redeploys automatically — revoked key stops working immediately
```

---

## Future: Upgrading to OAuth (multi-user)

When you outgrow API keys (more than a handful of beta testers):

```
Phase 1 (now):     API key → server validates hash
Phase 2 (later):   Cognito/Auth0 → user logs in → JWT issued → server validates JWT

The Play filter becomes a JWT validator instead of a hash checker.
No architecture change — just swap the filter implementation.
```

This keeps the door open without over-engineering the initial deployment.
