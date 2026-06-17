# TPS First Deployment

This directory contains the database package and the first-deployment helper for the TPS backend.

## Files

- `database/tps-init.sql`: MySQL/MariaDB schema and initial administrator data.
- `first-deploy.sh`: Creates the runtime upload directory and imports the SQL package.

## Usage

```bash
./deploy/first-deploy.sh
```

Common overrides:

```bash
TPS_DB_USERNAME=root \
TPS_DB_PASSWORD='root' \
TPS_DB_HOST=127.0.0.1 \
TPS_DB_PORT=3306 \
./deploy/first-deploy.sh
```

Check actions without touching MySQL:

```bash
./deploy/first-deploy.sh --dry-run
```

Then start the backend:

```bash
TPS_DB_USERNAME=root TPS_DB_PASSWORD='root' ./start-backend.sh
```
