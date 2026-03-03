# COSC 3P91 – Lab 6: Persistent Coffee Management System

This project implements the **Persistent Coffee Management System** described in
the Laboratory 6 assignment for COSC 3P91 (Advanced Object-Oriented Programming)
at Brock University.  It uses Java 17 with an embedded **H2** database (a
Type 4 / native-protocol all-Java driver) in place of a full MariaDB/MySQL
server, which lets the project run without any external infrastructure.

---

## How the Assignment Requirements Are Met

### 1. Schema Design (`schema.sql` + `createSchema`)

The `COFFEES` table is defined with:

| Column     | Type           | Constraints                        |
|------------|----------------|------------------------------------|
| `COF_ID`   | `INT`          | `NOT NULL`, primary key            |
| `COF_NAME` | `VARCHAR(32)`  | `NOT NULL`, `UNIQUE`               |
| `SUP_NAME` | `VARCHAR(40)`  | `NOT NULL`                         |
| `PRICE`    | `DECIMAL(10,2)`| `NOT NULL`, `CHECK (PRICE >= 0)`   |
| `SALES`    | `INT`          | `NOT NULL DEFAULT 0`, `CHECK (SALES >= 0)` |
| `TOTAL`    | `INT`          | `NOT NULL DEFAULT 0`, `CHECK (TOTAL >= 0)` |

The DDL script lives in `src/main/resources/schema.sql`.  At runtime
`createSchema(Connection)` executes the equivalent `CREATE TABLE IF NOT EXISTS`
statement so the table is created on first run and left unchanged on subsequent
runs.

### 2. JDBC Connectivity (`main`)

```java
Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
```

`DriverManager.getConnection` loads the H2 Type 4 driver automatically (it is
on the class path via `lib/h2-2.4.240.jar`) and opens a file-backed database at
`./coffeedb`.  The connection is wrapped in a try-with-resources block so it is
always closed, even when an exception is thrown.

### 3. Fail-Fast Validation (`validateCoffee`)

`validateCoffee(int, String, String, BigDecimal, int, int)` checks every
business rule **before** hitting the database:

* `cofId > 0`
* `cofName` non-blank, ≤ 32 characters
* `supName` non-blank, ≤ 40 characters
* `price ≥ 0`
* `sales ≥ 0`, `total ≥ 0`

An `IllegalArgumentException` is thrown immediately on the first violation.
This avoids an unnecessary round-trip to the database and gives a clearer error
message than a database constraint violation would.

### 4. Atomic Transactions with Rollback (`updateSalesAndTotal`)

```java
conn.setAutoCommit(false);
// ... two UPDATE PreparedStatements ...
conn.commit();
```

Both the `SALES` and `TOTAL` updates execute inside a single transaction.
The `catch (SQLException)` block calls `conn.rollback()` so that a failure in
either statement leaves the database unchanged (atomicity).  `setAutoCommit` is
restored to its previous value in the `finally` block so the caller's
auto-commit mode is not affected.

### 5. PreparedStatements for All CRUD

Every SQL operation uses a `PreparedStatement`:

| Method                | Operation |
|-----------------------|-----------|
| `insertCoffee`        | `INSERT`  |
| `displayAllCoffees`   | `SELECT`  |
| `updateSalesAndTotal` | `UPDATE`  |
| `deleteCoffee`        | `DELETE`  |

Benefits:
* **Performance** – the database driver pre-compiles the query plan once; only
  the bind parameters change on subsequent calls.
* **Security** – user-supplied values are bound as typed parameters, never
  concatenated into the SQL string, eliminating SQL-injection risk.

---

## Analysis Questions

**1. 2-tier vs 3-tier JDBC architecture**

In a *2-tier* architecture the Java application talks directly to the database
server; the JDBC driver on the client machine handles the network protocol.  In
a *3-tier* architecture a middle tier (application server, REST API, etc.)
accepts requests from thin clients, then uses JDBC to communicate with the
database.  A middle tier is required when client devices should not hold
database credentials, when business logic must be shared across heterogeneous
clients, or when connection pooling and caching need to be centralised.

**2. Type 4 driver vs Type 1 JDBC-ODBC bridge**

A Type 4 driver is written entirely in Java and communicates with the DBMS
using its native wire protocol.  It requires no native libraries, no ODBC
layer, and no additional installation on the client – it is just a JAR file.
Type 1 bridges depend on platform-specific ODBC drivers, add extra translation
layers (increasing latency and failure points), and are deprecated since Java 8.

**3. PreparedStatement vs Statement**

`PreparedStatement` sends the SQL template to the engine once; the engine parses
and compiles it and caches the execution plan.  Subsequent executions only
transfer the parameter values, making repeated queries faster.  Because values
are transmitted as typed bind parameters rather than string literals they cannot
alter the query's structure, preventing SQL injection.

**4. Role of `setAutoCommit(false)`**

By default JDBC commits each statement immediately.  Calling
`setAutoCommit(false)` groups subsequent statements into one transaction that is
only committed with an explicit `commit()` call.  If an exception occurs before
`commit()`, calling `rollback()` undoes all partial changes, preserving the
"all-or-nothing" guarantee required for consistent multi-row updates.

**5. `JdbcRowSet` vs `ResultSet`**

A `ResultSet` requires an open database connection for the entire iteration.
A `JdbcRowSet` is a connected, scrollable, updatable wrapper that fires property
change events; a `CachedRowSet` (its disconnected cousin) downloads the result
into memory, closing the connection, and supports filtering with a `Predicate`.
This allows rows to be processed or displayed without holding a database
connection open, which is valuable in resource-constrained or intermittently-
connected environments.

---

## Building and Running

### Prerequisites

* Java 17 or later
* Maven 3.6 or later

### Build

```bash
mvn package -q
```

### Run

```bash
java -cp target/coffee-management-1.0.jar:lib/h2-2.4.240.jar CoffeeManagementSystem
```

### Expected output

```
Schema initialised.
Inserted: Colombian
Inserted: French_Roast
Inserted: Espresso
Inserted: Colombian_Decaf
Inserted: French_Roast_Decaf

--- Initial inventory ---
---------------------------------------------------------------------------
ID     Name                   Supplier                  Price    Sales  Total
---------------------------------------------------------------------------
1      Colombian              Superior                  7.99     0      0
2      French_Roast           Superior                  8.99     0      0
3      Espresso               Total Control Mirage      9.99     0      0
4      Colombian_Decaf        Superior                  8.99     0      0
5      French_Roast_Decaf     Total Control Mirage      9.99     0      0
---------------------------------------------------------------------------
Updated COF_ID=1: SALES=50, TOTAL incremented by 50
Updated COF_ID=2: SALES=75, TOTAL incremented by 75

--- Inventory after sales updates ---
---------------------------------------------------------------------------
ID     Name                   Supplier                  Price    Sales  Total
---------------------------------------------------------------------------
1      Colombian              Superior                  7.99     50     50
2      French_Roast           Superior                  8.99     75     75
...
```

> **Note:** The H2 database file (`coffeedb.mv.db`) is written to the working
> directory.  On a second run, the `INSERT` statements will fail with a unique-
> key violation because the rows already exist.  Drop the database file to reset
> the state, or use `MERGE INTO` (H2's upsert syntax) in place of plain `INSERT`.
