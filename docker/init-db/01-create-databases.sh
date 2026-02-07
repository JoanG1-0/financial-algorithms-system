#!/bin/bash
# =============================================================================
# Crea las 3 bases de datos para los microservicios (US-03)
# Cada servicio tiene su propia BD - antipatrón compartir BD en microservicios
# =============================================================================

set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE etl_db;
    CREATE DATABASE algorithm_db;
    CREATE DATABASE report_db;
    
    -- Permisos (opcional: crear usuarios por servicio en sprints futuros)
    GRANT ALL PRIVILEGES ON DATABASE etl_db TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE algorithm_db TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE report_db TO postgres;
EOSQL
