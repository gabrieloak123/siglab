package br.ufrn.dimap.siglab.model;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidade de junção que representa o relacionamento entre um {@link User} e um {@link ResearchGroup}.
 * <p>
 * Contém a chave composta {@link User_ResearchGroupId} formada por email do usuário e id do grupo,
 * além do atributo adicional {@code isLeader} indicando se o usuário possui cargo de liderança no grupo.
 * </p>
 *
 */
@Entity
@Table(name = "user_research_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_ResearchGroup {

    /**
     * Chave primária composta formada por userEmail e groupId.
     */
    @EmbeddedId
    private User_ResearchGroupId id;

    /**
     * Referência para o usuário associado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_email", referencedColumnName = "email", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    /**
     * Referência para o grupo de pesquisa associado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", referencedColumnName = "group_id", insertable = false, updatable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ResearchGroup researchGroup;

    /**
     * Indica se o usuário é líder do grupo de pesquisa.
     */
    @Column(name = "is_leader", nullable = false)
    private boolean isLeader;

    /**
     * Construtor utilitário para criar a associação com a chave composta inicializada.
     *
     * @param user a entidade usuário
     * @param researchGroup a entidade grupo de pesquisa
     * @param isLeader se o usuário é líder do grupo
     */
    public User_ResearchGroup(User user, ResearchGroup researchGroup, boolean isLeader) {
        this.user = user;
        this.researchGroup = researchGroup;
        this.isLeader = isLeader;
        this.id = new User_ResearchGroupId(
                user != null ? user.getEmail() : null,
                researchGroup != null ? researchGroup.getGroup_id() : null
        );
    }
}
