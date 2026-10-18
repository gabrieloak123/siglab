package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.GroupMemberDTO;
import br.ufrn.dimap.siglab.dto.InviteResponseDTO;
import br.ufrn.dimap.siglab.dto.JoinGroupRequestDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.service.ResearchGroupService;
import br.ufrn.dimap.siglab.service.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST responsável pelos endpoints de gerenciamento de Grupos de Pesquisa,
 * convites por hash, ingresso de membros e atribuição de liderança.
 *
 */
@RestController
@RequestMapping("/api/research-groups")
@CrossOrigin(origins = "*")
public class ResearchGroupController {

    private final ResearchGroupService researchGroupService;
    private final SessionUser sessionUser;

    public ResearchGroupController(ResearchGroupService researchGroupService, SessionUser sessionUser) {
        this.researchGroupService = researchGroupService;
        this.sessionUser = sessionUser;
    }

    @GetMapping
    public ResponseEntity<List<ResearchGroup>> getAllResearchGroups() {
        List<ResearchGroup> groups = researchGroupService.findAll();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResearchGroup> getResearchGroupById(@PathVariable("id") String groupId) {
        return researchGroupService.findById(groupId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId));
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<GroupMemberDTO>> getGroupUsers(@PathVariable("id") String groupId) {
        List<GroupMemberDTO> members = researchGroupService.findMembersByGroupId(groupId);
        return ResponseEntity.ok(members);
    }

    /**
     * POST /api/research-groups/{groupId}/invites - Gera um token/hash de convite para um grupo de pesquisa (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param requesterEmailHeader o e-mail do líder solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @param expirationDays quantidade de dias até a expiração da hash (padrão 7)
     * @return {@link ResponseEntity} com os dados do convite gerado (201 Created)
     */
    @PostMapping("/{groupId}/invites")
    public ResponseEntity<InviteResponseDTO> createInviteToken(
            @PathVariable("groupId") String groupId,
            HttpServletRequest servletRequest,
            @RequestParam(value = "expirationDays", defaultValue = "7") Integer expirationDays) {
        String requesterEmail = sessionUser.require(servletRequest).getEmail();
        InviteResponseDTO invite = researchGroupService.generateInviteToken(groupId, requesterEmail, expirationDays);
        return ResponseEntity.status(HttpStatus.CREATED).body(invite);
    }

    /**
     * POST /api/research-groups/join - Permite que um usuário ingresse em um grupo utilizando uma hash de convite válida.
     *
     * @param request DTO {@link JoinGroupRequestDTO} contendo o token de convite e o e-mail do usuário
     * @return {@link ResponseEntity} com os dados do usuário ingressado (200 OK)
     */
    @PostMapping("/join")
    public ResponseEntity<GroupMemberDTO> joinGroupWithToken(@Valid @RequestBody JoinGroupRequestDTO request, HttpServletRequest servletRequest) {
        String email = sessionUser.require(servletRequest).getEmail();
        if (!email.equalsIgnoreCase(request.getUserEmail())) throw new SecurityException("Convite deve ser usado pela conta autenticada.");
        GroupMemberDTO member = researchGroupService.joinGroupWithToken(request.getInviteToken(), email);
        return ResponseEntity.ok(member);
    }

    /**
     * PUT /api/research-groups/{groupId}/members/{userEmail}/leader - Promove ou altera o status de liderança de um membro no grupo.
     *
     * @param groupId o ID do grupo de pesquisa
     * @param userEmail o e-mail do usuário
     * @param isLeader {@code true} para tornar líder, {@code false} caso contrário
     * @return {@link ResponseEntity} com os dados do membro atualizado (200 OK)
     */
    @PutMapping("/{groupId}/members/{userEmail:.+}/leader")
    public ResponseEntity<GroupMemberDTO> setGroupLeader(
            @PathVariable("groupId") String groupId,
            @PathVariable("userEmail") String userEmail,
            HttpServletRequest servletRequest,
            @RequestParam(defaultValue = "true") boolean isLeader) {
        researchGroupService.requireLeader(groupId, sessionUser.require(servletRequest).getEmail());
        GroupMemberDTO member = researchGroupService.setMemberLeadership(groupId, userEmail, isLeader);
        return ResponseEntity.ok(member);
    }

    /**
     * DELETE /api/research-groups/{groupId}/members/{userEmail} - Remove um membro do grupo de pesquisa (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param userEmail o e-mail do usuário a ser removido
     * @param requesterEmailHeader o e-mail do solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @return {@link ResponseEntity} sem conteúdo (204 No Content)
     */
    @DeleteMapping("/{groupId}/members/{userEmail:.+}")
    public ResponseEntity<Void> removeMemberFromGroup(
            @PathVariable("groupId") String groupId,
            @PathVariable("userEmail") String userEmail,
            HttpServletRequest servletRequest) {
        String requesterEmail = sessionUser.require(servletRequest).getEmail();
        researchGroupService.removeMemberFromGroup(groupId, userEmail, requesterEmail);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<ResearchGroup> createResearchGroup(@Valid @RequestBody ResearchGroup researchGroup, HttpServletRequest servletRequest) {
        ResearchGroup createdGroup = researchGroupService.createForUser(researchGroup, sessionUser.require(servletRequest).getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResearchGroup> updateResearchGroup(@PathVariable("id") String groupId, @Valid @RequestBody ResearchGroup groupDetails, HttpServletRequest servletRequest) {
        researchGroupService.requireLeader(groupId, sessionUser.require(servletRequest).getEmail());
        ResearchGroup updatedGroup = researchGroupService.update(groupId, groupDetails);
        return ResponseEntity.ok(updatedGroup);
    }

    /**
     * DELETE /api/research-groups/{id} - Remove um grupo de pesquisa (Apenas Líder, exige confirmação com senha do orientador/líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param requesterEmailHeader o e-mail do líder solicitante no Header {@code X-Requester-Email} (opcional se autenticado)
     * @param leaderPassword a senha do líder para confirmação de exclusão
     * @return {@link ResponseEntity} sem conteúdo (204 No Content)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResearchGroup(
            @PathVariable("id") String groupId,
            HttpServletRequest servletRequest,
            @RequestParam(value = "leaderPassword", required = false) String leaderPassword) {
        String requesterEmail = sessionUser.require(servletRequest).getEmail();
        researchGroupService.deleteById(groupId, requesterEmail, leaderPassword);
        return ResponseEntity.noContent().build();
    }
}
