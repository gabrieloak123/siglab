package br.ufrn.dimap.siglab.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @Size(max = 72) String password,
        @Size(max = 255) String authorId,
        @JsonProperty("interest_areas") String[] interestAreas) {}
