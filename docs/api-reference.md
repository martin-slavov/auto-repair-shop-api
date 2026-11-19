# Auto Repair Shop API — Reference for Frontend

This document is generated from the code (controllers, DTOs, `SecurityConfig`, `GlobalExceptionHandler`,
`PageRequestFactory`, the services and specifications). Anything not confirmed in code is marked **(verify)**.

**Basics**

- Base path: every endpoint starts with `/api`. Locally: `http://localhost:8080` (Spring Boot's default port,
  unless `server.port` is set). Other hosts depend on the deployment.
- Swagger UI: `/swagger-ui.html`, OpenAPI JSON: `/v3/api-docs` (can be switched off per environment).
- All bodies are JSON (`Content-Type: application/json`).
- Enums are sent and returned as strings (`"PENDING"`, `"ADMIN"`, ...). Unknown values are rejected (see 2.2).
- Dates (Spring Boot defaults, Jackson 3): dates are strings, never numeric timestamps.
  - `LocalDate` = `"yyyy-MM-dd"`.
  - `LocalDateTime` = ISO-8601 **without time zone** (`DateTimeFormatter.ISO_LOCAL_DATE_TIME`). Seconds are
    always present and the fraction appears only when it is non-zero, e.g. `"2026-11-20T09:00:00"` or
    `"2026-11-15T10:42:17.123456"`.
  - Server-side timestamps (`createdAt`, `issuedDate`, `addedAt`, `completedDate`) are in the server's local
    time. Parse them as local time (JavaScript `new Date("2026-11-20T09:00:00")` already does that).
  - When sending a `LocalDateTime` (`scheduledDate`), use the same format without a zone, e.g. `"2026-11-20T09:00:00"`.
- Money and hours are `BigDecimal` and are returned as JSON **numbers** (e.g. `30.00`, not `"30.00"`).
  JavaScript drops trailing zeros, so format amounts for display yourself.
- IDs are numbers (`Long`).

---

## 1. Authentication

### 1.1 Register — `POST /api/auth/register` (public)

Creates a **CUSTOMER** account. Field rules: see 5.1.

```json
{
  "username": "jdoe",
  "email": "jdoe@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "password": "secret123",
  "confirmPassword": "secret123"
}
```

Response `201 Created`:

```json
{ "id": 7, "username": "jdoe", "email": "jdoe@example.com", "role": "CUSTOMER" }
```

Errors: `400` validation; `409` `"Username is already taken"` or `"Email is already in use"`.

> If `password` and `confirmPassword` differ, the response is `400 "Validation failed"`, but the mismatch is
> a class-level error and is **not** listed in `fieldErrors` (that map only contains field errors). Check the
> match in the form before sending.

### 1.2 Login — `POST /api/auth/login` (public)

```json
{ "username": "jdoe", "password": "secret123" }
```

Response `200 OK`:

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9..." }
```

Errors: `400` if a field is blank (default Bean Validation message, see the note in 2.1);
`401 "Invalid username or password"` for an unknown user, a wrong password **or a disabled account** (the
same message on purpose, so the response never reveals whether an account exists).

The token is the only thing returned. It does not include the user's id. The JWT payload carries `sub`
(username), `role`, `iat`, `exp`. You can decode it to show the username and role, but the server never
trusts the `role` claim: it loads the role from the database on every request.

### 1.3 Sending the token

```
Authorization: Bearer <token>
```

- The header must start with exactly `Bearer ` (with a space). Any other form is ignored, and the request
  is treated as anonymous.
- Lifetime: set by the `jwt.expiration` setting, in milliseconds, for example `86400000` = 24 hours. The real
  value can differ per environment. Read the `exp` claim of the token instead of hard-coding it.
  There is **no refresh endpoint**. When the token expires, log in again.
- There is no logout endpoint. Logging out = deleting the token on the client.

### 1.4 Roles

| Role       | How the account is created                                      |
|------------|-----------------------------------------------------------------|
| `CUSTOMER` | Self-registration (`POST /api/auth/register`)                    |
| `MECHANIC` | By an admin (`POST /api/admin/users/mechanic`)                   |
| `ADMIN`    | By an admin (`POST /api/admin/users/admin`)                      |

Roles are fixed at creation. No endpoint changes a role.

### 1.5 Expired token, invalid token, disabled user

On every request the filter checks the token signature and expiry, loads the user from the database and
checks `enabled`. If any check fails, the request continues **as anonymous** (no error by itself):

| Situation                                   | Protected endpoint                         | Public endpoint (auth, catalog GET)         |
|---------------------------------------------|--------------------------------------------|---------------------------------------------|
| No / malformed / expired / bad-signature token | `401 "Authentication is required"`       | Works, as a guest                           |
| Valid token, user disabled since login      | `401 "Authentication is required"`         | Works, as a guest                           |
| Valid token, wrong role                     | `403 "You don't have permission to access this resource"` | —                            |
| Login with disabled account                 | `401 "Invalid username or password"`       | —                                           |

So on any `401` from a protected endpoint, drop the token and send the user to the login page.

### 1.6 Change own password — `PATCH /api/users/me/password` (any logged-in user)

```json
{ "currentPassword": "secret123", "newPassword": "newSecret456" }
```

Response `204 No Content`. Errors: `400 "Current password is incorrect"`,
`400 "New password must be different from the current password"`, `400` validation (see 5.3).
Tokens issued before the change **stay valid** until they expire.

---

## 2. Errors

### 2.1 ErrorResponse

Every error produced by the application has this shape:

```json
{
  "status": 400,
  "message": "Validation failed",
  "timestamp": "2026-11-15T10:42:17.123456",
  "fieldErrors": {
    "licensePlate": "License plate must not exceed 8 characters",
    "year": "Invalid Year"
  }
}
```

- `fieldErrors` maps field name → message, with **one message per field** (if a field breaks two rules, only one
  is kept). It is filled only for validation errors and is `{}` for all other errors.
- Validation messages are the ones in section 5. Show `fieldErrors[field]` next to the input, and `message`
  as a general error.
- **Language of messages:** the custom messages in the DTOs (e.g. `"License plate is required"`) are always
  English. Fields that have no custom message use Hibernate Validator's default message, which is translated
  according to the request's `Accept-Language` header (Spring's `LocaleContextMessageInterpolator`), e.g.
  `"must not be blank"` (en) or `"darf nicht leer sein"` (de). These fields are:
  - `LoginRequestDTO`: `username`, `password`
  - `RegisterRequestDTO`: `password` (blank only)
  - `ChangePasswordDTO`: `newPassword` (blank only)
  - `VisitPartCreateDTO`: all fields

  To get English, send `Accept-Language: en`, or map these fields to your own texts.

### 2.2 Status codes the frontend must handle

| Status | Typical cause | Example `message` |
|--------|---------------|-------------------|
| `400` | Body validation failed | `"Validation failed"` + `fieldErrors` |
| `400` | Invalid JSON, missing body, wrong type or unknown enum **in the body** | `"Malformed request body"` |
| `400` | Wrong type or unknown enum **in a query/path parameter** (e.g. `status=FOO`, `issuedFrom=15.11.2026`, `/api/vehicles/abc`) | `"Invalid value for parameter 'status'"` |
| `400` | Bad `sort` value | `"Unknown sort field 'price'. Allowed: ..."` |
| `400` | Range filter inverted | `"yearFrom must not be greater than yearTo"`, `"issuedFrom must not be after issuedTo"` |
| `400` | Action not allowed in the current status | `"Only pending requests can be approved"`, `"Invalid status transition"`, `"Visit status must be COMPLETED"` |
| `400` | Booking rule (approve / reschedule, see 4.5.1) | `"Scheduled date must be on a working day (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)"`, `"Scheduled time must be between 09:00 and 16:00 (working hours 09:00-17:00, a visit takes 60 minutes)"`, `"Scheduled time must start on a 30-minute slot (e.g. 09:00, 09:30)"` |
| `400` | Assistants (4.5.3, 5.11) | `"A visit can have at most 3 assistants"`, `"The lead mechanic cannot be removed"`, `"Assistants can only be changed on scheduled or in-progress visits"`, `"Hours are required for every assistant (missing assignment ids: 31)"`, `"Unknown assistant assignment: 30"` |
| `400` | Other business rule | `"Part is not active"`, `"User with ID 5 is not a mechanic"`, `"Mechanic account is disabled"`, `"Current password is incorrect"`, `"You cannot disable your own account"` |
| `401` | Not logged in, token expired/invalid, account disabled | `"Authentication is required"` |
| `401` | Login failed | `"Invalid username or password"` |
| `403` | Role not allowed for this endpoint | `"You don't have permission to access this resource"` |
| `403` | Right role, but not your resource (ownership / assignment) | `"You do not have permission to view this vehicle"`, `"You can only modify service requests for your own vehicles"`, `"Mechanic is not assigned to this service visit"` |
| `404` | ID does not exist (or a non-admin asks for an inactive catalog item) | `"Vehicle not found"`, `"Service request not found with ID: 12"` |
| `403` | Unknown URL, or a real path called with a method that has no rule (logged-in user) | `"You don't have permission to access this resource"` |
| `409` | Duplicate unique value | `"Username is already taken"`, `"Vehicle with this license plate already exists"`, `"Part with this serial number already exists"`, `"Catalog item with this name already exists"` |
| `409` | Resource still in use | `"Vehicle cannot be deleted because it has service requests or visits"`, `"Cannot disable the last enabled admin"` |
| `409` | Mechanic already booked (approve / reschedule / add assistant) | `"Mechanic is already booked at this time"` |
| `409` | Mechanic already on the visit (add assistant) | `"Mechanic is already assigned to this visit"` |
| `409` | Database constraint | `"The data conflicts with existing data"` |

Also possible: `415 "Unsupported Media Type"` (body not JSON) and `500 "Unexpected error"`. `404 "Not Found"`
and `405 "Method Not Allowed"` are rare. They occur only under the broad rules that cover a whole path prefix
(`/api/auth/**`, `/api/admin/**` for ADMIN, `/api/service-visits/**` for MECHANIC, `GET /api/catalog-items/**`).
For example, `GET /api/auth/login` returns 405.

Note: `SecurityConfig` ends with `.anyRequest().denyAll()` (default deny). Any URL or method without an
explicit rule is refused before it reaches a controller: **`401` for a guest**, **`403` for a logged-in user**.
So an unknown URL never gives `404` outside the broad rules above.

### 2.3 Two shapes of 401 / 403

The `401 "Authentication is required"` and `403 "You don't have permission..."` that come from the **security
filter** (no token, or wrong role for the URL) are written by hand in `SecurityConfig` and have **no
`fieldErrors` key**:

```json
{ "status": 401, "message": "Authentication is required", "timestamp": "2026-11-15T10:42:17.123456" }
```

The 401/403 produced by the **exception handler** (failed login, ownership checks) have `"fieldErrors": {}`.
Treat `fieldErrors` as optional.

The `timestamp` formats also differ slightly. Both are ISO-8601 local date-times without a zone:

- **Filter** (`LocalDateTime.toString()`): the fraction is printed in groups of 3, 6 or 9 digits, and the seconds are
  left out entirely when seconds and fraction are both zero (e.g. `"2026-11-15T10:42"`).
- **Handler** (Jackson, `ISO_LOCAL_DATE_TIME`): seconds are always printed, and the fraction only with as many
  digits as needed.

Parse `timestamp` with a lenient ISO parser (JavaScript `Date` handles both) and don't compare it as a string.

---

## 3. Lists (paging, sorting, filters)

All list endpoints except `GET /api/service-visits/{id}/parts` (a plain array) return a `PageResponse`.

### 3.1 PageResponse

```json
{
  "content": [ { "...": "..." } ],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```

`page` and `size` are the values that were actually used (after the rules below).

### 3.2 page / size

| Param | Default | Rule |
|-------|---------|------|
| `page` | `0` | 0-based. A negative value becomes `0`. A page past the end returns `content: []`. |
| `size` | `10` | A value below `1` becomes `10`. A value above `50` becomes `50` (no error). |

A non-numeric value returns `400 "Invalid value for parameter 'page'"`.

### 3.3 sort

- Format: `sort=field` or `sort=field,asc` / `sort=field,desc`. Only **one** field is allowed.
- The field name is case-sensitive and must be in the endpoint's whitelist (below). The direction is
  case-insensitive. Without a direction, the default is `asc`.
- Errors (`400`): unknown field → `"Unknown sort field 'x'. Allowed: a, b, c"`; unknown direction →
  `"Unknown sort direction 'x'. Allowed: asc, desc"`; more than one comma →
  `"Sort must be in the format 'field' or 'field,asc|desc' (only one sort field is allowed)"`.
- `id asc` is always added as a tie-breaker, so the order across pages is stable.

**Filter rules shared by all lists:** every filter is optional and filters are combined with AND. Text
filters are case-insensitive *contains* (trimmed; `%` and `_` are matched literally; a blank value is ignored).
License-plate filters also remove spaces first (`"ca 12"` finds `CA1234AB`). Date filters use `yyyy-MM-dd`,
both bounds inclusive. Price/total bounds are inclusive.

### 3.4 Per list endpoint

#### `GET /api/vehicles` — CUSTOMER, ADMIN
Customers see only their own vehicles. Admins see all.

| Filter | Type | Notes |
|--------|------|-------|
| `licensePlate` | string | contains, spaces ignored |
| `manufacturer` | string | contains |
| `model` | string | contains |
| `yearFrom`, `yearTo` | integer | inclusive; `yearFrom > yearTo` → 400 |
| `ownerId` | long | ADMIN only; ignored for customers |

Sort: `licensePlate`, `manufacturer`, `model`, `year`, `firstRegistrationDate`. Default `licensePlate,asc`.

#### `GET /api/service-requests` — CUSTOMER, ADMIN
Customers see requests for their own vehicles. Admins see all requests **in every status** (no default status filter).

| Filter | Type | Notes |
|--------|------|-------|
| `status` | enum | `PENDING`, `APPROVED`, `REJECTED` |
| `type` | enum | `SERVICE`, `INSPECTION`, `CONSULTATION` |
| `preferredFrom`, `preferredTo` | date | inclusive; requests without a preferred date never match; from > to → 400 |
| `licensePlate` | string | contains, spaces ignored |
| `vehicleId` | long | still limited to the caller's scope |

Sort: `preferredDate`, `status`, `type`, `createdAt`. Default `createdAt,desc`.

#### `GET /api/service-visits` — ADMIN, MECHANIC, CUSTOMER
Admins see all visits. Mechanics see visits they are assigned to. Customers see visits of their own vehicles.

| Filter | Type | Notes |
|--------|------|-------|
| `status` | enum | `SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `INVOICED` (exact match) |
| `scheduledFrom`, `scheduledTo` | date | date part of `scheduledDate`, inclusive; from > to → 400 |
| `licensePlate` | string | contains, spaces ignored |
| `vehicleId` | long | still limited to the caller's scope |
| `mechanicId` | long | ADMIN only; ignored for other roles |

Sort: `scheduledDate`, `completedDate`, `status`. Default `scheduledDate,desc`.

#### `GET /api/parts` — ADMIN, MECHANIC
Admins see all parts and get `PartResponseDTO`. Mechanics see **active parts only** and get `PartCatalogDTO`
(no `unitCost`).

| Filter | Type | Notes |
|--------|------|-------|
| `name` | string | contains |
| `serialNumber` | string | contains |
| `minPrice`, `maxPrice` | decimal | on `unitSellPrice`, inclusive; min > max → 400 |
| `active` | boolean | ADMIN only; ignored for mechanics |

Sort: `name`, `serialNumber`, `unitSellPrice`; admins also `unitCost` (a mechanic who sorts by `unitCost`
gets 400). Default `name,asc`.

#### `GET /api/catalog-items` — public
Guests and non-admins see active items only. With an ADMIN token you see all items and the `active` filter works.

| Filter | Type | Notes |
|--------|------|-------|
| `name` | string | contains |
| `minPrice`, `maxPrice` | decimal | on `basePrice`, inclusive; min > max → 400 |
| `active` | boolean | ADMIN only; ignored otherwise |

Sort: `name`, `price` (= `basePrice`), `estimatedHours`, `createdAt`. Default `name,asc`.

#### `GET /api/invoices` — CUSTOMER, ADMIN
Customers see invoices for their own vehicles. Admins see all.

| Filter | Type | Notes |
|--------|------|-------|
| `paid` | boolean | |
| `invoiceNumber` | string | contains |
| `licensePlate` | string | contains, spaces ignored |
| `issuedFrom`, `issuedTo` | date | date part of `issuedDate`, inclusive; from > to → 400 |
| `minTotal`, `maxTotal` | decimal | on `totalPrice`, inclusive; min > max → 400 |
| `ownerId` | long | ADMIN only; ignored for customers |

Sort: `invoiceNumber`, `totalPrice`, `issuedDate`, `paid`. Default `issuedDate,desc`.

#### `GET /api/admin/users` — ADMIN

| Filter | Type | Notes |
|--------|------|-------|
| `username` | string | contains |
| `email` | string | contains |
| `role` | enum | `ADMIN`, `MECHANIC`, `CUSTOMER` |
| `enabled` | boolean | |

Sort: `username`, `email`, `role`, `enabled`, `id`. Default `username,asc`.

---

## 4. Endpoints

Roles come from `SecurityConfig`. "Own" rules are checked in the services (a violation gives `403`).
Request body field rules are in section 5.

### 4.0 Response objects

| Object | Fields |
|--------|--------|
| `RegisterResponseDTO` | `id`, `username`, `email`, `role` |
| `UserAdminDTO` | `id`, `username`, `email`, `firstName`, `lastName`, `role`, `enabled` |
| `VehicleResponseDTO` | `id`, `licensePlate`, `manufacturer`, `model`, `year`, `ownerId`, `ownerName` ("First Last"), `firstRegistrationDate` (nullable) |
| `ServiceRequestResponseDTO` | `id`, `description`, `status`, `type`, `createdAt`, `licensePlate`, `preferredDate` (nullable) |
| `ServiceVisitResponseDTO` | `id`, `status`, `scheduledDate`, `completedDate` (nullable), `technicalNotes` (nullable), `licensePlate`, `mechanics[]`, `requestType` (`SERVICE` / `INSPECTION` / `CONSULTATION`), `requestDescription` (the work to do), `vehicleManufacturer`, `vehicleModel`, `myRole` (the calling mechanic's role in the visit, `LEAD` / `ASSISTANT`; `null` for ADMIN and CUSTOMER). `requestType` and `requestDescription` come from the service request the visit was created from and are `null` if the visit has no linked request |
| `MechanicAssignmentDTO` (in `mechanics[]`) | `mechanicName` ("First Last"), `roleInVisit` (`LEAD` / `ASSISTANT`), `hoursWorked` (`0.00` until the visit is completed), `mechanicId` (the mechanic's user id; **ADMIN only**, `null` for MECHANIC and CUSTOMER), `assignmentId` (id of this assignment; ADMIN and mechanics assigned to the visit, `null` for CUSTOMER) |
| `VisitPartResponseDTO` | `id` (= part line id), `partName`, `quantity`, `priceAtTimeOfUse` (**line total** = unit price × quantity), `addedAt` |
| `PartResponseDTO` (admin) | `id`, `name`, `serialNumber`, `unitCost`, `unitSellPrice`, `active` |
| `PartCatalogDTO` (mechanic) | `id`, `name`, `serialNumber`, `unitSellPrice`, `active` |
| `CatalogItemResponseDTO` | `id`, `name`, `description`, `basePrice`, `active`, `createdAt`, `estimatedHours` |
| `InvoiceResponseDTO` | `id`, `invoiceNumber`, `licensePlate`, `partsPrice`, `laborPrice`, `totalPrice`, `issuedDate`, `paid` |
| `InvoiceDetailDTO` (single invoice) | all `InvoiceResponseDTO` fields, plus `laborHours` (total `hoursWorked` of all assignments), `laborRate` (hourly rate, `30.00`), `partLines[]` |
| `AvailableSlotDTO` (array from `available-slots`) | `start` (`"yyyy-MM-ddTHH:mm:ss"`, server local time), `available` (boolean) |
| `InvoicePartLineDTO` (in `partLines[]`) | `partName`, `quantity`, `unitPrice` (= `lineTotal` ÷ `quantity`, 2 decimals), `lineTotal` (= the part line's `priceAtTimeOfUse`) |

Responses contain no ids for the linked vehicle, request or visit (only `licensePlate`), except
`VehicleResponseDTO.ownerId`, `MechanicAssignmentDTO.mechanicId` (admin only) and
`MechanicAssignmentDTO.assignmentId` (admin and assigned mechanics).

### 4.1 Auth

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| POST | `/api/auth/register` | public | `RegisterRequestDTO` | 201 | `RegisterResponseDTO` (role `CUSTOMER`) |
| POST | `/api/auth/login` | public | `LoginRequestDTO` | 200 | `{ "token": "..." }` |

### 4.2 Catalog items

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/catalog-items` | public | — | 200 | `PageResponse<CatalogItemResponseDTO>` |
| GET | `/api/catalog-items/{id}` | public | — | 200 | `CatalogItemResponseDTO` (inactive → 404 unless ADMIN) |
| POST | `/api/catalog-items` | ADMIN | `CatalogItemCreateDTO` | 201 | `CatalogItemResponseDTO` (`active: true`) |
| PUT | `/api/catalog-items/{id}` | ADMIN | `CatalogItemUpdateDTO` | 200 | `CatalogItemResponseDTO` |
| DELETE | `/api/catalog-items/{id}` | ADMIN | — | 204 | — (hard delete; to hide an item, set `active: false`) |

### 4.3 Vehicles

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/vehicles` | CUSTOMER, ADMIN | — | 200 | `PageResponse<VehicleResponseDTO>` |
| GET | `/api/vehicles/{id}` | CUSTOMER (own), ADMIN | — | 200 | `VehicleResponseDTO` |
| POST | `/api/vehicles` | CUSTOMER | `VehicleCreateDTO` | 201 | `VehicleResponseDTO` (owner = caller) |
| PUT | `/api/vehicles/{id}` | CUSTOMER (own), ADMIN | `VehicleUpdateDTO` | 200 | `VehicleResponseDTO` |
| DELETE | `/api/vehicles/{id}` | CUSTOMER (own), ADMIN | — | 204 | — (409 if it has service requests or visits) |

Admins cannot create vehicles (POST is CUSTOMER only).

### 4.4 Service requests

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/service-requests` | CUSTOMER, ADMIN | — | 200 | `PageResponse<ServiceRequestResponseDTO>` |
| POST | `/api/service-requests` | CUSTOMER (own vehicle) | `ServiceRequestCreateDTO` | 201 | `ServiceRequestResponseDTO` (status `PENDING`) |
| PUT | `/api/service-requests/{id}` | CUSTOMER (own), ADMIN | `ServiceRequestUpdateDTO` | 200 | `ServiceRequestResponseDTO` (PENDING only) |
| DELETE | `/api/service-requests/{id}` | CUSTOMER (own), ADMIN | — | 204 | — (PENDING only) |
| PATCH | `/api/service-requests/{id}/approve` | ADMIN | `ApproveRequestDTO` | 200 | `ServiceRequestResponseDTO` (status `APPROVED`; booking rules in 4.5.1, 409 if the mechanic is booked) |
| PATCH | `/api/service-requests/{id}/reject` | ADMIN | — | 200 | `ServiceRequestResponseDTO` (status `REJECTED`) |

After a request is created, an email notification goes to the shop asynchronously. This does not affect the response.

### 4.5 Service visits

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/service-visits` | ADMIN, MECHANIC, CUSTOMER | — | 200 | `PageResponse<ServiceVisitResponseDTO>` |
| PATCH | `/api/service-visits/{id}/status` | MECHANIC (assigned) | `UpdateVisitStatusDTO` | 200 | `ServiceVisitResponseDTO` |
| PATCH | `/api/service-visits/{id}/complete` | MECHANIC (lead) | `CompleteVisitDTO` | 200 | `ServiceVisitResponseDTO` (hours of every assistant required, see 5.11) |
| POST | `/api/service-visits/{id}/mechanics` | ADMIN | `AddAssistantDTO` | 200 | `ServiceVisitResponseDTO` (all assignments; see 4.5.3) |
| DELETE | `/api/service-visits/{id}/mechanics/{mechanicId}` | ADMIN | — | 200 | `ServiceVisitResponseDTO` (remaining assignments; see 4.5.3) |
| GET | `/api/service-visits/available-slots` | ADMIN | — | 200 | `AvailableSlotDTO[]` (plain array, see 4.5.2) |
| PUT | `/api/service-visits/{id}` | ADMIN | `ServiceVisitUpdateDTO` | 200 | `ServiceVisitResponseDTO` (SCHEDULED only; booking rules in 4.5.1, 409 if a mechanic is booked) |
| DELETE | `/api/service-visits/{id}` | ADMIN | — | 204 | — (SCHEDULED, no parts; request → PENDING) |
| PATCH | `/api/service-visits/{id}/invoice` | ADMIN | — | 201 | `InvoiceResponseDTO` (visit must be COMPLETED; visit → INVOICED) |

Visits are created only by approving a request. In the `PATCH .../status` and `PATCH .../complete` responses,
`mechanics[]` contains **only the calling mechanic's** assignment. The list, `PUT` and `POST` / `DELETE .../mechanics`
responses contain all assignments.

Who sees which ids and `myRole`:

| Caller | `mechanics[].mechanicId` (user id) | `mechanics[].assignmentId` | `myRole` |
|--------|------------------------------------|----------------------------|----------|
| ADMIN | yes | yes | `null` |
| MECHANIC assigned to the visit (lead or assistant) | `null` | yes | `LEAD` / `ASSISTANT` |
| CUSTOMER | `null` | `null` | `null` |

`mechanicId` is the value to pass to `available-slots` (4.5.2) and to `DELETE .../mechanics/{mechanicId}` (4.5.3).
`assignmentId` is what the lead sends in `assistantHours` when completing (5.11).

Example `ServiceVisitResponseDTO` as an admin sees it:

```json
{
  "id": 12,
  "status": "IN_PROGRESS",
  "scheduledDate": "2026-11-23T10:00:00",
  "completedDate": null,
  "technicalNotes": null,
  "licensePlate": "CA1234AB",
  "mechanics": [
    { "mechanicName": "Ivan Petrov", "roleInVisit": "LEAD", "hoursWorked": 0.00, "mechanicId": 4, "assignmentId": 30 },
    { "mechanicName": "Georgi Ivanov", "roleInVisit": "ASSISTANT", "hoursWorked": 0.00, "mechanicId": 5, "assignmentId": 31 }
  ],
  "requestType": "SERVICE",
  "requestDescription": "Brakes squeak when stopping",
  "vehicleManufacturer": "Toyota",
  "vehicleModel": "Corolla",
  "myRole": null
}
```

The same visit in the assistant's list: both `mechanicId` values are `null`, the `assignmentId` values stay, and
`"myRole": "ASSISTANT"`. A customer gets `null` for all three.

#### 4.5.1 Booking rules (approve and reschedule)

Checked whenever a `scheduledDate` is set: `PATCH /api/service-requests/{id}/approve` and `PUT /api/service-visits/{id}`.
All times are the server's local time. The values are defaults in code (`BookingProperties`) and can be
overridden with `app.booking.*` properties.

| Rule | Default | Error |
|------|---------|-------|
| Not in the past | now or later | `400` `"Validation failed"`, `fieldErrors.scheduledDate: "Scheduled date cannot be in the past"` |
| Working day | Monday to Friday | `400` `"Scheduled date must be on a working day (MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY)"` |
| Working hours | 09:00–17:00; every visit takes **60 minutes**, so the start must be 09:00–16:00 | `400` `"Scheduled time must be between 09:00 and 16:00 (working hours 09:00-17:00, a visit takes 60 minutes)"` |
| Slot | start on a 30-minute slot (`:00` or `:30`, seconds `0`) | `400` `"Scheduled time must start on a 30-minute slot (e.g. 09:00, 09:30)"` |
| No double booking | see below | `409` `"Mechanic is already booked at this time"` |

- Order of checks: body validation (incl. "not in the past"), request/visit status, mechanic (approve only:
  404 / 400), working day, working hours, slot, double booking.
- **Double booking**: a mechanic (any assignment, `LEAD` or `ASSISTANT`) cannot have two `SCHEDULED` or
  `IN_PROGRESS` visits whose ranges `[start, start + 60 min)` intersect. Back-to-back visits (10:00 and 11:00)
  are fine. `COMPLETED` and `INVOICED` visits never block.
- On reschedule **every** mechanic assigned to the visit must be free, and the visit being rescheduled is
  ignored, so moving it by 30 minutes into time it partly occupies already is allowed.

#### 4.5.2 `GET /api/service-visits/available-slots` — ADMIN

| Param | Type | Required | Notes |
|-------|------|----------|-------|
| `mechanicId` | long | yes | unknown → `404 "User not found with id: x"`; not a mechanic → `400 "User with ID x is not a mechanic"`; disabled → `400 "Mechanic account is disabled"` |
| `date` | date (`yyyy-MM-dd`) | yes | wrong format → `400 "Invalid value for parameter 'date'"` |
| `excludeVisitId` | long | no | a visit to ignore, normally the one being rescheduled, so its own time shows as free |

Missing `mechanicId` or `date` → `400 "Bad Request"`.

Returns **all** slot starts of the day in order (with the defaults 15 slots: 09:00, 09:30, …, 16:00), each with
`available`. A slot is `false` if it starts before now, if the day is not a working day (then all are `false`),
or if the mechanic has a `SCHEDULED` / `IN_PROGRESS` visit overlapping the 60 minutes from that start. Example
for a mechanic with a visit at 11:00 (10:30, 11:00 and 11:30 are blocked):

```json
[
  { "start": "2026-11-23T09:00:00", "available": true },
  { "start": "2026-11-23T09:30:00", "available": true },
  { "start": "2026-11-23T10:00:00", "available": true },
  { "start": "2026-11-23T10:30:00", "available": false },
  { "start": "2026-11-23T11:00:00", "available": false },
  { "start": "2026-11-23T11:30:00", "available": false },
  { "start": "2026-11-23T12:00:00", "available": true },
  ...
  { "start": "2026-11-23T16:00:00", "available": true }
]
```

A slot shown as available can still be taken before you approve or reschedule, so always handle the `409`.
The endpoint covers one mechanic; for a visit that also has an assistant, the reschedule checks all of them.

#### 4.5.3 Assistant mechanics — ADMIN

A visit has one `LEAD` (chosen on approve, cannot be removed or changed) and up to **3** `ASSISTANT` mechanics.
Assistants see the visit in their list, can start it (`PATCH .../status`) and manage its parts like the lead.
Only the lead can complete it.

`POST /api/service-visits/{id}/mechanics` with `{ "mechanicId": 5 }` adds an assistant. Checks, in this order:

| Check | Error |
|-------|-------|
| `mechanicId` sent | `400` `"Validation failed"`, `fieldErrors.mechanicId: "Mechanic ID is required"` |
| visit exists | `404` `"Service visit not found with ID: 12"` |
| visit is `SCHEDULED` or `IN_PROGRESS` | `400` `"Assistants can only be changed on scheduled or in-progress visits"` |
| user exists | `404` `"User not found with id: 5"` |
| user is a mechanic / enabled | `400` `"User with ID 5 is not a mechanic"` / `"Mechanic account is disabled"` |
| not already on the visit (as lead or assistant) | `409` `"Mechanic is already assigned to this visit"` |
| fewer than 3 assistants | `400` `"A visit can have at most 3 assistants"` |
| free for 60 minutes from the visit's `scheduledDate` (4.5.1 double-booking rule, this visit ignored) | `409` `"Mechanic is already booked at this time"` |

Working day / hours / slot rules are not re-checked here (the visit's time was checked when it was set).

`DELETE /api/service-visits/{id}/mechanics/{mechanicId}` removes an assistant (`mechanicId` = user id from
`mechanics[].mechanicId`). Visit missing → `404`; wrong status → the same `400` as above; mechanic not on the visit
→ `404 "Mechanic is not assigned to this visit"`; the lead → `400 "The lead mechanic cannot be removed"`.

Both return `200` with the visit as an admin sees it (all remaining assignments, with ids).

### 4.6 Visit parts

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/service-visits/{id}/parts` | ADMIN, MECHANIC (assigned) | — | 200 | `VisitPartResponseDTO[]` (plain array, not paged) |
| POST | `/api/service-visits/{id}/parts` | MECHANIC (assigned) | `VisitPartCreateDTO` | 201 | `VisitPartResponseDTO` |
| PUT | `/api/service-visits/{id}/parts/{partLineId}` | ADMIN, MECHANIC (assigned) | `VisitPartUpdateDTO` | 200 | `VisitPartResponseDTO` |
| DELETE | `/api/service-visits/{id}/parts/{partLineId}` | ADMIN, MECHANIC (assigned) | — | 204 | — |

- POST: the visit must be `IN_PROGRESS` and the part must be active (else `400 "Part is not active"`). The price is
  taken from the part's `unitSellPrice` at that moment.
- PUT/DELETE: not allowed once the visit is `COMPLETED` or `INVOICED`. A PUT keeps the original unit price.
  `partLineId` is the `id` from `VisitPartResponseDTO`.
- Customers cannot call this endpoint. They see the part lines of an invoiced visit only through
  `GET /api/invoices/{id}` (`partLines`).

### 4.7 Parts

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/parts` | ADMIN, MECHANIC | — | 200 | `PageResponse<PartResponseDTO>` (admin) / `PageResponse<PartCatalogDTO>` (mechanic) |
| POST | `/api/parts` | ADMIN | `PartCreateDTO` | 201 | `PartResponseDTO` (`active: true`) |
| PUT | `/api/parts/{id}` | ADMIN | `PartUpdateDTO` | 200 | `PartResponseDTO` |
| DELETE | `/api/parts/{id}` | ADMIN | — | 204 | — (409 if used in a visit; deactivate instead) |

### 4.8 Invoices

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/invoices` | CUSTOMER, ADMIN | — | 200 | `PageResponse<InvoiceResponseDTO>` |
| GET | `/api/invoices/{id}` | CUSTOMER (own vehicle), ADMIN | — | 200 | `InvoiceDetailDTO` (with `partLines`, `laborHours`, `laborRate`) |
| PATCH | `/api/invoices/{id}/pay` | ADMIN | — | 200 | `InvoiceResponseDTO` (`paid: true`; already paid → 400) |
| DELETE | `/api/invoices/{id}` | ADMIN | — | 204 | — (unpaid only; visit → COMPLETED) |

Invoices are created via `PATCH /api/service-visits/{id}/invoice`. Calculation: `partsPrice` = sum of the
visit's part-line totals; `laborPrice` = 30.00 × total `hoursWorked` of all assignments; `totalPrice` = both
added. `invoiceNumber` = `INV-<current year>-<visit id, 5 digits>`, e.g. `INV-2026-00012`.

Only the single invoice (`GET /api/invoices/{id}`) has the line items; the list and the other invoice
responses keep `InvoiceResponseDTO`.
- `partLines`: one entry per part line of the visit, in the order they were added; an empty array when the
  visit used no parts. The sum of `lineTotal` equals `partsPrice`.
- `laborHours` × `laborRate` equals `laborPrice`.
- The lines are read from the visit, which cannot change after it is `COMPLETED`. `laborRate` is the current
  hourly rate, not a value stored on the invoice.
- Same access as before: a customer gets `403` for another customer's invoice.

### 4.9 Admin users

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| GET | `/api/admin/users` | ADMIN | — | 200 | `PageResponse<UserAdminDTO>` |
| POST | `/api/admin/users/mechanic` | ADMIN | `RegisterRequestDTO` | 201 | `RegisterResponseDTO` (role `MECHANIC`) |
| POST | `/api/admin/users/admin` | ADMIN | `RegisterRequestDTO` | 201 | `RegisterResponseDTO` (role `ADMIN`) |
| PATCH | `/api/admin/users/{id}/enabled` | ADMIN | `UpdateUserEnabledDTO` | 200 | `UserAdminDTO` |

Enable/disable: sending the current value changes nothing and returns the user. You cannot disable yourself
(`400`) or the last enabled admin (`409`). A disabled user is locked out on their next request (see 1.5).

### 4.10 Own password

| Method | Path | Roles | Body | Success | Response |
|--------|------|-------|------|---------|----------|
| PATCH | `/api/users/me/password` | any authenticated | `ChangePasswordDTO` | 204 | — |

---

## 5. Field rules (request DTOs)

"Required" means `@NotNull`/`@NotBlank` (blank = empty or whitespace only). "Max" is `@Size(max)`, which
is the value for `maxLength`. `@Digits(i, f)` = at most `i` digits before and `f` after the decimal point.

### 5.1 RegisterRequestDTO (register, create mechanic, create admin)

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `username` | string | yes | 1–50 | unique (409) |
| `email` | string | yes | max 254 | valid email; unique (409) |
| `firstName` | string | yes | max 50 | |
| `lastName` | string | yes | max 50 | |
| `password` | string | yes | min 8; max 72 UTF-8 bytes (not in the DTO, see 7) | over 72 bytes → `400 "password cannot be more than 72 bytes"` |
| `confirmPassword` | string | yes | max 255 | must equal `password` (class-level, not in `fieldErrors`) |

### 5.2 LoginRequestDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `username` | string | yes | — | |
| `password` | string | yes | — | |

### 5.3 ChangePasswordDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `currentPassword` | string | yes | — | must match (else 400) |
| `newPassword` | string | yes | min 8 | must differ from current (else 400) |

### 5.4 UpdateUserEnabledDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `enabled` | boolean | yes | |

### 5.5 VehicleCreateDTO / VehicleUpdateDTO (same rules)

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `licensePlate` | string | yes | max 8 | whitespace removed and upper-cased **before** validation, so `"CA 1234 AB"` → `"CA1234AB"` (8, valid); unique (409) |
| `manufacturer` | string | yes | max 50 | |
| `model` | string | yes | max 80 | |
| `year` | integer | yes | — | 1886 … current year (`"Invalid Year"`) |
| `firstRegistrationDate` | date | no | — | not in the future |

### 5.6 ServiceRequestCreateDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `vehicleId` | long | yes | — | must be the caller's vehicle (403); unknown → 404 |
| `description` | string | yes | max 500 | |
| `preferredDate` | date | no | — | today or later |
| `type` | enum | no | — | `SERVICE` (default), `INSPECTION`, `CONSULTATION` |

### 5.7 ServiceRequestUpdateDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `description` | string | yes | max 500 | |
| `preferredDate` | date | no | — | today or later. **Sending null or omitting it clears the stored date.** |
| `type` | enum | **yes** | — | `SERVICE`, `INSPECTION`, `CONSULTATION` |

The vehicle of a request cannot be changed.

### 5.8 ApproveRequestDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `mechanicId` | long | yes | must be an **enabled** user with role MECHANIC: otherwise 400 `"User with ID x is not a mechanic"` or 400 `"Mechanic account is disabled"`; unknown id → 404 |
| `scheduledDate` | datetime | yes | now or later (`"Scheduled date cannot be in the past"`); working day, 09:00–16:00, 30-minute slot (400); mechanic free for 60 minutes (409). See 4.5.1 |

### 5.9 ServiceVisitUpdateDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `scheduledDate` | datetime | yes | now or later; working day, 09:00–16:00, 30-minute slot (400); every assigned mechanic free for 60 minutes, ignoring this visit (409). See 4.5.1 |

### 5.10 UpdateVisitStatusDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `newStatus` | enum | yes | the only accepted value is `IN_PROGRESS` (from `SCHEDULED`); anything else → `400 "Invalid status transition"` |

### 5.11 CompleteVisitDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `technicalNotes` | string | yes | max 5000 | |
| `hoursWorked` | decimal | yes | — | the lead's hours; > 0; `@Digits(3, 2)` (max 999.99) |
| `assistantHours` | array | when the visit has assistants | — | one entry per ASSISTANT: `{ "assignmentId": 31, "hoursWorked": 1.25 }`. May be omitted or `[]` when there are no assistants |
| `assistantHours[].assignmentId` | long | yes | — | `mechanics[].assignmentId` of an assistant on this visit |
| `assistantHours[].hoursWorked` | decimal | yes | — | same rules and messages as `hoursWorked` |

Field errors come back as e.g. `fieldErrors["assistantHours[0].hoursWorked"]`. After the lead/status check, the
service checks the list (all `400`, nothing is saved):

- an id that is not an assistant of this visit (including the lead's own) → `"Unknown assistant assignment: 30"`
- the same id twice → `"Duplicate assistant assignment: 31"`
- an assistant without an entry → `"Hours are required for every assistant (missing assignment ids: 31, 32)"`

Anyone assigned but not the lead gets `400 "Only the lead mechanic can complete a visit that is in progress"`.
The invoice labour is the hourly rate × the hours of **all** assignments (lead and assistants).

Example:

```json
{
  "technicalNotes": "Replaced front pads and discs",
  "hoursWorked": 2.50,
  "assistantHours": [ { "assignmentId": 31, "hoursWorked": 1.25 } ]
}
```

### 5.11a AddAssistantDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `mechanicId` | long | yes | user id of an enabled MECHANIC; other rules in 4.5.3 |

### 5.12 PartCreateDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `name` | string | yes | max 100 | |
| `serialNumber` | string | yes | max 60 | unique (409) |
| `unitCost` | decimal | yes | — | > 0; `@Digits(6, 2)` |
| `unitSellPrice` | decimal | yes | — | > 0; `@Digits(6, 2)` |

### 5.13 PartUpdateDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `name` | string | yes | max 100 | |
| `serialNumber` | string | yes | max 60 | unique (409) |
| `unitCost` | decimal | yes | — | > 0; `@Digits(6, 2)` |
| `unitSellPrice` | decimal | yes | — | > 0; `@Digits(6, 2)` |
| `active` | boolean | yes | — | |

Create and update use the same price rules: **0.01 … 999999.99 with at most 2 decimals**, which fits the
`DECIMAL(8, 2)` columns (migration `V7__create_part_table.sql`). Messages: `"Unit cost is required"`,
`"Unit cost must be greater than zero"`, `"Unit cost must have up to 6 digits and 2 decimal places"` (and the
same for "Unit sell price"). A part saved earlier with a price of 0 must get a price above 0 before it can be updated.

### 5.14 VisitPartCreateDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `partId` | long | yes | unknown → 404; inactive → 400 |
| `quantity` | integer | yes | > 0 |

### 5.15 VisitPartUpdateDTO

| Field | Type | Required | Other |
|-------|------|----------|-------|
| `quantity` | integer | yes | > 0 |

### 5.16 CatalogItemCreateDTO / CatalogItemUpdateDTO

| Field | Type | Required | Length | Other |
|-------|------|----------|--------|-------|
| `name` | string | yes | max 100 | unique (409) |
| `description` | string | yes | max 500 | |
| `basePrice` | decimal | yes | — | ≥ 0; `@Digits(6, 2)` |
| `estimatedHours` | decimal | yes | — | > 0; `@Digits(3, 2)` |
| `active` | boolean | update only, required | — | new items are always active |

---

## 6. Status workflows

### 6.1 Service request (`PENDING`, `APPROVED`, `REJECTED`)

- **create → `PENDING`**: CUSTOMER, for their own vehicle.
- **`PENDING` → `APPROVED`**: ADMIN (`/approve`). Creates a visit in `SCHEDULED` with the chosen mechanic as `LEAD`.
- **`PENDING` → `REJECTED`**: ADMIN (`/reject`). Final.
- **edit / delete**: owner CUSTOMER or ADMIN, only while `PENDING`.
- **`APPROVED` → `PENDING`**: happens automatically when an ADMIN deletes the (still `SCHEDULED`) visit.

### 6.2 Service visit (`SCHEDULED`, `IN_PROGRESS`, `COMPLETED`, `INVOICED`)

- **→ `SCHEDULED`**: created by approving a request (ADMIN).
- **reschedule / delete**: ADMIN, only while `SCHEDULED` (delete also needs no parts; the request goes back to `PENDING`).
- **`SCHEDULED` → `IN_PROGRESS`**: an assigned MECHANIC (`PATCH /status` with `newStatus: IN_PROGRESS`).
- **add parts**: an assigned MECHANIC, only while `IN_PROGRESS`. Change/remove parts: ADMIN or an assigned
  MECHANIC, until `COMPLETED`.
- **add / remove assistants**: ADMIN, while `SCHEDULED` or `IN_PROGRESS` (4.5.3). The lead never changes.
- **`IN_PROGRESS` → `COMPLETED`**: the **LEAD** mechanic only (`PATCH /complete`, sets notes, the hours of the lead
  and of every assistant, and `completedDate`).
- **`COMPLETED` → `INVOICED`**: ADMIN (`PATCH /invoice`, creates the invoice).
- **`INVOICED` → `COMPLETED`**: automatically when an ADMIN deletes the unpaid invoice.
- There is no way back from `IN_PROGRESS` to `SCHEDULED`.

### 6.3 Invoice (`paid: false` / `true`)

- **create (unpaid)**: ADMIN, from a `COMPLETED` visit.
- **unpaid → paid**: ADMIN (`/pay`). Final, because a paid invoice cannot be deleted or un-paid.
- **delete**: ADMIN, unpaid only.

---

## 7. Things the frontend must know

- **`GET /api/service-requests`**: an admin sees **all statuses** by default. For a "pending requests" view,
  send `status=PENDING`.
- **`GET /api/service-visits`**:
  - `status` is an exact match: `status=COMPLETED` does **not** include invoiced visits. Use `status=INVOICED` for those.
  - Visibility: admin = all visits, mechanic = visits they are assigned to, customer = visits of their own vehicles.
  - Every role gets the same fields, so **customers also see `technicalNotes`, `hoursWorked` and mechanic
    names**, and every role sees the request's `requestType` / `requestDescription` and the vehicle's
    `vehicleManufacturer` / `vehicleModel`. The only difference: `mechanics[].mechanicId` is filled for admins and `null` for others. In the list, a mechanic sees **all** assignments of each visit, not only their own.
  - The `LEAD` is created on approve; an ADMIN adds or removes `ASSISTANT` mechanics with
    `POST` / `DELETE /api/service-visits/{id}/mechanics` (4.5.3). Use `myRole` to show or hide the "Complete"
    action for a mechanic (only `LEAD` can complete).
- **`GET /api/parts`**: ADMIN and MECHANIC get different shapes (`PartView`): `PartResponseDTO` with `unitCost`
  vs `PartCatalogDTO` without it. Sorting by `unitCost` is admin only (mechanic → 400). Mechanics only see
  active parts.
- **`GET /api/invoices/{id}`** returns `InvoiceDetailDTO` with the part lines and labour hours/rate, for an
  itemised printout. `GET /api/invoices` (the list) does not include them.
- **`GET /api/catalog-items`**: public. With an ADMIN token, inactive items appear and the `active` filter
  works. An invalid or expired token here is ignored (you get the guest view, not a 401).
- **Passwords**: minimum 8 characters (`password`, `newPassword`), maximum **72 bytes in UTF-8**.
  - The DTOs have no max length. The limit comes from Spring Security 7.1.1's `BCrypt.hashpw`, which throws
    `IllegalArgumentException("password cannot be more than 72 bytes")` when a new password is hashed.
    `GlobalExceptionHandler` turns that into **`400 "password cannot be more than 72 bytes"`** with
    `fieldErrors: {}` (this applies to register, create mechanic/admin, and change password).
  - The limit counts bytes, not characters: ASCII is 1 byte, but e.g. Cyrillic letters are 2 bytes and emoji
    are 4. Check `new TextEncoder().encode(pw).length <= 72` in the form.
  - At login no length error is raised. A login password longer than 72 bytes just won't match.
- **Roles never change** after an account is created. Admins create mechanics and other admins; customers register themselves.
- **Tokens**: a token issued before a password change **stays valid** until it expires. Disabling an account
  takes effect on the **next request** (the server loads role and `enabled` from the database every time). The
  same would apply to a role changed directly in the database.
- **Endpoints that do NOT exist yet** (may be needed): `GET /api/users/me`, `GET /api/service-requests/{id}`,
  `GET /api/service-visits/{id}`, `GET /api/parts/{id}`, `GET /api/admin/users/{id}`. Until they exist, read
  these from the list endpoints. What a call returns today follows from `SecurityConfig`, which ends with
  `.anyRequest().denyAll()` (default deny):
  - A guest always gets **401**.
  - A logged-in user gets **403**, except where a broad rule lets the request through to Spring MVC (last two rows):

  | Call | Guest | Logged in |
  |------|-------|-----------|
  | `GET /api/users/me` | 401 | 403 |
  | `GET /api/service-requests/{id}` | 401 | 403 |
  | `GET /api/parts/{id}` | 401 | 403 |
  | `GET /api/service-visits/{id}` | 401 | 403 for ADMIN/CUSTOMER; **405** for MECHANIC (the `/api/service-visits/**` MECHANIC rule matches, and PUT/DELETE exist on that path) |
  | `GET /api/admin/users/{id}` | 401 | 403 for non-admins; **404** for ADMIN (the `/api/admin/**` rule matches, nothing is mapped there) |
- **Scheduling**: before approving or rescheduling, load `GET /api/service-visits/available-slots` for the
  chosen mechanic and day (with `excludeVisitId` when rescheduling) and offer only `available: true` starts.
  When rescheduling, take the mechanic from the visit's `mechanics[].mechanicId` (admin responses only).
  Send the slot's `start` unchanged as `scheduledDate`. See 4.5.1.
- **Approve response**: `/approve` returns the request, not the new visit. Reload the visit list to get it.
  Responses do not include the ids of linked records (only `licensePlate`), apart from the exceptions in 4.0.
- **Service request update** replaces `preferredDate`. Always send the current value, or it is cleared.
- **License plates** are stored without spaces and in upper case. `maxLength` 8 applies after removing spaces.
- **CORS**: only origins listed in `APP_CORS_ALLOWED_ORIGINS` (property `app.cors.allowed-origins`,
  comma-separated) are allowed. If it is empty, no cross-origin call is allowed. Allowed methods: `GET, POST,
  PUT, PATCH, DELETE, OPTIONS`. Allowed request headers: **only `Authorization` and `Content-Type`** (any
  other custom header fails the preflight). No cookies/credentials (`allowCredentials=false`); preflight
  cached for 3600 s.
