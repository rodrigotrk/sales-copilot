package app.salescopilot.playbook.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import app.salescopilot.playbook.api.PlaybookDocument;
import app.salescopilot.playbook.api.PlaybookDocument.Installments;
import app.salescopilot.playbook.api.PlaybookDocument.Plan;
import app.salescopilot.playbook.api.PlaybookDocument.PriceTable;
import app.salescopilot.shared.FieldProblem;

import static org.assertj.core.api.Assertions.assertThat;

class DraftRulesTest {

    @Test
    @DisplayName("[C46a] à vista aceita 4500 e 4500,50 e recusa 0, 10000000,01 e três casas")
    void cashRule() {
        assertThat(cashProblems("4500")).isEmpty();
        assertThat(cashProblems("4500.50")).isEmpty();
        assertThat(cashProblems("10000000.00")).isEmpty();
        assertThat(cashProblems("0")).extracting(FieldProblem::message).containsExactly("Informe um valor em reais maior que zero.");
        assertThat(cashProblems("10000000.01")).extracting(FieldProblem::code).containsExactly("invalid_cash");
        assertThat(cashProblems("4500.505")).extracting(FieldProblem::field).containsExactly("cash");
        assertThat(cashProblems("-1")).hasSize(1);
    }

    @Test
    @DisplayName("[C47a] parcelado aceita 2 a 60 parcelas positivas e recusa o resto")
    void installmentsRule() {
        assertThat(installmentProblems(2, "800.00")).isEmpty();
        assertThat(installmentProblems(60, "100.00")).isEmpty();
        assertThat(installmentProblems(1, "800.00")).extracting(FieldProblem::message)
                .containsExactly("Parcelas de 2 a 60, com valor maior que zero.");
        assertThat(installmentProblems(61, "800.00")).hasSize(1);
        assertThat(installmentProblems(6, "0")).hasSize(1);
        assertThat(installmentProblems(6, null)).hasSize(1);
        assertThat(DraftRules.validate(draft(plan(new BigDecimal("4500"), null), null))).isEmpty();
    }

    @Test
    @DisplayName("[C51a] desconto 4499,99 aceito, 4500 recusado e 0 aceito com menor à vista 4500")
    void discountRule() {
        var plans = List.of(plan(new BigDecimal("7000"), null), plan(new BigDecimal("4500"), null));
        assertThat(discountProblems(plans, "4499.99")).isEmpty();
        assertThat(discountProblems(plans, "0")).isEmpty();
        assertThat(discountProblems(plans, "4500")).extracting(FieldProblem::message)
                .containsExactly("O desconto máximo precisa ser menor que o menor preço à vista (R$ 4.500).");
        assertThat(discountProblems(plans, "-0.01")).hasSize(1);
    }

    private static List<FieldProblem> cashProblems(String cash) {
        return DraftRules.validate(draft(plan(new BigDecimal(cash), null), null));
    }

    private static List<FieldProblem> installmentProblems(int count, String value) {
        return DraftRules.validate(draft(plan(new BigDecimal("4500"), new Installments(count, value == null ? null : new BigDecimal(value))), null));
    }

    private static List<FieldProblem> discountProblems(List<Plan> plans, String discount) {
        return DraftRules.validate(new PlaybookDocument(1, List.of(), List.of(), List.of(),
                new PriceTable(null, new BigDecimal(discount), null, plans)));
    }

    private static Plan plan(BigDecimal cash, Installments installments) {
        return new Plan(UUID.randomUUID(), "Plano", null, cash, installments, null);
    }

    private static PlaybookDocument draft(Plan plan, BigDecimal discount) {
        return new PlaybookDocument(1, List.of(), List.of(), List.of(), new PriceTable(null, discount, null, List.of(plan)));
    }
}
