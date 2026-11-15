package app.salescopilot.auth.domain;

import java.util.Locale;

public final class EmailAddresses {

    private EmailAddresses() {
    }

    /** E-mail como chave: sem espaços nas pontas e em minúsculas. */
    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    public static boolean looksValid(String email) {
        int at = email.indexOf('@');
        return at > 0 && at == email.lastIndexOf('@') && at < email.length() - 1 && !email.contains(" ");
    }
}
