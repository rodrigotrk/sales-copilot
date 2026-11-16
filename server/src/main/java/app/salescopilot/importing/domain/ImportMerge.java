package app.salescopilot.importing.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.api.PlaybookDocument.Installments;
import app.salescopilot.playbook.api.PlaybookDocument.Plan;
import app.salescopilot.playbook.api.PlaybookDocument.PriceTable;
import app.salescopilot.playbook.api.PlaybookDocument.Review;
import app.salescopilot.playbook.api.PlaybookLimits;
import app.salescopilot.shared.BrazilianReais;

/**
 * Acrescenta ao rascunho o que o documento trouxe (AD-017, AD-035): listas ao fim de cada bloco, sem
 * mudar o que já existe; preços dentro dos limites de valor e sempre marcados para o gestor conferir (AD-067);
 * desconto e nome da coluna só preenchidos quando vazios.
 */
public final class ImportMerge {

    /** O trecho do documento que acompanha a marca "Importado · conferir" (AD-035). */
    private static final int EXCERPT_LIMIT = 300;
    private static final BigDecimal MAX_CASH = new BigDecimal("10000000.00");

    public record Result(PlaybookDocument draft, Map<String, Integer> counts, int skipped, List<String> notices) {
    }

    private ImportMerge() {
    }

    public static Result merge(PlaybookDocument current, ExtractedPlaybook extracted) {
        int[] skipped = {0};

        var stages = new ArrayList<>(current.stages());
        int stagesAdded = append(stages, extracted.stages().stream()
                .map(s -> new PlaybookDocument.Stage(UUID.randomUUID(), cut(s.name(), PlaybookLimits.SHORT_TEXT)))
                .toList(), PlaybookLimits.MAX_STAGES, skipped);

        var discovery = new ArrayList<>(current.discovery());
        int discoveryAdded = append(discovery, extracted.discovery().stream()
                .map(d -> new PlaybookDocument.Discovery(UUID.randomUUID(), cut(d.information(), PlaybookLimits.SHORT_TEXT),
                        cut(d.question(), PlaybookLimits.LONG_TEXT)))
                .toList(), PlaybookLimits.MAX_DISCOVERY, skipped);

        var objections = new ArrayList<>(current.objections());
        int objectionsAdded = append(objections, extracted.objections().stream()
                .map(o -> new PlaybookDocument.Objection(UUID.randomUUID(), cut(o.name(), PlaybookLimits.SHORT_TEXT),
                        cut(o.leadSays(), PlaybookLimits.LONG_TEXT), cut(o.approvedResponse(), PlaybookLimits.LONG_TEXT)))
                .toList(), PlaybookLimits.MAX_OBJECTIONS, skipped);

        PriceTable table = current.priceTable();
        var plans = new ArrayList<>(table.plans());
        int plansAdded = append(plans, extracted.priceTable().plans().stream()
                .map(ImportMerge::plan)
                .toList(), PlaybookLimits.MAX_PLANS, skipped);

        List<String> notices = new ArrayList<>();
        String columnName = table.extraColumnName();
        String documentColumn = blankToNull(cut(extracted.priceTable().extraColumnName(), PlaybookLimits.SHORT_TEXT));
        if (documentColumn != null) {
            if (blankToNull(columnName) == null) {
                columnName = documentColumn;
            } else if (!columnName.strip().equals(documentColumn.strip())) {
                notices.add("O documento chama a coluna de \"" + documentColumn + "\"; o nome atual (\"" + columnName
                        + "\") foi mantido.");
            }
        }

        BigDecimal discount = table.maxDiscount();
        Review discountReview = table.maxDiscountReview();
        BigDecimal documentDiscount = extracted.priceTable().maxDiscount();
        if (documentDiscount != null && documentDiscount.signum() >= 0
                && twoDecimals(documentDiscount)) {
            if (discount == null) {
                discount = scale(documentDiscount);
                discountReview = Review.pending(excerpt(extracted.priceTable().maxDiscountExcerpt()));
            } else if (discount.compareTo(documentDiscount) != 0) {
                notices.add("O documento traz desconto máximo de R$ " + BrazilianReais.format(documentDiscount)
                        + "; o atual (R$ " + BrazilianReais.format(discount) + ") foi mantido.");
            }
        }

        Map<String, Integer> counts = new LinkedHashMap<>();
        counts.put("stages", stagesAdded);
        counts.put("discovery", discoveryAdded);
        counts.put("objections", objectionsAdded);
        counts.put("plans", plansAdded);
        var draft = new PlaybookDocument(current.schemaVersion(), stages, discovery, objections,
                new PriceTable(columnName, discount, discountReview, plans));
        return new Result(draft, counts, skipped[0], notices);
    }

    private static Plan plan(ExtractedPlaybook.Plan p) {
        BigDecimal cash = p.cash() != null && twoDecimals(p.cash()) && p.cash().signum() > 0
                && p.cash().compareTo(MAX_CASH) <= 0 ? scale(p.cash()) : null;
        Installments installments = null;
        Integer count = p.installmentsCount();
        BigDecimal value = p.installmentsValue();
        if (count != null && count >= PlaybookLimits.MIN_INSTALLMENTS && count <= PlaybookLimits.MAX_INSTALLMENTS
                && value != null && value.signum() > 0 && twoDecimals(value)) {
            installments = new Installments(count, scale(value));
        }
        return new Plan(UUID.randomUUID(), cut(p.name(), PlaybookLimits.SHORT_TEXT),
                cut(p.extraValue(), PlaybookLimits.SHORT_TEXT), cash, installments, Review.pending(excerpt(p.excerpt())));
    }

    private static <T> int append(List<T> target, List<T> items, int limit, int[] skipped) {
        int added = 0;
        for (T item : items) {
            if (target.size() >= limit) {
                skipped[0]++;
            } else {
                target.add(item);
                added++;
            }
        }
        return added;
    }

    /** A mesma regra do rascunho (AD-025): um valor do modelo maior que numeric(12,2) não chega ao rascunho. */
    private static boolean twoDecimals(BigDecimal value) {
        return BrazilianReais.isExactReais(value);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(2, java.math.RoundingMode.UNNECESSARY);
    }

    private static String excerpt(String text) {
        return text == null ? "" : cut(text.strip(), EXCERPT_LIMIT);
    }

    private static String cut(String text, int max) {
        if (text == null) {
            return null;
        }
        String stripped = text.strip();
        return stripped.length() > max ? stripped.substring(0, max) : stripped;
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text;
    }
}
