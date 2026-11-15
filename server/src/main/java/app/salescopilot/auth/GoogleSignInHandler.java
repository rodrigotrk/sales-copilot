package app.salescopilot.auth;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Depois do retorno do Google, traduz a decisão de {@link GoogleSignIn} em sessão ou redirecionamento. */
@Component
class GoogleSignInHandler implements AuthenticationSuccessHandler {

    private final GoogleSignIn signIn;
    private final SignedInSession session;

    GoogleSignInHandler(GoogleSignIn signIn, SignedInSession session) {
        this.signIn = signIn;
        this.session = session;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        OidcUser oidc = (OidcUser) authentication.getPrincipal();
        var identity = new GoogleSignIn.GoogleIdentity(oidc.getSubject(), oidc.getEmail(),
                Boolean.TRUE.equals(oidc.getEmailVerified()), oidc.getClaimAsString("hd"));
        switch (signIn.handle(identity)) {
            case GoogleSignIn.Outcome.SignedIn signedIn -> {
                session.remember(request, signedIn.user());
                response.sendRedirect("/");
            }
            case GoogleSignIn.Outcome.Rejected rejected -> {
                session.signOut(request);
                response.sendRedirect("/entrar?erro=google_unverified");
            }
            case GoogleSignIn.Outcome.ConfirmationSent sent -> {
                session.signOut(request);
                response.sendRedirect(UriComponentsBuilder.fromPath("/entrar")
                        .queryParam("confirmar", sent.email())
                        .encode()
                        .toUriString());
            }
        }
    }
}
