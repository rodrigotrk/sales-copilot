package app.salescopilot.auth.domain;

import java.util.Locale;

/**
 * O Google só prova a posse do e-mail quando é autoridade sobre ele (AD-047): conta Gmail, ou conta
 * de domínio Workspace identificada pela claim `hd`. Fora disso, `email_verified` pode refletir uma
 * verificação antiga.
 */
public final class GoogleAuthority {

    private GoogleAuthority() {
    }

    public static boolean isAuthoritative(String email, String hostedDomain) {
        if (hostedDomain != null && !hostedDomain.isBlank()) {
            return true;
        }
        return email.toLowerCase(Locale.ROOT).endsWith("@gmail.com");
    }
}
