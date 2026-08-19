#!/bin/bash
set -e

echo "🐳 Iniciando PostgreSQL + PgAdmin con Docker Compose..."
docker compose up -d

echo "✅ PostgreSQL en localhost:5432"
echo "✅ PgAdmin en localhost:5050"
echo "Esperando 30 segundos para que PostgreSQL esté listo..."
sleep 30
