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
 * <p>Applies the same SQL scripts the tests use: {@code db/schema.sql} (schemas {@code aat_app}
 * and {@code datamesh} + tables) and {@code db/seed.sql} (templates + sample insight documents).
 * Keeping DDL and seed data in SQL files — not Java — makes them easy to manage and reuse.
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
 *   user=postgres  db=postgres  (no SSL)  — tables live in aat_app.* and datamesh.*
 * </pre>
 */
public final class EmbeddedPostgresRunner {

    private static final int PORT = 5433;

    private EmbeddedPostgresRunner() {
    }

    public static void main(String[] args) throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.builder().setPort(PORT).start()) {

            DataSource ds = pg.getPostgresDatabase();
            try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
                st.execute(readClasspathFile("/db/schema.sql"));
                st.execute(readClasspathFile("/db/seed.sql"));

                // Example of querying the embedded DB programmatically (plain JDBC):
                System.out.println("---- seeded data ----");
                try (ResultSet rs = st.executeQuery(
                        "SELECT id, code FROM aat_app.email_template ORDER BY id")) {
                    while (rs.next()) {
                        System.out.printf("  aat_app.email_template id=%d  code=%s%n",
                                rs.getLong("id"), rs.getString("code"));
                    }
                }
                try (ResultSet rs = st.executeQuery(
                        "SELECT ace_id, lead_client_name FROM datamesh.staat_insight_document ORDER BY id")) {
                    while (rs.next()) {
                        System.out.printf("  datamesh.staat_insight_document ace_id=%-9s  lead_client=%s%n",
                                rs.getString("ace_id"), rs.getString("lead_client_name"));
                    }
                }
            }

            System.out.println();
            System.out.println("Embedded PostgreSQL is up. Connect with any client:");
            System.out.println("  JDBC : jdbc:postgresql://localhost:" + PORT + "/postgres");
            System.out.println("  R2DBC: r2dbc:postgresql://localhost:" + PORT + "/postgres");
            System.out.println("  psql : psql -h localhost -p " + PORT + " -U postgres postgres");
            System.out.println("  (user=postgres, db=postgres, trust auth — tables in aat_app.* / datamesh.*)");
            System.out.println();
            System.out.println("Press Ctrl-C to stop and tear down the database.");
            Thread.currentThread().join();
        }
    }

    private static String readClasspathFile(String path) throws Exception {
        try (var in = EmbeddedPostgresRunner.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Classpath resource not found: " + path);
            }
            return new String(in.readAllBytes());
        }
    }
}
