package app.salescopilot.shared;

import java.util.UUID;
import java.util.function.Supplier;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Abre uma transação com as variáveis que as políticas de RLS leem (AD-037). SET LOCAL vale só
 * até o fim da transação, então uma conexão devolvida ao pool não carrega o negócio de outra
 * requisição.
 */
@Component
public class Tenancy {

    private final JdbcClient jdbc;
    private final TransactionTemplate transactions;

    public Tenancy(JdbcClient jdbc, TransactionTemplate transactions) {
        this.jdbc = jdbc;
        this.transactions = transactions;
    }

    public <T> T asUser(UUID userId, Supplier<T> work) {
        return transactions.execute(status -> {
            set("app.user_id", userId);
            return work.get();
        });
    }

    public <T> T inBusiness(UUID userId, UUID businessId, Supplier<T> work) {
        return transactions.execute(status -> {
            if (userId != null) {
                set("app.user_id", userId);
            }
            set("app.business_id", businessId);
            return work.get();
        });
    }

    public void runInBusiness(UUID userId, UUID businessId, Runnable work) {
        inBusiness(userId, businessId, () -> {
            work.run();
            return null;
        });
    }

    private void set(String name, UUID value) {
        jdbc.sql("SELECT set_config(?, ?, true)").params(name, value.toString()).query().singleValue();
    }
}
