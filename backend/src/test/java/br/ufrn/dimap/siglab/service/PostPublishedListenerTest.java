package br.ufrn.dimap.siglab.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostPublishedListenerTest {

    @Mock FollowerNotificationService notifications;
    PostPublishedListener listener;

    @BeforeEach void setup() { listener=new PostPublishedListener(notifications); }

    private PostPublishedEvent event() {
        return new PostPublishedEvent("lab", 9L, 4L, "Título", null, "Corpo", new String[0], new String[0]);
    }

    @Test void delegatesTheNotificationToTheService() {
        PostPublishedEvent event=event();
        listener.onPostPublished(event);
        verify(notifications).notifyFollowers(event);
    }

    @Test void notificationFailureDoesNotPropagate() {
        PostPublishedEvent event=event();
        doThrow(new IllegalStateException("falha")).when(notifications).notifyFollowers(event);
        assertDoesNotThrow(() -> listener.onPostPublished(event));
    }
}
