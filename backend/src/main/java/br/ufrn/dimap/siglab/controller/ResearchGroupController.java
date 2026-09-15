package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.GroupMemberDTO;
import br.ufrn.dimap.siglab.dto.InviteResponseDTO;
import br.ufrn.dimap.siglab.dto.JoinGroupRequestDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.service.ResearchGroupService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    public ResearchGroupController(ResearchGroupService researchGroupService) {
        this.researchGroupService = researchGroupService;
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
     * Extrai o e-mail do solicitante priorizando o contexto de segurança (SecurityContextHolder).
     * Se o usuário estiver autenticado no Spring Security, seu identificador é utilizado;
     * caso contrário, utiliza o valor do header 'X-Requester-Email' como fallback.
     *
     * @param headerEmail o e-mail informado no header da requisição (opcional)
     * @return o e-mail do solicitante identificado
     */
    private String resolveRequesterEmail(String headerEmail) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return authentication.getName();
        }
        return headerEmail;
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
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader,
            @RequestParam(value = "expirationDays", defaultValue = "7") Integer expirationDays) {
        String requesterEmail = resolveRequesterEmail(requesterEmailHeader);
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
    public ResponseEntity<GroupMemberDTO> joinGroupWithToken(@Valid @RequestBody JoinGroupRequestDTO request) {
        GroupMemberDTO member = researchGroupService.joinGroupWithToken(request.getInviteToken(), request.getUserEmail());
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
            @RequestParam(defaultValue = "true") boolean isLeader) {
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
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader) {
        String requesterEmail = resolveRequesterEmail(requesterEmailHeader);
        researchGroupService.removeMemberFromGroup(groupId, userEmail, requesterEmail);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    public ResponseEntity<ResearchGroup> createResearchGroup(@Valid @RequestBody ResearchGroup researchGroup) {
        ResearchGroup createdGroup = researchGroupService.save(researchGroup);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResearchGroup> updateResearchGroup(@PathVariable("id") String groupId, @Valid @RequestBody ResearchGroup groupDetails) {
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
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmailHeader,
            @RequestParam(value = "leaderPassword", required = false) String leaderPassword) {
        String requesterEmail = resolveRequesterEmail(requesterEmailHeader);
        researchGroupService.deleteById(groupId, requesterEmail, leaderPassword);
        return ResponseEntity.noContent().build();
    }
}
