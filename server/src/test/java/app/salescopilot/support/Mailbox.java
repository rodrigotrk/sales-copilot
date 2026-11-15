package app.salescopilot.support;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

/** Guarda os e-mails em vez de enviá-los por SMTP. */
public class Mailbox implements MailSender {

    private final List<SimpleMailMessage> sent = new CopyOnWriteArrayList<>();
    private volatile boolean down;

    @Override
    public void send(SimpleMailMessage... messages) {
        if (down) {
            throw new org.springframework.mail.MailSendException("servidor de e-mail fora do ar (simulado)");
        }
        sent.addAll(List.of(messages));
    }

    /** Simula o servidor SMTP fora do ar. */
    public void setDown(boolean down) {
        this.down = down;
    }

    public List<SimpleMailMessage> to(String email) {
        return sent.stream().filter(m -> m.getTo() != null && List.of(m.getTo()).contains(email)).toList();
    }

    /** O token do último link enviado ao e-mail para o caminho dado ("/entrar/link" ou "/entrar/vincular"). */
    public String lastToken(String email, String path) {
        var messages = to(email);
        for (int i = messages.size() - 1; i >= 0; i--) {
            Matcher matcher = Pattern.compile(Pattern.quote(path) + "\\?token=([A-Za-z0-9_-]+)").matcher(messages.get(i).getText());
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        throw new AssertionError("nenhum link " + path + " enviado para " + email);
    }

    public void clear() {
        down = false;
        sent.clear();
    }
}
