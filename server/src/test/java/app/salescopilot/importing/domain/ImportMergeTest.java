package app.salescopilot.importing.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.api.PlaybookDocument.PriceTable;

import static org.assertj.core.api.Assertions.assertThat;

class ImportMergeTest {

    private static final String TEXT = "Plano de 6 meses por R$ 7.000, ou 6x de R$ 1.166,67. Desconto máximo na call: R$ 500.";

    @Test
    @DisplayName("[C71] valor ausente do texto é descartado e presente é importado")
    void moneyMustBeInTheText() {
        var present = merge(PlaybookDocument.empty(), plan(new BigDecimal("7000"), 6, new BigDecimal("1166.67")), null, null);
        var plan = present.draft().priceTable().plans().getFirst();
        assertThat(plan.cash()).isEqualByComparingTo("7000.00");
        assertThat(plan.installments().value()).isEqualByComparingTo("1166.67");
        assertThat(plan.review().status()).isEqualTo("pending");

        var absent = merge(PlaybookDocument.empty(), plan(new BigDecimal("6600"), 6, new BigDecimal("1100")), null, null);
        var invented = absent.draft().priceTable().plans().getFirst();
        assertThat(invented.cash()).isNull();
        assertThat(invented.installments()).isNull();
        assertThat(invented.review().status()).isEqualTo("pending");

        var seven = merge(PlaybookDocument.empty(), plan(new BigDecimal("7"), null, null), null, null);
        assertThat(seven.draft().priceTable().plans().getFirst().cash()).isNull();
        var seventy = merge(PlaybookDocument.empty(), plan(new BigDecimal("70"), null, null), null, null);
        assertThat(seventy.draft().priceTable().plans().getFirst().cash()).isNull();
    }

    @Test
    @DisplayName("[C72] desconto e nome da coluna vazios recebem o valor do documento")
    void emptyScalarsAreFilled() {
        var result = merge(PlaybookDocument.empty(), plan(new BigDecimal("7000"), null, null), "Sessões", new BigDecimal("500"));
        PriceTable table = result.draft().priceTable();
        assertThat(table.extraColumnName()).isEqualTo("Sessões");
        assertThat(table.maxDiscount()).isEqualByComparingTo("500.00");
        assertThat(table.maxDiscountReview().status()).isEqualTo("pending");
        assertThat(table.maxDiscountReview().excerpt()).isEqualTo("Desconto máximo na call: R$ 500.");
        assertThat(result.notices()).isEmpty();
    }

    @Test
    @DisplayName("[C73] desconto e coluna diferentes ficam com o atual e geram aviso")
    void differentScalarsKeepTheCurrentValue() {
        var current = new PlaybookDocument(1, List.of(), List.of(), List.of(),
                new PriceTable("Encontros", new BigDecimal("300.00"), null, List.of()));
        var result = merge(current, plan(new BigDecimal("7000"), null, null), "Sessões", new BigDecimal("500"));
        PriceTable table = result.draft().priceTable();
        assertThat(table.extraColumnName()).isEqualTo("Encontros");
        assertThat(table.maxDiscount()).isEqualByComparingTo("300.00");
        assertThat(table.maxDiscountReview()).isNull();
        assertThat(result.notices()).containsExactly(
                "O documento chama a coluna de \"Sessões\"; o nome atual (\"Encontros\") foi mantido.",
                "O documento traz desconto máximo de R$ 500; o atual (R$ 300) foi mantido.");
    }

    @Test
    @DisplayName("[C107] desconto do documento sem plano na tabela não entra e gera aviso")
    void discountNeedsAPlan() {
        var extracted = new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(null, new BigDecimal("500"), "Desconto máximo na call: R$ 500.", List.of()));
        var result = ImportMerge.merge(PlaybookDocument.empty(), extracted, TEXT);
        PriceTable table = result.draft().priceTable();
        assertThat(table.maxDiscount()).isNull();
        assertThat(table.maxDiscountReview()).isNull();
        assertThat(result.notices()).containsExactly(
                "O documento traz desconto máximo de R$ 500; cadastre um plano para informar o desconto.");
    }

    private static ImportMerge.Result merge(PlaybookDocument current, ExtractedPlaybook.Plan plan, String column,
            BigDecimal discount) {
        var extracted = new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(column, discount, "Desconto máximo na call: R$ 500.", List.of(plan)));
        return ImportMerge.merge(current, extracted, TEXT);
    }

    private static ExtractedPlaybook.Plan plan(BigDecimal cash, Integer count, BigDecimal value) {
        return new ExtractedPlaybook.Plan("6 meses " + UUID.randomUUID(), "12", cash, count, value, "6 meses por R$ 7.000");
    }
}
