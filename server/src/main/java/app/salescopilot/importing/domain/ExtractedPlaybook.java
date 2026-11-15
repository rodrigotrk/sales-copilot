package app.salescopilot.importing.domain;

import java.math.BigDecimal;
import java.util.List;

/** O que o modelo devolve ao ler um documento, no schema pedido a ele. Dinheiro chega como número. */
public record ExtractedPlaybook(List<Stage> stages, List<Discovery> discovery, List<Objection> objections,
        PriceTable priceTable) {

    public record Stage(String name) {
    }

    public record Discovery(String information, String question) {
    }

    public record Objection(String name, String leadSays, String approvedResponse) {
    }

    public record PriceTable(String extraColumnName, BigDecimal maxDiscount, String maxDiscountExcerpt, List<Plan> plans) {
    }

    public record Plan(String name, String extraValue, BigDecimal cash, Integer installmentsCount,
            BigDecimal installmentsValue, String excerpt) {
    }

    /** Lista nula vira vazia: o modelo às vezes omite um bloco que o documento não tem. */
    public ExtractedPlaybook {
        stages = stages == null ? List.of() : stages;
        discovery = discovery == null ? List.of() : discovery;
        objections = objections == null ? List.of() : objections;
        priceTable = priceTable == null ? new PriceTable(null, null, null, List.of())
                : new PriceTable(priceTable.extraColumnName(), priceTable.maxDiscount(), priceTable.maxDiscountExcerpt(),
                        priceTable.plans() == null ? List.of() : priceTable.plans());
    }

    public boolean isEmpty() {
        return stages.isEmpty() && discovery.isEmpty() && objections.isEmpty() && priceTable.plans().isEmpty();
    }
}
