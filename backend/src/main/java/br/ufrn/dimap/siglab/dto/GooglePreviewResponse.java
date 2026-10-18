package br.ufrn.dimap.siglab.dto;

import java.util.List;

public record GooglePreviewResponse(boolean existingUser, String name, String email,
                                    String institutionalDomain, List<ScholarCandidate> candidates) {}
