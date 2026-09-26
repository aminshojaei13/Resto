# Resto SaaS Database Backup & Disaster Recovery Guide

## 1. Overview & Policy

This document defines the official backup and disaster recovery policy for Resto's PostgreSQL database (`resto_production`). PostgreSQL is the single source of truth for all multi-tenant organizations, products, sales orders, inventory ledgers, and double-entry accounting journals.

---

## 2. Backup Strategy & Frequency

- **Full Logical Backup**: Executed every 24 hours at 02:00 UTC via `pg_dump` compressed format.
- **WAL Point-in-Time Archiving**: Executed continuously every 15 minutes for Point-in-Time Recovery (PITR).
- **Retention Period**:
  - Daily backups: Retained for 30 days in encrypted S3 bucket (`s3://resto-db-backups/daily/`).
  - Monthly archives: Retained for 12 months.
- **Encryption**: AES-256 server-side encryption (`SSE-S3`).

---

## 3. Automated Backup Command Script (`backup.sh`)

```bash
#!/usr/bin/env bash
set -e

TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_DIR="/var/backups/resto"
BACKUP_FILE="${BACKUP_DIR}/resto_db_${TIMESTAMP}.dump"

mkdir -p ${BACKUP_DIR}

# Logical Compressed Dump
pg_dump -h ${DB_HOST:-postgres} \
        -U ${DB_USERNAME:-postgres} \
        -d ${DB_DATABASE:-resto_production} \
        -F c -b -v \
        -f ${BACKUP_FILE}

# Upload to Encrypted Cloud Storage
aws s3 cp ${BACKUP_FILE} s3://resto-db-backups/daily/resto_db_${TIMESTAMP}.dump --sse AES256

# Remove local dump older than 7 days
find ${BACKUP_DIR} -type f -name "*.dump" -mtime +7 -exec rm {} \;

echo "Backup completed successfully: resto_db_${TIMESTAMP}.dump"
```

---

## 4. Tested Restore Procedure

To test or execute a disaster recovery restore:

### Step 1: Download Latest Cloud Dump
```bash
aws s3 cp s3://resto-db-backups/daily/resto_db_latest.dump /tmp/restore.dump
```

### Step 2: Restore to Disposable Database
```bash
# Create temporary disposable database for verification
createdb -h localhost -U postgres resto_restore_verify

# Execute Restore
pg_restore -h localhost -U postgres -d resto_restore_verify -v /tmp/restore.dump
```

### Step 3: Verify Integrity & Accounting Balance
Run verification SQL queries against restored database:
```sql
-- 1. Verify Organization & Users Count
SELECT COUNT(*) FROM organizations;
SELECT COUNT(*) FROM users;

-- 2. Verify Double-Entry Accounting Invariant (Total Debits == Total Credits)
SELECT 
    SUM(total_debit) AS total_debits, 
    SUM(total_credit) AS total_credits,
    (SUM(total_debit) - SUM(total_credit)) AS balance_diff
FROM journal_entries;
```
*Expected `balance_diff`: `0.00`.*

### Step 4: Cleanup Disposable Test Database
```bash
dropdb -h localhost -U postgres resto_restore_verify
```

---

## 5. Excluded Data

- **Redis Cache & Sessions**: Transient cache and session keys are excluded from database dumps. Redis rebuilds cache automatically on system startup.
- **Temporary Uploads**: Ephemeral temp files are purged every 24 hours.
