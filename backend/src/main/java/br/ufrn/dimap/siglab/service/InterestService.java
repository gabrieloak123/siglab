package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.EmbeddingProvider;
import br.ufrn.dimap.siglab.integration.ResearchProfileProvider;
import br.ufrn.dimap.siglab.exception.InvalidInterestAreasException;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.Post;
import br.ufrn.dimap.siglab.repository.VectorRepository;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class InterestService {
    private final ResearchProfileProvider scholar;
    private final EmbeddingProvider embedding;
    private final VectorRepository vectors;
    public InterestService(ResearchProfileProvider scholar, EmbeddingProvider embedding, VectorRepository vectors) {
        this.scholar = scholar; this.embedding = embedding; this.vectors = vectors;
    }
    public List<String> scholarInterests(String authorId) {
        if (authorId == null || authorId.isBlank()) throw new IllegalArgumentException("author_id é obrigatório.");
        return scholar.interests(authorId.trim());
    }
    public String[] withScholar(String authorId, String[] additional) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        scholarInterests(authorId).stream().filter(s -> s != null && !s.isBlank())
                .map(String::trim).limit(3).forEach(merged::add);
        if (additional != null) for (String area : additional)
            if (area != null && !area.isBlank() && merged.size() < 3) merged.add(area.trim());
        return three(merged.toArray(String[]::new));
    }
    public String[] three(String[] values) {
        if (values == null) throw new InvalidInterestAreasException("Informe três áreas de interesse.");
        String[] cleaned = Arrays.stream(values).filter(v -> v != null && !v.isBlank())
                .map(String::trim).collect(java.util.stream.Collectors.toMap(v -> v.toLowerCase(java.util.Locale.ROOT), v -> v,
                        (a,b) -> a, java.util.LinkedHashMap::new)).values().toArray(String[]::new);
        if (cleaned.length != 3) throw new InvalidInterestAreasException("Informe exatamente três áreas de interesse distintas.");
        if (Arrays.stream(cleaned).anyMatch(value -> value.length() > 255))
            throw new InvalidInterestAreasException("Cada área deve ter no máximo 255 caracteres.");
        return cleaned;
    }
    public void index(User user) { vectors.user(user.getId(), embedding.embed(String.join(", ", three(user.getInterest_areas())))); }
    public void index(ResearchGroup group) { vectors.group(group.getGroup_id(), embedding.embed(String.join(", ", three(group.getResearch_fields())))); }
    public void indexPost(Post post) {
        if (post == null || post.getId() == null) throw new IllegalArgumentException("Post salvo é obrigatório para indexação.");
        String terms = post.getKeywords() == null ? "" : String.join(", ", post.getKeywords());
        String abstractText = post.getDescription();
        if (abstractText == null || abstractText.isBlank()) abstractText = post.getContent();
        String semanticText = (terms + "\n" + (abstractText == null ? "" : abstractText.trim())).trim();
        if (semanticText.isBlank()) throw new IllegalArgumentException("Post precisa de palavras-chave ou resumo para indexação.");
        vectors.post(post.getId(), embedding.embed(semanticText));
    }
}
