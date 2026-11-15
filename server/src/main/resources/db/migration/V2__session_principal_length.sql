-- O login aceita e-mails de até 320 caracteres; o principal da sessão é o e-mail.
ALTER TABLE spring_session ALTER COLUMN principal_name TYPE varchar(320);
