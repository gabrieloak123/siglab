package br.ufrn.dimap.siglab.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User_ResearchGroupId implements Serializable {

    @Column(name = "user_email")
    private String userEmail;

    @Column(name = "group_id")
    private String groupId;
}
