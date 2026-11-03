# Transaction Service (Microservicio de Transacciones)

Microservicio orquestador encargado del procesamiento de transferencias de dinero entre cuentas corrientes dentro del sistema bancario distribuido. Implementa una arquitectura síncrona con el patrón Saga de compensación para garantizar la atomicidad (todo o nada) y consistencia en el balance de saldos.

## 1. Descripción de la Arquitectura

* **Framework:** Quarkus (Java 21+)

* **Capa Web:** RESTEasy Reactive con serialización JSON (Jackson) y Bean Validation.

* **Cliente HTTP:** Quarkus REST Client Reactive (comunicación inter-servicio hacia `account-service`).

* **Persistencia:** Hibernate ORM con Panache (patrón Active Record).

* **Base de Datos:** PostgreSQL independiente (`transaction_db`).

* **Seguridad:** Autenticación y Autorización basada en JWT (SmallRye JWT) con propagación automática de contexto hacia servicios dependientes.

* **Observabilidad:** SmallRye Health (`/q/health`), Micrometer Prometheus (`/q/metrics`), Manejo de Excepciones global y Logs estructurados con Correlation IDs.

* **Patrón de Consistencia:** Saga Síncrono (Validación -> Débito -> Crédito con compensación automática ante fallo).

### Flujo de Orquestación y Compensación (Saga)

1. **Validación:** Verifica que origen y destino sean distintos y consulta síncronamente su existencia en `account-service`.

2. **Registro:** Persiste la transacción con estado inicial `PENDING`.

3. **Paso 1 (Débito):** Solicita el débito a la cuenta origen. Si no hay saldo, marca la transacción como `FAILED`.

4. **Paso 2 (Crédito):** Solicita el crédito en la cuenta destino.

5. **Compensación (Rollback):** Si el crédito falla, emite inmediatamente una acreditación correctiva a la cuenta origen para restaurar su saldo y fija el estado de la transacción en `FAILED`.

6. **Finalización:** Si ambos pasos concluyen sin anomalías, fija el estado en `COMPLETED`.

## 2. Instrucciones de Ejecución

### Prerrequisitos

* Java Development Kit (JDK) 17 o superior.

* PostgreSQL local en el puerto `5432` con la base de datos `transaction_db` creada.

* Microservicios previos activos:

    * `customer-service` en `http://localhost:8081`

    * `account-service` en `http://localhost:8082`

### Pasos para iniciar

1. Clonar el repositorio y entrar a la carpeta del proyecto:

   ```
   cd transaction-service
   
   ```

2. Verificar o ajustar credenciales en `src/main/resources/application.properties`.

3. Ejecutar en modo desarrollo:

   ```
   ./mvnw quarkus:dev
   
   ```

   *En Windows (CMD/PowerShell):*

   ```
   mvnw.cmd quarkus:dev
   
   ```

El servicio iniciará en: `http://localhost:8083`

## 3. Endpoints Disponibles

| Método | Endpoint | Descripción | Requiere Auth | 
| ----- | ----- | ----- | ----- | 
| **POST** | `/transactions` | Inicia una transferencia monetaria entre cuentas (RF3) | **Sí (ROLE_USER)** | 
| **GET** | `/transactions/account/{accountNumber}` | Historial de transacciones de una cuenta (RF4) | No | 
| **GET** | `/transactions/{id}` | Consulta del detalle de una transacción por ID (RF4) | No | 
| **GET** | `/q/health` | Health checks de Liveness y Readiness | No | 
| **GET** | `/q/metrics` | Métricas del servicio en formato Prometheus | No | 

## 4. Observabilidad, Monitoreo y Manejo de Errores

Este servicio central actúa como el principal punto de monitoreo del negocio:

* **Health Checks (`/q/health`):** Incluye validación de base de datos y un `OrchestratorReadinessCheck` para confirmar que los clientes REST están inicializados y el patrón Saga activo.

* **Métricas de Negocio (`/q/metrics`):** Implementa medidores personalizados con Micrometer:

    * `transfers_successful_count_total`: Contador de transferencias exitosas.

    * `transfers_failed_count_total`: Contador de transferencias fallidas (incluye compensaciones).

    * `transfers_total_amount`: Gauge que rastrea el volumen total de dinero movido.

* **Trazabilidad (Correlation IDs):** Intercepta o genera un `X-Correlation-ID` en cada petición y lo propaga mediante MDC a los logs estructurados (JSON) y a las peticiones hacia el `account-service`.

* **Manejo de Errores Global:** Implementa `ExceptionMappers` (ej. `ValidationExceptionMapper`) para capturar excepciones como ConstraintViolations (Bean Validation) y traducirlas a respuestas JSON estandarizadas (RFC 7807), evitando la fuga de stacktraces.

## 5. Seguridad (JWT)

El endpoint de creación de transacciones requiere un token JWT firmado. Para garantizar la seguridad end-to-end, este orquestador clona el JWT recibido y el Correlation ID, propagándolos automáticamente hacia el `account-service` mediante la configuración `org.eclipse.microprofile.rest.client.propagateHeaders`.

> **🔒 Nota de Seguridad sobre JWT y Credenciales**
> Las llaves criptográficas para la verificación de tokens y las credenciales de base de datos se proveen "hardcodeadas" en `application.properties`, exclusivamente para facilitar la ejecución y evaluación del reto técnico en local. En producción, estos valores deben gestionarse mediante variables de entorno o un vault de secretos.

## 6. Ejemplos de Uso (cURL)

*Nota: Reemplaza `<TOKEN_AQUI>` con un JWT generado desde Quarkus Dev UI.*

### Iniciar Transferencia (Protegido)

```
curl -X POST http://localhost:8083/transactions \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN_AQUI>" \
  -d '{
    "sourceAccount": "456-789-001",
    "targetAccount": "456-789-002",
    "amount": 500.00,
    "description": "Pago de alquiler"
  }'

```

**Respuesta Exitosa (201 Created):**

```
{
  "id": 1,
  "sourceAccount": "456-789-001",
  "targetAccount": "456-789-002",
  "amount": 500.00,
  "transactionDate": "2026-10-31T18:08:09.3680124",
  "status": "COMPLETED",
  "description": "Pago de alquiler"
}

```

### Manejo de Errores (Ejemplo de respuesta estructurada)

Si se envía una petición inválida (ej. mismo origen y destino):

```
{
  "error": "La cuenta origen y destino no pueden ser la misma",
  "status": 400
}

```

### Consultar Historial de una Cuenta

```
curl -X GET http://localhost:8083/transactions/account/456-789-001

```

## 7. Datos y Credenciales de Prueba

### Conexión a Base de Datos Local

* **Host:** `localhost:5432`

* **Base de Datos:** `transaction_db`

* **Usuario / Contraseña:** `postgres` / `postgres`

### Cuentas Utilizadas en el Flujo

* **Cuenta Débito:** `456-789-001`

* **Cuenta Crédito:** `456-789-002`