# Artisan Charcuterie - Curing Lifecycle Management API

A production-quality Spring Boot REST API for **The Marrow & Salt Collective**, managing the full lifecycle of long-term dry-cured meat batches (Prosciutto, Bresaola, Culatello) from raw intake to retail-ready inventory.

---

## Quick Start

```bash
# Clone / open the project in IntelliJ IDEA or VS Code
# Requires Java 17+ and Maven 3.8+

mv spring-boot:run
```

The application starts on **http://localhost:8080**.

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | Interactive Swagger UI |
| http://localhost:8080/api-docs | Raw OpenAPI JSON spec |
| http://localhost:8080/h2-console | H2 database console |

**H2 Console credentials:**
- JDBC URL: `jdbc:h2:mem:clmdb`
- Username: `sa`
- Password: *(empty)*

---

## Running Tests

```bash
mvn test
```

Tests cover:
- `ChamberTransitionServiceTest`: all workflow enforcement rules (18 cases)
- `InventoryValuationServiceTest`: weight-loss formula accuracy, aggregation (9 cases)
- `BatchControllerIntegrationTest`: HTTP layer, error responses, seed data (6 cases)

---

## Domain Model

```
Farm --< Batch >-- Chamber
           |
           |--< BatchTransition (audit trail)
           |--< QualityControlLog (immutable)
           |--- BatchValuation (computed, not persisted)

Chamber --< SensorReading

MarketSpotPrice (one per ProductType)
```

---

## API Reference

### Farms - `/api/farms`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/farms` | List all registered farms |
| GET | `/api/farms/{id}` | Get farm by ID |
| POST | `/api/farms` | Register a new farm |
| PUT | `/api/farms/{id}` | Update farm details |
| DELETE | `/api/farms/{id}` | Delete a farm |

**Every batch requires a valid `farmId`. This constraint is enforced at the database and service layers.**

---

### Chambers - `/api/chambers`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/chambers` | List chambers with occupancy |
| GET | `/api/chambers/{id}` | Get chamber by ID |
| POST | `/api/chambers` | Register a new chamber |
| PUT | `/api/chambers/{id}` | Update chamber configuration |
| DELETE | `/api/chambers/{id}` | Soft-deactivate a chamber |

**Chamber Types:** `COLD_SMOKE`, `FERMENTATION_ROOM`, `PRIMARY_AGING_CELLAR`

---

### Batches - `/api/batches`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/batches` | List batches (filter: `?status=`, `?productType=`, `?farmId=`) |
| GET | `/api/batches/{id}` | Get batch by numeric ID |
| GET | `/api/batches/code/{batchCode}` | Get batch by code (e.g., `PRO-2024-001` |
| POST | `/api/batches` | Register a new batch (GREEN status) |
| DELETE | `/api/batches/{id}` | Delete batch (GREEN status only) |

**Batch Create Request:**
```json
{
  "farmId": 1,
  "productType": "PROSCIUTTO",
  "animalBreed": "Large White x Landrace",
  "initialWeightKg": 12.500,
  "saltCureStartDate": "2024-10-15",
  "targetAgingMonths": 24,
  "notes": "Heritage breed - priority batch"
}
```

**Product Types:** `PROSCIUTTO` (standard 24mo, 30% loss), `BRESAOLA` (standard 6mo, 35% loss), `CULATELLO` (standard 36mo, 28% loss).

---

### Chamber Transition - `/api/batches/{id}/transitions`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/batches/{id}/transitions` | Full chain-of-custody history |
| POST | `/api/batches/{id}/transitions` | Transition to a new chamber |

**Transition to Retail-Ready** (set `targetChamberId: null`):
```json
{
  "targetChamberId": null,
  "performedBy": "M. Rossi - Head Salumiere",
  "notes": "24-month target achieved. Cleared for retail."
}
```

**Enforcement rules:**
- A GREEN batch cannot jump directly to an Aging Cellar
- A batch must spend a minimum of **72 hours** in fermentation before entering an Aging Cellar
- A batch can only be marked RETAIL_READY after meeting its `targetAgingMonths`
- RETAIL_READY and REJECTED are terminal states
- Chambers at full capacity cannot accept new batches

---

### Environmental Monitoring

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/chambers/{id}/sensor-readings` | List readings (most recent first) |
| POST | `/api/chambers/{id}/sensor-readings` | Ingest a sensor reading |
| GET | `/api/stress-alerts` | Active stress alerts - all chambers |
| GET | `/api/chambers/{id}/stress-alerts` | Stress alerts for one chamber |

**Sensor Reading Request:**
```json
{
  "temperatureCelsius": 13.80,
  "humidityPercent": 74.50,
  "sensorId": "SENS-ALPHA-01"
}
```

**Stress Alert Logic:**
A stress alert is raised when a chamber's temperature has been **outside the 2C safety band** of the chamber's target set-point for a **continuous period exceeding 4 hours**. The response includes all batches currently housed in the affected chamber.

---

### Quality Control Logging - `/api/batches/{id}/qc-logs`

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/batches/{id}/qc-logs` | List QC logs for batch (by week) |
| GET | `/api/batches/{id}/qc-logs/{logId}` | Get single log entry |
| POST | `/api/batches/{id}/qc-logs` | File a new QC log entry |
| PUT | `/api/batches/{id}/qc-logs/{logId}` | **Always 422**, logs are immutable |
| DELETE | `/api/batches/{id}/qc-logs/{logId}` | **Always 422**, logs are immutable |

**QC Log Request:**
```json
{
  "phLevel": 5.40,
  "aromaProfile": "Nutty, sweet, mild lactic notes. Excellent development",
  "inspector": "G. Ferrari - QC Dept",
  "weekNumber": 4,
  "notes": "Surface mold development consistent and healthy."
}
```

**Immutability:** Once a QC log is submitted, no field may be modified or deleted. Duplicate `weekNumber` entries for the same batch are rejected. This is enforced by the service layer to ensure regulatory compliance.

---

### Inventory Valuation

| Method | Path                         | Description |
|--------|------------------------------|-------------|
| GET | `/api/inventory/valuation`   | Aggregate value across all active batches |
| GET | `/api/batches/{id}/valuation | Single batch valuation | 
| GET | `/api/market-prices`         | List current spot prices |
| PUT | `api/market-prices/{productType}` | Update spot price |

**Weight-Loss Projection Formula:**
```
monthlyLossRate      = productType.standardWeightLossPercent / batch.targetAgingMonths
projectedLoss%       = min(monthElapsed x monthlyLossRate, standardLossPercent)
estimatedWeight (kg) = initialWeight x (1 - projectedLoss% / 100)
estimatedValue       = estimatedWeight x spotPricePerKg [rounded to 2dp, HALF_UP]
```

**Standard Weight-Loss Profiles:**

| Product | Standard Loss | Standard Aging |
|---------|---------------|----------------|
| Prosciutto | 30% | 24 months |
| Bresaola | 35% | 6 months |
| Culatello | 28% | 36 months |

---

## Error Responses

All errors returns a standardized JSON envelope:

```json
{
  "timestamp": "2024-03-01T14:30:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Batch must complete the Fermentation phase before entering the Primary Aging Cellar.",
  "path": "/api/batches/5/transitions"
}
```

| Status | Scenario | 
|--------|----------|
| 400 | Validation failure, malformed request |
| 404 | Resource not found |
| 422 | Illegal workflow transition, immutable resource modification attempt |
| 500 | Unexpected internal error |