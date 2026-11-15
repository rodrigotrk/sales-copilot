package app.salescopilot;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;

import app.salescopilot.support.TestDatabase;

/**
 * Sobe o servidor local sobre o Postgres de teste (com os papéis de produção), para usar o app
 * web de verdade: `make e2e-server`. Os e-mails vão para o arquivo de `app.dev.mail-file`.
 */
public class TestServerApplication {

    public static void main(String[] args) throws IOException {
        System.setProperty("spring.datasource.url", TestDatabase.POSTGRES.getJdbcUrl());
        System.setProperty("app.import.directory", Files.createTempDirectory("imports").toString());
        SpringApplication.from(ServerApplication::main).with(LocalBeans.class).run(args);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class LocalBeans {

        @Bean
        @Primary
        MailSender fileMail(@Value("${app.dev.mail-file}") Path file) {
            return new MailSender() {
                @Override
                public void send(SimpleMailMessage... messages) {
                    for (SimpleMailMessage message : messages) {
                        String line = String.join(",", message.getTo()) + " " + message.getText().replace('\n', ' ') + "\n";
                        try {
                            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                }
            };
        }

        @Bean
        ApplicationRunner allowLocalEmail(@Value("${app.dev.allowed-email}") String email) {
            return args -> TestDatabase.admin()
                    .sql("INSERT INTO allowed_signups (email) VALUES (?) ON CONFLICT DO NOTHING").param(email).update();
        }
    }
}
