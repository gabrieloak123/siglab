package br.ufrn.dimap.siglab.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Recebe o evento de publicação de post e aciona a notificação dos seguidores.
 * <p>
 * A escuta ocorre depois do commit da transação que salvou o post, para que
 * apenas publicações realmente gravadas sejam anunciadas, e em uma thread
 * separada, para que a resposta HTTP não dependa do servidor SMTP e uma falha
 * de envio não jeopardize a publicação.
 * </p>
 *
 */
@Component
public class PostPublishedListener {

    private static final Logger log = LoggerFactory.getLogger(PostPublishedListener.class);

    private final FollowerNotificationService notifications;

    public PostPublishedListener(FollowerNotificationService notifications) {
        this.notifications = notifications;
    }

    /**
     * Notifica os seguidores após a confirmação da transação de publicação.
     *
     * @param event o evento de publicação do post
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostPublished(PostPublishedEvent event) {
        try {
            notifications.notifyFollowers(event);
        } catch (RuntimeException ex) {
            log.warn("Falha ao notificar seguidores sobre o post {}: {}", event.postId(), ex.getMessage());
        }
    }
}
