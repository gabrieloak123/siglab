package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.KeywordClient;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.List;
import java.util.Optional;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class IndexBackfillServiceTest {
    @Mock UserRepository users;
    @Mock ResearchGroupRepository groups;
    @Mock PostRepository posts;
    @Mock VectorRepository vectors;
    @Mock InterestService interests;
    @Mock KeywordClient keywords;

    @Test void indexesLegacyPostAndStoresGeneratedKeywords() {
        Post old = Post.builder().id(8L).title("Pesquisa").content("Texto completo").build();
        String[] terms = {"IA", "Visão", "Robótica"};
        when(vectors.missingPosts()).thenReturn(List.of(8L));
        when(posts.findById(8L)).thenReturn(Optional.of(old));
        when(keywords.keywords("Texto completo")).thenReturn(terms);
        new IndexBackfillService(users,groups,posts,vectors,interests,keywords).ensurePosts();
        verify(posts).save(old);
        verify(interests).indexPost(8L, terms);
        org.junit.jupiter.api.Assertions.assertEquals("OUTROS", old.getPostType());
    }
}
