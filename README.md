# Employee Payroll Management System (PS-10)

## Overview
The **Employee Payroll Management System** is a robust, production-grade desktop application designed for automating employee records, designation-based compensation policies, monthly payroll processing, professional PDF salary slip generation, and historical payroll audit archives.

Built as an enterprise-grade desktop solution for a college hackathon, it demonstrates clean architecture, strict Object-Oriented Programming (OOP) design patterns, pure CSV persistence with thread safety and atomic file operations, and a desktop user interface built strictly with JavaFX + FXML + native JavaFX styling.

---

## Critical Hackathon Compliance & Technology Stack

| Component | Permitted / Used | Prohibited & Excluded |
| :--- | :--- | :--- |
| **Language** | **Java 21 (LTS)** | Deprecated Java runtimes |
| **UI Framework** | **JavaFX 21 + FXML + Java Controllers** | Web frameworks, Electron, Swing |
| **UI Styling** | **Pure Native JavaFX (`UiTheme.java`)**<br>Styling rendered purely via JavaFX objects (`Background`, `Border`, `Insets`, `Font`, `Color`) | **STRICTLY ZERO CSS** (`.css` stylesheets, inline `setStyle()`, or `styleClass` attributes) |
| **Web Technologies** | **None** | **STRICTLY NO HTML, NO JavaScript, NO WebViews** |
| **Document Generation**| **Apache PDFBox 3.0.2** direct vector rendering | Browser/HTML-based PDF converters |
| **Data Persistence** | **Pure CSV Flat-File Persistence** (UTF-8, RFC4180 atomic writes) | SQL databases, NoSQL, ORMs |
| **Build System** | **Apache Maven 3.x** (`pom.xml`) | External package managers |
| **Unit Testing** | **JUnit 5 (Jupiter)** (119 automated tests passing) | Manual-only verification |

---

## Core Domain Rules & Business Logic

1. **Employee Categories**:
   - **Permanent Employee**: Salaried personnel receiving designation-based monthly basic salary, pro-rated attendance (working days, present days, absent days / LOP), and pro-rated statutory allowances (HRA, DA, Conveyance).
   - **Bench Employee**: Project-based consultants paid strictly a fixed daily rate for eligible project-working days. **Strictly ₹0.00 for HRA, DA, and Conveyance allowances**.
2. **Fixed Designation-Based Compensation**:
   - Basic salary and allowances are strictly governed by administrator-configured designation policies. No individual arbitrary salary overrides.
3. **Statutory PF Separation**:
   - Employee PF contribution (12% of basic up to statutory ceiling) is deducted from employee gross salary.
   - Employer PF contribution (12%) is tracked strictly as a separate corporate statutory expense and **never reduces employee take-home Net Salary**.
4. **Professional Tax (PT)**:
   - Configured via state-specific progressive salary slabs (e.g., Telangana PT slabs).
5. **Immutable Historical Snapshots**:
   - Past payroll records preserve complete snapshots of employee details, designation, allowances, and policies at calculation time. Changing designation policies later never alters past payroll records.
6. **Non-Destructive Authorized Corrections**:
   - Legitimate administrative corrections (e.g. attendance regularization) mark the original record as `SUPERSEDED` and append a new `CORRECTED` record with a mandatory audit justification reason and timestamp.
   - Both rows are permanently preserved in `payroll.csv` for auditability.
   - Deletion of finalized payroll records is strictly prohibited across the repository and UI.

---

## Application Modules (7 Active Screens)

```text
┌────────────────────────────────────────────────────────────────────────┐
│  💼 TechCorp - Employee Payroll Management System                      │
├───────────────┬────────────────────────────────────────────────────────┤
│ 📊 Dashboard  │ Live KPIs, active employee headcounts, disbursed sums, │
│               │ and employer PF liabilities.                          │
│ 👤 Register   │ Form with validation for Permanent & Bench employees. │
│ 👥 Directory  │ Real-time search, filters, and active employee status. │
│ ⚙️ Policies    │ Designation compensation, PF rates, and PT slabs.     │
│ 💰 Payroll    │ Attendance-based calculations, duplicate prevention,   │
│               │ permanent & bench workflows, atomic persistence.      │
│ 📄 Payslips   │ PDF preview, Indian Rupee words conversion, and       │
│               │ vector PDF generation via Apache PDFBox.              │
│ 📜 History    │ Immutable historical archive, multi-criteria filters, │
│               │ snapshot inspector card, and authorized corrections.  │
└───────────────┴────────────────────────────────────────────────────────┘
```

---

## Project Structure

```text
EmployeePayrollSystem/
├── pom.xml                                  # Maven dependencies & JavaFX 21 configuration
├── README.md                                # System documentation
├── data/                                    # Persistent CSV database (RFC4180 compliant)
│   ├── company_settings.csv                 # Corporate identity, address & contact details
│   ├── designation_policies.csv             # Fixed designation basic pay & allowances
│   ├── employees.csv                        # Permanent and Bench employee records
│   ├── payroll.csv                          # Immutable payroll records & audit archive
│   ├── pf_policies.csv                      # Statutory Provident Fund rules
│   ├── professional_tax_policies.csv        # State-wise progressive PT slabs
│   └── projects.csv                         # Bench project allocations
├── payslips/                                # Generated PDF salary slips (PDFBox 3.0.2)
├── src/
│   ├── main/
│   │   ├── java/com/payroll/
│   │   │   ├── MainApp.java                 # JavaFX Application entrypoint
│   │   │   ├── controller/                  # FXML Controllers (Pure JavaFX)
│   │   │   │   ├── LoginController.java
│   │   │   │   ├── MainLayoutController.java# Persistent 7-module navigation sidebar
│   │   │   │   ├── DashboardController.java
│   │   │   │   ├── EmployeeFormController.java
│   │   │   │   ├── EmployeeDirectoryController.java
│   │   │   │   ├── SettingsController.java
│   │   │   │   ├── PayrollController.java
│   │   │   │   ├── SalarySlipController.java
│   │   │   │   └── HistoryController.java   # Audit archive & snapshot inspector
│   │   │   ├── model/                       # Domain Entities (Permanent, Bench, Policies)
│   │   │   ├── service/                     # Calculations, PDF Generator, Authentication
│   │   │   ├── storage/                     # Thread-safe atomic CSV repositories
│   │   │   └── util/                        # CurrencyFormatter, NumberToWords, UiTheme
│   │   └── resources/com/payroll/           # Screen FXML Layouts (Strictly Zero CSS)
│   └── test/java/com/payroll/               # 119 Comprehensive Automated Unit & UI Tests
```

---

## Credentials for Demonstration

- **Employee ID**: `1234`
- **Password**: `5678`

*(Authentication managed via `AuthService.java` using SHA-256 password hashing with salt).*

---

## How to Build & Run

### 1. Execute Full Automated Test Suite (119 Tests)
```bash
mvn test
```

### 2. Launch the Desktop Application
```bash
mvn javafx:run
```

### 3. Package Production JAR
```bash
mvn clean package```