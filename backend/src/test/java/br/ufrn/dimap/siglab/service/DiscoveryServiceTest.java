package br.ufrn.dimap.siglab.service;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DiscoveryServiceTest {
    private final NamedParameterJdbcTemplate jdbc = mock(NamedParameterJdbcTemplate.class);
    private final DiscoveryService service = new DiscoveryService(jdbc);

    @Test void rejectsDuplicateAndEmptySelectionsWithoutWriting() {
        assertThrows(IllegalArgumentException.class, () -> service.save(1L, List.of(5L, 5L)));
        assertThrows(IllegalArgumentException.class, () -> service.save(1L, List.of()));
        verifyNoInteractions(jdbc);
    }

    @Test void rejectsSelectionWhenAnyPostHasNoValidVector() {
        when(jdbc.update(anyString(), any(MapSqlParameterSource.class))).thenReturn(0);
        assertThrows(IllegalArgumentException.class, () -> service.save(1L, List.of(4L)));
    }

    @Test void savesTheNormalizedMeanWhenAllPostsAreIndexed() {
        when(jdbc.update(contains("l2_normalize(avg(l2_normalize(embedding)))"), any(MapSqlParameterSource.class))).thenReturn(1);
        assertDoesNotThrow(() -> service.save(1L, List.of(4L, 5L)));
    }
}
