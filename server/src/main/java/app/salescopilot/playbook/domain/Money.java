package app.salescopilot.playbook.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Valores em reais: decimal exato com duas casas (AD-025). */
public final class Money {

    public static final BigDecimal MAX_CASH = new BigDecimal("10000000.00");

    private Money() {
    }

    /** 15 dígitos inteiros: mais que qualquer valor de venda, e longe do que um número curto como 1E+100000000 expande. */
    static final int MAX_INTEGER_DIGITS = 15;

    /**
     * Valor em reais com no máximo duas casas e representação de tamanho limitado. A representação é conferida
     * antes de qualquer conta: `setScale` ou `toPlainString` em 1E+2147483647 estoura ou esgota a memória.
     */
    public static boolean hasAtMostTwoDecimals(BigDecimal value) {
        long integerDigits = (long) value.precision() - value.scale();
        boolean bounded = value.precision() <= 40 && value.scale() <= 20 && value.scale() >= -MAX_INTEGER_DIGITS
                && integerDigits <= MAX_INTEGER_DIGITS;
        return bounded && value.stripTrailingZeros().scale() <= 2;
    }

    /** Só para valores já conferidos com {@link #hasAtMostTwoDecimals}: não arredonda nada. */
    public static BigDecimal normalize(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static boolean isValidCash(BigDecimal cash) {
        return hasAtMostTwoDecimals(cash) && cash.signum() > 0 && cash.compareTo(MAX_CASH) <= 0;
    }
}
