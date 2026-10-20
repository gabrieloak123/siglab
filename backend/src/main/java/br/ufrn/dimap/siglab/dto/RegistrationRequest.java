package br.ufrn.dimap.siglab.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(@NotBlank @Size(max=255) String name, @Email @NotBlank @Size(max=255) String email,
                                  @NotBlank @Size(max=72) String password, @Size(max=255) String authorId,
                                  String[] interestAreas) {}
