package app.salescopilot.importing.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Os valores em dinheiro escritos no documento. Um valor do modelo só entra no rascunho se estiver
 * aqui (critério 71): o modelo não inventa preço. Cada número do texto é lido como "7.000,50" e como
 * "7,000.50", porque um playbook pode vir dos dois jeitos.
 */
public final class MoneyInText {

    private static final Pattern NUMBER = Pattern.compile("\\d[\\d.,]*\\d|\\d");

    private final Set<BigDecimal> values = new HashSet<>();

    public MoneyInText(String text) {
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            String token = matcher.group();
            add(token.replace(".", "").replace(",", "."));
            add(token.replace(",", ""));
        }
    }

    public boolean contains(BigDecimal value) {
        return value != null && values.contains(value.stripTrailingZeros());
    }

    private void add(String candidate) {
        if (candidate.chars().filter(c -> c == '.').count() > 1) {
            return;
        }
        try {
            values.add(new BigDecimal(candidate).stripTrailingZeros());
        } catch (NumberFormatException ignored) {
            // Um token como "1.2.3" não é valor em dinheiro em nenhuma das duas leituras.
        }
    }
}
