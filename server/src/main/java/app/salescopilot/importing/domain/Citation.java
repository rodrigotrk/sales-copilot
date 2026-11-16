package app.salescopilot.importing.domain;

import java.math.BigDecimal;

/**
 * Citação verificável (AC 71, AD-065): um valor em dinheiro do modelo só entra no rascunho se o trecho que o
 * modelo citou estiver literalmente no documento e contiver esse valor escrito. Espaços são normalizados nos
 * dois lados, porque o texto do documento e o que o modelo leu podem quebrar linhas em pontos diferentes.
 */
final class Citation {

    private final String document;

    Citation(String documentText) {
        this.document = normalize(documentText);
    }

    boolean confirms(String excerpt, BigDecimal value) {
        if (excerpt == null || value == null) {
            return false;
        }
        String quoted = normalize(excerpt);
        return !quoted.isEmpty() && document.contains(quoted) && new MoneyInText(quoted).contains(value);
    }

    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("[\\s\\u00A0\\u202F\\u2007\\u2009]+", " ").strip();
    }
}
