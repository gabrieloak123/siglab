package br.ufrn.dimap.siglab.integration;

import br.ufrn.dimap.siglab.dto.ScholarCandidate;
import java.util.List;

public interface ResearchProfileProvider {
    List<String> interests(String authorId);
    ScholarCandidate profile(String authorId);
    List<ScholarCandidate> searchProfiles(String name);
}
