package br.ufrn.dimap.siglab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.Objects;

/**
 * Entidade que representa um Grupo de Pesquisa no sistema SIGLab.
 * <p>
 * Armazena informações como o código identificador do grupo (group_id),
 * nome do grupo, linhas de pesquisa e seus membros associados.
 * </p>
 *
 */
@Entity
@Table(name = "research_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchGroup {

    /**
     * Identificador em texto do grupo de pesquisa (chave primária).
     */
    @Id
    @Column(name = "group_id", nullable = false, unique = true)
    private String group_id;

    /**
     * Nome do grupo de pesquisa.
     */
    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Array contendo as áreas/linhas de atuação em pesquisa do grupo.
     */
    @Column(name = "research_fields")
    private String[] research_fields;

    /**
     * Lista de relacionamentos com usuários através da entidade {@link User_ResearchGroup}.
     */
    @OneToMany(mappedBy = "researchGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<User_ResearchGroup> groupMembers;

    /**
     * Array transiente de usuários membros do grupo.
     */
    @Transient
    @JsonIgnore
    private User[] members;

    /**
     * Getter para obter o identificador do grupo de pesquisa.
     *
     * @return o identificador em texto do grupo
     */
    @JsonIgnore
    public String getGroupId() {
        return group_id;
    }

    /**
     * Setter para definir o identificador do grupo de pesquisa.
     *
     * @param groupId o identificador do grupo
     */
    public void setGroupId(String groupId) {
        this.group_id = groupId;
    }

    /**
     * Getter utilitário para obter o array de linhas de pesquisa.
     *
     * @return array de strings com as linhas de pesquisa
     */
    @JsonIgnore
    public String[] getResearchFields() {
        return research_fields;
    }

    /**
     * Setter utilitário para definir as linhas de pesquisa.
     *
     * @param researchFields array de linhas de pesquisa
     */
    public void setResearchFields(String[] researchFields) {
        this.research_fields = researchFields;
    }

    /**
     * Retorna os usuários membros pertencentes a este grupo de pesquisa.
     *
     * @return array de {@link User} membros do grupo
     */
    @JsonIgnore
    public User[] getMembers() {
        if (members != null) {
            return members;
        }
        if (groupMembers != null) {
            return groupMembers.stream()
                    .map(User_ResearchGroup::getUser)
                    .filter(Objects::nonNull)
                    .toArray(User[]::new);
        }
        return new User[0];
    }

    /**
     * Define os membros do grupo.
     *
     * @param members array de usuários membros
     */
    public void setMembers(User[] members) {
        this.members = members;
    }
}
