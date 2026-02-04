# Rate Limiter Microservice

A distributed, scalable rate limiting service using the Token Bucket algorithm with Redis for atomic operations across multiple instances.

## Features

- **Token Bucket Algorithm** with atomic Lua scripts for distributed safety
- **Multi-tier support** (free, premium, enterprise)
- **Horizontal scaling** with Nginx load balancing
- **PostgreSQL** for configuration persistence
- **Redis** for distributed token bucket state
- **Prometheus metrics** via Spring Boot Actuator
- **Swagger UI** for API documentation

## Tech Stack

- Java 17 / Spring Boot 3.2.2
- PostgreSQL 16
- Redis 7
- Nginx (load balancer)
- Docker / Docker Compose

## Prerequisites

### Installing Docker

#### Windows

1. Download Docker Desktop from https://www.docker.com/products/docker-desktop/
2. Run the installer and follow the prompts
3. Enable WSL 2 backend when prompted (recommended)
4. Restart your computer after installation
5. Launch Docker Desktop and wait for it to start

Verify installation:
```powershell
docker --version
docker-compose --version
```

#### Linux (Ubuntu/Debian)

```bash
# Update packages
sudo apt-get update

# Install prerequisites
sudo apt-get install ca-certificates curl gnupg

# Add Docker's official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# Add the repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# Install Docker
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# Add your user to docker group (optional, avoids sudo)
sudo usermod -aG docker $USER
```

#### macOS

1. Download Docker Desktop from https://www.docker.com/products/docker-desktop/
2. Open the `.dmg` file and drag Docker to Applications
3. Launch Docker from Applications
4. Grant permissions when prompted

## Running the Application

### 1. Start with Docker Compose

From the project root directory:

```bash
# Build and start all services (3 app instances + Nginx + Redis + PostgreSQL)
docker-compose up --build -d

# View logs
docker-compose logs -f

# Check service status
docker-compose ps
```

This starts:
- **Nginx** on port `8080` (load balancer)
- **PostgreSQL** on port `5432`
- **Redis** on port `6379`
- **Redis Commander** on port `8083` (Redis GUI)
- **3 Spring Boot instances** (internal, load-balanced via Nginx)

### 2. Verify Services Are Running

```bash
# Health check
curl http://localhost:8080/api/health

# Or using PowerShell
Invoke-RestMethod http://localhost:8080/api/health
```

Expected response:
```json
{
  "status": "healthy",
  "timestamp": "2026-02-04T12:00:00Z"
}
```

### 3. Stop Services

```bash
docker-compose down

# To also remove volumes (database data)
docker-compose down -v
```

## Running the Test Script

The `test-multi-instance.ps1` script performs comprehensive load testing through the Nginx load balancer.

### Prerequisites

- PowerShell 5.1+ (Windows) or PowerShell Core (Linux/macOS)
- Docker containers running (see above)

### Running the Test

```powershell
# Navigate to the project root
cd "C:\Users\Ryanl\OneDrive\Desktop\REP NTU\Projects\RateLimiter"

# Run the test script
.\src\test\test-multi-instance.ps1
```

### What the Test Does

The script runs 5 load scenarios:

| Scenario | Requests | Concurrency |
|----------|----------|-------------|
| Warm-up | 50 | 10 |
| Light Load | 200 | 20 |
| Medium Load | 500 | 50 |
| Heavy Load | 1,000 | 100 |
| Sustained Load | 1,500 | 150 |

### Test Output

The script reports:
- Success rate and failures
- Latency metrics (min, p50, p95, p99, max)
- Throughput (requests/second)
- Docker container status
- Rate limiter metrics (allowed/denied counts)

Example output:
```
============================================================
                    PERFORMANCE SUMMARY
============================================================
Scenario          Requests  Success%  p50(ms)  p95(ms)  Throughput
Warm-up                 50   100.00%       45       89     142.3/s
Light Load             200   100.00%       52      112     187.5/s
Medium Load            500   100.00%       68      145     234.8/s
Heavy Load           1,000   100.00%       85      198     312.4/s
Sustained Load       1,500   100.00%       92      215     378.6/s
```

## API Endpoints

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/check-limit` | POST | Check if request is allowed |
| `/api/limit-status` | GET | Get remaining tokens without consuming |
| `/api/admin/limits` | POST | Create/update rate limit config |
| `/api/metrics/summary` | GET | Get rate limiter metrics |
| `/api/health` | GET | Health check |
| `/swagger-ui.html` | GET | Interactive API documentation |

### Example: Check Rate Limit

```bash
curl -X POST http://localhost:8080/api/check-limit \
  -H "Content-Type: application/json" \
  -d '{"userId": "user123", "resource": "/api/data"}'
```

Response:
```json
{
  "allowed": true,
  "remainingTokens": 9,
  "resetTime": "2026-02-04T12:01:00Z",
  "tier": "free",
  "message": "Request allowed (free tier) [Distributed-safe]"
}
```

## Rate Limit Tiers

| Tier | Requests/Minute | Burst Capacity |
|------|-----------------|----------------|
| free | 10 | 15 |
| premium | 100 | 150 |
| enterprise | 1000 | 1500 |

## Project Structure

```
RateLimiter/
├── src/main/java/com/project/ratelimiter/
│   ├── controller/         # REST endpoints
│   ├── service/impl/       # Token bucket implementation
│   ├── model/              # JPA entities
│   ├── dto/                # Request/response objects
│   └── config/             # Spring configuration
├── src/main/resources/
│   ├── application.yml     # Local config
│   ├── application-docker.yml
│   └── redis/token-bucket-check.lua
├── src/test/               # PowerShell test scripts
├── Dockerfile
├── docker-compose.yml
└── nginx.conf
```

## Monitoring

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Redis Commander**: http://localhost:8083
- **Actuator Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
