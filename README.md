# Insurance Policy and Claim Management System (Backend REST API)

An enterprise-grade, RESTful backend web service engineered with **Spring Boot** and **Java 17** to simulate real-world insurance business workflows. The system provides comprehensive policy lifecycle tracking, concurrency-safe claim handling, and high-fidelity role-based data isolation across multiple administrative layers.

## 🏗️ System Architecture & Core Modules

The application is architected strictly around a **Layered Domain-Driven Design (DDD)** pattern, completely isolating database entities from external clients using optimized Data Transfer Objects (DTOs).

Use code with caution.[ Client / React Frontend ]│▼  (Exposes REST JSON Payloads via DTOs)[ Controller Layer ] ➔ Direct payload validation, routes traffic safely│▼  (Coordinates Core Transactions & Business Logic)[ Service Layer ]    ➔ Enforces rules, limits, states, and safety constraints│▼  (Abstracts Data Access Queries)[ Repository Layer ] ➔ Fetches, saves, filters, and paginates data rows│▼[ Database Matrix (MySQL) ]
### 📦 Implemented SRS Core Modules
1. **Authentication & Authorization (`MOD-001`)**: Secure customer registration gates, internal profile creation, and cryptographically signed JWT credentials.
2. **Customer Management (`MOD-003`)**: High-precision profile builders enforcing minimum age constraints and email verification overlays.
3. **Policy Engine (`MOD-006`)**: Automated random alphanumeric business number generation (`POL-XXXXXXXX`), duration calculations, and active premium lifecycle trackers.
4. **Simulated Premium Payments (`MOD-007`)**: Safe financial accounting tracking that enforces absolute server-side token state validity.
5. **Claims & Timeline Auditor (`MOD-008`, `MOD-009`)**: High-fidelity claim submission pipeline featuring mutual-exclusion locks for auditing personnel and deep sub-list historical timelines.
6. **Centralized Exception Handler (`MOD-010`)**: Consistent API error response mappings intercepting validation failures, rule breaches, and token expiries safely.

---

## 🔒 Security Architecture & Role-Based Access Matrix

The API enforces strict data isolation boundaries using **Spring Security** and stateless **JWT Bearer Token verification**. Passwords are cryptographically transformed before physical persistence using high-entropy hashing algorithms.

### 🛠️ Role Authorization Permissions Reference

| Feature Capability | Admin (`ROLE_ADMIN`) | Agent (`ROLE_AGENT`) | Customer (`ROLE_CUSTOMER`) |
| :--- | :---: | :---: | :---: |
| **Public Registration** | ❌ No | ❌ No |  Yes |
| **Create Agent/Officer Profiles** |  Yes | ❌ No | ❌ No |
| **Activate / Deactivate Users** |  Yes | ❌ No | ❌ No |
| **Configure Products & Policy Plans** |  Yes | ❌ No | ❌ No |
| **Issue Policy to Customer** |  Yes |  Yes | ❌ No |
| **Record Premium Payment Records** | ❌ No |  Yes |  Yes (Own Only) |
| **Acquire Claim Review Lock** | ❌ No |  Yes | ❌ No |
| **Submit Claim Recommendation** | ❌ No |  Yes | ❌ No |
| **Process Final Claim Decision** |  Yes | ❌ No | ❌ No |
| **View Relational Data Fields** | Global Scope | Global Scope | Restricted (Self Only) |

---

## ⚙️ Concrete Business Rules & Safeguard Implementations

### 1. Registration & Email Change Interception (`USR-BR-001`)
* Public signup endpoints are strictly locked to `CUSTOMER` parameters.
* If an established customer attempts to update their profile email address, the transaction is gated behind a secondary, 6-digit verification token sent via **JavaMailSender**. The transaction is blocked if the email is already registered to another active ledger row.

### 2. Precise Age Constraint Verification (`CUS-BR-003`)
* Enforces an exact, high-precision calendar age barrier. Users are completely blocked from completing a profile if their `dateOfBirth` calculation determines they are under **18 years old**.

### 3. Policy Lockout & Activation Rules (`POL-BR-005`, `PAY-BR-007`)
* New policies initialize exclusively as `PENDING_PAYMENT`.
* A successful payment equal to or greater than the required premium shifts the state to `ACTIVE`.
* **Annual Lock Status**: Once a premium is recorded, an annual lock is activated on the policy, shifting the `nextPremiumDueDate` exactly 1 year into the future and blocking duplicate payments during that active window.

### 4. Claim Budget Protection & Open Ticket Guards (`CLM-BR-004`, `CLM-BR-010`)
* **Financial Cap Guard**: The system automatically queries `getApprovedClaimAmount` against past records. If the newly requested claim amount plus historic payouts exceeds the total policy plan coverage limit, the transaction is rejected with an exception.
* **Open Ticket Restriction**: Customers are strictly prohibited from opening a new claim if an active ticket (`SUBMITTED` or `UNDER_REVIEW`) is currently outstanding for that policy.

### 5. Multi-Agent Claim Mutual Exclusion Lock (`CLC-RUL-002`)
* To prevent data corruption, when an Agent requests details for a claim, the backend automatically transitions the state from `SUBMITTED` to `UNDER_REVIEW` and associates the individual Agent's primary key identifier.
* If a different Agent tries to open or update that specific ticket, the backend triggers an explicit lockout block:
  `BusinessRuleException("This claim file is currently locked and being audited by another agent.")`

---

## 🛣️ API Endpoint Directory (RESTful Specifications)

All request and response structures leverage clean DTO schemas, ensuring internal entity relationship structures are never leaked directly to clients.

### 🔓 Authentication Endpoints
* `POST /api/auth/register` ➔ Customer self-registration.
* `POST /api/auth/login` ➔ Validates credentials; issues a high-entropy JWT.
* `POST /api/auth/forgot-password` ➔ Hashes a unique UUID and fires an offsite recovery link.
* `POST /api/auth/reset-password` ➔ Decrypts incoming link hashes to securely override keys.

### 👥 Profile & Policy Operations
* `POST /api/customers/profile` ➔ Initializes customer details (Enforces 18+ limit).
* `GET /api/customers/me` ➔ Fetches active customer session profile data.
* `GET /api/customers/profile/exists` ➔ Returns `boolean` value checking profile state.
* `POST /api/policies/purchase` ➔ Customer policy acquisition (Initializes as `PENDING_PAYMENT`).
* `POST /api/premium-payments` ➔ Records payment; server hardcodes success states.

### 💼 Agent & Admin Claim Auditing
* `POST /api/claims` ➔ Customer claim submission (Requires evidence documentation references).
* `GET /api/claims/{claimId}` ➔ Fetches claim structures. If called by an Agent, locks the row to `UNDER_REVIEW`.
* `PUT /api/claims/{claimId}/review` ➔ Agent attaches `RECOMMENDED_APPROVAL` or `RECOMMENDED_REJECTION` along with remarks.
* `PUT /api/claims/{claimId}/decision` ➔ Admin signs final `APPROVED` or `REJECTED` status change.

---

## 🛠️ Data Quality, Pagination, & Error Mappings

### 📊 Unified Pagination, Sorting, and Filtering (`OBJ-017`)
All listing data tables are managed by a centralized engine inside `PaginationUtil.buildPageable(...)` to enforce project-level structural standards globally:
* **Default Page Size**: 10 records per view index.
* **Maximum Boundary Cap**: 100 records max per index.
* **Default Sort Field**: `createdDate` sorted in a `DESCENDING` timeline sequence.

### 🎯 High-Fidelity Centralized Error JSON Contract
Exceptions thrown inside the filter layer or service layer are safely mapped by the resolver to ensure error responses always follow a uniform schema:

```json
{
  "timestamp": "2026-07-07T15:33:12.432",
  "status": 400,
  "error": "Bad Request",
  "message": "Claim request denied. Requested amount exceeds your remaining policy coverage limit of ₹45000",
  "path": "/api/claims"
}
```

---

## 🚀 Execution & Runtime Verification Setup

### ⚙️ Prerequisites
* **Java SDK**: Version 17+ installed.
* **Database**: MySQL Server 8.0+ running locally.

### 📄 Application Properties (`src/main/resources/application.properties`)
Ensure your configuration settings are correctly populated with your unique local variables before compiling:

```properties
spring.datasource.url=jdbc:mysql://YOUR_DATABASE_HOST:YOUR_DATABASE_PORT/your_database_name?createDatabaseIfNotExist=true
spring.datasource.username=your_mysql_username
spring.datasource.password=your_mysql_password
spring.jpa.hibernate.ddl-auto=update

# Cryptographic Token Variables Configuration
jwt.secret=your_64_character_long_jwt_secret_key_placeholder
jwt.expiration=86400000

# Twilio Verify OTP Infrastructure Keys
twilio.account.sid=your_twilio_account_sid_placeholder
twilio.auth.token=your_twilio_auth_token_placeholder
twilio.verify.sid=your_twilio_verify_sid_placeholder
```

### 💻 Build and Execution Commands
Run these commands in your project root terminal to build and start your application:

```bash
# Clean, compile, and execute the robust Mockito unit test suite
mvn clean package

# Run the Spring Boot application on local port 8080
mvn spring-boot:run
```

Once up and running, your full live API documentation matrix can be reviewed directly via the interactive Swagger UI panel at:  
👉 **`http://localhost:8080/swagger-ui/index.html`**
