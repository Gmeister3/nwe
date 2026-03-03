import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * COSC 3P91 - Laboratory 6: Database Integration with JDBC
 *
 * <p>Implements a "Persistent Coffee Management System" using an H2 embedded
 * database and the JDBC API.  All CRUD operations use {@link PreparedStatement}
 * objects to improve performance and prevent SQL-injection.  Multi-statement
 * updates are wrapped in explicit transactions so that a failure in any step
 * triggers an automatic rollback (atomicity).
 */
public class CoffeeManagementSystem {

    // Type 4 (Native-Protocol All-Java) JDBC URL for an H2 file-based database
    private static final String DB_URL  = "jdbc:h2:./coffeedb";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    // -----------------------------------------------------------------------
    // Entry point
    // -----------------------------------------------------------------------

    public static void main(String[] args) {
        CoffeeManagementSystem cms = new CoffeeManagementSystem();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            cms.createSchema(conn);

            // Insert sample coffee rows
            cms.insertCoffee(conn, 1, "Colombian",          "Superior",              new BigDecimal("7.99"), 0,   0);
            cms.insertCoffee(conn, 2, "French_Roast",       "Superior",              new BigDecimal("8.99"), 0,   0);
            cms.insertCoffee(conn, 3, "Espresso",           "Total Control Mirage",  new BigDecimal("9.99"), 0,   0);
            cms.insertCoffee(conn, 4, "Colombian_Decaf",    "Superior",              new BigDecimal("8.99"), 0,   0);
            cms.insertCoffee(conn, 5, "French_Roast_Decaf", "Total Control Mirage",  new BigDecimal("9.99"), 0,   0);

            System.out.println("\n--- Initial inventory ---");
            cms.displayAllCoffees(conn);

            // Atomically update SALES and TOTAL for two coffees
            cms.updateSalesAndTotal(conn, 1, 50);
            cms.updateSalesAndTotal(conn, 2, 75);

            System.out.println("\n--- Inventory after sales updates ---");
            cms.displayAllCoffees(conn);

        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println("Validation error: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Schema initialisation
    // -----------------------------------------------------------------------

    /**
     * Reads and executes the SQL schema script to create the COFFEES table when
     * it does not yet exist.
     *
     * @param conn an active JDBC {@link Connection}
     * @throws SQLException if the DDL statement cannot be executed
     */
    void createSchema(Connection conn) throws SQLException {
        String ddl =
            "CREATE TABLE IF NOT EXISTS COFFEES ("
            + "  COF_ID   INT         NOT NULL, "
            + "  COF_NAME VARCHAR(32) NOT NULL UNIQUE, "
            + "  SUP_NAME VARCHAR(40) NOT NULL, "
            + "  PRICE    DECIMAL(10,2) NOT NULL, "
            + "  SALES    INT         NOT NULL DEFAULT 0, "
            + "  TOTAL    INT         NOT NULL DEFAULT 0, "
            + "  CONSTRAINT pk_coffees PRIMARY KEY (COF_ID), "
            + "  CONSTRAINT chk_price  CHECK (PRICE >= 0), "
            + "  CONSTRAINT chk_sales  CHECK (SALES >= 0), "
            + "  CONSTRAINT chk_total  CHECK (TOTAL >= 0)"
            + ")";

        try (Statement stmt = conn.createStatement()) {
            stmt.execute(ddl);
            System.out.println("Schema initialised.");
        }
    }

    // -----------------------------------------------------------------------
    // Fail-fast validation
    // -----------------------------------------------------------------------

    /**
     * Validates coffee data <em>before</em> attempting a database operation.
     * Throws {@link IllegalArgumentException} immediately if any value violates
     * the business rules, avoiding a round-trip to the database.
     *
     * @param cofId    unique coffee identifier (must be &gt; 0)
     * @param cofName  coffee name (must not be blank, max 32 chars)
     * @param supName  supplier name (must not be blank, max 40 chars)
     * @param price    unit price (must be &ge; 0)
     * @param sales    current period sales (must be &ge; 0)
     * @param total    cumulative total sold (must be &ge; 0)
     * @throws IllegalArgumentException if any constraint is violated
     */
    void validateCoffee(int cofId, String cofName, String supName,
                        BigDecimal price, int sales, int total) {
        if (cofId <= 0)
            throw new IllegalArgumentException("Coffee ID must be a positive integer.");
        if (cofName == null || cofName.isBlank())
            throw new IllegalArgumentException("Coffee name must not be blank.");
        if (cofName.length() > 32)
            throw new IllegalArgumentException("Coffee name exceeds 32-character limit.");
        if (supName == null || supName.isBlank())
            throw new IllegalArgumentException("Supplier name must not be blank.");
        if (supName.length() > 40)
            throw new IllegalArgumentException("Supplier name exceeds 40-character limit.");
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Price must not be negative.");
        if (sales < 0)
            throw new IllegalArgumentException("Sales value must not be negative.");
        if (total < 0)
            throw new IllegalArgumentException("Total value must not be negative.");
    }

    // -----------------------------------------------------------------------
    // CRUD operations – all use PreparedStatement
    // -----------------------------------------------------------------------

    /**
     * Inserts a new coffee row after fail-fast validation.
     *
     * @param conn    an active JDBC {@link Connection}
     * @param cofId   unique coffee identifier
     * @param cofName coffee name
     * @param supName supplier name
     * @param price   unit price
     * @param sales   current period sales
     * @param total   cumulative total sold
     * @throws IllegalArgumentException if validation fails
     * @throws SQLException             if the INSERT statement fails
     */
    void insertCoffee(Connection conn, int cofId, String cofName,
                      String supName, BigDecimal price, int sales, int total)
            throws SQLException {
        validateCoffee(cofId, cofName, supName, price, sales, total);

        String sql = "INSERT INTO COFFEES (COF_ID, COF_NAME, SUP_NAME, PRICE, SALES, TOTAL)"
                   + " VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cofId);
            pstmt.setString(2, cofName);
            pstmt.setString(3, supName);
            pstmt.setBigDecimal(4, price);
            pstmt.setInt(5, sales);
            pstmt.setInt(6, total);
            pstmt.executeUpdate();
            System.out.println("Inserted: " + cofName);
        }
    }

    /**
     * Atomically updates both the SALES and the TOTAL columns for a single
     * coffee row.  Both statements share one transaction; if either fails the
     * transaction is rolled back, preserving data consistency.
     *
     * @param conn        an active JDBC {@link Connection}
     * @param cofId       the coffee to update
     * @param salesAmount the number of units sold in this period
     * @throws SQLException if either UPDATE fails or the rollback itself fails
     */
    void updateSalesAndTotal(Connection conn, int cofId, int salesAmount)
            throws SQLException {
        if (salesAmount < 0)
            throw new IllegalArgumentException("Sales amount must not be negative.");

        boolean previousAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);

        String sqlSales = "UPDATE COFFEES SET SALES = ? WHERE COF_ID = ?";
        String sqlTotal = "UPDATE COFFEES SET TOTAL = TOTAL + ? WHERE COF_ID = ?";

        try (PreparedStatement pstmtSales = conn.prepareStatement(sqlSales);
             PreparedStatement pstmtTotal = conn.prepareStatement(sqlTotal)) {

            pstmtSales.setInt(1, salesAmount);
            pstmtSales.setInt(2, cofId);
            pstmtSales.executeUpdate();

            pstmtTotal.setInt(1, salesAmount);
            pstmtTotal.setInt(2, cofId);
            pstmtTotal.executeUpdate();

            conn.commit();
            System.out.printf("Updated COF_ID=%d: SALES=%d, TOTAL incremented by %d%n",
                              cofId, salesAmount, salesAmount);

        } catch (SQLException e) {
            System.err.println("Transaction failed – rolling back: " + e.getMessage());
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(previousAutoCommit);
        }
    }

    /**
     * Retrieves and displays all rows from the COFFEES table using a
     * {@link PreparedStatement} and a forward-only {@link ResultSet} cursor.
     *
     * @param conn an active JDBC {@link Connection}
     * @throws SQLException if the SELECT statement fails
     */
    void displayAllCoffees(Connection conn) throws SQLException {
        String sql = "SELECT COF_ID, COF_NAME, SUP_NAME, PRICE, SALES, TOTAL"
                   + " FROM COFFEES ORDER BY COF_ID";

        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            System.out.println("-".repeat(75));
            System.out.printf("%-6s %-22s %-25s %-8s %-6s %-6s%n",
                              "ID", "Name", "Supplier", "Price", "Sales", "Total");
            System.out.println("-".repeat(75));

            while (rs.next()) {
                System.out.printf("%-6d %-22s %-25s %-8s %-6d %-6d%n",
                                  rs.getInt("COF_ID"),
                                  rs.getString("COF_NAME"),
                                  rs.getString("SUP_NAME"),
                                  rs.getBigDecimal("PRICE"),
                                  rs.getInt("SALES"),
                                  rs.getInt("TOTAL"));
            }
            System.out.println("-".repeat(75));
        }
    }

    /**
     * Deletes a coffee row identified by its primary key.
     *
     * @param conn  an active JDBC {@link Connection}
     * @param cofId the primary key of the row to delete
     * @throws SQLException if the DELETE statement fails
     */
    void deleteCoffee(Connection conn, int cofId) throws SQLException {
        String sql = "DELETE FROM COFFEES WHERE COF_ID = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cofId);
            int rows = pstmt.executeUpdate();
            System.out.printf("Deleted %d row(s) for COF_ID=%d%n", rows, cofId);
        }
    }
}
