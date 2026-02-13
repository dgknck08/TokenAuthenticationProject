# E-Commerce Authentication System

JWT-based authentication system. Built with Spring Boot, it includes Redis caching, account lockout, and detailed audit logging.

## Features

### Security Features
- JWT Authentication: Access Token (Bearer) + Refresh Token (HttpOnly Cookie)
- Account Lockout to prevent brute force attacks
- Redis Blacklist for token revocation and session management
- Audit Logging for all login and security events
- Suspicious Activity Detection

### Performance Features
- Redis Cache for JWT claims and token validation
- Caffeine Cache for local in-memory caching
- Asynchronous processing for non-blocking authentication
- Redis Pipeline for efficient batch operations

### Management Features
- Account management: lock and unlock accounts
- Token management: token metadata and blacklist control

## Technology Stack

| Technology      | Version | Description                 |
|-----------------|---------|-----------------------------|
| Spring Boot     | 3.x     | Core framework              |
| Spring Security | 6.x     | Security layer              |
| JJWT (JWT)      | 0.12.x  | Token creation and validation|
| Redis           | 7.x     | Cache and blacklist storage |
| PostgreSQL      | 15.x    | Main database               |
| Caffeine        | 3.x     | Local cache                 |
| Docker          | -       | Containerization            |

## Installation

### Requirements

```bash
Java 21+
Maven 3.6+
Docker & Docker Compose
PostgreSQL 15+
Redis 7+

1. Clone the repository
git clone https://github.com/yourusername/ecommerce-auth.git
cd ecommerce-auth

2. Quick start with Docker
# Start services
docker-compose up -d
# Run the application
mvn spring-boot:run

API Endpoints
Authentication
Method	Endpoint	Description
POST	/api/auth/register	Create a new user
POST	/api/auth/login	Login and receive tokens
POST	/api/auth/logout	Logout current session
POST	/api/auth/logout-all	Logout from all devices
POST	/api/auth/refresh-token	Refresh access token

Redis Key Patterns
Key Pattern	Description
auth:failed_attempts:{username}	Failed login attempts count
auth:account_locked:{username}	Account lock information
auth:ip_attempts:{ip}	IP-based attempt count
jwt:blacklist:{token}	Blacklisted tokens
jwt:metadata:{tokenId}	Token metadata
