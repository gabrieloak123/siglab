package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import jakarta.mail.Session;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock JavaMailSender mailSender;
    EmailService service;

    @BeforeEach void setup() {
        service = new EmailService(mailSender, "noreplyprojetopds@gmail.com");
    }

    @Test void sendsPlainTextMessageFromConfiguredAddress() throws Exception {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);

        service.sendEmail("destinatario@lab.org", "Assunto", "Corpo do e-mail");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        MimeMessage sent = captor.getValue();
        assertEquals("noreplyprojetopds@gmail.com", sent.getFrom()[0].toString());
        assertEquals("destinatario@lab.org", sent.getAllRecipients()[0].toString());
        assertEquals("Assunto", sent.getSubject());
        assertEquals("Corpo do e-mail", sent.getContent());
    }

    @Test void wrapsMailFailureInExternalServiceException() {
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(message);
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> service.sendEmail("destinatario@lab.org", "Assunto", "Corpo"));

        assertTrue(ex.getMessage().contains("Connection refused"));
    }
}
