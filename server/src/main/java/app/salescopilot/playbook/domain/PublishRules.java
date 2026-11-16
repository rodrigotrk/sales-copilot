package app.salescopilot.playbook.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.api.PlaybookDocument.Plan;

/** O que impede publicar (AD-019, AD-035), na ordem em que é conferido. */
public final class PublishRules {

    public record Refusal(String code, String detail, List<String> itemIds) {
    }

    private PublishRules() {
    }

    public static Optional<Refusal> check(PlaybookDocument draft) {
        if (draft.stages().isEmpty() || draft.discovery().isEmpty() || draft.objections().isEmpty()) {
            return Optional.of(new Refusal("publish_minimum",
                    "Para publicar, cadastre ao menos uma etapa, uma informação a descobrir e uma objeção.", List.of()));
        }
        List<String> incomplete = new ArrayList<>();
        draft.stages().stream().filter(s -> blank(s.name())).forEach(s -> incomplete.add(s.id().toString()));
        draft.discovery().stream().filter(d -> blank(d.information()) || blank(d.question()))
                .forEach(d -> incomplete.add(d.id().toString()));
        draft.objections().stream().filter(o -> blank(o.name()) || blank(o.approvedResponse()))
                .forEach(o -> incomplete.add(o.id().toString()));
        draft.priceTable().plans().stream().filter(p -> blank(p.name()) || p.cash() == null)
                .forEach(p -> incomplete.add(p.id().toString()));
        if (!incomplete.isEmpty()) {
            return Optional.of(new Refusal("incomplete_rows", "Complete ou remova as linhas destacadas.", incomplete));
        }
        var discount = DraftRules.discountProblem(draft.priceTable());
        if (discount.isPresent()) {
            return Optional.of(new Refusal("discount_invalid", discount.get().message(), List.of()));
        }
        List<String> pending = draft.priceTable().plans().stream().filter(p -> p.review() != null)
                .map(Plan::id).map(Object::toString).toList();
        // Sem plano a linha do desconto não aparece (AC 52) e não há onde usar "Conferido"; a marca volta a
        // contar quando a tabela ganha um plano (AD-061).
        boolean discountPending = draft.priceTable().maxDiscountReview() != null && !draft.priceTable().plans().isEmpty();
        if (!pending.isEmpty() || discountPending) {
            return Optional.of(new Refusal("pending_review",
                    "Confira os valores marcados \"Importado · conferir\" antes de publicar.", pending));
        }
        return Optional.empty();
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
