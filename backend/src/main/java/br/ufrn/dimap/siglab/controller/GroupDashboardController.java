package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.exception.AccessDeniedException;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import br.ufrn.dimap.siglab.service.GroupDashboardDefinition;
import br.ufrn.dimap.siglab.service.SessionUser;
import br.ufrn.dimap.siglab.service.SiglabDashboardConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupDashboardController {

    private final SessionUser sessionUser;
    private final User_ResearchGroupRepository memberships;
    private final GroupDashboardDefinition<SiglabDashboardConfiguration.Context> dashboard;

    public GroupDashboardController(
            SessionUser sessionUser,
            User_ResearchGroupRepository memberships,
            GroupDashboardDefinition<SiglabDashboardConfiguration.Context> dashboard
    ) {
        this.sessionUser = sessionUser;
        this.memberships = memberships;
        this.dashboard = dashboard;
    }

    @GetMapping("/api/research-groups/{groupId}/dashboard")
    public GroupDashboardDefinition.Dashboard getDashboard(
            @PathVariable("groupId") String groupId,
            HttpServletRequest request
    ) {
        var user = sessionUser.require(request);
        if (!memberships.existsMemberByUserIdAndGroupId(user.getId(), groupId)) {
            throw new AccessDeniedException("Apenas membros podem acessar o dashboard do grupo.");
        }
        return dashboard.calculate(groupId);
    }
}
