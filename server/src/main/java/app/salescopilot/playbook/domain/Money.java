package app.salescopilot.playbook.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em reais: decimal exato com duas casas (AD-025). */
public final class Money {

    public static final BigDecimal MAX_CASH = new BigDecimal("10000000.00");

    private Money() {
    }

    public static boolean hasAtMostTwoDecimals(BigDecimal value) {
        return value.stripTrailingZeros().scale() <= 2;
    }

    /** Só para valores já conferidos com {@link #hasAtMostTwoDecimals}: não arredonda nada. */
    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static boolean isValidCash(BigDecimal cash) {
        return hasAtMostTwoDecimals(cash) && cash.signum() > 0 && cash.compareTo(MAX_CASH) <= 0;
    }
}
