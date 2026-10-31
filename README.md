# Transaction Service (Microservicio de Transacciones)

Microservicio orquestador encargado del procesamiento de transferencias de dinero entre cuentas corrientes dentro del sistema bancario distribuido. Implementa una arquitectura síncrona con el patrón Saga de compensación para garantizar la atomicidad (todo o nada) y consistencia en el balance de saldos.

---

## 1. Descripción de la Arquitectura

* **Framework:** Quarkus (Java 17+)
* **Capa Web:** RESTEasy Reactive con serialización JSON (Jackson) y Bean Validation
* **Cliente HTTP:** Quarkus REST Client Reactive (comunicación inter-servicio hacia `account-service`)
* **Persistencia:** Hibernate ORM con Panache (patrón Active Record)
* **Base de Datos:** PostgreSQL independiente (`transaction_db`)
* **Observabilidad:** SmallRye Health (`/q/health`) y Micrometer Prometheus (`/q/metrics`)
* **Patrón de Consistencia:** Saga Síncrono (Validación -> Débito -> Crédito con compensación automática ante fallo)

### Flujo de Orquestación y Compensación (Saga)
1. **Validación:** Verifica que origen y destino sean distintos y consulta síncronamente su existencia en `account-service`.
2. **Registro:** Persiste la transacción con estado inicial `PENDING`.
3. **Paso 1 (Débito):** Solicita el débito a la cuenta origen. Si no hay saldo, marca la transacción como `FAILED`.
4. **Paso 2 (Crédito):** Solicita el crédito en la cuenta destino.
5. **Compensación (Rollback):** Si el crédito falla, emite inmediatamente una acreditación correctiva a la cuenta origen para restaurar su saldo y fija el estado de la transacción en `FAILED`.
6. **Finalización:** Si ambos pasos concluyen sin anomalías, fija el estado en `COMPLETED`.

---

## 2. Instrucciones de Ejecución

### Prerrequisitos
* Java Development Kit (JDK) 17 o superior
* PostgreSQL local en el puerto `5432` con la base de datos `transaction_db` creada
* Microservicios previos activos:
    * `customer-service` en `http://localhost:8081`
    * `account-service` en `http://localhost:8082`

### Pasos para iniciar

1. Clonar el repositorio y entrar a la carpeta del proyecto:
   ```bash
   cd transaction-service
   ```

2. Verificar o ajustar credenciales en `src/main/resources/application.properties`:
   ```properties
   quarkus.http.port=8083
   quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/transaction_db
   quarkus.datasource.username=postgres
   quarkus.datasource.password=postgres
   quarkus.rest-client.account-api.url=http://localhost:8082
   ```

3. Ejecutar en modo desarrollo:
   ```bash
   ./mvnw quarkus:dev
   ```
   *En Windows (CMD/PowerShell):*
   ```cmd
   mvnw.cmd quarkus:dev
   ```

El servicio iniciará en: `http://localhost:8083`

---

## 3. Endpoints Disponibles

| Método | Endpoint | Descripción | Requiere Auth |
| :--- | :--- | :--- | :--- |
| **POST** | `/transactions` | Inicia una transferencia monetaria entre cuentas (RF3) | Pendiente JWT (ROLE_USER) |
| **GET** | `/transactions/account/{accountNumber}` | Historial de transacciones de una cuenta (RF4) | No |
| **GET** | `/transactions/{id}` | Consulta del detalle de una transacción por ID (RF4) | No |
| **GET** | `/q/health` | Health checks de Liveness y Readiness | No |
| **GET** | `/q/metrics` | Métricas del servicio en formato Prometheus | No |

---

## 4. Ejemplos de Uso (cURL)

### Iniciar Transferencia Exitosa
```bash
curl -X POST http://localhost:8083/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "sourceAccount": "456-789-001",
    "targetAccount": "456-789-002",
    "amount": 500.00,
    "description": "Pago de alquiler"
  }'
```

**Respuesta (201 Created):**
```json
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

### Consultar Historial de una Cuenta
```bash
curl -X GET http://localhost:8083/transactions/account/456-789-001
```

### Consultar Detalle por ID
```bash
curl -X GET http://localhost:8083/transactions/1
```

---

## 5. Datos y Credenciales de Prueba

### Conexión a Base de Datos Local
* **Host:** `localhost:5432`
* **Base de Datos:** `transaction_db`
* **Usuario / Contraseña:** `postgres` / `postgres`

### Cuentas Utilizadas en el Flujo
* **Cuenta Débito:** `456-789-001`
* **Cuenta Crédito:** `456-789-002`