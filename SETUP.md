# msg-pipeline-validator-sesion-04 — Guía de Configuración

**Sesión 04 — SNS + Lambda Validator + SOLID + Patrones de Diseño**  
Especialista Spring Boot + AWS Serverless — Anku Academy 2026C2

---

## Arquitectura

```
API Gateway (POST /messages)
        │
        ▼
ValidatorHandler  ← Lambda Entry Point (APIGatewayProxyRequestEvent)
        │
        ▼
ValidateMessageUseCase
        │
        ├── ValidatorFactory.getStrategy(messageType)    [Patrón Factory]
        │           │
        │           ├── NotificationValidator              [Patrón Strategy]
        │           └── RecordValidator                    [Patrón Strategy]
        │
        └── MessageQueuePort.enqueue(payload)
                    │
              ┌─────┴──────┐
              │             │
  [aws]  SqsMessageQueueAdapter    [local]  InMemoryQueueAdapter
              │
              ▼
          SQS Queue (msg-pipeline-queue)
              │
              ▼
    Lambda Processor → DynamoDB + SNS
```

### Estructura del Proyecto (Hexagonal Architecture)

```
src/main/java/com/msgpipeline/validator/
├── ValidatorHandler.java                     ← Entry point Lambda (raíz del pkg)
├── domain/
│   ├── model/
│   │   └── MessagePayload.java               ← Entidad del dominio
│   └── port/
│       ├── in/
│       │   └── ValidateMessagePort.java      ← Puerto de entrada (Use Case Interface)
│       └── out/
│           └── MessageQueuePort.java         ← Puerto de salida (Queue Interface)
├── application/
│   ├── usecase/
│   │   └── ValidateMessageUseCase.java       ← Caso de uso principal
│   └── validation/
│       ├── strategy/
│       │   ├── ValidationStrategy.java       ← Interfaz Strategy (SOLID ISP)
│       │   ├── ValidationResult.java         ← Value Object resultado
│       │   ├── NotificationValidator.java    ← Strategy concreta para NOTIFICATION
│       │   └── RecordValidator.java          ← Strategy concreta para RECORD
│       └── factory/
│           └── ValidatorFactory.java         ← Factory Method (selección de strategy)
├── adapter/
│   ├── in/
│   │   └── web/
│   │       ├── ValidatorController.java      ← REST Controller (@Profile local)
│   │       └── dto/
│   │           ├── ValidateRequest.java      ← DTO de entrada con validaciones
│   │           └── ValidateResponse.java     ← DTO de respuesta estandarizada
│   └── out/
│       └── queue/
│           ├── SqsMessageQueueAdapter.java   ← Adaptador SQS (@Profile aws)
│           └── InMemoryQueueAdapter.java     ← Adaptador memoria (@Profile local)
└── config/
    ├── ValidatorApplication.java             ← Spring Boot entry point (local)
    ├── AppConfig.java                        ← Configuración centralizada
    ├── SqsConfig.java                        ← Beans AWS (@Profile aws)
    └── OpenApiConfig.java                    ← Swagger UI (@Profile local)
```

---

## Patrones de Diseño Aplicados

| Patrón | Clase | Descripción |
|--------|-------|-------------|
| **Strategy** | `ValidationStrategy`, `NotificationValidator`, `RecordValidator` | Algoritmos de validación intercambiables por tipo de mensaje |
| **Factory Method** | `ValidatorFactory.getStrategy()` | Selecciona la estrategia correcta según `messageType` |
| **Adapter** | `ValidatorHandler`, `ValidatorController`, `SqsMessageQueueAdapter` | Adaptan interfaces externas al contrato del dominio |
| **Value Object** | `ValidationResult`, `ValidationResponse` | Objetos inmutables sin identidad propia |
| **Repository / Gateway** | `MessageQueuePort`, `SqsMessageQueueAdapter`, `InMemoryQueueAdapter` | Abstrae el acceso a la cola SQS |

---

## Principios SOLID

| Principio | Aplicación |
|-----------|------------|
| **S** — Single Responsibility | `NotificationValidator` solo valida NOTIFICATION; `RecordValidator` solo RECORD |
| **O** — Open/Closed | Agregar tipo `AUDIT`: crear `AuditValidator` + registrar en factory. Sin modificar código existente |
| **L** — Liskov Substitution | `NotificationValidator` y `RecordValidator` sustituyen a `ValidationStrategy` sin alterar el comportamiento |
| **I** — Interface Segregation | `ValidationStrategy` tiene solo 3 métodos; `MessageQueuePort` tiene solo 1 |
| **D** — Dependency Inversion | `ValidateMessageUseCase` depende de `MessageQueuePort` (abstracción), no de `SqsMessageQueueAdapter` (detalle) |

---

## Reglas de Validación

### NOTIFICATION
| Campo | Regla |
|-------|-------|
| `messageType` | Obligatorio: `NOTIFICATION` |
| `channel` | Obligatorio: `EMAIL` \| `SMS` \| `PUSH` |
| `recipientEmail` | Obligatorio, debe contener `@` y `.` |
| `content` | Obligatorio, mínimo 10 caracteres, máximo 1000 caracteres |

### RECORD
| Campo | Regla |
|-------|-------|
| `messageType` | Obligatorio: `RECORD` |
| `content` | Obligatorio, máximo 5000 caracteres |
| `priority` | Obligatorio, rango 1–5 (1=baja, 5=crítica) |

---

## Desarrollo Local

### 1. Iniciar la aplicación

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

### 2. Acceder a Swagger UI

```
http://localhost:8081/swagger-ui.html
```

### 3. Probar con curl

**NOTIFICATION válida (esperar 202):**
```bash
curl -X POST http://localhost:8081/api/v1/validator/validate \
  -H 'Content-Type: application/json' \
  -d '{
    "messageType": "NOTIFICATION",
    "channel": "EMAIL",
    "recipientEmail": "test@test.com",
    "content": "Mensaje de prueba para validación en modo local"
  }'
```

**RECORD válido (esperar 202):**
```bash
curl -X POST http://localhost:8081/api/v1/validator/validate \
  -H 'Content-Type: application/json' \
  -d '{
    "messageType": "RECORD",
    "content": "Registro de auditoría generado correctamente",
    "priority": 3
  }'
```

**Ver mensajes encolados en memoria:**
```bash
curl http://localhost:8081/api/v1/validator/queue
```

---

## Despliegue en AWS Lambda

### 1. Compilar el ZIP

```bash
./gradlew clean buildZip
```

Artefacto generado: `build/distributions/msg-pipeline-validator-lambda.zip`

**Estructura interna del ZIP:**
```
msg-pipeline-validator-lambda.zip
├── com/msgpipeline/validator/ValidatorHandler.class     ← clases del proyecto (raíz)
├── com/msgpipeline/validator/application/...
├── com/msgpipeline/validator/domain/...
└── lib/
    ├── spring-boot-3.5.0.jar                            ← dependencias
    ├── aws-lambda-java-core-1.2.3.jar
    └── ...
```

### 2. Crear la función Lambda

En la consola AWS → Lambda → Crear función:

| Campo | Valor |
|-------|-------|
| Nombre | `msg-pipeline-validator` |
| Runtime | `Java 17` |
| Arquitectura | `x86_64` |
| Rol de ejecución | `msg-pipeline-lambda-role` |

### 3. Subir el ZIP

Lambda → Código → Subir desde → Archivo .zip  
→ Seleccionar `msg-pipeline-validator-lambda.zip`

### 4. Configurar el Handler

```
com.msgpipeline.validator.ValidatorHandler::handleRequest
```

### 5. Variables de entorno requeridas

| Variable | Valor |
|----------|-------|
| `SQS_QUEUE_URL` | `https://sqs.us-east-1.amazonaws.com/{accountId}/msg-pipeline-queue` |
| `AWS_REGION_NAME` | `us-east-1` |

> **Nota:** `AWS_REGION` la inyecta Lambda automáticamente. `AWS_REGION_NAME` es la variable que usa nuestra aplicación.

### 6. Aumentar el timeout

Lambda → Configuración → Configuración general:  
→ Tiempo de espera: **30 segundos** (Spring Boot cold start puede tomar ~5-8 segundos)

### 7. Configurar memoria

→ Memoria: **512 MB** mínimo recomendado para Spring Boot en Lambda

### 8. Conectar API Gateway

En API Gateway → POST /messages → Integration:
- Tipo de integración: **Lambda function**
- Función Lambda: `msg-pipeline-validator`
- Habilitar proxy Lambda: ✅
- Redesplegar la API

### 9. Rol IAM — permisos requeridos

Agregar al rol `msg-pipeline-lambda-role`:
- `AmazonSQSFullAccess` (para enviar mensajes a la cola)
- `AWSLambdaBasicExecutionRole` (para logs en CloudWatch)

---

## Respuestas de la API

### 202 Accepted — Mensaje válido y encolado

```json
{
  "success": true,
  "messageId": "550e8400-e29b-41d4-a716-446655440000",
  "messageType": "NOTIFICATION",
  "message": "Mensaje aceptado para procesamiento asíncrono"
}
```

### 400 Bad Request — Error de validación

```json
{
  "success": false,
  "message": "El email del destinatario es obligatorio para mensajes NOTIFICATION"
}
```

### 500 Internal Server Error — Error inesperado

```json
{
  "success": false,
  "message": "Error interno en el servicio de validación"
}
```

---

## Verificación del Flujo Completo (Sesión 04)

1. **POST** a API Gateway → validator retorna **202 Accepted**
2. Validator envía el mensaje a **SQS** (msg-pipeline-queue)
3. Lambda Processor consume el mensaje de SQS
4. Processor persiste en **DynamoDB** con status `COMPLETED`
5. Processor publica en **SNS** (msg-pipeline-email-notifications)
6. **Email** llega a la dirección suscripta al tópico

### CloudWatch — Logs esperados

**msg-pipeline-validator:**
```
ValidatorHandler — Inicialización Cold Start (Sesión 04)
Payload deserializado [tipo=NOTIFICATION] [canal=EMAIL]
Estrategia seleccionada: NotificationValidator
Validación NOTIFICATION exitosa [id=550e8400-...]
Mensaje encolado exitosamente [id=550e8400-...] [sqsMessageId=...]
Solicitud aceptada [messageId=550e8400-...]
```

**msg-pipeline-processor:**
```
SqsHandler — Inicialización Cold Start (Sesión 04)
Mensaje procesado exitosamente [messageId=550e8400-...]
Notificación SNS publicada exitosamente [messageId=550e8400-...]
```
