# 🔐 Gasolinera JSM - QR Code Signing & Key Management

This directory contains the cryptographic key management utilities for the Gasolinera JSM platform's QR code signing system. The QR codes are used to securely identify gas station dispensers and prevent tampering or fraud.

## 🎯 Overview

The QR code signing system provides:

- **Cryptographic Authentication**: Each QR code is signed with RSA-2048 keys
- **Tamper Prevention**: Invalid or modified QR codes are rejected
- **Time-based Expiration**: QR codes automatically expire to prevent replay attacks
- **Station/Dispenser Identification**: Each QR code uniquely identifies a station and dispenser
- **Cross-platform Compatibility**: Works with both TypeScript (ops) and Kotlin (backend)

## 📁 Files

| File               | Purpose                   | Language   |
| ------------------ | ------------------------- | ---------- |
| `sign-qr.ts`       | Generate signed QR tokens | TypeScript |
| `verify-qr.ts`     | Verify signed QR tokens   | TypeScript |
| `generate-keys.kt` | Generate RSA key pairs    | Kotlin     |
| `package.json`     | NPM configuration         | JSON       |

## 🚀 Quick Start

### 1. Install Dependencies

```bash
npm install
```

### 2. Generate Keys

```bash
# Generate RSA key pair
npm run generate-keys

# This creates:
# - private-key.pem (for signing)
# - public-key.pem (for verification)
```

### 3. Sign a QR Code

```bash
# Generate a signed QR token
npm run sign-qr
```

### 4. Verify a QR Code

```bash
# Verify a signed token
npm run verify-qr <signed-token>
```

### 5. Test Complete Flow

```bash
# Test the entire flow
npm run test-qr-flow
```

## 🔧 Usage Examples

### TypeScript/Node.js

```typescript
import { sign } from './sign-qr';
import { verify } from './verify-qr';
import * as fs from 'fs';

// Load keys
const privateKey = fs.readFileSync('private-key.pem', 'utf-8');
const publicKey = fs.readFileSync('public-key.pem', 'utf-8');

// Create QR payload
const qrPayload = {
  s: 'JSM-01-ALAJUELITA', // Station ID
  d: 'D03', // Dispenser ID
  n: uuidv4(), // Unique nonce
  t: Math.floor(Date.now() / 1000), // Timestamp
  exp: Math.floor(Date.now() / 1000) + 3600, // Expires in 1 hour
};

// Sign the QR code
const signedToken = sign(qrPayload, privateKey);

// Verify the QR code
const result = verify(signedToken, publicKey);
console.log('Valid:', result.valid);
console.log('Payload:', result.payload);
```

### Kotlin/Spring Boot

```kotlin
@Autowired
private lateinit var qrSigningService: QrSigningService

@Autowired
private lateinit var qrSigningKeys: QrSigningKeys

// Generate signed QR token
val result = qrSigningService.generateSignedQrToken(
    stationId = "JSM-01-ALAJUELITA",
    dispenserId = "D03",
    privateKey = qrSigningKeys.privateKey,
    expirationHours = 1L
)

// Verify signed QR token
val verificationResult = qrSigningService.verifySignedQrToken(
    signedToken = token,
    publicKey = qrSigningKeys.publicKey
)
```

## 🏗️ QR Token Structure

### Payload Format

```json
{
  "s": "JSM-01-ALAJUELITA", // Station ID
  "d": "D03", // Dispenser ID
  "n": "uuid-v4-string", // Unique nonce
  "t": 1640995200, // Timestamp (Unix)
  "exp": 1640998800 // Expiration (Unix)
}
```

### Signed Token Format

```
<base64url-payload>.<base64url-signature>
```

Example:

```
eyJzIjoiSlNNLTAxLUFMQUpVRUxJVEEiLCJkIjoiRDAzIiwibiI6IjEyMzQ1Njc4LTkwYWItY2RlZi0xMjM0LTU2Nzg5MGFiY2RlZiIsInQiOjE2NDA5OTUyMDAsImV4cCI6MTY0MDk5ODgwMH0.MEUCIQDXvKxP7QZ8W9J2K5L3M4N6O7P8Q9R0S1T2U3V4W5X6Y7Z8aQIgABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdef
```

## 🔒 Security Considerations

### Key Management

1. **Private Key Security**

   - Store private keys in HashiCorp Vault (production)
   - Never commit private keys to version control
   - Use HSM (Hardware Security Module) for production
   - Implement key rotation policies

2. **Key Rotation**

   - Rotate keys regularly (recommended: weekly)
   - Maintain multiple valid public keys during rotation
   - Implement graceful key rollover

3. **Access Control**
   - Limit access to key generation utilities
   - Use RBAC for QR code generation endpoints
   - Log all key operations

### Token Security

1. **Expiration**

   - Default expiration: 1 hour
   - Maximum expiration: 24 hours
   - Implement server-side expiration checks

2. **Nonce Usage**

   - Each QR code has a unique nonce
   - Prevent replay attacks
   - Store used nonces (optional)

3. **Signature Verification**
   - Always verify signatures server-side
   - Use constant-time comparison
   - Handle verification errors gracefully

## 🌐 Integration with Backend Services

### Station Service

The Station Service provides REST endpoints for QR code operations:

```bash
# Generate QR code
POST /api/v1/qr/generate
{
  "stationId": "JSM-01-ALAJUELITA",
  "dispenserId": "D03",
  "expirationHours": 1
}

# Verify QR token
POST /api/v1/qr/verify
{
  "token": "eyJ..."
}

# Get dispenser QR code
GET /api/v1/qr/dispenser/{stationId}/{dispenserId}
```

### Coupon Service

The Coupon Service verifies QR tokens during redemption:

```kotlin
// Verify QR token before processing coupon
val qrVerification = qrSigningService.verifySignedQrToken(token, publicKey)
if (qrVerification.isSuccess) {
    // Process coupon redemption
    processCouponRedemption(couponCode, qrVerification.getOrNull()!!)
}
```

## 🧪 Testing

### Unit Tests

```bash
# Run TypeScript tests
npm test

# Run Kotlin tests
./gradlew test
```

### Integration Tests

```bash
# Test complete QR flow
npm run test-qr-flow

# Test with backend services
./gradlew integrationTest
```

### Security Tests

```bash
# Test signature verification
npm run test-security

# Test key rotation
npm run test-key-rotation
```

## 📊 Monitoring & Logging

### Metrics to Monitor

- QR code generation rate
- Verification success/failure rate
- Token expiration events
- Key rotation events
- Invalid signature attempts

### Log Events

```json
{
  "event": "qr_generated",
  "stationId": "JSM-01-ALAJUELITA",
  "dispenserId": "D03",
  "expiresAt": "2024-01-01T12:00:00Z",
  "timestamp": "2024-01-01T11:00:00Z"
}

{
  "event": "qr_verified",
  "stationId": "JSM-01-ALAJUELITA",
  "dispenserId": "D03",
  "valid": true,
  "timestamp": "2024-01-01T11:30:00Z"
}
```

## 🚨 Troubleshooting

### Common Issues

1. **"Invalid signature" errors**

   - Check key pair compatibility
   - Verify key format (PEM)
   - Ensure consistent algorithms

2. **"Token expired" errors**

   - Check system clock synchronization
   - Verify expiration time calculation
   - Consider clock skew tolerance

3. **"Key not found" errors**
   - Verify key file paths
   - Check file permissions
   - Ensure keys are properly generated

### Debug Commands

```bash
# Check key format
openssl rsa -in private-key.pem -text -noout

# Verify key pair
openssl rsa -in private-key.pem -pubout | diff - public-key.pem

# Test token manually
echo "payload" | openssl dgst -sha256 -sign private-key.pem | base64
```

## 📚 References

- [RFC 7515 - JSON Web Signature (JWS)](https://tools.ietf.org/html/rfc7515)
- [RFC 3447 - PKCS #1: RSA Cryptography](https://tools.ietf.org/html/rfc3447)
- [OWASP Cryptographic Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)

## 🤝 Contributing

1. Follow security best practices
2. Add tests for new features
3. Update documentation
4. Review security implications
5. Test key rotation scenarios

---

**⚠️ Security Notice**: This system handles cryptographic keys and tokens. Always follow security best practices and conduct regular security audits.
