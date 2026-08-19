# Configuración de Desarrollo

## Requisitos previos

- Docker y Docker Compose
- Java 21
- Maven

## Variables de entorno

Copia (o revisa) el archivo `.env` en la raíz del proyecto — `docker-compose.yml` lo lee automáticamente:

```
DB_USERNAME=ecommerce_user
DB_PASSWORD=ecommerce_password
DB_NAME=ecommerce_dev
```

> `.env` no se commitea (ver `.gitignore`).

## Iniciar Base de Datos

```bash
bash start-docker.sh
```

## Verificar conexión

```bash
docker exec -it ecommerce_postgres psql -U ecommerce_user -d ecommerce_dev -c "SELECT 1"
```

## Acceder a PgAdmin

- URL: http://localhost:5050
- Email: admin@example.com
- Password: admin

Para registrar el servidor Postgres dentro de PgAdmin, usa el host `postgres` (nombre del servicio en la red de Docker), puerto `5432`, y las credenciales de `.env`.

## Levantar la aplicación

```bash
mvn spring-boot:run
```

## Verificar que la app está viva

```bash
curl http://localhost:8080/actuator/health
```

## Detener todo

```bash
docker compose down
```

Para borrar también los datos persistidos (volumen `pgdata`):

```bash
docker compose down -v
```
