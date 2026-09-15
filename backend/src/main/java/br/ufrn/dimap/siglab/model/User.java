package br.ufrn.dimap.siglab.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.Objects;

/**
 * Entidade que representa um Usuário no sistema SIGLab.
 * <p>
 * Armazena informações cadastrais como e-mail (único), senha, nome, áreas de interesse
 * e os identificadores dos grupos de pesquisa dos quais o usuário participa.
 * </p>
 *
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    /**
     * Identificador único auto-incremental do usuário.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "ID auto-incremental gerado pelo banco de dados")
    private Long id;

    /**
     * E-mail único do usuário para autenticação e identificação.
     */
    @NotBlank
    @Email
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    /**
     * Senha de acesso do usuário.
     */
    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;

    /**
     * Nome completo do usuário.
     */
    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Array com as áreas de interesse acadêmico/científico do usuário.
     */
    @Column(name = "interest_areas")
    private String[] interest_areas;

    /**
     * Array com os identificadores dos grupos de pesquisa aos quais o usuário pertence.
     */
    @Column(name = "groups")
    private String[] groups;

    /**
     * Lista de associações com grupos de pesquisa através da entidade de junção {@link User_ResearchGroup}.
     */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<User_ResearchGroup> userResearchGroups;

    /**
     * Getter utilitário para obter o array de áreas de interesse.
     *
     * @return array de strings com as áreas de interesse
     */
    @JsonIgnore
    public String[] getInterestAreas() {
        return interest_areas;
    }

    /**
     * Setter utilitário para definir as áreas de interesse.
     *
     * @param interestAreas array de áreas de interesse
     */
    public void setInterestAreas(String[] interestAreas) {
        this.interest_areas = interestAreas;
    }

    /**
     * Getter utilitário para obter a lista de objetos {@link ResearchGroup} nos quais o usuário participa.
     *
     * @return lista de grupos de pesquisa
     */
    @JsonIgnore
    public List<ResearchGroup> getResearchGroupsList() {
        if (userResearchGroups == null) {
            return List.of();
        }
        return userResearchGroups.stream()
                .map(User_ResearchGroup::getResearchGroup)
                .filter(Objects::nonNull)
                .toList();
    }
}
