package br.ufrn.dimap.siglab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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
    private Long[] groups;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<User_ResearchGroup> userResearchGroups;

    public String[] getInterestAreas() {
        return interest_areas;
    }

    public void setInterestAreas(String[] interestAreas) {
        this.interest_areas = interestAreas;
    }

    public Long[] getGroups() {
        return groups;
    }

    public void setGroups(Long[] groups) {
        this.groups = groups;
    }
}
