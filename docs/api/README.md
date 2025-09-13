# 🚀 Gasolinera JSM API Documentation

## 🌟 Overview

Welcome to the **Gasolinera JSM API** - the most comprehensive fuel coupon and raffle management system in Mexico! Our API provides a complete digital ecosystem for purchasing fuel coupons, redeeming them at gas stations, and participating in exciting raffles.

## 🎯 Key Features

### 🔐 Authentication & Security

- **JWT-based Authentication** with refresh tokens
- **Role-based Access Control** (User, Station Operator, Admin)
- **Rate Limiting** and **DDoS Protection**
- **End-to-end Encryption** for sensitive data
- **OAuth 2.0** integration for third-party apps

### 🎫 Digital Coupon System

- **QR Code Generation** with cryptographic signatures
- **Multi-fuel Support** (Regular, Premium, Diesel)
- **Station-specific Coupons** with geolocation validation
- **Flexible Expiration** policies (30-day default)
- **Bulk Purchase** options for fleet customers

### ⛽ Gas Station Network

- **Real-time Fuel Pricing** updated every 15 minutes
- **Geospatial Search** with radius-based filtering
- **Station Availability** and operating hours
- **Amenities Filtering** (car wash, convenience store, ATM)
- **Performance Analytics** for station operators

### 🎰 Raffle & Rewards System

- **Automatic Ticket Generation** based on fuel purchases
- **Multiplier System** for premium fuel and loyalty members
- **Ad Engagement Bonuses** for extra tickets
- **Monthly Draws** with exciting prizes
- **Loyalty Points** integration

### 📊 Analytics & Insights

- **Real-time Dashboards** for users and admins
- **Usage Analytics** and spending patterns
- **Performance Metrics** for business intelligence
- **Predictive Analytics** for demand forecasting
- **Custom Reports** with data export

## 🏗️ Architecture

### Microservices Design

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │────│  Auth Service   │────│  User Service   │
│   (Port 8080)   │    │   (Port 8081)   │    │   (Port 8082)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ├───────────────────────┼───────────────────────┤
         │                       │                       │
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│ Station Service │    │ Coupon Service  │    │ Raffle Service  │
│   (Port 8083)   │    │   (Port 8084)   │    │   (Port 8085)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────┐
                    │ Dashboard API   │
                    │   (Port 8086)   │
                    └─────────────────┘
```

### Technology Stack

- **Backend**: Kotlin + Spring Boot 3.2
- **Database**: PostgreSQL 15 with read replicas
- **Cache**: Redis Cluster for session management
- **Message Queue**: RabbitMQ for async processing
- **Search**: Elasticsearch for station discovery
- **Monitoring**: Prometheus + Grafana
- **Container**: Docker + Kubernetes
- **API Documentation**: OpenAPI 3.0 + Swagger UI

## 🚀 Quick Start

### 1. Authentication

First, obtain an access token by registering or logging in:

```bash
# Register a new user
curl -X POST https://api.gasolinera-jsm.com/api/v1/auth/register \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "juan.perez@example.com",
    "phone": "5551234567",
    "firstName": "Juan",
    "lastName": "Pérez",
    "password": "SecurePassword123!"
  }'

# Login to get access token
curl -X POST https://api.gasolinera-jsm.com/api/v1/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{
    "identifier": "juan.perez@example.com",
    "password": "SecurePassword123!"
  }'
```

### 2. Find Nearby Gas Stations

```bash
curl -X GET "https://api.gasolinera-jsm.com/api/v1/stations/nearby?latitude=19.4326&longitude=-99.1332&radius=10" \\
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3. Purchase a Fuel Coupon

```bash
curl -X POST https://api.gasolinera-jsm.com/api/v1/coupons/purchase \\
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
    "amount": 500.00,
    "fuelType": "REGULAR",
    "paymentMethod": "CREDIT_CARD",
    "paymentToken": "tok_1234567890abcdef"
  }'
```

### 4. Redeem Coupon at Gas Station

```bash
curl -X POST https://api.gasolinera-jsm.com/api/v1/coupons/redeem \\
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN" \\
  -H "Content-Type: application/json" \\
  -d '{
    "qrCode": "QR_A1B2C3D4E5F6G7H8I9J0K1L2M3N4O5P6",
    "stationId": "987fcdeb-51a2-43d7-b456-426614174999",
    "fuelAmount": 22.22,
    "pricePerLiter": 22.50
  }'
```

## 📚 API Endpoints

### 🔐 Authentication (`/api/v1/auth`)

| Method | Endpoint           | Description               |
| ------ | ------------------ | ------------------------- |
| POST   | `/register`        | Register new user account |
| POST   | `/login`           | User authentication       |
| POST   | `/refresh`         | Refresh access token      |
| POST   | `/logout`          | User logout               |
| GET    | `/profile`         | Get user profile          |
| PUT    | `/profile`         | Update user profile       |
| POST   | `/change-password` | Change user password      |
| POST   | `/forgot-password` | Initiate password reset   |
| POST   | `/reset-password`  | Complete password reset   |
| POST   | `/verify-email`    | Verify email address      |

### ⛽ Gas Stations (`/api/v1/stations`)

| Method | Endpoint                 | Description                   |
| ------ | ------------------------ | ----------------------------- |
| GET    | `/nearby`                | Find nearby gas stations      |
| GET    | `/{stationId}`           | Get station details           |
| GET    | `/prices`                | Get current fuel prices       |
| GET    | `/search`                | Search stations by criteria   |
| GET    | `/availability`          | Get station availability      |
| PUT    | `/{stationId}/prices`    | Update fuel prices (Operator) |
| GET    | `/{stationId}/analytics` | Get station analytics (Admin) |

### 🎫 Coupons (`/api/v1/coupons`)

| Method | Endpoint                    | Description                    |
| ------ | --------------------------- | ------------------------------ |
| POST   | `/purchase`                 | Purchase new fuel coupon       |
| GET    | `/`                         | Get user's coupons (paginated) |
| GET    | `/{couponId}`               | Get coupon details             |
| POST   | `/redeem`                   | Redeem coupon at station       |
| POST   | `/{couponId}/cancel`        | Cancel active coupon           |
| GET    | `/statistics`               | Get user coupon statistics     |
| POST   | `/{couponId}/regenerate-qr` | Regenerate QR code             |
| GET    | `/statistics/system`        | System statistics (Admin)      |

### 🎰 Raffles (`/api/v1/raffles`)

| Method | Endpoint       | Description               |
| ------ | -------------- | ------------------------- |
| GET    | `/active`      | Get active raffles        |
| GET    | `/{raffleId}`  | Get raffle details        |
| GET    | `/tickets`     | Get user's raffle tickets |
| POST   | `/participate` | Participate in raffle     |
| GET    | `/winners`     | Get raffle winners        |
| GET    | `/statistics`  | Get raffle statistics     |

### 📊 Dashboard (`/api/v1/dashboard`)

| Method | Endpoint           | Description                      |
| ------ | ------------------ | -------------------------------- |
| GET    | `/overview`        | User dashboard overview          |
| GET    | `/analytics`       | User analytics data              |
| GET    | `/notifications`   | Get user notifications           |
| GET    | `/recommendations` | Get personalized recommendations |

## 🔒 Authentication

### JWT Token Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "123e4567-e89b-12d3-a456-426614174000",
    "email": "juan.perez@example.com",
    "roles": ["USER"],
    "iat": 1642680000,
    "exp": 1642683600
  }
}
```

### Authorization Header

```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Expiration

- **Access Token**: 1 hour
- **Refresh Token**: 7 days
- **Password Reset Token**: 1 hour
- **Email Verification Token**: 24 hours

## 📊 Response Format

### Success Response

```json
{
  "data": {
    // Response data here
  },
  "meta": {
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req_123456789",
    "version": "1.0.0"
  }
}
```

### Error Response

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Invalid input data",
    "details": {
      "field": "Specific error message"
    }
  },
  "meta": {
    "timestamp": "2024-01-15T10:30:00Z",
    "requestId": "req_123456789",
    "path": "/api/v1/coupons/purchase"
  }
}
```

## 🚦 Rate Limiting

### Rate Limits by Endpoint Type

| Endpoint Type   | Rate Limit    | Window     |
| --------------- | ------------- | ---------- |
| Authentication  | 5 requests    | 15 minutes |
| Coupon Purchase | 10 requests   | 1 hour     |
| Station Search  | 100 requests  | 1 hour     |
| General API     | 1000 requests | 1 hour     |

### Rate Limit Headers

```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 999
X-RateLimit-Reset: 1642683600
```

## 🌍 Environments

### Development

- **Base URL**: `https://dev-api.gasolinera-jsm.com`
- **Swagger UI**: `https://dev-api.gasolinera-jsm.com/swagger-ui.html`
- **Rate Limits**: Relaxed for testing

### Staging

- **Base URL**: `https://staging-api.gasolinera-jsm.com`
- **Swagger UI**: `https://staging-api.gasolinera-jsm.com/swagger-ui.html`
- **Rate Limits**: Production-like

### Production

- **Base URL**: `https://api.gasolinera-jsm.com`
- **Swagger UI**: `https://api.gasolinera-jsm.com/swagger-ui.html`
- **Rate Limits**: Strict enforcement

## 🔧 SDKs and Libraries

### Official SDKs

- **JavaScript/TypeScript**: `@gasolinera-jsm/api-client`
- **Python**: `gasolinera-jsm-api`
- **Java/Kotlin**: `com.gasolinerajsm:api-client`
- **Swift**: `GasolineraJSMAPI`
- **Dart/Flutter**: `gasolinera_jsm_api`

### Installation Examples

```bash
# JavaScript/Node.js
npm install @gasolinera-jsm/api-client

# Python
pip install gasolinera-jsm-api

# Java/Maven
<dependency>
  <groupId>com.gasolinerajsm</groupId>
  <artifactId>api-client</artifactId>
  <version>1.0.0</version>
</dependency>
```

## 📱 Mobile Integration

### QR Code Scanning

Our QR codes are optimized for mobile scanning with:

- **High Error Correction** (Level H - 30%)
- **Optimal Size** (21x21 modules minimum)
- **High Contrast** for various lighting conditions
- **Cryptographic Signatures** for security

### Deep Linking

Support for custom URL schemes:

```
gasolinerajsm://coupon/123e4567-e89b-12d3-a456-426614174000
gasolinerajsm://station/987fcdeb-51a2-43d7-b456-426614174999
gasolinerajsm://raffle/monthly-jan-2024
```

## 🔔 Webhooks

### Supported Events

- `coupon.purchased` - New coupon purchased
- `coupon.redeemed` - Coupon redeemed at station
- `coupon.expired` - Coupon expired
- `raffle.ticket_generated` - New raffle ticket
- `raffle.winner_selected` - Raffle winner announced
- `user.registered` - New user registration

### Webhook Payload Example

```json
{
  "event": "coupon.purchased",
  "data": {
    "couponId": "123e4567-e89b-12d3-a456-426614174000",
    "userId": "user_123",
    "amount": 500.0,
    "fuelType": "REGULAR"
  },
  "timestamp": "2024-01-15T10:30:00Z",
  "signature": "sha256=..."
}
```

## 📈 Monitoring & Analytics

### Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Metrics**: `/actuator/metrics`

### Performance Metrics

- **Response Time**: P95 < 200ms
- **Availability**: 99.9% uptime SLA
- **Throughput**: 10,000 requests/second
- **Error Rate**: < 0.1%

## 🛠️ Development Tools

### Postman Collection

Import our comprehensive Postman collection:

```
https://api.gasolinera-jsm.com/postman/collection.json
```

### OpenAPI Specification

Download the complete OpenAPI spec:

```
https://api.gasolinera-jsm.com/v3/api-docs
```

### Code Generators

Generate client code using OpenAPI Generator:

```bash
openapi-generator-cli generate \\
  -i https://api.gasolinera-jsm.com/v3/api-docs \\
  -g typescript-fetch \\
  -o ./generated-client
```

## 🆘 Support & Contact

### Technical Support

- **Email**: dev@gasolinera-jsm.com
- **Slack**: #api-support
- **Response Time**: < 4 hours during business hours

### Business Inquiries

- **Email**: business@gasolinera-jsm.com
- **Phone**: +52 55 1234 5678
- **Address**: Av. Reforma 123, CDMX, México

### Documentation

- **API Docs**: https://docs.gasolinera-jsm.com
- **Developer Portal**: https://developers.gasolinera-jsm.com
- **Status Page**: https://status.gasolinera-jsm.com

## 📄 Legal

### Terms of Service

By using our API, you agree to our [Terms of Service](https://gasolinera-jsm.com/terms).

### Privacy Policy

We protect your data according to our [Privacy Policy](https://gasolinera-jsm.com/privacy).

### API License

This API is proprietary software. Unauthorized use is prohibited.

---

**🚀 Ready to fuel your applications with Gasolinera JSM API?**

Start building amazing fuel management experiences today!

_Last updated: January 15, 2024_
_API Version: 1.0.0_
