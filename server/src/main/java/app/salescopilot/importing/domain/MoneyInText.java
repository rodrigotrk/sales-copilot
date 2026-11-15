package app.salescopilot.importing.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Os valores em dinheiro escritos no documento. Um valor do modelo só entra no rascunho se estiver
 * aqui (critério 71): o modelo não inventa preço. Cada número tem uma leitura só: "7.000" e
 * "7,000" são milhar; "1.166,67" e "1,166.67" são milhar com centavos; "4500,50" e "4500.50" são
 * centavos. A troca de um valor do documento por outro também do documento é pega pela marca
 * "Importado · conferir" (AD-035), não aqui.
 */
public final class MoneyInText {

    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)*");
    private static final Pattern BRAZILIAN_THOUSANDS = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?");
    private static final Pattern ENGLISH_THOUSANDS = Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?");
    private static final Pattern COMMA_CENTS = Pattern.compile("\\d+,\\d{1,2}");
    private static final Pattern DOT_CENTS = Pattern.compile("\\d+\\.\\d{1,2}");
    private static final Pattern INTEGER = Pattern.compile("\\d+");

    private final Set<BigDecimal> values = new HashSet<>();

    public MoneyInText(String text) {
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            String plain = read(token);
            if (plain != null) {
                values.add(new BigDecimal(plain).stripTrailingZeros());
            }
        }
    }

    public boolean contains(BigDecimal value) {
        return value != null && values.contains(value.stripTrailingZeros());
    }

    private static String read(String token) {
        if (BRAZILIAN_THOUSANDS.matcher(token).matches()) {
            return token.replace(".", "").replace(",", ".");
        }
        if (ENGLISH_THOUSANDS.matcher(token).matches()) {
            return token.replace(",", "");
        }
        if (COMMA_CENTS.matcher(token).matches()) {
            return token.replace(",", ".");
        }
        if (DOT_CENTS.matcher(token).matches() || INTEGER.matcher(token).matches()) {
            return token;
        }
        return null;
    }
}
