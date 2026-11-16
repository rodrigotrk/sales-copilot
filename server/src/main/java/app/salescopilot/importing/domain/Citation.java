package app.salescopilot.importing.domain;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Citação verificável (AC 71, AD-065): um valor em dinheiro do modelo só entra no rascunho se vier de um número
 * escrito no documento que esteja inteiro dentro do trecho citado. Os números são lidos uma vez no documento
 * original, com posição; o trecho é localizado nele aceitando qualquer espaço entre as palavras, porque o texto
 * do documento e o que o modelo leu podem quebrar linhas em pontos diferentes.
 */
final class Citation {

    private static final String SPACE = "[\\s\\u00A0\\u202F\\u2007\\u2009]+";
    static final int MAX_EXCERPT = 2_000;

    private final String document;
    private final MoneyInText money;

    Citation(String documentText) {
        this.document = documentText == null ? "" : documentText;
        this.money = new MoneyInText(this.document);
    }

    boolean confirms(String excerpt, BigDecimal value) {
        // Uma citação é uma frase ou uma linha de tabela; um "trecho" do tamanho do documento não cita nada.
        if (excerpt == null || value == null || excerpt.isBlank() || excerpt.length() > MAX_EXCERPT) {
            return false;
        }
        String pattern = Arrays.stream(excerpt.strip().split(SPACE)).map(Pattern::quote).collect(Collectors.joining(SPACE));
        Matcher occurrence = Pattern.compile(pattern).matcher(document);
        while (occurrence.find()) {
            // Um número cortado pela borda do trecho ("4.500" dentro de "14.500") não vale.
            if (money.containsWithin(value, occurrence.start(), occurrence.end())) {
                return true;
            }
        }
        return false;
    }
}
