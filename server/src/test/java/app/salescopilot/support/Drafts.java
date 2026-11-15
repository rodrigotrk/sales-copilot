package app.salescopilot.support;

import java.util.UUID;

/** Rascunhos de exemplo em JSON, de um negócio inventado de energia solar. */
public final class Drafts {

    private Drafts() {
    }

    public static String complete() {
        return """
                {"schemaVersion":1,
                 "stages":[{"id":"%s","name":"Abertura"},{"id":"%s","name":"Diagnóstico"}],
                 "discovery":[{"id":"%s","information":"Conta de luz","question":"Quanto você paga de luz por mês?"}],
                 "objections":[{"id":"%s","name":"Preço","leadSays":"Está caro","approvedResponse":"O que você compararia com esse valor?"}],
                 "priceTable":{"extraColumnName":"Placas","maxDiscount":"300.00","maxDiscountReview":null,
                   "plans":[{"id":"%s","name":"Residencial","extraValue":"8","cash":"4500.00","installments":{"count":6,"value":"800.00"},"review":null},
                            {"id":"%s","name":"Comercial","extraValue":"20","cash":"12000.00","installments":null,"review":null}]}}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        UUID.randomUUID());
    }

    public static String saveBody(UUID playbookId, String draft, long revision) {
        return "{\"playbookId\":\"" + playbookId + "\",\"draft\":" + draft + ",\"revision\":" + revision + "}";
    }

    public static String publishBody(UUID playbookId, long revision) {
        return "{\"playbookId\":\"" + playbookId + "\",\"revision\":" + revision + "}";
    }
}
