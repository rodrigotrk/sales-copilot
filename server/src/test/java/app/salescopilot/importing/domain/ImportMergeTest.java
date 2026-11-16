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
        // Também sem plano na tabela (AD-061): o desconto entra com a marca, e a linha aparece quando houver plano.
        var withoutPlans = ImportMerge.merge(PlaybookDocument.empty(),
                new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                        new ExtractedPlaybook.PriceTable(null, new BigDecimal("500"), "Desconto máximo na call: R$ 500.", List.of())),
                TEXT);
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
    @DisplayName("[C129] número em dinheiro maior que numeric(12,2) não entra no rascunho, mesmo presente no documento")
    void hugeNumberFromTheDocumentIsDropped() {
        String digits = "9".repeat(50_000);
        String text = "Plano Grande por R$ 7.000 em 6x de R$ " + digits + ". Desconto máximo na call: R$ " + digits + ".";
        var huge = new BigDecimal(digits);
        var extracted = new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(null, huge, text, List.of(
                        new ExtractedPlaybook.Plan("Grande", null, new BigDecimal("7000"), 6, huge, text))));
        var result = ImportMerge.merge(PlaybookDocument.empty(), extracted, text);
        PriceTable table = result.draft().priceTable();
        assertThat(table.plans().getFirst().installments()).isNull();
        assertThat(table.maxDiscount()).isNull();
        assertThat(table.plans().getFirst().cash()).isEqualByComparingTo("7000.00");
    }

    @Test
    @DisplayName("[C131] valor negativo do documento não autoriza o positivo, e hífen de lista, separador ou faixa não é sinal")
    void negativeNumbersDoNotAuthorizePositives() {
        // O trecho citado é o próprio texto: o teste exercita a leitura dos números dentro da citação.
        java.util.function.Function<String, ExtractedPlaybook> extracted = text -> new ExtractedPlaybook(
                List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(null, new BigDecimal("500"), text, List.of(
                        new ExtractedPlaybook.Plan("Plano", null, new BigDecimal("7000"), 6, new BigDecimal("1200"), text))));
        for (String text : List.of("Plano: R$ -7.000,00 ou 6x de -R$ 1.200. Desconto: -500.",
                "Plano: -R$7.000 ou 6x de R$-1.200. Desconto:-500",
                "Plano: -R$\u00a07.000,00 ou 6x de R$\u202f-1.200. Desconto: -500",
                "Estorno: R$ \u22127.000,00 ou 6x de \u2212R$ 1.200. Desconto: \u2212500.")) {
            PriceTable table = ImportMerge.merge(PlaybookDocument.empty(), extracted.apply(text), text).draft().priceTable();
            assertThat(table.plans().getFirst().cash()).as(text).isNull();
            assertThat(table.plans().getFirst().installments()).as(text).isNull();
            assertThat(table.maxDiscount()).as(text).isNull();
        }
        // Hífen seguido de espaço é marcador de lista ou separador, não sinal.
        String bullets = "Planos:\n- Plano A - R$ 7.000 à vista\n- 6x de R$ 1.200\nDesconto máximo - 500";
        PriceTable listed = ImportMerge.merge(PlaybookDocument.empty(), extracted.apply(bullets), bullets).draft().priceTable();
        assertThat(listed.plans().getFirst().cash()).isEqualByComparingTo("7000.00");
        assertThat(listed.plans().getFirst().installments().value()).isEqualByComparingTo("1200.00");
        assertThat(listed.maxDiscount()).isEqualByComparingTo("500.00");

        String range = "Plano de R$ 7.000 - R$ 8.000, em 6x de 1.200-1.400. Desconto de 300-500.";
        PriceTable table = ImportMerge.merge(PlaybookDocument.empty(), extracted.apply(range), range).draft().priceTable();
        assertThat(table.plans().getFirst().cash()).isEqualByComparingTo("7000.00");
        assertThat(table.plans().getFirst().installments().value()).isEqualByComparingTo("1200.00");
        assertThat(table.maxDiscount()).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("[C133] número em notação científica não autoriza nenhum pedaço dele")
    void scientificNotationIsNotSplit() {
        java.util.function.Function<String, ExtractedPlaybook> extracted = text -> new ExtractedPlaybook(
                List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(null, new BigDecimal("3"), text, List.of(
                        new ExtractedPlaybook.Plan("Plano", null, new BigDecimal("1"), null, null, text))));
        for (String text : List.of("Plano por R$ 1E3 à vista. Desconto de 1e3.", "Plano por R$ 1,5E+3. Desconto 2E-3",
                "Plano por R$ 1E\u22123. Desconto 3E\uFE631", "Plano por R$ 1E\uFF0D3. Desconto 3e\u20131")) {
            PriceTable table = ImportMerge.merge(PlaybookDocument.empty(), extracted.apply(text), text).draft().priceTable();
            assertThat(table.plans().getFirst().cash()).as(text).isNull();
            assertThat(table.maxDiscount()).as(text).isNull();
        }
        // Um número comum ao lado de uma letra continua valendo: "Plano 1 E 3" não é notação científica.
        PriceTable table = ImportMerge.merge(PlaybookDocument.empty(), extracted.apply("Plano 1 por R$ 1 e desconto de 3."), "Plano 1 por R$ 1 e desconto de 3.")
                .draft().priceTable();
        assertThat(table.plans().getFirst().cash()).isEqualByComparingTo("1.00");
        assertThat(table.maxDiscount()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("[C135] valor só entra se o trecho citado estiver no documento e contiver o valor escrito")
    void citationMustBeLiteralAndContainTheValue() {
        String document = "Plano Ouro: R$ 4.500 à vista ou 6x de R$ 800.\nDesconto máximo na call: R$ 300.";
        java.util.function.BiFunction<String, String, PriceTable> merged = (planExcerpt, discountExcerpt) -> ImportMerge.merge(
                PlaybookDocument.empty(), new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                        new ExtractedPlaybook.PriceTable(null, new BigDecimal("300"), discountExcerpt, List.of(new ExtractedPlaybook.Plan(
                                "Ouro", null, new BigDecimal("4500"), 6, new BigDecimal("800"), planExcerpt)))),
                document).draft().priceTable();

        // Citação literal, com espaço e quebra de linha diferentes: entra.
        PriceTable cited = merged.apply("Plano Ouro: R$ 4.500  à vista\nou 6x de R$ 800.", "Desconto máximo na call: R$ 300.");
        assertThat(cited.plans().getFirst().cash()).isEqualByComparingTo("4500.00");
        assertThat(cited.plans().getFirst().installments().value()).isEqualByComparingTo("800.00");
        assertThat(cited.maxDiscount()).isEqualByComparingTo("300.00");

        // Trecho que não está no documento (o modelo resumiu) ou que não contém o valor: fica vazio.
        PriceTable paraphrased = merged.apply("Plano Ouro custa R$ 4.500 à vista ou 6x de R$ 800", "Desconto de R$ 300");
        assertThat(paraphrased.plans().getFirst().cash()).isNull();
        assertThat(paraphrased.plans().getFirst().installments()).isNull();
        assertThat(paraphrased.maxDiscount()).isNull();
        PriceTable withoutValue = merged.apply("Plano Ouro:", "Desconto máximo na call:");
        assertThat(withoutValue.plans().getFirst().cash()).isNull();
        assertThat(withoutValue.maxDiscount()).isNull();
        // O valor está no documento, mas não no trecho citado: fica vazio.
        PriceTable elsewhere = merged.apply("Plano Ouro: R$ 4.500 à vista", "Plano Ouro: R$ 4.500");
        assertThat(elsewhere.plans().getFirst().installments()).isNull();
        assertThat(elsewhere.maxDiscount()).isNull();
    }

    private static ImportMerge.Result merge(PlaybookDocument current, ExtractedPlaybook.Plan plan, String column,
            BigDecimal discount) {
        var extracted = new ExtractedPlaybook(List.of(new ExtractedPlaybook.Stage("Abertura")), List.of(), List.of(),
                new ExtractedPlaybook.PriceTable(column, discount, "Desconto máximo na call: R$ 500.", List.of(plan)));
        return ImportMerge.merge(current, extracted, TEXT);
    }

    private static ExtractedPlaybook.Plan plan(BigDecimal cash, Integer count, BigDecimal value) {
        return new ExtractedPlaybook.Plan("6 meses " + UUID.randomUUID(), "12", cash, count, value,
                "Plano de 6 meses por R$ 7.000, ou 6x de R$ 1.166,67.");
    }
}
