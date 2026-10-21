package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

/**
 * Serviço de envio de e-mails utilizando o servidor SMTP do Gmail,
 * configurado através das variáveis de ambiente {@code SMTP_USERNAME} e {@code SMTP_PASSWORD}.
 *
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailService(JavaMailSender mailSender,
                        @Value("${spring.mail.username:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    /**
     * Envia um e-mail em texto puro de forma síncrona.
     *
     * @param to      o endereço de e-mail do destinatário
     * @param subject o assunto do e-mail
     * @param body    o corpo do e-mail em texto puro
     * @throws ExternalServiceException se o servidor SMTP recusar ou falhar no envio
     */
    public void sendEmail(String to, String subject, String body) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(body, false); // texto puro
            mailSender.send(message);
            log.info("E-mail enviado para {} com assunto '{}'", to, subject);
        } catch (MessagingException | MailException ex) {
            throw new ExternalServiceException("Falha ao enviar e-mail: " + ex.getMessage(), ex);
        }
    }
}
