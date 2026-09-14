package br.ufrn.dimap.siglab.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "research_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResearchGroup {

    @Id
    @Column(name = "group_id", nullable = false, unique = true)
    private String group_id;

    @NotBlank
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "research_fields")
    private String[] research_fields;

    @OneToMany(mappedBy = "researchGroup", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<User_ResearchGroup> groupMembers;

    @Transient
    private User[] members;

    public String getGroupId() {
        return group_id;
    }

    public void setGroupId(String groupId) {
        this.group_id = groupId;
    }

    public String[] getResearchFields() {
        return research_fields;
    }

    public void setResearchFields(String[] researchFields) {
        this.research_fields = researchFields;
    }

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

    public void setMembers(User[] members) {
        this.members = members;
    }
}
