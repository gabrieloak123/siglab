package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.NotBlank;

public record GoogleLoginRequest(@NotBlank String credential, String authorId, String[] interestAreas) {}
