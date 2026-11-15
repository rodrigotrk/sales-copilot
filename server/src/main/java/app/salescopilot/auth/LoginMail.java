package app.salescopilot.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

/** Envia os links de acesso e de confirmação de associação com o Google. */
@Component
class LoginMail {

    private final MailSender mail;
    private final String baseUrl;
    private final String from;

    LoginMail(MailSender mail, @Value("${app.base-url}") String baseUrl, @Value("${app.mail-from}") String from) {
        this.mail = mail;
        this.baseUrl = baseUrl;
        this.from = from;
    }

    void sendLoginLink(String email, String token) {
        send(email, "Seu link de acesso ao Sales Copilot",
                "Use este link para entrar no Sales Copilot. Ele vale por 15 minutos e só uma vez:\n\n"
                        + link("/entrar/link", token));
    }

    void sendIdentityLink(String email, String token) {
        send(email, "Confirme seu e-mail no Sales Copilot",
                "Use este link para confirmar seu e-mail e entrar com sua conta Google. Ele vale por 15 minutos e só uma vez:\n\n"
                        + link("/entrar/vincular", token));
    }

    private String link(String path, String token) {
        return UriComponentsBuilder.fromUriString(baseUrl).path(path).queryParam("token", token).toUriString();
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mail.send(message);
    }
}
