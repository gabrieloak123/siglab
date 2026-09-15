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

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(accessMode = Schema.AccessMode.READ_ONLY, description = "ID auto-incremental gerado pelo banco de dados")
    private Long id;

    @NotBlank
    @Email
    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(name = "password", nullable = false)
    private String password;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "interest_areas")
    private String[] interest_areas;

    @Column(name = "groups")
    private String[] groups;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    private List<User_ResearchGroup> userResearchGroups;

    @JsonIgnore
    public String[] getInterestAreas() {
        return interest_areas;
    }

    public void setInterestAreas(String[] interestAreas) {
        this.interest_areas = interestAreas;
    }

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
