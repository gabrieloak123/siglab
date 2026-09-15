package br.ufrn.dimap.siglab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO que representa as informações de um membro integrante de um grupo de pesquisa,
 * incluindo o indicador de liderança.
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupMemberDTO {
    /**
     * Identificador do usuário.
     */
    private Long id;

    /**
     * E-mail do usuário.
     */
    private String email;

    /**
     * Nome do usuário.
     */
    private String name;

    /**
     * Áreas de interesse do usuário.
     */
    private String[] interest_areas;

    /**
     * Indica se o usuário é líder do grupo de pesquisa.
     */
    private boolean isLeader;
}
