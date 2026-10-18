package br.ufrn.dimap.siglab.dto;

import java.util.List;

public record ScholarCandidate(String authorId, String name, String affiliation,
                               String verifiedDomain, List<String> interests) {}
