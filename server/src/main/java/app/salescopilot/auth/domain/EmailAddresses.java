package app.salescopilot.auth.domain;

import java.util.Locale;

public final class EmailAddresses {

    private EmailAddresses() {
    }

    /** E-mail como chave: sem espaços nas pontas e em minúsculas. */
    public static String normalize(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }

    /** Tamanho máximo de um endereço de e-mail (RFC 3696), o mesmo das colunas que o guardam. */
    public static final int MAX_LENGTH = 320;

    public static boolean looksValid(String email) {
        if (email.length() > MAX_LENGTH) {
            return false;
        }
        int at = email.indexOf('@');
        return at > 0 && at == email.lastIndexOf('@') && at < email.length() - 1 && !email.contains(" ");
    }
}
