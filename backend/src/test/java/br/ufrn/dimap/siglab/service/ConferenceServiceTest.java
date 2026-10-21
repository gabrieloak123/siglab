package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.ConferenceSuggestionDTO;
import br.ufrn.dimap.siglab.exception.InvalidConferenceSearchException;
import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import br.ufrn.dimap.siglab.integration.ConferenceClient;
import br.ufrn.dimap.siglab.integration.ConferenceClient.Venue;
import br.ufrn.dimap.siglab.integration.EmbeddingClient;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.ConferenceRepository;
import br.ufrn.dimap.siglab.repository.ConferenceRepository.StoredVenue;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConferenceServiceTest {

    @Mock UserRepository users;
    @Mock ConferenceClient client;
    @Mock ConferenceRepository conferences;
    @Mock EmbeddingClient embeddings;
    ConferenceService service;

    @BeforeEach void setup() { service = new ConferenceService(users, client, conferences, embeddings); }

    @Test void mapsPortugueseAndEnglishAreasToApiFields() {
        assertEquals(List.of("ai", "vision", "security"),
                ConferenceService.keys(new String[]{"Inteligência Artificial", "Visão Computacional", "Segurança"}));
        assertEquals(List.of("ai"), ConferenceService.keys(new String[]{"Redes Neurais", "Machine Learning", "AI"}));
        assertEquals(List.of("se"), ConferenceService.keys(new String[]{"Engenharia de Software"}));
        assertEquals(List.of("systems"), ConferenceService.keys(new String[]{"Sistemas Distribuídos"}));
        assertEquals(List.of("data"), ConferenceService.keys(new String[]{"Estatística"}));
        assertEquals(List.of("robotics"), ConferenceService.keys(new String[]{"Robótica"}));
        assertEquals(List.of("network"), ConferenceService.keys(new String[]{"Redes de Computador"}));
        assertEquals(List.of("nlp"), ConferenceService.keys(new String[]{"Processamento de Linguagem Natural"}));
    }

    @Test void keepsAmbiguousAreasInTheRightField() {
        // "redes neurais" precisa cair em ai, não em network (avaliado primeiro)
        assertEquals(List.of("ai", "network"),
                ConferenceService.keys(new String[]{"Redes Neurais", "Redes de Computador"}));
    }

    @Test void fallsBackToQuerySearchForUnmappedAreas() {
        assertEquals(List.of("q:futebol", "q:formula 1", "q:democracia"),
                ConferenceService.keys(new String[]{"Futebol", "Formula 1", "Democracia"}));
    }

    @Test void skipsBlankAreasAndDeduplicates() {
        assertEquals(List.of("security"),
                ConferenceService.keys(new String[]{"Segurança", " ", "Security da Informação"}));
        assertEquals(List.of(), ConferenceService.keys(null));
    }

    @Test void fetchesAndStoresWhenCacheIsMissing() {
        User user = User.builder().id(7L).interest_areas(new String[]{"Redes Neurais", "Segurança", "Dados"}).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(conferences.refreshedAt(any())).thenReturn(Optional.empty());
        when(client.search(eq("ai"), isNull(), any())).thenReturn(
                List.of(new Venue(1, "ICML", "International Conference on Machine Learning",
                        LocalDate.of(2027, 3, 1), LocalDate.of(2027, 7, 1), "Vienna, Austria",
                        "A", "https://www.myhuiban.com/conference/1", 90.5)));
        when(embeddings.embedBatch(any())).thenReturn(List.of(new float[]{0.1f}));
        when(conferences.ranked(eq(7L), any(), eq(6), anyBoolean())).thenReturn(List.of());

        service.recommend(7L);

        verify(client).search("ai", null, LocalDate.now());
        ArgumentCaptor<List<StoredVenue>> captor = ArgumentCaptor.forClass(List.class);
        verify(conferences).replaceSnapshot(eq("ai"), captor.capture());
        StoredVenue stored = captor.getValue().getFirst();
        assertEquals(1L, stored.id());
        assertEquals("ai", stored.field());
        assertEquals("International Conference on Machine Learning", stored.fullName());
        assertNotNull(stored.embedding());
    }

    @Test void skipsApiCallWhenCacheIsFresh() {
        User user = User.builder().id(7L).interest_areas(new String[]{"Robótica", "Visão", "Bioinformática"}).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(conferences.refreshedAt(any())).thenReturn(Optional.of(LocalDateTime.now()));
        when(conferences.hasUserVector(7L)).thenReturn(true);
        when(conferences.ranked(eq(7L), any(), eq(6), eq(true))).thenReturn(List.of());

        service.recommend(7L);

        verify(client, never()).search(any(), any(), any());
        verify(conferences).ranked(7L, List.of("robotics", "vision", "bio"), 6, true);
    }

    @Test void usesDateOrderingWhenUserHasNoVector() {
        User user = User.builder().id(7L).interest_areas(new String[]{"Energia", "Materiais", "Civil"}).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(conferences.refreshedAt(any())).thenReturn(Optional.of(LocalDateTime.now()));
        when(conferences.hasUserVector(7L)).thenReturn(false);
        when(conferences.ranked(eq(7L), any(), eq(6), eq(false))).thenReturn(List.of());

        service.recommend(7L);

        verify(conferences).ranked(7L, List.of("energy", "materials", "civil"), 6, false);
    }

    @Test void returnsEmptyWhenProfileHasNoInterestAreas() {
        when(users.findById(7L)).thenReturn(Optional.of(User.builder().id(7L).build()));

        assertTrue(service.recommend(7L).isEmpty());

        verifyNoInteractions(client, conferences, embeddings);
    }

    @Test void propagatesApiFailureAsExternalServiceError() {
        User user = User.builder().id(7L).interest_areas(new String[]{"Futebol", "Democracia", "Formula 1"}).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(conferences.refreshedAt("q:futebol")).thenReturn(Optional.empty());
        when(client.search(isNull(), eq("futebol"), any()))
                .thenThrow(new ExternalServiceException("Cota diária da Conference Partner API esgotada; tente mais tarde."));

        ExternalServiceException ex = assertThrows(ExternalServiceException.class,
                () -> service.recommend(7L));

        assertTrue(ex.getMessage().contains("Cota diária"));
        verify(conferences, never()).replaceSnapshot(any(), any());
    }

    @Test void keepsOrderReturnedBySimilarityRanking() {
        var first = ConferenceSuggestionDTO.builder().id(1L).build();
        var second = ConferenceSuggestionDTO.builder().id(2L).build();
        User user = User.builder().id(7L).interest_areas(new String[]{"IA", "Web", "Educação"}).build();
        when(users.findById(7L)).thenReturn(Optional.of(user));
        when(conferences.refreshedAt(any())).thenReturn(Optional.of(LocalDateTime.now()));
        when(conferences.hasUserVector(7L)).thenReturn(true);
        when(conferences.ranked(eq(7L), any(), eq(6), eq(true))).thenReturn(List.of(first, second));

        List<ConferenceSuggestionDTO> result = service.recommend(7L);

        assertSame(first, result.get(0));
        assertSame(second, result.get(1));
    }

    @Test void searchRejectsInvalidFilters() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);

        assertThrows(InvalidConferenceSearchException.class, () -> service.search("nope", start, end, null));
        assertThrows(InvalidConferenceSearchException.class, () -> service.search("ai", null, end, null));
        assertThrows(InvalidConferenceSearchException.class, () -> service.search("ai", start, null, null));
        assertThrows(InvalidConferenceSearchException.class, () -> service.search("ai", end, start, null));
        assertThrows(InvalidConferenceSearchException.class,
                () -> service.search("ai", start, start.plusYears(6), null));
        assertThrows(InvalidConferenceSearchException.class, () -> service.search("ai", start, end, "Atlantis"));

        verifyNoInteractions(client);
    }

    @Test void searchFiltersByCountryAndSortsByDate() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(client.searchPage("ai", null, start, end, 1)).thenReturn(new ConferenceClient.SearchPage(List.of(
                venue(1, "GECCO", "2027-07-12", "Krakow, Poland"),
                venue(2, "AAAI", "2027-02-16", "Montreal, Quebec, Canada"),
                venue(3, "PPAM", "2027-06-01", "Warsaw, Poland")), 1));

        List<ConferenceSuggestionDTO> result = service.search("ai", start, end, "Poland");

        assertEquals(List.of(3L, 1L), result.stream().map(ConferenceSuggestionDTO::getId).toList());
        assertEquals("Warsaw, Poland", result.getFirst().getLocation());
        assertEquals("GECCO", result.get(1).getShortName());
    }

    @Test void searchWithoutCountryReturnsEveryResultSortedByDate() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(client.searchPage("ai", null, start, end, 1)).thenReturn(new ConferenceClient.SearchPage(List.of(
                venue(1, "GECCO", "2027-07-12", "Krakow, Poland"),
                venue(2, "AAAI", "2027-02-16", "Montreal, Quebec, Canada")), 1));

        List<ConferenceSuggestionDTO> result = service.search("ai", start, end, null);

        assertEquals(List.of(2L, 1L), result.stream().map(ConferenceSuggestionDTO::getId).toList());
    }

    @Test void searchPaginatesUntilTheLastPageAndDeduplicates() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(client.searchPage("security", null, start, end, 1)).thenReturn(new ConferenceClient.SearchPage(List.of(
                venue(1, "S1", "2027-03-01", "Paris, France"),
                venue(2, "S2", "2027-04-01", "Tokyo, Japan")), 2));
        when(client.searchPage("security", null, start, end, 2)).thenReturn(new ConferenceClient.SearchPage(List.of(
                venue(2, "S2", "2027-04-01", "Tokyo, Japan"),
                venue(3, "S3", "2027-05-01", "Busan, South Korea")), 2));

        List<ConferenceSuggestionDTO> result = service.search("security", start, end, null);

        assertEquals(List.of(1L, 2L, 3L), result.stream().map(ConferenceSuggestionDTO::getId).toList());
        verify(client).searchPage("security", null, start, end, 1);
        verify(client).searchPage("security", null, start, end, 2);
    }

    @Test void searchCachesRepeatedQueries() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(client.searchPage("ai", null, start, end, 1)).thenReturn(new ConferenceClient.SearchPage(List.of(
                venue(1, "GECCO", "2027-07-12", "Krakow, Poland")), 1));

        service.search("ai", start, end, "Poland");
        List<ConferenceSuggestionDTO> second = service.search("ai", start, end, "Poland");

        assertEquals(1, second.size());
        verify(client, times(1)).searchPage("ai", null, start, end, 1);
    }

    @Test void searchStopsAtThePageCap() {
        LocalDate start = LocalDate.of(2027, 1, 1);
        LocalDate end = LocalDate.of(2027, 12, 31);
        when(client.searchPage(eq("ai"), isNull(), eq(start), eq(end), anyInt()))
                .thenReturn(new ConferenceClient.SearchPage(List.of(
                        venue(1, "GECCO", "2027-07-12", "Krakow, Poland")), 99));

        service.search("ai", start, end, null);

        verify(client, times(ConferenceService.SEARCH_MAX_PAGES))
                .searchPage(eq("ai"), isNull(), eq(start), eq(end), anyInt());
    }

    private static Venue venue(long id, String shortName, String conferenceDate, String location) {
        return new Venue(id, shortName, shortName + " Conference", null, LocalDate.parse(conferenceDate),
                location, "A", "https://www.myhuiban.com/conference/" + id, 80.0);
    }
}
