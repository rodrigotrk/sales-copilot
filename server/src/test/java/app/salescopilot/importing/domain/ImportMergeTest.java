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

    private static final String PLAN_EXCERPT = "Plano de 6 meses por R$ 7.000, ou 6x de R$ 1.166,67.";
    private static final String DISCOUNT_EXCERPT = "Desconto máximo na call: R$ 500.";

    @Test
    @DisplayName("[C71] valor do modelo dentro dos limites entra marcado com o trecho, e fora deles fica vazio")
    void modelValuesEnterMarkedWithinTheLimits() {
        var plan = merge(PlaybookDocument.empty(), plan(new BigDecimal("7000"), 6, new BigDecimal("1166.67")), null, null)
                .draft().priceTable().plans().getFirst();
        assertThat(plan.cash()).isEqualByComparingTo("7000.00");
        assertThat(plan.installments().count()).isEqualTo(6);
        assertThat(plan.installments().value()).isEqualByComparingTo("1166.67");
        assertThat(plan.review().status()).isEqualTo("pending");
        assertThat(plan.review().excerpt()).isEqualTo(PLAN_EXCERPT);

        for (String invalid : List.of("-5", "0", "0.005", "10000000.01")) {
            var refused = merge(PlaybookDocument.empty(), plan(new BigDecimal(invalid), 6, new BigDecimal(invalid)), null, null)
                    .draft().priceTable().plans().getFirst();
            assertThat(refused.cash()).as(invalid).isNull();
            assertThat(refused.review().status()).as(invalid).isEqualTo("pending");
        }
        var oneInstallment = merge(PlaybookDocument.empty(), plan(new BigDecimal("7000"), 1, new BigDecimal("7000")), null, null)
                .draft().priceTable().plans().getFirst();
        assertThat(oneInstallment.installments()).isNull();
    }

    @Test
    @DisplayName("[C72] desconto e nome da coluna vazios recebem o valor do documento")
    void emptyScalarsAreFilled() {
        var result = merge(PlaybookDocument.empty(), plan(new BigDecimal("7000"), null, null), "Sessões", new BigDecimal("500"));
        PriceTable table = result.draft().priceTable();
        assertThat(table.extraColumnName()).isEqualTo("Sessões");
        assertThat(table.maxDiscount()).isEqualByComparingTo("500.00");
        assertThat(table.maxDiscountReview().status()).isEqualTo("pending");
        assertThat(table.maxDiscountReview().excerpt()).isEqualTo(DISCOUNT_EXCERPT);
        assertThat(result.notices()).isEmpty();
        // Também sem plano na tabela (AD-061): o desconto entra com a marca, e a linha aparece quando houver plano.
        var withoutPlans = ImportMerge.merge(PlaybookDocument.empty(),
                new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                        new ExtractedPlaybook.PriceTable(null, new BigDecimal("500"), DISCOUNT_EXCERPT, List.of())));
        assertThat(withoutPlans.draft().priceTable().maxDiscount()).isEqualByComparingTo("500.00");
        assertThat(withoutPlans.draft().priceTable().maxDiscountReview().status()).isEqualTo("pending");
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
    @DisplayName("[C129] número em dinheiro maior que numeric(12,2) não entra no rascunho")
    void hugeNumberIsDropped() {
        // 11 dígitos inteiros: o primeiro tamanho acima de numeric(12,2) (AD-025).
        var huge = new BigDecimal("12345678901.00");
        var result = merge(PlaybookDocument.empty(), plan(new BigDecimal("7000"), 6, huge), null, huge);
        PriceTable table = result.draft().priceTable();
        assertThat(table.plans().getFirst().installments()).isNull();
        assertThat(table.maxDiscount()).isNull();
        assertThat(table.plans().getFirst().cash()).isEqualByComparingTo("7000.00");
    }

    private static ImportMerge.Result merge(PlaybookDocument current, ExtractedPlaybook.Plan plan, String column,
            BigDecimal discount) {
        var extracted = new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(column, discount, DISCOUNT_EXCERPT, List.of(plan)));
        return ImportMerge.merge(current, extracted);
    }

    private static ExtractedPlaybook.Plan plan(BigDecimal cash, Integer count, BigDecimal value) {
        return new ExtractedPlaybook.Plan("6 meses " + UUID.randomUUID(), "12", cash, count, value, PLAN_EXCERPT);
    }
}
