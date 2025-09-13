# Seed Data Documentation

This document provides comprehensive information about the seed data for the Gasolinera JSM Platform.

## Overview

The platform includes extensive seed data for development and testing purposes, covering all major entities and relationships across all microservices.

## Seed Data Structure

### Auth Service (`auth_schema`)

#### Users

- **System Admin**: 1 user with full system access
- **Station Admins**: 3 users managing different stations
- **Employees**: 6 users working at various stations
- **Customers**: 14 regular customers + 2 VIP customers
- **Test Users**: 2 users with different verification states

**Phone Number Pattern**: `+525555XXXXXX` (Mexican format)

#### OTP Sessions

- 3 test OTP sessions for development
- Various purposes: LOGIN, REGISTRATION
- Different expiration times for testing

#### Audit Logs

- Login success/failure events
- Registration events
- OTP sending events
- User action tracking

### Station Service (`station_schema`)

#### Stations

- **10 Active Stations** across major Mexican cities:
  - Mexico City (3): Centro, Polanco, Roma Norte
  - Guadalajara (2): Centro, Zapopan
  - Monterrey (2): Centro, San Pedro
  - Other cities (3): Puebla, Tijuana, Cancún
- **2 Test Stations**: Maintenance and Construction status

**Station Types**:

- `FULL_SERVICE`: Traditional full-service stations
- `SELF_SERVICE`: Self-service stations
- `HYBRID`: Mixed service stations
- `PREMIUM`: Premium service stations

#### Employees

- 19 employees across different stations
- Various roles: MANAGER, SUPERVISOR, CASHIER, ATTENDANT, MAINTENANCE, SECURITY
- Different employment types: FULL_TIME, PART_TIME, TEMPORARY
- Realistic salary and hourly rate data

### Coupon Service (`coupon_schema`)

#### Campaigns

- **10 Active Campaigns**:
  - Welcome Campaign (15% discount)
  - Summer Campaign (fixed $25 discount)
  - Premium Fuel Campaign (10% discount)
  - Weekend Special (20% discount)
  - Loyalty Program (25% discount)
  - Student Discount (fixed $15 discount)
  - Corporate Campaign (12% discount)
  - And more...

**Campaign Types**:

- `PERCENTAGE`: Percentage-based discounts
- `FIXED_AMOUNT`: Fixed amount discounts

#### Coupons

- **25+ Test Coupons** across different campaigns
- Various statuses: ACTIVE, USED, EXPIRED, CANCELLED, SUSPENDED
- QR codes and digital signatures for validation
- Usage tracking and history

#### Usage History

- 17 coupon usage records
- Different payment methods: CREDIT_CARD, CASH, DEBIT_CARD, CORPORATE_CARD
- Transaction references and correlation IDs
- Raffle ticket awards tracking

### Redemption Service (`redemption_schema`)

#### Redemptions

- **12 Test Redemptions** with various statuses:
  - COMPLETED: Successfully processed redemptions
  - PENDING: Awaiting processing
  - IN_PROGRESS: Currently being processed
  - FAILED: Failed redemptions
  - CANCELLED: Cancelled redemptions

#### Fraud Detection

- Fraud detection logs with risk scores
- Various fraud types: DUPLICATE_REDEMPTION, SUSPICIOUS_PATTERN, VELOCITY_FRAUD
- Detection rules and additional data for analysis

#### Legacy Support

- QR validation records
- Points ledger entries for backward compatibility

### Ad Engine Service (`ad_schema`)

#### Ad Campaigns

- **8 Test Campaigns** with different objectives:
  - Premium Fuel Promotion
  - New Customer Welcome
  - Loyalty Program
  - Summer Discounts
  - Corporate Campaign
  - Test campaigns with various statuses

**Campaign Types**:

- `STANDARD`: Regular campaigns
- `PROMOTIONAL`: Promotional campaigns
- `PREMIUM`: Premium campaigns

#### Advertisements

- **11 Test Advertisements** across campaigns
- Various ad types: VIDEO, IMAGE, INTERACTIVE
- Different approval statuses: APPROVED, PENDING, REJECTED
- Ticket multipliers for raffle integration

#### Ad Engagements

- **12 Engagement Records** with different outcomes:
  - COMPLETION: Fully watched/completed ads
  - VIEW: Partially viewed ads
  - SKIP: Skipped ads
  - ABANDONED: Abandoned engagements

#### Analytics

- Daily analytics data for advertisements
- Metrics: impressions, views, clicks, completions, engagement rates
- Cost tracking and ROI calculations

### Raffle Service (`raffle_schema`)

#### Raffles

- **5 Test Raffles** with different statuses:
  - Weekly Raffle (OPEN)
  - Anniversary Raffle (CLOSED)
  - New Year Raffle (COMPLETED)
  - Monthly Raffle (DRAFT)
  - VIP Raffle (OPEN, exclusive)

**Raffle Types**:

- `STANDARD`: Regular raffles
- `SPECIAL_EVENT`: Special event raffles
- `EXCLUSIVE`: Exclusive/VIP raffles

#### Prizes

- **13 Different Prizes** across raffles:
  - Cash prizes ($1,000 - $25,000)
  - Fuel credits (500 - 1,000 liters)
  - Vehicles (Nissan Versa 2024)
  - Travel vouchers
  - Merchandise and experiences

#### Tickets and Entries

- **17 Raffle Tickets** from various sources
- **15 Raffle Entries** via mobile app and web portal
- Ticket sources: COUPON_REDEMPTION, AD_ENGAGEMENT

#### Winners

- **6 Winners** for completed raffles
- Prize claiming status tracking
- Notification management

## Data Relationships

### Cross-Service Relationships

The seed data maintains referential integrity across services:

1. **Users** (Auth) → **Employees** (Station)
2. **Users** (Auth) → **Coupon Usage** (Coupon)
3. **Coupons** (Coupon) → **Redemptions** (Redemption)
4. **Redemptions** (Redemption) → **Raffle Tickets** (Raffle)
5. **Ad Engagements** (Ad Engine) → **Raffle Tickets** (Raffle)

### ID Mapping

- User IDs 1-24 are consistently used across services
- Station IDs 1-12 map to specific locations
- Campaign IDs 1-10 represent different promotional campaigns

## Usage Scenarios

### Development Testing

The seed data supports various development scenarios:

1. **User Authentication Flow**
   - Test login with different user roles
   - OTP validation testing
   - Account lockout scenarios

2. **Station Operations**
   - Employee management
   - Station status changes
   - Performance tracking

3. **Coupon Lifecycle**
   - Campaign creation and management
   - Coupon generation and validation
   - Usage tracking and analytics

4. **Redemption Processing**
   - End-to-end redemption flow
   - Fraud detection testing
   - Payment processing scenarios

5. **Advertisement System**
   - Ad serving and targeting
   - Engagement tracking
   - Performance analytics

6. **Raffle Management**
   - Raffle creation and management
   - Ticket generation from various sources
   - Winner selection and prize distribution

### Load Testing

The seed data provides sufficient volume for basic load testing:

- 24 users for concurrent operations
- 12 stations for distributed load
- 25+ coupons for validation testing
- Multiple active raffles for entry testing

## Data Generation

### Automated Generation

Use the provided scripts to generate additional seed data:

```bash
# Generate additional seed data
./scripts/generate-seed-data.sh --users 100 --stations 20 --campaigns 15

# Validate seed data integrity
./scripts/validate-seed-data.sh --verbose
```

### Manual Data Creation

For specific test scenarios, you can create custom seed data by:

1. Following the existing patterns in migration files
2. Maintaining referential integrity
3. Using realistic Mexican data (names, locations, phone numbers)
4. Including proper audit trails

## Validation

### Automated Validation

The validation script checks:

- Minimum record counts
- Data format compliance (phone numbers, coordinates)
- Referential integrity
- Date consistency
- Unique constraints

### Manual Validation

Verify seed data by:

1. Running the validation script
2. Checking service health endpoints
3. Testing API endpoints with seed data
4. Verifying cross-service integrations

## Maintenance

### Updates

When updating seed data:

1. Maintain backward compatibility
2. Update all related services
3. Run validation tests
4. Update documentation

### Cleanup

For production deployments:

1. Remove test-specific data
2. Keep only essential reference data
3. Update configuration for production IDs
4. Implement proper data archival

## Security Considerations

### Development Safety

- All passwords are development-only
- Phone numbers use test ranges
- Email addresses use test domains
- No real personal information

### Production Migration

Before production:

1. Replace all test credentials
2. Remove development-specific users
3. Update phone number patterns
4. Implement proper data encryption

## Troubleshooting

### Common Issues

#### Missing Data

If seed data is missing:

1. Check migration execution order
2. Verify database schema creation
3. Run validation script to identify gaps

#### Referential Integrity Errors

If foreign key constraints fail:

1. Check ID mappings between services
2. Verify migration execution sequence
3. Review cross-service dependencies

#### Performance Issues

If seed data causes performance problems:

1. Check index creation in migrations
2. Verify query optimization
3. Consider data volume reduction

### Debugging Tools

```bash
# Check migration status
./scripts/test-migrations.sh

# Validate data integrity
./scripts/validate-seed-data.sh --verbose

# Generate additional data
./scripts/generate-seed-data.sh --help
```

## Future Enhancements

### Planned Improvements

1. **Localization**: Multi-language seed data
2. **Scalability**: Larger datasets for performance testing
3. **Realism**: More realistic transaction patterns
4. **Automation**: Continuous data generation for CI/CD

### Extension Points

The seed data structure supports:

1. Additional user roles and permissions
2. More complex campaign rules
3. Advanced fraud detection scenarios
4. Multi-currency support
5. International station data

---

For more information about specific seed data files, see the individual migration scripts in each service's `src/main/resources/db/migration/` directory.
