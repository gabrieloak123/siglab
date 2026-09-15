package br.ufrn.dimap.siglab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * Chave primária composta para a entidade de junção {@link User_ResearchGroup}.
 * <p>
 * Une as chaves {@code userEmail} da entidade {@link User} e {@code groupId}
 * da entidade {@link ResearchGroup}.
 * </p>
 *
 */
@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_ResearchGroupId implements Serializable {

    /**
     * E-mail do usuário participante do grupo de pesquisa.
     */
    @Column(name = "user_email")
    private String userEmail;

    /**
     * Identificador do grupo de pesquisa ao qual o usuário pertence.
     */
    @Column(name = "group_id")
    private String groupId;
}
