package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.EmbeddingClient;
import br.ufrn.dimap.siglab.integration.ScholarClient;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.VectorRepository;
import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class InterestService {
    private final ScholarClient scholar;
    private final EmbeddingClient embedding;
    private final VectorRepository vectors;
    public InterestService(ScholarClient scholar, EmbeddingClient embedding, VectorRepository vectors) {
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
        if (values == null) throw new IllegalArgumentException("Informe três áreas de interesse.");
        String[] cleaned = Arrays.stream(values).filter(v -> v != null && !v.isBlank())
                .map(String::trim).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)).toArray(String[]::new);
        if (cleaned.length != 3) throw new IllegalArgumentException("Informe exatamente três áreas de interesse distintas.");
        return cleaned;
    }
    public void index(User user) { vectors.user(user.getId(), embedding.embed(String.join(", ", three(user.getInterest_areas())))); }
    public void index(ResearchGroup group) { vectors.group(group.getGroup_id(), embedding.embed(String.join(", ", three(group.getResearch_fields())))); }
    public void indexPost(Long id, String[] keywords) { vectors.post(id, embedding.embed(String.join(", ", keywords))); }
}
