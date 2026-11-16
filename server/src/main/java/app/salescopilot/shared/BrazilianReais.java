package app.salescopilot.shared;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** "4.500" ou "1.166,67": ponto de milhar e centavos só quando diferentes de zero, como nas telas. */
public final class BrazilianReais {

    private BrazilianReais() {
    }

    /** `numeric(12,2)` (AD-025): até 10 dígitos inteiros e 2 casas, ou seja, até R$ 9.999.999.999,99. */
    public static final int MAX_INTEGER_DIGITS = 10;

    /**
     * Valor em reais com no máximo duas casas e até {@link #MAX_INTEGER_DIGITS} dígitos inteiros. A representação é
     * conferida antes de qualquer conta: `setScale` ou `toPlainString` em 1E+2147483647 estoura ou esgota a memória.
     */
    public static boolean isExactReais(BigDecimal value) {
        long integerDigits = (long) value.precision() - value.scale();
        boolean bounded = value.precision() <= 40 && value.scale() <= 20 && value.scale() >= -MAX_INTEGER_DIGITS
                && integerDigits <= MAX_INTEGER_DIGITS;
        return bounded && value.stripTrailingZeros().scale() <= 2;
    }

    public static String format(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("pt", "BR"));
        boolean whole = value.stripTrailingZeros().scale() <= 0;
        return new DecimalFormat(whole ? "#,##0" : "#,##0.00", symbols).format(value);
    }
}
