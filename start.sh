#!/bin/bash

# Colores
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}  🐳 INICIANDO DOCKER + PGADMIN${NC}"
echo -e "${BLUE}========================================${NC}\n"

# ==================== INICIAR DOCKER ====================
echo -e "${YELLOW}1️⃣  Iniciando Docker daemon...${NC}"

if ! sudo systemctl is-active --quiet docker; then
    sudo systemctl start docker
    sleep 2
fi

if sudo systemctl is-active --quiet docker; then
    echo -e "${GREEN}✅ Docker está corriendo${NC}\n"
else
    echo -e "${RED}❌ No se pudo iniciar Docker${NC}"
    exit 1
fi

# ==================== INICIAR DOCKER-COMPOSE ====================
echo -e "${YELLOW}2️⃣  Iniciando PostgreSQL + PgAdmin...${NC}"

cd ~/proyectos/ecommerce-api

# Detener contenedores previos (sin borrar volúmenes, para no perder datos)
docker-compose down 2>/dev/null

# Iniciar
docker-compose up -d

sleep 5

# ==================== VERIFICAR POSTGRES ====================
if docker ps | grep -q ecommerce_postgres; then
    echo -e "${GREEN}✅ PostgreSQL corriendo${NC}"
else
    echo -e "${RED}❌ PostgreSQL no inició${NC}"
    exit 1
fi

if docker ps | grep -q ecommerce_pgadmin; then
    echo -e "${GREEN}✅ PgAdmin corriendo${NC}\n"
else
    echo -e "${RED}❌ PgAdmin no inició${NC}"
    exit 1
fi

# ==================== ESPERAR A POSTGRESQL ====================
echo -e "${YELLOW}3️⃣  Esperando a que PostgreSQL esté listo...${NC}"

counter=0
max_attempts=30

while [ $counter -lt $max_attempts ]; do
    if docker exec ecommerce_postgres pg_isready -U ecommerce_user &>/dev/null; then
        echo -e "${GREEN}✅ PostgreSQL está listo${NC}\n"
        break
    fi
    echo -n "."
    sleep 1
    counter=$((counter + 1))
done

if [ $counter -eq $max_attempts ]; then
    echo -e "${RED}❌ PostgreSQL tardó demasiado${NC}"
    exit 1
fi

# ==================== LISTO ====================
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}✅ TODO LISTO!${NC}"
echo -e "${BLUE}========================================${NC}\n"

echo -e "${GREEN}Servicios disponibles:${NC}"
echo -e "   📍 PgAdmin:    http://localhost:5050"
echo -e "   📍 PostgreSQL: localhost:5432"
echo -e "   👤 PgAdmin User: admin@example.com"
echo -e "   🔐 PgAdmin Pass: admin"
echo -e "   📊 DB User: ecommerce_user"
echo -e "   🔑 DB Pass: ecommerce_password\n"

echo -e "${YELLOW}Ahora:${NC}"
echo -e "   1. Abre IntelliJ IDEA"
echo -e "   2. Abre proyecto: ~/proyectos/ecommerce-api"
echo -e "   3. Abre: src/main/java/com/ecommerce/EcommerceApplication.java"
echo -e "   4. Click flecha verde ▶️ para iniciar Spring Boot\n"

echo -e "${BLUE}========================================${NC}"
echo -e "Este terminal se mantiene abierto"
echo -e "Presiona Ctrl+C para detener servicios"
echo -e "${BLUE}========================================${NC}\n"

# Mantener abierto
tail -f /dev/null

EOF
