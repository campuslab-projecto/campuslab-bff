# campuslab-bff

Backend For Frontend del sistema CampusLab, desarrollado con Spring Boot y Spring Security.

## Descripción

Este componente funciona como punto de entrada protegido para el frontend Angular.  
El BFF recibe las peticiones desde el frontend, valida el JWT emitido por Azure AD y redirige las solicitudes hacia los microservicios de dominio.

En el caso CampusLab, el flujo seguro definido es:

```text
JWT → API Gateway → ms-campuslab-bff → microservicio de dominio
```

## Tecnologías utilizadas

- Java
- Spring Boot
- Spring Security
- OAuth2 Resource Server
- JWT
- Azure AD
- Docker
- Docker Hub
- GitHub

## Responsabilidad del servicio

- Validar tokens JWT emitidos por Azure AD.
- Verificar autenticación antes de permitir el acceso a endpoints protegidos.
- Actuar como intermediario entre frontend y microservicios.
- Redirigir solicitudes hacia `ms-campuslab-bookings`.
- Responder con códigos adecuados ante accesos no autorizados.

## Seguridad

El BFF utiliza Spring Security como Resource Server OAuth2.

Configuración principal:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: https://login.microsoftonline.com/902cf874-0ee4-4917-b9cb-6b55af9993be/v2.0
```

Audience configurada:

```yaml
campuslab:
  security:
    audience: api://36ccc99d-6294-4333-a064-d62fa6237c7c/access_as_user
```

## Variables de entorno

| Variable | Descripción | Valor local |
|---|---|---|
| `BOOKINGS_SERVICE_URL` | URL del microservicio de reservas | `http://localhost:8081` |

En Docker Compose se usa comunicación interna:

```text
http://campuslab-ms-bookings:8081
```

## Endpoints expuestos

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/api/bookings` | Lista reservas |
| GET | `/api/bookings/{id}` | Obtiene una reserva por ID |
| POST | `/api/bookings` | Crea una reserva |
| PUT | `/api/bookings/{id}/status` | Actualiza estado de reserva |

## Ejecución local

```bash
mvn spring-boot:run
```

Puerto local:

```text
http://localhost:8080
```

## Ejecución con Docker

Construir imagen:

```bash
docker build -t campuslab-bff .
```

Ejecutar contenedor:

```bash
docker run --name campuslab-bff -p 8080:8080 -e BOOKINGS_SERVICE_URL=http://host.docker.internal:8081 campuslab-bff
```

## Imagen Docker Hub

```text
lukmezac/campuslab-bff:latest
```

Para descargar la imagen:

```bash
docker pull lukmezac/campuslab-bff:latest
```

## Evidencia esperada

Sin token:

```bash
curl http://localhost:8080/api/bookings
```

Respuesta esperada:

```text
401 Unauthorized
```

Con token válido desde Angular:

```text
200 OK
```

## Flujo de comunicación

```text
Angular Frontend → campuslab-bff → campuslab-ms-bookings
```

## Gestión del proyecto

Este repositorio se gestiona mediante GitHub Projects y metodología Kanban.

Flujo utilizado:

```text
Issue → Rama feature → Commit → Pull Request → Revisión → Merge a main
```

La rama `main` se mantiene protegida y los cambios se integran mediante Pull Request.
