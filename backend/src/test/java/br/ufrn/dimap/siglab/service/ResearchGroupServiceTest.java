package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import br.ufrn.dimap.siglab.model.User_ResearchGroupId;
import br.ufrn.dimap.siglab.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResearchGroupServiceTest {
    @Mock ResearchGroupRepository groups;
    @Mock User_ResearchGroupRepository members;
    @Mock UserRepository users;
    @Mock GroupInviteRepository invites;
    @Mock PasswordEncoder encoder;
    @Mock InterestService interests;
    @Mock IndexingQueue indexing;

    @Test void flushesGroupBeforeWritingVectorForeignKey() {
        String[] fields={"IA","Robótica","Visão"};
        ResearchGroup group=ResearchGroup.builder().group_id("lab").name("Lab").research_fields(fields).build();
        when(interests.three(fields)).thenReturn(fields);
        when(groups.saveAndFlush(group)).thenReturn(group);
        new ResearchGroupService(groups,members,users,invites,encoder,interests,indexing).save(group);
        InOrder order=inOrder(groups,indexing);
        order.verify(groups).saveAndFlush(group);
        order.verify(indexing).enqueue("GROUP", "lab");
    }

    @Test void refusesToDemoteTheLastLeader() {
        var group = ResearchGroup.builder().group_id("lab").build();
        var user = new User();
        var membership = new User_ResearchGroup();
        membership.setLeader(true);
        when(groups.findForUpdate("lab")).thenReturn(Optional.of(group));
        when(users.findByEmail("leader@example.com")).thenReturn(Optional.of(user));
        when(members.findById(new User_ResearchGroupId("leader@example.com", "lab"))).thenReturn(Optional.of(membership));
        when(members.findByGroupId("lab")).thenReturn(List.of(membership));
        var service = new ResearchGroupService(groups,members,users,invites,encoder,interests,indexing);
        assertThrows(IllegalStateException.class, () -> service.setMemberLeadership("lab", "leader@example.com", false));
        verify(members, never()).save(any());
    }
}
