package app.salescopilot.playbook;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.api.PlaybookDocument.Installments;
import app.salescopilot.playbook.api.PlaybookDocument.Plan;
import app.salescopilot.playbook.api.PlaybookDocument.PriceTable;
import app.salescopilot.playbook.domain.Money;

/** Põe todo valor em reais com escala 2, para que rascunho e versão se comparem por igualdade. */
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
        return new PlaybookDocument(draft.schemaVersion(), draft.stages(), draft.discovery(), draft.objections(),
                new PriceTable(table.extraColumnName(), Money.normalize(table.maxDiscount()), table.maxDiscountReview(),
                        plans));
    }
}
