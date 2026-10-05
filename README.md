# Banco XYZ - Microservicios resilientes y seguros

Actividad sumativa Semana 8 de Desarrollo Backend III: plataforma de microservicios para datos legacy del Banco XYZ, preparada para un entorno Cloud con Spring Cloud, OAuth2, Resilience4j, Kafka y Docker.

## Objetivo

Modernizar los datos base de `bank_legacy_data` y exponerlos mediante una arquitectura distribuida que incluye:

- Configuracion centralizada con Spring Cloud Config.
- Registro de servicios con Netflix Eureka.
- Seguridad OAuth2.0 con JWT emitidos por Keycloak.
- Tolerancia a fallos con Resilience4j Circuit Breaker y fallback local.
- Mensajeria asincrona con Kafka para eventos bancarios.
- Dockerizacion individual de cada microservicio.
- Orquestacion completa con `docker-compose.yaml`.

## Propuesta tecnica

La solucion separa responsabilidades en los siguientes componentes:

- `config-server`: publica configuracion centralizada desde `config-repo/`.
- `discovery-server`: registra servicios mediante Eureka.
- `bank-account-service`: consume Config Server, se registra en Eureka y expone API bancaria protegida por OAuth2.
- `event-audit-service`: consume eventos Kafka emitidos por el microservicio bancario y deja trazabilidad por logs.
- `keycloak`: servidor OAuth2 local con realm `bankxyz`.
- `kafka`: broker de eventos para arquitectura orientada a eventos.

El microservicio bancario lee `transacciones.csv`, `intereses.csv` y `cuentas_anuales.csv`, descarta registros invalidos y publica datos consistentes. La seguridad valida tokens Bearer JWT y extrae roles desde `realm_access.roles`. La resiliencia se evidencia en `/api/accounts/{cuentaId}/migration-report`, donde un fallo del auditor legacy simulado activa el fallback local. La mensajeria publica eventos en el topico `bank-account-events` al generar reportes de migracion y al solicitar retiros ATM; `event-audit-service` los consume de forma asincrona.

## Estructura

```text
.
├── bank-account-service/
│   ├── Dockerfile
│   └── src/main/java/cl/bancoxyz/bff
│       ├── config
│       ├── controller
│       ├── dto
│       ├── event
│       ├── model
│       └── service
├── config-repo/
│   └── bank-account-service.properties
├── config-server/
│   ├── Dockerfile
│   └── src/main/java/cl/bancoxyz/config
├── discovery-server/
│   ├── Dockerfile
│   └── src/main/java/cl/bancoxyz/discovery
├── event-audit-service/
│   ├── Dockerfile
│   └── src/main/java/cl/bancoxyz/audit
├── docs/evidencias/
├── keycloak/
│   └── bankxyz-realm.json
├── docker-compose.yaml
└── pom.xml
```

## Requisitos

- Java 17 o superior.
- Maven.
- Docker Desktop o Docker Engine con Docker Compose.

## Ejecutar pruebas

```bash
mvn -Dmaven.repo.local=target/.m2 test
```

## Levantar todo con Docker Compose

Desde la raiz del proyecto:

```bash
docker compose up --build
```

Puertos:

- Keycloak: `http://localhost:8080`
- Config Server: `http://localhost:8888`
- Eureka: `http://localhost:8761`
- Bank Account Service: `http://localhost:8081`
- Event Audit Service: `http://localhost:8082`
- Kafka externo local: `localhost:9094`

Credenciales de Keycloak:

- Administrador: `admin` / `admin123`
- Usuario API: `api-user` / `api123` con rol `BANK_API`
- Usuario ATM: `atm-user` / `atm123` con rol `ATM`

## Obtener tokens OAuth2

Token para APIs bancarias:

```bash
API_TOKEN=$(curl -s -X POST "http://localhost:8080/realms/bankxyz/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=bank-account-service" \
  -d "username=api-user" \
  -d "password=api123" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
```

Token para canal ATM:

```bash
ATM_TOKEN=$(curl -s -X POST "http://localhost:8080/realms/bankxyz/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=bank-account-service" \
  -d "username=atm-user" \
  -d "password=atm123" | sed -n 's/.*"access_token":"\([^"]*\)".*/\1/p')
```

## Endpoints principales

```bash
curl -s http://localhost:8888/bank-account-service/default
curl -s http://localhost:8761/eureka/apps
curl -s -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/accounts/106
curl -s -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/accounts/106/migration-report
curl -s -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/migration/quality
curl -s -H "Authorization: Bearer $ATM_TOKEN" http://localhost:8081/api/atm/cuentas/106/saldo
```

Ejemplo de rechazo por rol incorrecto:

```bash
curl -i -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/atm/cuentas/106/saldo
```

Para ver la comunicacion asincrona entre microservicios:

```bash
docker compose logs -f event-audit-service
```

## Evidencia

La evidencia de ejecucion con salidas de consola esta en:

- `docs/evidencias/evidencia-ejecucion.md`
