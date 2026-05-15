# vivaeventos-tickets

Microservicio de boleteria digital para VivaEventos.

Responsabilidades:
- Emitir boletas digitales con codigo QR unico tras una compra confirmada.
- Validar boletas en puerta mediante el escaneo del QR.
- Impedir el uso doble de una boleta (un solo ingreso por boleta emitida).
- Permitir la revocacion de boletas (cancelaciones, fraude, devoluciones).
- Registrar el historial de validaciones para trazabilidad y soporte.

## Stack
- Spring Boot 3.4.3
- Java 21
- Maven
- PostgreSQL 16
- JWT (jjwt 0.12.5)
- Lombok

## Puertos
- HTTP: 8085
- Management/Actuator: 8086

## Variables de entorno
- `JWT_SECRET` (opcional) — clave para firmar/verificar tokens.

## Levantar localmente
```bash
mvn clean package
java -jar target/vivaeventos-tickets-0.1.0-SNAPSHOT.jar
```

## Levantar con Docker
```bash
docker build -t vivaeventos-tickets:0.1.0 .
docker run -p 8085:8085 -p 8086:8086 vivaeventos-tickets:0.1.0
```

## Seguridad y roles

Todos los endpoints (excepto `/actuator`) requieren JWT firmado por `auth` en el header
`Authorization: Bearer <token>`. El claim `role` del token determina lo que se puede hacer:

| Endpoint | Metodo | Roles permitidos | Respuesta sin permiso |
| --- | --- | --- | --- |
| `/api/v1/issued-tickets/issue` | POST | CLIENT, ORGANIZER, ADMIN | 403 |
| `/api/v1/issued-tickets/validate` | POST | ORGANIZER, ADMIN | 403 |
| `/api/v1/issued-tickets/{id}/revoke` | POST | ORGANIZER, ADMIN | 403 |
| `/api/v1/issued-tickets/**` (resto) | GET | cualquier autenticado | 401 |

Errores: token ausente o invalido → 401, rol insuficiente → 403.

> Nota: en una version productiva, `/issue` deberia exigir un token tipo SYSTEM (servicio
> a servicio), no CLIENT. Aqui se permite a CLIENT para que el frontend pueda orquestar la
> emision tras una compra exitosa.

## Endpoints principales

Prefijo: `/api/v1/issued-tickets`

> Nota: `events` ya posee `/api/v1/tickets` para los **tipos** de boleta (VIP, General, etc.).
> Este microservicio gestiona las **boletas digitales emitidas** (una por compra confirmada).

| Metodo | Ruta | Descripcion |
| --- | --- | --- |
| POST | `/issue` | Emite una boleta digital tras una compra confirmada. |
| GET | `/{id}` | Consulta una boleta por su id. |
| GET | `/qr/{qrCode}` | Consulta una boleta por su codigo QR. |
| GET | `/event/{eventId}` | Lista las boletas emitidas para un evento. |
| POST | `/validate` | Valida una boleta en puerta a partir del QR. |
| POST | `/{id}/revoke` | Revoca una boleta (cancelacion o fraude). |
| GET | `/{id}/validations` | Historial de intentos de validacion de la boleta. |

## Flujo esperado
1. El microservicio `vivaeventos-orders` confirma el pago.
2. Llama a `POST /api/v1/issued-tickets/issue` con `orderId`, `eventId`, `ticketTypeId`, datos del comprador y precio.
3. `tickets` genera un QR unico y persiste la boleta con estado `ISSUED`.
4. En la puerta del evento, logistica escanea el QR y dispara `POST /api/v1/issued-tickets/validate`.
5. Si la boleta esta `ISSUED`, se marca como `USED` y se registra la validacion.
6. Si la boleta ya esta `USED` o `REVOKED`, se rechaza el ingreso y se deja traza.
