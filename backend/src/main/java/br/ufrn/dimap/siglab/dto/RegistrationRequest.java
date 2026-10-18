package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegistrationRequest(@NotBlank String name, @Email @NotBlank String email,
                                  @NotBlank String password, String authorId,
                                  String[] interestAreas) {}
