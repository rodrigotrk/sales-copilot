package app.salescopilot.playbook.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.shared.BrazilianReais;
import static app.salescopilot.playbook.api.PlaybookLimits.*;
import app.salescopilot.playbook.api.PlaybookDocument.Discovery;
import app.salescopilot.playbook.api.PlaybookDocument.Installments;
import app.salescopilot.playbook.api.PlaybookDocument.Objection;
import app.salescopilot.playbook.api.PlaybookDocument.Plan;
import app.salescopilot.playbook.api.PlaybookDocument.PriceTable;
import app.salescopilot.playbook.api.PlaybookDocument.Stage;
import app.salescopilot.shared.FieldProblem;

/**
 * O que o rascunho aceita ao ser gravado. Linhas incompletas são aceitas; quem as barra é a
 * publicação ({@link PublishRules}).
 */
public final class DraftRules {


    private DraftRules() {
    }

    public static List<FieldProblem> validate(PlaybookDocument draft) {
        List<FieldProblem> problems = new ArrayList<>();
        if (draft.schemaVersion() != PlaybookDocument.SCHEMA_VERSION || draft.stages() == null
                || draft.discovery() == null || draft.objections() == null || draft.priceTable() == null
                || draft.priceTable().plans() == null) {
            problems.add(new FieldProblem(null, "draft", "invalid_document", "Documento do playbook inválido."));
            return problems;
        }
        limit(problems, "stages", draft.stages().size(), MAX_STAGES);
        limit(problems, "discovery", draft.discovery().size(), MAX_DISCOVERY);
        limit(problems, "objections", draft.objections().size(), MAX_OBJECTIONS);
        limit(problems, "plans", draft.priceTable().plans().size(), MAX_PLANS);
        Set<UUID> ids = new HashSet<>();
        for (Stage stage : draft.stages()) {
            id(problems, ids, stage.id());
            text(problems, stage.id(), "name", stage.name(), SHORT_TEXT);
        }
        for (Discovery item : draft.discovery()) {
            id(problems, ids, item.id());
            text(problems, item.id(), "information", item.information(), SHORT_TEXT);
            text(problems, item.id(), "question", item.question(), LONG_TEXT);
        }
        for (Objection item : draft.objections()) {
            id(problems, ids, item.id());
            text(problems, item.id(), "name", item.name(), SHORT_TEXT);
            text(problems, item.id(), "leadSays", item.leadSays(), LONG_TEXT);
            text(problems, item.id(), "approvedResponse", item.approvedResponse(), LONG_TEXT);
        }
        PriceTable table = draft.priceTable();
        text(problems, null, "extraColumnName", table.extraColumnName(), SHORT_TEXT);
        for (Plan plan : table.plans()) {
            id(problems, ids, plan.id());
            text(problems, plan.id(), "name", plan.name(), SHORT_TEXT);
            text(problems, plan.id(), "extraValue", plan.extraValue(), SHORT_TEXT);
            if (plan.cash() != null && !Money.isValidCash(plan.cash())) {
                problems.add(new FieldProblem(plan.id().toString(), "cash", "invalid_cash",
                        "Informe um valor em reais maior que zero."));
            }
            if (plan.installments() != null && !isValidInstallments(plan.installments())) {
                problems.add(new FieldProblem(plan.id().toString(), "installments", "invalid_installments",
                        "Parcelas de 2 a 60, com valor maior que zero."));
            }
        }
        discountProblem(table).ifPresent(problems::add);
        return problems;
    }

    public static boolean isValidInstallments(Installments installments) {
        Integer count = installments.count();
        BigDecimal value = installments.value();
        return count != null && count >= MIN_INSTALLMENTS && count <= MAX_INSTALLMENTS && value != null
                && value.signum() > 0 && Money.hasAtMostTwoDecimals(value);
    }

    /** O desconto máximo precisa ser maior ou igual a zero e menor que o menor à vista (AD-014). */
    public static Optional<FieldProblem> discountProblem(PriceTable table) {
        BigDecimal discount = table.maxDiscount();
        if (discount == null) {
            return Optional.empty();
        }
        Optional<BigDecimal> lowest = lowestCash(table);
        boolean valid = discount.signum() >= 0 && Money.hasAtMostTwoDecimals(discount)
                && lowest.map(min -> discount.compareTo(min) < 0).orElse(true);
        if (valid) {
            return Optional.empty();
        }
        String detail = lowest
                .map(min -> "O desconto máximo precisa ser menor que o menor preço à vista (R$ " + BrazilianReais.format(min) + ").")
                .orElse("O desconto máximo precisa ser maior ou igual a zero.");
        return Optional.of(new FieldProblem(null, "maxDiscount", "discount_invalid", detail));
    }

    public static Optional<BigDecimal> lowestCash(PriceTable table) {
        return table.plans().stream().map(Plan::cash).filter(Objects::nonNull).min(BigDecimal::compareTo);
    }

    private static void limit(List<FieldProblem> problems, String block, int size, int max) {
        if (size > max) {
            problems.add(new FieldProblem(null, block, "too_many_items", "Limite de " + max + " itens neste bloco."));
        }
    }

    private static void text(List<FieldProblem> problems, UUID itemId, String field, String value, int max) {
        if (value != null && value.length() > max) {
            problems.add(new FieldProblem(itemId == null ? null : itemId.toString(), field, "too_long",
                    "Máximo de " + max + " caracteres."));
        }
    }

    private static void id(List<FieldProblem> problems, Set<UUID> ids, UUID id) {
        if (id == null || !ids.add(id)) {
            problems.add(new FieldProblem(id == null ? null : id.toString(), "id", "invalid_id",
                    "Cada item precisa de um id único."));
        }
    }
}
