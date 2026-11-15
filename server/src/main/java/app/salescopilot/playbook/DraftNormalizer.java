package app.salescopilot.playbook;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.api.PlaybookDocument.Installments;
import app.salescopilot.playbook.api.PlaybookDocument.Plan;
import app.salescopilot.playbook.api.PlaybookDocument.PriceTable;
import app.salescopilot.playbook.domain.Money;

/** Põe todo valor em reais com escala 2, para que rascunho e versão se comparem por igualdade, e tira o desconto sem plano. */
final class DraftNormalizer {

    private DraftNormalizer() {
    }

    static PlaybookDocument normalize(PlaybookDocument draft) {
        PriceTable table = draft.priceTable();
        var plans = table.plans().stream()
                .map(p -> new Plan(p.id(), p.name(), p.extraValue(), Money.normalize(p.cash()),
                        p.installments() == null ? null
                                : new Installments(p.installments().count(), Money.normalize(p.installments().value())),
                        p.review()))
                .toList();
        // Sem plano não há linha de desconto na tela (AC 52) nem preço para compará-lo (AD-014): o desconto
        // e a marca de conferência saem junto com o último plano.
        boolean hasPlans = !plans.isEmpty();
        return new PlaybookDocument(draft.schemaVersion(), draft.stages(), draft.discovery(), draft.objections(),
                new PriceTable(table.extraColumnName(), hasPlans ? Money.normalize(table.maxDiscount()) : null,
                        hasPlans ? table.maxDiscountReview() : null, plans));
    }
}
