package br.ufrn.dimap.siglab.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user_research_group")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_ResearchGroup {

    @EmbeddedId
    private User_ResearchGroupId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userEmail")
    @JoinColumn(name = "user_email", referencedColumnName = "email")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("groupId")
    @JoinColumn(name = "group_id", referencedColumnName = "group_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ResearchGroup researchGroup;

    @Column(name = "is_leader", nullable = false)
    private boolean isLeader;

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
