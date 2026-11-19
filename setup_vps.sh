#!/usr/bin/env bash
set -e

echo "=== [1/4] Instalando OpenJDK 17, PostgreSQL y utilidades de red ==="
sudo apt-get update -y
sudo apt-get install -y openjdk-17-jre-headless postgresql postgresql-client netcat-openbsd

echo "=== [2/4] Configurando PostgreSQL local temporal para pruebas de los 8 CUs ==="
sudo systemctl enable --now postgresql
sudo -u postgres psql -tc "SELECT 1 FROM pg_roles WHERE rolname='grupo11sa'" | grep -q 1 || \
  sudo -u postgres psql -c "CREATE USER grupo11sa WITH PASSWORD 'grup11grup11*';"
sudo -u postgres psql -tc "SELECT 1 FROM pg_database WHERE datname='db_grupo11sa'" | grep -q 1 || \
  sudo -u postgres psql -c "CREATE DATABASE db_grupo11sa OWNER grupo11sa;"

echo "=== [3/4] Cargando esquema y datos semilla en PostgreSQL local ==="
PGPASSWORD='grup11grup11*' psql -h localhost -p 5432 -U grupo11sa -d db_grupo11sa -f ~/dedte-mail/sql/01_schema_dedte.sql
PGPASSWORD='grup11grup11*' psql -h localhost -p 5432 -U grupo11sa -d db_grupo11sa -f ~/dedte-mail/sql/02_seed_dedte.sql

echo "=== [4/4] Verificando conectividad hacia mail.tecnoweb.org.bo (5432, 110, 25) ==="
nc -zv -w 5 mail.tecnoweb.org.bo 5432 110 25 || echo "[AVISO] Aún esperando habilitación de IP o puerto en mail.tecnoweb.org.bo"

echo "=== VPS listo! Ejecutando batería de pruebas de los 8 Casos de Uso ==="
chmod +x ~/dedte-mail/test_8_cus.sh
bash ~/dedte-mail/test_8_cus.sh
