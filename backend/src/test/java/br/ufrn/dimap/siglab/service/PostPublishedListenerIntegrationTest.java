package br.ufrn.dimap.siglab.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostPublishedListenerIntegrationTest {

    @Mock FollowerNotificationService notifications;

    @Test void notifiesFollowersOnlyAfterTheTransactionCommitsAndOutsideTheCallerThread() throws Exception {
        CountDownLatch notified = new CountDownLatch(1);
        AtomicReference<String> thread = new AtomicReference<>();
        doAnswer(invocation -> { thread.set(Thread.currentThread().getName()); notified.countDown(); return null; })
                .when(notifications).notifyFollowers(any());

        new ApplicationContextRunner()
                .withUserConfiguration(AsyncConfiguration.class)
                .withBean(FollowerNotificationService.class, () -> notifications)
                .withBean(PostPublishedListener.class)
                .run(context -> {
                    assertNull(context.getStartupFailure());
                    TransactionTemplate transaction = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
                    String caller = Thread.currentThread().getName();
                    transaction.executeWithoutResult(status -> {
                        context.publishEvent(new PostPublishedEvent("lab", 9L, 4L, "Título", null, "Corpo", new String[0], new String[0]));
                        try {
                            assertFalse(notified.await(200, TimeUnit.MILLISECONDS), "a notificação não pode ocorrer antes do commit");
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            fail("espera interrompida");
                        }
                    });
                    assertTrue(notified.await(5, TimeUnit.SECONDS), "a notificação deve ocorrer após o commit");
                    assertNotEquals(caller, thread.get());
                });
    }

    @Configuration
    @EnableAsync
    @EnableTransactionManagement
    static class AsyncConfiguration {
        @Bean
        PlatformTransactionManager transactionManager() {
            return new StubTransactionManager();
        }
    }

    /** Gerenciador transacional sem banco, suficiente para exercitar o commit. */
    static class StubTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}
