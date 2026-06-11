package com.ubs.wma.aat.rampuppack;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;

/**
 * Dev utility (not a test) — starts the SAME Zonky embedded PostgreSQL the integration tests use,
 * but on a FIXED port and kept alive, so you can query it interactively with any client while it
 * runs (psql, DBeaver, psycopg2, or by pointing the app at it).
 *
 * <p>Run it from your IDE (run {@code main}) or from the command line:
 * <pre>{@code
 * mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java \
 *     -Dexec.classpathScope=test \
 *     -Dexec.mainClass=com.ubs.wma.aat.rampuppack.EmbeddedPostgresRunner
 * }</pre>
 *
 * <p>Then connect (trust auth — any password is accepted):
 * <pre>
 *   JDBC : jdbc:postgresql://localhost:5433/postgres
 *   R2DBC: r2dbc:postgresql://localhost:5433/postgres
 *   psql : psql -h localhost -p 5433 -U postgres postgres
 *   user=postgres  db=postgres  (no SSL)
 * </pre>
 */
public final class EmbeddedPostgresRunner {

    private static final int PORT = 5433;

    private EmbeddedPostgresRunner() {
    }

    public static void main(String[] args) throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder().setPort(PORT).start()) {

            String schema = new String(
                    EmbeddedPostgresRunner.class.getResourceAsStream("/schema.sql").readAllBytes());

            DataSource ds = pg.getPostgresDatabase();
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.execute(schema);
                st.execute("INSERT INTO ramp_up_pack(name, description, status) "
                        + "VALUES ('Sample Pack', 'created by EmbeddedPostgresRunner', 'DRAFT')");

                // Example of querying the embedded DB programmatically (plain JDBC):
                System.out.println("---- SELECT * FROM ramp_up_pack ----");
                try (ResultSet rs = st.executeQuery(
                        "SELECT id, name, status, created_at FROM ramp_up_pack ORDER BY id")) {
                    while (rs.next()) {
                        System.out.printf("  id=%d  name=%-12s  status=%-9s  created_at=%s%n",
                                rs.getLong("id"), rs.getString("name"),
                                rs.getString("status"), rs.getTimestamp("created_at"));
                    }
                }
            }

            System.out.println();
            System.out.println("Embedded PostgreSQL is up. Connect with any client:");
            System.out.println("  JDBC : jdbc:postgresql://localhost:" + PORT + "/postgres");
            System.out.println("  R2DBC: r2dbc:postgresql://localhost:" + PORT + "/postgres");
            System.out.println("  psql : psql -h localhost -p " + PORT + " -U postgres postgres");
            System.out.println("  (user=postgres, db=postgres, trust auth — any password)");
            System.out.println();
            System.out.println("Press Ctrl-C to stop and tear down the database.");
            Thread.currentThread().join();
        }
    }
}
