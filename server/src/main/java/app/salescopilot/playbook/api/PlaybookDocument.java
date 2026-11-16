package app.salescopilot.playbook.api;

import tools.jackson.databind.annotation.JsonDeserialize;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Rascunho e versões do playbook (AD-023): quatro listas ordenadas, cada item com `id` estável
 * entre rascunho e versões. Dinheiro vai em BigDecimal com escala 2 e sai em JSON como texto
 * decimal ("4500.00", AD-025).
 */
public record PlaybookDocument(int schemaVersion, List<Stage> stages, List<Discovery> discovery,
        List<Objection> objections, PriceTable priceTable) {

    public static final int SCHEMA_VERSION = 1;

    public record Stage(UUID id, String name) {
    }

    public record Discovery(UUID id, String information, String question) {
    }

    public record Objection(UUID id, String name, String leadSays, String approvedResponse) {
    }

    public record PriceTable(String extraColumnName, BigDecimal maxDiscount, Review maxDiscountReview, List<Plan> plans) {
    }

    public record Plan(UUID id, String name, String extraValue, BigDecimal cash, Installments installments,
            Review review) {
    }

    /** `count` passa por {@link WholeNumber}: o Jackson truncaria 60.9 para 60 antes da validação. */
    public record Installments(@JsonDeserialize(using = WholeNumber.class) Integer count, BigDecimal value) {
    }

    /** Marca "Importado · conferir" (AD-035): `status` é sempre "pending"; sem marca, o campo é nulo. */
    public record Review(String status, String excerpt) {

        public static Review pending(String excerpt) {
            return new Review("pending", excerpt);
        }
    }

    public static PlaybookDocument empty() {
        return new PlaybookDocument(SCHEMA_VERSION, List.of(), List.of(), List.of(),
                new PriceTable(null, null, null, List.of()));
    }

    public boolean hasItems() {
        return !stages.isEmpty() || !discovery.isEmpty() || !objections.isEmpty() || !priceTable.plans().isEmpty();
    }
}
