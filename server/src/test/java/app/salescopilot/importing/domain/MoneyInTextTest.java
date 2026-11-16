package app.salescopilot.importing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyInTextTest {

    @Test
    @DisplayName("[C134] só número isolado autoriza valor: pedaço de outra grafia nunca entra, e as grafias usuais valem")
    void onlyIsolatedNumbersAuthorize() {
        // Nenhum pedaço destas grafias pode autorizar 1, 3, 7, 10 ou 0.
        for (String text : List.of("R$ 1E\u20103", "R$ 1E\u20113", "R$ 1E\u20123", "R$ 1E\u207B3", "1e\u00B3", "1E\uFF13",
                "1E+\u22123", "1E 3", "1\u00D710\u00B3", "1x10^3", "R$ 7\u00A0000", "R$ 7\u202F000", "R$ 7'000",
                "R$ 1 \u00D7 10\u00B3", "1,5 x 10^3", "1 E 3", "1E++3", "R$ 7 000")) {
            var money = new MoneyInText(text);
            for (String value : List.of("1", "3", "7", "10", "0")) {
                assertThat(money.contains(new BigDecimal(value))).as(text + " autoriza " + value).isFalse();
            }
        }
        // "E" com só espaços antes, no começo do texto, não derruba a leitura; sem dígito antes, o 3 é um número escrito.
        org.assertj.core.api.Assertions.assertThatCode(() -> new MoneyInText("  E 3\nEtapas: Abertura.")).doesNotThrowAnyException();
        assertThat(new MoneyInText("  E 3\nEtapas: Abertura.").contains(new BigDecimal("3"))).isTrue();

        // As grafias usuais de preço continuam valendo.
        var usual = new MoneyInText("""
                Planos:
                - Plano A - R$ 4.500 à vista, ou 6x de R$ 800.
                - Plano B: R$4.500,50em 12x (R$ 1.200) e desconto de até 300.
                Faixa 1.200-1.400. Mensal: 99/mês. Taxa 5%.
                """);
        for (String value : List.of("4500", "6", "800", "4500.50", "12", "1200", "300", "1400", "99", "5")) {
            assertThat(usual.contains(new BigDecimal(value))).as(value).isTrue();
        }
        // Listas, tabelas e o "e" de "e também" separam valores.
        var lists = new MoneyInText("À vista: R$ 4.500\n12x de R$ 450\n1. Plano A: R$ 3.900\n2. Plano B: R$ 5.200\n"
                + "Ouro\t6.800\t24\t10%\nR$ 2.500 e 10x de R$ 300");
        for (String value : List.of("4500", "12", "450", "3900", "5200", "6800", "24", "10", "2500", "300")) {
            assertThat(lists.contains(new BigDecimal(value))).as("lista: " + value).isTrue();
        }
    }
}
