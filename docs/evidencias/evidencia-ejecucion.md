# Evidencia de ejecucion

Pruebas ejecutadas desde la raiz del proyecto.

## Build y pruebas

Comando:

```bash
mvn -Dmaven.repo.local=target/.m2 test
```

Salida relevante:

```text
Reactor Summary for bank-cloud-platform 0.0.1-SNAPSHOT:
bank-cloud-platform ................................ SUCCESS
config-server ...................................... SUCCESS
discovery-server ................................... SUCCESS
bank-account-service ............................... SUCCESS
event-audit-service ................................ SUCCESS

Tests run: 4, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Validacion de docker-compose

Comando:

```bash
docker compose config
```

Salida relevante:

```text
services:
  keycloak:
    image: quay.io/keycloak/keycloak:25.0
  kafka:
    image: bitnami/kafka:3.7
  config-server:
    build:
      dockerfile: config-server/Dockerfile
  discovery-server:
    build:
      dockerfile: discovery-server/Dockerfile
  bank-account-service:
    build:
      dockerfile: bank-account-service/Dockerfile
  event-audit-service:
    build:
      dockerfile: event-audit-service/Dockerfile
```

## OAuth2 con Keycloak

Comando para obtener token de API:

```bash
curl -s -X POST "http://localhost:8080/realms/bankxyz/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=bank-account-service" \
  -d "username=api-user" \
  -d "password=api123"
```

Salida esperada:

```json
{
  "access_token": "eyJ...",
  "expires_in": 300,
  "token_type": "Bearer"
}
```

El token contiene el rol `BANK_API` dentro de `realm_access.roles`.

## Config Server

Comando:

```bash
curl -s http://localhost:8888/bank-account-service/default
```

Salida relevante:

```json
{
  "name": "bank-account-service",
  "profiles": ["default"],
  "propertySources": [
    {
      "source": {
        "server.port": "8081",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri": "http://localhost:8080/realms/bankxyz/protocol/openid-connect/certs",
        "bank.resilience.force-legacy-audit-failure": "true",
        "bank.events.kafka.topic": "bank-account-events"
      }
    }
  ]
}
```

## Service Discovery

Comando:

```bash
curl -s http://localhost:8761/eureka/apps
```

Salida relevante:

```xml
<applications>
  <application>
    <name>BANK-ACCOUNT-SERVICE</name>
    <instance>
      <status>UP</status>
      <vipAddress>bank-account-service</vipAddress>
    </instance>
  </application>
</applications>
```

## API de datos migrados

Comando:

```bash
curl -s -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/accounts/106
```

Salida relevante:

```json
{
  "cuenta": {
    "cuentaId": 106,
    "nombre": "Jane Smith",
    "saldo": 12000,
    "edad": 40,
    "tipo": "prestamo"
  },
  "saldoDisponible": 12000
}
```

## Tolerancia a fallos

El entorno Docker activa `bank.resilience.force-legacy-audit-failure=true`. Por eso el auditor legacy simulado falla y Resilience4j responde mediante fallback local.

Comando:

```bash
curl -s -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/accounts/106/migration-report
```

Salida relevante:

```json
{
  "origen": "fallback-local-cache",
  "cuentaId": 106,
  "estado": "VALIDATED_WITH_FALLBACK"
}
```

## Seguridad por roles

Consulta permitida para rol `ATM`:

```bash
curl -s -H "Authorization: Bearer $ATM_TOKEN" http://localhost:8081/api/atm/cuentas/106/saldo
```

Salida:

```json
{"canal":"atm","cuentaId":106,"saldoDisponible":12000}
```

Consulta rechazada al intentar usar el token de API en una ruta ATM:

```bash
curl -i -H "Authorization: Bearer $API_TOKEN" http://localhost:8081/api/atm/cuentas/106/saldo
```

Salida relevante:

```text
HTTP/1.1 403
WWW-Authenticate: Bearer error="insufficient_scope"
```

## Mensajeria asincrona

Con `BANK_EVENTS_KAFKA_ENABLED=true`, `bank-account-service` publica eventos JSON en el topico `bank-account-events` y `event-audit-service` los consume de forma asincrona.

Eventos implementados:

```text
MIGRATION_REPORT_GENERATED
ATM_WITHDRAWAL_REQUESTED
```

Ejemplo de consumo desde Docker:

```bash
docker compose logs -f event-audit-service
```

Salida esperada:

```text
Evento bancario auditado: tipo=MIGRATION_REPORT_GENERATED, cuentaId=106, canal=bank-account-service, estado=VALIDATED_WITH_FALLBACK
Evento bancario auditado: tipo=ATM_WITHDRAWAL_REQUESTED, cuentaId=106, canal=atm, estado=APPROVED
```
