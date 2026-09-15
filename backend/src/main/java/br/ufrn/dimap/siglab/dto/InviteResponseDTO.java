package br.ufrn.dimap.siglab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO que contém as informações da hash de convite gerada para um grupo de pesquisa.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteResponseDTO {
    /**
     * O token/hash único do convite.
     */
    private String inviteToken;

    /**
     * O identificador do grupo de pesquisa.
     */
    private String groupId;

    /**
     * E-mail do líder que gerou a hash.
     */
    private String createdByEmail;

    /**
     * Data/hora de criação da hash.
     */
    private LocalDateTime createdAt;

    /**
     * Data/hora de expiração da hash.
     */
    private LocalDateTime expiresAt;
}
