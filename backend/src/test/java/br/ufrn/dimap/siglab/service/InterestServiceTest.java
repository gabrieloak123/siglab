package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.EmbeddingClient;
import br.ufrn.dimap.siglab.integration.ScholarClient;
import br.ufrn.dimap.siglab.exception.InvalidInterestAreasException;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.repository.VectorRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class InterestServiceTest {
    @Mock ScholarClient scholar;
    @Mock EmbeddingClient embeddings;
    @Mock VectorRepository vectors;
    InterestService service;
    @BeforeEach void setup() { service = new InterestService(scholar, embeddings, vectors); }

    @Test void requiresThreeDistinctNonEmptyAreas() {
        assertThrows(InvalidInterestAreasException.class, () -> service.three(new String[]{"IA", " IA ", "Robótica"}));
        assertThrows(InvalidInterestAreasException.class, () -> service.three(new String[]{"IA", "", "Robótica"}));
        assertArrayEquals(new String[]{"IA", "Robótica", "Visão"}, service.three(new String[]{" IA ", "Robótica", "Visão"}));
    }

    @Test void indexesJoinedAreasWithCloudflareVector() {
        float[] vector = new float[1024];
        User user = User.builder().id(7L).interest_areas(new String[]{"IA", "Robótica", "Visão"}).build();
        when(embeddings.embed("IA, Robótica, Visão")).thenReturn(vector);
        service.index(user);
        verify(vectors).user(7L, vector);
    }

    @Test void combinesScholarAreasWithMissingManualArea() {
        when(scholar.interests("author")).thenReturn(List.of("IA", "Robótica"));
        assertArrayEquals(new String[]{"IA", "Robótica", "Visão"},
                service.withScholar("author", new String[]{"Visão"}));
        assertThrows(InvalidInterestAreasException.class, () -> service.withScholar("author", new String[0]));
    }

    @Test void indexesArticleUsingKeywordsAndAbstract() {
        float[] vector = new float[1024];
        Post post = Post.builder().id(9L).keywords(new String[]{"IA", "Saúde"})
                .description("Diagnóstico por imagens médicas").content("Texto completo").build();
        when(embeddings.embed("IA, Saúde\nDiagnóstico por imagens médicas")).thenReturn(vector);
        service.indexPost(post);
        verify(vectors).post(9L, vector);
    }
}
