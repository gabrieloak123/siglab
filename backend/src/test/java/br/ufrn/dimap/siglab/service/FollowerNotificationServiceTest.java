package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.FollowRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FollowerNotificationServiceTest {

    @Mock FollowRepository follows;
    @Mock ResearchGroupRepository groups;
    @Mock UserRepository users;
    @Mock EmailService emails;
    FollowerNotificationService service;

    @BeforeEach void setup() {
        service = new FollowerNotificationService(follows, groups, users, emails, "http://localhost:3000");
    }

    private FollowRepository.Follower follower(long id, String email, String name, String... areas) {
        return new FollowRepository.Follower(id, email, name, areas);
    }

    private PostPublishedEvent event(String title, String content, String... keywords) {
        return new PostPublishedEvent("lab", 9L, 4L, title, null, content, new String[0], keywords);
    }

    private void publisher() {
        when(users.findById(4L)).thenReturn(Optional.of(User.builder().id(4L).name("Líder").email("lider@lab.org").build()));
    }

    private void group() {
        when(groups.findById("lab")).thenReturn(Optional.of(ResearchGroup.builder().group_id("lab").name("Laboratório").build()));
    }

    @Test void notifiesGroupFollowersAndResearcherFollowersWithoutDuplicates() {
        publisher();
        group();
        when(follows.groupFollowers("lab")).thenReturn(List.of(follower(1L, "ana@lab.org", "Ana")));
        when(follows.researcherFollowers(4L)).thenReturn(List.of(
                follower(1L, "ana@lab.org", "Ana"),
                follower(2L, "bruno@lab.org", "Bruno")));

        service.notifyFollowers(event("Novo artigo", "Conteúdo"));

        ArgumentCaptor<String> recipients = ArgumentCaptor.forClass(String.class);
        verify(emails, times(2)).sendEmail(recipients.capture(), anyString(), anyString());
        assertEquals(List.of("ana@lab.org", "bruno@lab.org"), recipients.getAllValues());
    }

    @Test void neverNotifiesTheAuthor() {
        publisher();
        when(follows.groupFollowers("lab")).thenReturn(List.of(follower(4L, "lider@lab.org", "Líder")));
        when(follows.researcherFollowers(4L)).thenReturn(List.of(follower(4L, "LIDER@lab.org", "Líder")));

        service.notifyFollowers(event("Novo artigo", "Conteúdo"));

        verifyNoInteractions(emails);
    }

    @Test void researcherFollowerIsNotifiedWhenPostMatchesInterestArea() {
        publisher();
        group();
        when(follows.groupFollowers("lab")).thenReturn(List.of());
        when(follows.researcherFollowers(4L)).thenReturn(List.of(follower(3L, "carla@lab.org", "Carla", "Deep Learning")));

        service.notifyFollowers(event("Deep learning aplicado", "Redes neurais convolucionais"));

        verify(emails).sendEmail(eq("carla@lab.org"), anyString(), anyString());
    }

    @Test void researcherFollowerIsSkippedWhenPostDoesNotMatchInterestArea() {
        publisher();
        when(follows.groupFollowers("lab")).thenReturn(List.of());
        when(follows.researcherFollowers(4L)).thenReturn(List.of(follower(3L, "carla@lab.org", "Carla", "Deep Learning")));

        service.notifyFollowers(event("Colheita de café", "Relatório de campo"));

        verifyNoInteractions(emails);
    }

    @Test void researcherFollowerWithoutInterestAreasReceivesEveryPost() {
        publisher();
        group();
        when(follows.groupFollowers("lab")).thenReturn(List.of());
        when(follows.researcherFollowers(4L)).thenReturn(List.of(follower(3L, "carla@lab.org", "Carla")));

        service.notifyFollowers(event("Colheita de café", "Relatório de campo"));

        verify(emails).sendEmail(eq("carla@lab.org"), anyString(), anyString());
    }

    @Test void failureOnOneRecipientDoesNotStopTheOthers() {
        publisher();
        group();
        when(follows.groupFollowers("lab")).thenReturn(List.of(
                follower(1L, "ana@lab.org", "Ana"), follower(2L, "bruno@lab.org", "Bruno")));
        when(follows.researcherFollowers(4L)).thenReturn(List.of());
        doThrow(new ExternalServiceException("Falha ao enviar e-mail: Connection refused"))
                .when(emails).sendEmail(eq("ana@lab.org"), anyString(), anyString());

        service.notifyFollowers(event("Novo artigo", "Conteúdo"));

        verify(emails).sendEmail(eq("ana@lab.org"), anyString(), anyString());
        verify(emails).sendEmail(eq("bruno@lab.org"), anyString(), anyString());
    }

    @Test void messageContainsGroupTitleAndLink() {
        publisher();
        group();
        when(follows.groupFollowers("lab")).thenReturn(List.of(follower(1L, "ana@lab.org", "Ana")));
        when(follows.researcherFollowers(4L)).thenReturn(List.of());

        service.notifyFollowers(event("Novo artigo", "Conteúdo do post"));

        ArgumentCaptor<String> subject = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(emails).sendEmail(eq("ana@lab.org"), subject.capture(), body.capture());
        assertTrue(subject.getValue().contains("Laboratório"));
        assertTrue(subject.getValue().contains("Novo artigo"));
        assertTrue(body.getValue().contains("Ana"));
        assertTrue(body.getValue().contains("Novo artigo"));
        assertTrue(body.getValue().contains("http://localhost:3000/groups/lab/posts/9"));
    }

    @Test void nothingIsSentWhenThereAreNoFollowers() {
        publisher();
        when(follows.groupFollowers("lab")).thenReturn(List.of());
        when(follows.researcherFollowers(4L)).thenReturn(List.of());

        service.notifyFollowers(event("Novo artigo", "Conteúdo"));

        verifyNoInteractions(emails);
    }
}
