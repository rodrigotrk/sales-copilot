package app.salescopilot.playbook.api;

/** Limites do rascunho (plano, critérios 35 e 36), usados também pela importação. */
public final class PlaybookLimits {

    public static final int SHORT_TEXT = 120;
    public static final int LONG_TEXT = 500;
    public static final int MAX_STAGES = 30;
    public static final int MAX_DISCOVERY = 50;
    public static final int MAX_OBJECTIONS = 50;
    public static final int MAX_PLANS = 20;
    public static final int MIN_INSTALLMENTS = 2;
    public static final int MAX_INSTALLMENTS = 60;

    private PlaybookLimits() {
    }
}
