# Database Migration Guide

This document provides comprehensive information about the database migration scripts for the Gasolinera JSM Digital Coupon Platform.

## Overview

The platform uses **Flyway** for database migrations across all microservices. Each service maintains its own database schema to ensure proper data isolation and service autonomy.

## Schema Architecture

### Service Schemas

| Service            | Schema Name         | Description                                |
| ------------------ | ------------------- | ------------------------------------------ |
| Auth Service       | `auth_schema`       | User authentication and authorization data |
| Station Service    | `station_schema`    | Gas station and employee management        |
| Coupon Service     | `coupon_schema`     | Coupon campaigns and validation            |
| Redemption Service | `redemption_schema` | Coupon redemptions and raffle tickets      |
| Ad Engine Service  | `ad_schema`         | Advertisement campaigns and engagements    |
| Raffle Service     | `raffle_schema`     | Raffle management and prize distribution   |

## Migration Files Structure

```
services/
├── auth-service/src/main/resources/db/migration/
│   └── V1__Create_auth_schema.sql
├── station-service/src/main/resources/db/migration/
│   └── V1__init.sql
├── coupon-service/src/main/resources/db/migration/
│   └── V1__Create_coupon_schema.sql
├── redemption-service/src/main/resources/db/migration/
│   ├── V1__init.sql
│   ├── V2__add_outbox_table.sql
│   └── V3__Create_points_ledger_table.sql
├── ad-engine/src/main/resources/db/migration/
│   └── V1__Create_ad_schema.sql
└── raffle-service/src/main/resources/db/migration/
    └── V1__init.sql
```

## Schema Details

### 1. Auth Schema (`auth_schema`)

**Tables:**

- `users` - User accounts with authentication data
- `otp_sessions` - OTP verification sessions
- `refresh_tokens` - JWT refresh token management
- `auth_audit_log` - Authentication event audit trail

**Key Features:**

- Phone number-based authentication
- Role-based access control (RBAC)
- Account lockout protection
- Comprehensive audit logging

### 2. Station Schema (`station_schema`)

**Tables:**

- `stations` - Gas station information and location data
- `employees` - Station employee management
- `station_audit_log` - Station operation audit trail

**Key Features:**

- Geographic location support with lat/lng coordinates
- Employee role management and permissions
- Station operational status tracking
- Performance and capacity metrics

### 3. Coupon Schema (`coupon_schema`)

**Tables:**

- `campaigns` - Coupon campaign management
- `coupons` - Individual coupon instances with QR codes
- `coupon_usage_history` - Coupon redemption tracking
- `coupon_validation_attempts` - Fraud detection and validation logs

**Key Features:**

- QR code generation with digital signatures
- Campaign-based coupon management
- Usage tracking and fraud detection
- Flexible discount types (percentage, fixed amount)

### 4. Redemption Schema (`redemption_schema`)

**Tables:**

- `redemptions` - Coupon redemption transactions
- `raffle_tickets` - Generated raffle tickets from redemptions
- `fraud_detection_log` - Fraud detection and prevention
- `qr_validations` - QR code validation tracking
- `points_ledger` - Legacy points system support

**Key Features:**

- Complete redemption transaction tracking
- Automatic raffle ticket generation
- Fraud detection and prevention
- Fuel transaction support

### 5. Ad Schema (`ad_schema`)

**Tables:**

- `advertisements` - Advertisement content and metadata
- `ad_engagements` - User engagement tracking
- `ad_campaigns` - Advertisement campaign management
- `ad_analytics` - Performance metrics and analytics

**Key Features:**

- Multi-format ad support (video, image, interactive)
- Engagement quality scoring
- Fraud detection for ad interactions
- Comprehensive analytics and reporting

### 6. Raffle Schema (`raffle_schema`)

**Tables:**

- `raffles` - Raffle event management
- `raffle_tickets` - Individual raffle tickets
- `raffle_prizes` - Prize definitions and inventory
- `raffle_winners` - Winner selection and prize fulfillment

**Key Features:**

- Multiple raffle types and algorithms
- Provably fair drawing mechanisms
- Prize inventory management
- Winner notification and fulfillment tracking

## Migration Features

### Common Features Across All Schemas

1. **Audit Trails**
   - Comprehensive logging of all significant operations
   - Correlation ID tracking for distributed tracing
   - User action tracking with IP addresses and user agents

2. **Data Integrity**
   - Extensive CHECK constraints for data validation
   - Foreign key relationships with proper cascade rules
   - Unique constraints to prevent duplicates

3. **Performance Optimization**
   - Strategic indexing for common query patterns
   - Composite indexes for multi-column searches
   - Partial indexes for filtered queries

4. **Automatic Timestamps**
   - `created_at` and `updated_at` fields with automatic triggers
   - Timezone-aware timestamp handling

5. **Flexible Metadata**
   - JSONB columns for extensible data storage
   - Structured metadata for complex configurations

### Security Features

1. **Data Validation**
   - Phone number format validation
   - Email format validation
   - Postal code format validation
   - Currency and amount validation

2. **Access Control**
   - Role-based permissions
   - Account lockout mechanisms
   - Session management

3. **Fraud Prevention**
   - Duplicate detection
   - Velocity checking
   - Pattern analysis
   - Risk scoring

## Views and Analytics

### Materialized Views

Each schema includes optimized views for common queries:

- **Active Records Views** - Filter for currently active/valid records
- **Statistics Views** - Aggregate data for reporting and analytics
- **Summary Views** - Denormalized data for dashboard displays

### Example Views

```sql
-- Active users view
CREATE VIEW auth_schema.active_users AS
SELECT id, phone_number, first_name, last_name, role, created_at
FROM auth_schema.users
WHERE is_active = true;

-- Raffle summary view
CREATE VIEW raffle_schema.raffle_summary AS
SELECT r.id, r.name, r.status, COUNT(rt.id) as total_tickets
FROM raffle_schema.raffles r
LEFT JOIN raffle_schema.raffle_tickets rt ON r.id = rt.raffle_id
GROUP BY r.id, r.name, r.status;
```

## Functions and Triggers

### Automatic Updates

- **Updated At Triggers** - Automatically update `updated_at` timestamps
- **Participant Count Updates** - Maintain accurate participant counts
- **Usage Count Updates** - Track campaign and coupon usage

### Business Logic Functions

- **Raffle Ticket Generation** - Automatically generate tickets on redemption completion
- **Campaign Usage Tracking** - Update campaign statistics on coupon usage
- **Fraud Detection** - Automated fraud scoring and detection

## Sample Data

Each migration includes sample data for development and testing:

- Default system administrator user
- Sample gas stations with realistic data
- Example coupon campaigns
- Test advertisements and raffles

## Migration Validation

Use the provided test script to validate migration syntax:

```bash
./scripts/test-migrations.sh
```

This script performs:

- SQL syntax validation
- Schema reference checking
- Constraint validation
- Basic structure verification

## Deployment Considerations

### Environment-Specific Settings

1. **Development**
   - Sample data included
   - Relaxed constraints for testing
   - Debug logging enabled

2. **Production**
   - Remove sample data inserts
   - Enable all security constraints
   - Configure proper user permissions

### Performance Tuning

1. **Index Optimization**
   - Monitor query performance
   - Add indexes based on actual usage patterns
   - Consider partial indexes for large tables

2. **Partitioning**
   - Consider table partitioning for large audit tables
   - Implement time-based partitioning for analytics data

### Backup and Recovery

1. **Schema Backup**
   - Regular schema-only backups
   - Migration rollback procedures
   - Point-in-time recovery planning

2. **Data Backup**
   - Service-specific backup strategies
   - Cross-service data consistency
   - Disaster recovery procedures

## Troubleshooting

### Common Issues

1. **Migration Failures**
   - Check database permissions
   - Verify schema creation order
   - Review constraint violations

2. **Performance Issues**
   - Analyze query execution plans
   - Check index usage
   - Monitor connection pooling

3. **Data Consistency**
   - Verify foreign key relationships
   - Check constraint violations
   - Validate data integrity

### Rollback Procedures

1. **Schema Rollback**
   - Use Flyway rollback commands
   - Manual schema restoration from backup
   - Service-by-service rollback strategy

2. **Data Recovery**
   - Point-in-time recovery
   - Selective data restoration
   - Cross-service data synchronization

## Best Practices

### Migration Development

1. **Version Control**
   - Never modify existing migration files
   - Use sequential version numbers
   - Include descriptive migration names

2. **Testing**
   - Test migrations on development environment first
   - Validate data integrity after migration
   - Test rollback procedures

3. **Documentation**
   - Document schema changes
   - Include business logic explanations
   - Maintain change logs

### Production Deployment

1. **Deployment Strategy**
   - Deploy during maintenance windows
   - Use blue-green deployment for zero downtime
   - Monitor migration progress

2. **Monitoring**
   - Monitor database performance during migration
   - Track migration execution time
   - Alert on migration failures

3. **Validation**
   - Verify data integrity post-migration
   - Test critical application functions
   - Validate cross-service communication

## Support and Maintenance

### Regular Maintenance

1. **Index Maintenance**
   - Rebuild fragmented indexes
   - Update table statistics
   - Monitor index usage

2. **Data Cleanup**
   - Archive old audit logs
   - Clean up expired sessions
   - Remove obsolete data

3. **Performance Monitoring**
   - Monitor query performance
   - Track database growth
   - Optimize slow queries

### Monitoring and Alerts

1. **Database Health**
   - Connection pool monitoring
   - Query performance tracking
   - Storage usage alerts

2. **Data Quality**
   - Constraint violation monitoring
   - Data consistency checks
   - Orphaned record detection

For additional support or questions about database migrations, please refer to the development team documentation or create an issue in the project repository.
