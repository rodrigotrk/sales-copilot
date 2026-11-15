package app.salescopilot.auth;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import app.salescopilot.auth.api.CurrentUser;
import app.salescopilot.auth.domain.EmailAddresses;
import app.salescopilot.auth.domain.GoogleAuthority;

/** Decide o que um login com Google produz (AD-039, AD-047, AD-052). */
@Component
class GoogleSignIn {

    record GoogleIdentity(String subject, String email, boolean emailVerified, String hostedDomain) {
    }

    sealed interface Outcome {
        record Rejected() implements Outcome {
        }

        record SignedIn(CurrentUser user) implements Outcome {
        }

        record ConfirmationSent(String email) implements Outcome {
        }
    }

    private final UserAccounts users;
    private final IdentityLinkChallenges challenges;
    private final LoginMail mail;

    GoogleSignIn(UserAccounts users, IdentityLinkChallenges challenges, LoginMail mail) {
        this.users = users;
        this.challenges = challenges;
        this.mail = mail;
    }

    @Transactional
    Outcome handle(GoogleIdentity identity) {
        if (!identity.emailVerified()) {
            return new Outcome.Rejected();
        }
        var linked = users.findByGoogleSubject(identity.subject());
        if (linked.isPresent()) {
            return new Outcome.SignedIn(linked.get());
        }
        String email = EmailAddresses.normalize(identity.email());
        if (GoogleAuthority.isAuthoritative(email, identity.hostedDomain())) {
            CurrentUser user = users.findOrCreateByEmail(email);
            users.linkGoogle(user.id(), identity.subject());
            return new Outcome.SignedIn(user);
        }
        String token = challenges.create(email, identity.subject());
        mail.sendIdentityLink(email, token);
        return new Outcome.ConfirmationSent(email);
    }
}
