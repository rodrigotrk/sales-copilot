package app.salescopilot.support;

import java.util.Map;

import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Um Postgres para toda a suíte, com os papéis de deploy/postgres/initdb (AD-037, AD-049): o
 * servidor conecta como `app` e o Flyway como `migrator`, como em produção.
 */
public final class TestDatabase {

    public static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(DockerImageName.parse("postgres:17"))
            .withEnv(Map.of("MIGRATOR_PASSWORD", "migrator", "APP_PASSWORD", "app"))
            .withCopyFileToContainer(MountableFile.forHostPath("../deploy/postgres/initdb/01-roles.sh", 0755),
                    "/docker-entrypoint-initdb.d/01-roles.sh");

    static {
        POSTGRES.start();
    }

    private TestDatabase() {
    }

    /** Superusuário, só para preparar e inspecionar dados nos testes. */
    public static JdbcClient admin() {
        return JdbcClient.create(dataSource(POSTGRES.getUsername(), POSTGRES.getPassword()));
    }

    /** O papel do servidor, para provar o RLS direto no banco. */
    public static DataSource appRole() {
        return dataSource("app", "app");
    }

    private static DataSource dataSource(String user, String password) {
        var dataSource = new DriverManagerDataSource(POSTGRES.getJdbcUrl(), user, password);
        dataSource.setSchema("sales_copilot");
        return dataSource;
    }
}
