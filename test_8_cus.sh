#!/usr/bin/env bash
set -e

cd ~/dedte-mail
JAR="dedte-mail-system-1.0.0-jar-with-dependencies.jar"

# Usar PostgreSQL local del VPS para validar los 8 CUs al 100%
export DEDTE_DB_HOST="localhost"
export DEDTE_DB_PORT="5432"
export DEDTE_DB_NAME="db_grupo11sa"
export DEDTE_DB_USER="grupo11sa"
export DEDTE_DB_PASSWORD="grup11grup11*"

echo "========================================================"
echo "  PRUEBA INTEGRAL DE LOS 8 CASOS DE USO (DEDTE-UAGRM)"
echo "========================================================"

java -jar "$JAR" --test-db

comandos=(
  'HELP["*"]'
  'LISUSU["*"]'
  'INSUSU["8877665","Carlos","Suarez","DOCENTE","carlos.suarez@uagrm.edu.bo","71122334"]'
  'LISPAR["*"]'
  'LISENT["*"]'
  'LISAPE["FICCT"]'
  'LISMON["*"]'
  'INSMON["1","60","56","50","6","78.5"]'
  'LISRIE["*"]'
  'INSRIE["2","45","16","5","Deserción detectada en prueba"]'
  'DASHBOARD["2026-2"]'
  'LISHIS["*"]'
  'COMPHIS["2025-2","2026-2"]'
  'REPFAC["*"]'
  'REPENT["*"]'
)

for cmd in "${comandos[@]}"; do
  echo ""
  echo ">>> Probando: $cmd"
  java -jar "$JAR" --simular "$cmd" | grep -E "Exito|Asunto|Log"
done

echo ""
echo ">>> Probando manejo de error de sintaxis (debe devolver Exito: false):"
java -jar "$JAR" --simular 'COMANDO_MALO sin comillas' | grep -E "Exito|Asunto|Log"

echo ""
echo "=== TODAS LAS PRUEBAS DE LOS 8 CUs FINALIZADAS ==="
