# ms-campuslab-bff

Backend For Frontend de CampusLab. Recibe las llamadas del frontend Angular y las reenvía al microservicio de reservas.

## Tecnologías

- Java 17
- Spring Boot
- Spring Security
- OAuth2 Resource Server
- JWT Azure AD / Microsoft Entra ID
- RestClient

## Funcionalidades EP1

- Valida access tokens emitidos por Azure AD.
- Valida `issuer`.
- Valida `audience` mediante `AudienceValidator`.
- Valida firma y expiración mediante OAuth2 Resource Server.
- Convierte roles de Azure AD a authorities de Spring Security.
- Aplica autorización por rol en endpoints.
- Responde `401` cuando no existe token o el token es inválido.
- Responde `403` cuando el token es válido, pero el usuario no tiene rol suficiente.
- Expone endpoints proxy hacia `ms-campuslab-bookings`.

## Configuración

Archivo: `src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/<TENANT_ID>/v2.0

campuslab:
  security:
    audience: <API_CLIENT_ID>
  services:
    bookings-url: http://localhost:8081
```

## Endpoints

- `GET /api/bookings`
- `POST /api/bookings`
- `GET /api/bookings/{id}`
- `PUT /api/bookings/{id}/status`

## Ejecutar localmente

```bash
mvn spring-boot:run
```

El BFF queda disponible en:

```text
http://localhost:8080
```
