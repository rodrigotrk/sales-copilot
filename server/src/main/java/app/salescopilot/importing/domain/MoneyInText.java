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

    /** O número inteiro, inclusive um expoente com qualquer sinal: "1E3" e "1E−3" são um token só, e nenhum pedaço vale. */
    private static final Pattern NUMBER = Pattern.compile(
            "\\d+(?:[.,]\\d+)*(?:[eE][+\\-\u2212\uFE63\uFF0D\u2013\u2014]?\\d+)?");
    /** Hífen e as variantes Unicode do sinal de menos; o travessão (–) é separador, não sinal. */
    private static final String MINUS = "-\u2212\uFE63\uFF0D";
    private static final Pattern BRAZILIAN_THOUSANDS = Pattern.compile("\\d{1,3}(?:\\.\\d{3})+(?:,\\d{1,2})?");
    private static final Pattern ENGLISH_THOUSANDS = Pattern.compile("\\d{1,3}(?:,\\d{3})+(?:\\.\\d{1,2})?");
    private static final Pattern COMMA_CENTS = Pattern.compile("\\d+,\\d{1,2}");
    private static final Pattern DOT_CENTS = Pattern.compile("\\d+\\.\\d{1,2}");
    private static final Pattern INTEGER = Pattern.compile("\\d+");

    private final Set<BigDecimal> values = new HashSet<>();

    public MoneyInText(String text) {
        Matcher matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            if (isNegative(text, matcher.start()) || !isIsolated(text, matcher.start(), matcher.end())) {
                continue;
            }
            String token = matcher.group();
            // Notação científica não é como se escreve preço; o token inteiro fica de fora.
            if (token.indexOf('e') >= 0 || token.indexOf('E') >= 0) {
                continue;
            }
            String plain = read(token);
            if (plain != null) {
                values.add(new BigDecimal(plain).stripTrailingZeros());
            }
        }
    }

    /**
     * O sinal de menos fica colado: "-7.000", "R$ -7.000" ou "-R$ 7.000" é negativo e não autoriza o valor
     * positivo. Hífen seguido de espaço é marcador de lista ou separador ("- R$ 4.500 à vista", "Plano A -
     * R$ 4.500"), e hífen depois de outro número é faixa ("7.000-8.000").
     */
    private static boolean isNegative(String text, int start) {
        int at = start - 1;
        if (at >= 1 && text.startsWith("R$", at - 1)) {
            at -= 2;
        } else if (at >= 0 && isSpace(text.charAt(at))) {
            // "R$ -7.000" ou "-R$ 7.000": espaço só entre o R$ e o número.
            int beforeSpace = skipSpaces(text, at);
            if (beforeSpace >= 1 && text.startsWith("R$", beforeSpace - 1)) {
                at = beforeSpace - 2;
            } else {
                return false;
            }
        }
        if (at < 0 || MINUS.indexOf(text.charAt(at)) < 0) {
            return false;
        }
        return at == 0 || !Character.isDigit(text.charAt(at - 1));
    }

    /** Antes do número: começo, espaço, "$", abertura, pontuação, sinal ou travessão de faixa. */
    private static final String BEFORE = "$([{\"'*•:;,/+-\u2212\uFE63\uFF0D\u2013\u2014";
    /** Depois do número: pontuação e fechamento. Letras e espaço têm regra própria. */
    private static final String AFTER = ".,;:!?)]}\"'*%/";
    private static final String SIGNS = "+-\u2212\uFE63\uFF0D\u2010\u2011\u2012\u207B";

    /**
     * Só um número isolado autoriza um valor (AD-064). Um pedaço de outra grafia nunca entra: "1e³", "1×10³",
     * "7 000" com espaço não separável, "7'000" ou "Kit3" não são lidos; o campo fica vazio para o gestor.
     * Valem os delimitadores comuns de preço: "R$ 4.500 à vista", "6x de R$ 800", "R$ 4.500em 6x" (palavra).
     */
    private static boolean isIsolated(String text, int start, int end) {
        if (groupsThousands(text, start, end) || continuesExponent(text, start) || startsExponent(text, end)) {
            return false;
        }
        if (start > 0) {
            char before = text.charAt(start - 1);
            if (isSpace(before)) {
                // "1E 3" ou "1x 3": a letra colada a um dígito antes do espaço faz deste número a continuação de outro.
                int previous = skipSpaces(text, start - 1);
                if (previous >= 1 && Character.isLetter(text.charAt(previous)) && Character.isDigit(text.charAt(previous - 1))) {
                    return false;
                }
            } else if (BEFORE.indexOf(before) < 0) {
                return false;
            } else if (start >= 2 && Character.isDigit(text.charAt(start - 2)) && "-\u2013\u2014".indexOf(before) < 0) {
                // "7'000" ou "1,5": pontuação entre dígitos continua o mesmo número; só a faixa separa dois valores.
                return false;
            }
        }
        if (end >= text.length()) {
            return true;
        }
        char after = text.charAt(end);
        if (isSpace(after) || after == '-' || after == '\u2013' || after == '\u2014') {
            // Fim de palavra, faixa ("1.200-1.400") ou separador ("4.500 - à vista").
            return true;
        }
        if (AFTER.indexOf(after) >= 0) {
            // "7'000" ou "7.000,": a pontuação seguida de dígito continua o número de outra forma.
            return end + 1 >= text.length() || !Character.isDigit(text.charAt(end + 1));
        }
        if (Character.isLetter(after)) {
            // Uma palavra colada ("4.500em", "6x de") vale; letra seguida de dígito, expoente ou "x10" não.
            char next = end + 1 < text.length() ? text.charAt(end + 1) : ' ';
            boolean multiplication = (after == 'x' || after == 'X') && !Character.isLetter(next) && !isSpace(next)
                    && AFTER.indexOf(next) < 0;
            boolean exponent = (after == 'e' || after == 'E') && !Character.isLetter(next);
            return !multiplication && !exponent && (Character.isLetter(next) || isSpace(next) || AFTER.indexOf(next) >= 0
                    || end + 1 >= text.length());
        }
        return false;
    }

    /**
     * "7 000" com espaço horizontal: 1 a 3 dígitos e um grupo de exatamente 3 dígitos formam um número só. Quebra
     * de linha e tabulação separam valores, como numa lista ou numa tabela.
     */
    private static boolean groupsThousands(String text, int start, int end) {
        String token = text.substring(start, end);
        if (token.matches("\\d{1,3}") && end + 4 <= text.length() && isHorizontalSpace(text.charAt(end))
                && text.substring(end + 1, end + 4).matches("\\d{3}")
                && (end + 4 == text.length() || !Character.isDigit(text.charAt(end + 4)))) {
            return true;
        }
        return token.matches("\\d{3}(?:[.,]\\d{1,2})?") && start >= 2 && isHorizontalSpace(text.charAt(start - 1))
                && Character.isDigit(text.charAt(start - 2));
    }

    /** O número depois de "E" colado a um dígito ("1E++3", "1e 3") ou de "E" maiúsculo solto ("1 E 3") é expoente. */
    private static boolean continuesExponent(String text, int start) {
        int at = start - 1;
        while (at >= 0 && (isHorizontalSpace(text.charAt(at)) || SIGNS.indexOf(text.charAt(at)) >= 0)) {
            at--;
        }
        if (at < 0 || (text.charAt(at) != 'e' && text.charAt(at) != 'E')) {
            return false;
        }
        boolean glued = at >= 1 && Character.isDigit(text.charAt(at - 1));
        boolean spacedCapital = text.charAt(at) == 'E' && at >= 2 && isHorizontalSpace(text.charAt(at - 1))
                && Character.isDigit(text.charAt(skipSpaces(text, at - 1)));
        return glued || spacedCapital;
    }

    /** A mantissa de "1 × 10³", "1 x 10^3" ou "1 E 3" não vale sozinha. */
    private static boolean startsExponent(String text, int end) {
        int at = end;
        while (at < text.length() && isHorizontalSpace(text.charAt(at))) {
            at++;
        }
        if (at == end || at >= text.length()) {
            return false;
        }
        char operator = text.charAt(at);
        int next = at + 1;
        while (next < text.length() && (isHorizontalSpace(text.charAt(next)) || SIGNS.indexOf(text.charAt(next)) >= 0)) {
            next++;
        }
        if (operator == 'E') {
            return next < text.length() && Character.isDigit(text.charAt(next)) && next > at + 1;
        }
        if ("\u00D7xX*".indexOf(operator) >= 0) {
            return text.startsWith("10", next) && next + 2 < text.length()
                    && (text.charAt(next + 2) == '^' || Character.getType(text.charAt(next + 2)) == Character.OTHER_NUMBER);
        }
        return false;
    }

    private static boolean isHorizontalSpace(char c) {
        return c == ' ' || c == '\u00A0' || c == '\u202F' || c == '\u2009' || c == '\u2007';
    }

    /** Inclui o espaço não separável, que a formatação de moeda em pt-BR põe entre "R$" e o número. */
    private static boolean isSpace(char c) {
        return Character.isWhitespace(c) || Character.isSpaceChar(c);
    }

    private static int skipSpaces(String text, int at) {
        while (at >= 0 && isSpace(text.charAt(at))) {
            at--;
        }
        return at;
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
