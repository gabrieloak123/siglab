package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResearchGroupServiceTest {
    @Mock ResearchGroupRepository groups;
    @Mock User_ResearchGroupRepository members;
    @Mock UserRepository users;
    @Mock GroupInviteRepository invites;
    @Mock PasswordEncoder encoder;
    @Mock InterestService interests;

    @Test void flushesGroupBeforeWritingVectorForeignKey() {
        String[] fields={"IA","Robótica","Visão"};
        ResearchGroup group=ResearchGroup.builder().group_id("lab").name("Lab").research_fields(fields).build();
        when(interests.three(fields)).thenReturn(fields);
        when(groups.saveAndFlush(group)).thenReturn(group);
        new ResearchGroupService(groups,members,users,invites,encoder,interests).save(group);
        InOrder order=inOrder(groups,interests);
        order.verify(groups).saveAndFlush(group);
        order.verify(interests).index(group);
    }
}
