package app.salescopilot.shared;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/** "4.500" ou "1.166,67": ponto de milhar e centavos só quando diferentes de zero, como nas telas. */
public final class BrazilianReais {

    private BrazilianReais() {
    }

    public static String format(BigDecimal value) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.of("pt", "BR"));
        boolean whole = value.stripTrailingZeros().scale() <= 0;
        return new DecimalFormat(whole ? "#,##0" : "#,##0.00", symbols).format(value);
    }
}
