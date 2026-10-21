package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConferenceClientTest {

    @Test void parsesEnvelopeIntoVenues() {
        Map<String, Object> response = Map.of(
                "code", 200,
                "message", "Success",
                "data", Map.of("conferences", List.of(
                        Map.of("id", 433, "short_name", "GECCO",
                                "full_name", "Genetic and Evolutionary Computation Conference",
                                "conference_date", "2027-07-12", "location", "Krakow, Poland",
                                "ccf_rank", "C", "detail_page", "https://www.myhuiban.com/conference/433",
                                "cp_index", Map.of("score", 87.89)),
                        Map.of("id", 5661, "short_name", "EUSFLAT",
                                "full_name", "Conference of the European Society for Fuzzy Logic",
                                "conference_date", "2027-09-13", "ccf_rank", "",
                                "detail_page", "https://www.myhuiban.com/conference/5661"))));

        List<ConferenceClient.Venue> venues = ConferenceClient.parse(response);

        assertEquals(2, venues.size());
        ConferenceClient.Venue first = venues.getFirst();
        assertEquals(433L, first.id());
        assertEquals("GECCO", first.shortName());
        assertEquals(LocalDate.of(2027, 7, 12), first.conferenceDate());
        assertEquals("Krakow, Poland", first.location());
        assertEquals("C", first.ccfRank());
        assertEquals(87.89, first.cpIndexScore());
        assertEquals("https://www.myhuiban.com/conference/433", first.detailPage());
        assertNull(venues.get(1).cpIndexScore());
        assertEquals("", venues.get(1).ccfRank());
    }

    @Test void toleratesMissingOptionalFields() {
        Map<String, Object> response = Map.of(
                "data", Map.of("conferences", List.of(Map.of(
                        "id", 1, "short_name", "X", "full_name", "X Conf", "conference_date", "2027-01-01"))));

        ConferenceClient.Venue venue = ConferenceClient.parse(response).getFirst();

        assertEquals(1L, venue.id());
        assertNull(venue.submissionDate());
        assertNull(venue.location());
        assertNull(venue.cpIndexScore());
    }

    @Test void rejectsUnexpectedEnvelope() {
        assertThrows(ExternalServiceException.class, () -> ConferenceClient.parse(null));
        assertThrows(ExternalServiceException.class, () -> ConferenceClient.parse(Map.of("code", 400)));
        assertThrows(ExternalServiceException.class,
                () -> ConferenceClient.parse(Map.of("data", Map.of("unexpected", List.of()))));
    }

    @Test void convertsNullAndBlankDatesToNull() {
        Map<String, Object> response = Map.of(
                "data", Map.of("conferences", List.of(Map.of(
                        "id", 2, "short_name", "Y", "full_name", "Y Conf",
                        "conference_date", "2027-05-05", "submission_date", ""))));

        ConferenceClient.Venue venue = ConferenceClient.parse(response).getFirst();

        assertEquals(LocalDate.of(2027, 5, 5), venue.conferenceDate());
        assertNull(venue.submissionDate());
    }

    @Test void readsPaginationForSearchPages() {
        Map<String, Object> response = Map.of(
                "data", Map.of(
                        "conferences", List.of(Map.of(
                                "id", 1, "short_name", "X", "full_name", "X Conf",
                                "conference_date", "2027-01-01")),
                        "pagination", Map.of("total", 171, "page", 1, "per_page", 100, "total_pages", 2)));

        ConferenceClient.SearchPage page = ConferenceClient.parsePage(response);

        assertEquals(2, page.totalPages());
        assertEquals(1, page.venues().size());
    }

    @Test void defaultsToSinglePageWithoutPagination() {
        Map<String, Object> response = Map.of("data", Map.of("conferences", List.of()));

        assertEquals(1, ConferenceClient.parsePage(response).totalPages());
        assertTrue(ConferenceClient.parsePage(response).venues().isEmpty());
    }
}
