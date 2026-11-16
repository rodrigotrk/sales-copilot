package app.salescopilot.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import app.salescopilot.support.IntegrationTest;

/**
 * As propriedades de e-mail do application.yml, aplicadas a um servidor SMTP falso que não anuncia
 * STARTTLS: o envio falha antes de mandar usuário, senha ou o link.
 */
class MailTlsTest extends IntegrationTest {

    @Autowired
    Environment environment;

    @Test
    @DisplayName("[C115] SMTP sem STARTTLS recusa o envio antes de mandar senha ou mensagem")
    void smtpRequiresTls() throws Exception {
        Map<String, String> configured = Binder.get(environment)
                .bind("spring.mail.properties", Bindable.mapOf(String.class, String.class)).get();
        assertThat(configured).containsEntry("mail.smtp.starttls.enable", "true")
                .containsEntry("mail.smtp.starttls.required", "true")
                .containsEntry("mail.smtp.ssl.checkserveridentity", "true");

        List<String> received = new CopyOnWriteArrayList<>();
        try (ServerSocket server = new ServerSocket(0)) {
            Thread smtp = Thread.ofVirtual().start(() -> answerWithoutStartTls(server, received));
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost("localhost");
            sender.setPort(server.getLocalPort());
            sender.setUsername("usuario");
            sender.setPassword("segredo");
            Properties properties = new Properties();
            properties.putAll(configured);
            properties.put("mail.smtp.auth", "true");
            sender.setJavaMailProperties(properties);
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom("no-reply@exemplo.test");
            message.setTo("pessoa@exemplo.test");
            message.setSubject("Seu link");
            message.setText("link de acesso");

            assertThatThrownBy(() -> sender.send(message)).isInstanceOf(MailException.class);
            smtp.join(5_000);
        }
        assertThat(received).isNotEmpty();
        assertThat(received).noneMatch(line -> line.toUpperCase().startsWith("AUTH"));
        assertThat(received).noneMatch(line -> line.toUpperCase().startsWith("MAIL FROM"));
        assertThat(received).noneMatch(line -> line.toUpperCase().startsWith("DATA"));
    }

    private static void answerWithoutStartTls(ServerSocket server, List<String> received) {
        try (Socket client = server.accept();
                var in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
                var out = new PrintWriter(client.getOutputStream(), true, StandardCharsets.US_ASCII)) {
            out.print("220 falso ESMTP\r\n");
            out.flush();
            String line;
            while ((line = in.readLine()) != null) {
                received.add(line);
                String command = line.toUpperCase();
                if (command.startsWith("EHLO")) {
                    out.print("250-falso\r\n250 AUTH LOGIN PLAIN\r\n");
                } else if (command.startsWith("QUIT")) {
                    out.print("221 tchau\r\n");
                    out.flush();
                    return;
                } else {
                    out.print("250 ok\r\n");
                }
                out.flush();
            }
        } catch (Exception e) {
            // O cliente fechou a conexão: é o comportamento esperado quando ele recusa seguir sem TLS.
        }
    }
}
