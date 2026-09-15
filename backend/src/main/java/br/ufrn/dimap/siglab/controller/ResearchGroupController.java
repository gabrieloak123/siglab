package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.GroupMemberDTO;
import br.ufrn.dimap.siglab.dto.InviteResponseDTO;
import br.ufrn.dimap.siglab.dto.JoinGroupRequestDTO;
import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.service.ResearchGroupService;
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
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/users")
    public ResponseEntity<List<GroupMemberDTO>> getGroupUsers(@PathVariable("id") String groupId) {
        try {
            List<GroupMemberDTO> members = researchGroupService.findMembersByGroupId(groupId);
            return ResponseEntity.ok(members);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * POST /api/research-groups/{groupId}/invites - Gera um token/hash de convite para um grupo de pesquisa (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param requesterEmail o e-mail do líder solicitante enviado no Header {@code X-Requester-Email}
     * @param expirationDays quantidade de dias até a expiração da hash (padrão 7)
     * @return {@link ResponseEntity} com os dados do convite gerado (201 Created) ou 403 Forbidden
     */
    @PostMapping("/{groupId}/invites")
    public ResponseEntity<?> createInviteToken(
            @PathVariable("groupId") String groupId,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmail,
            @RequestParam(value = "expirationDays", defaultValue = "7") Integer expirationDays) {
        try {
            InviteResponseDTO invite = researchGroupService.generateInviteToken(groupId, requesterEmail, expirationDays);
            return ResponseEntity.status(HttpStatus.CREATED).body(invite);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * POST /api/research-groups/join - Permite que um usuário ingresse em um grupo utilizando uma hash de convite válida.
     *
     * @param request DTO {@link JoinGroupRequestDTO} contendo o token de convite e o e-mail do usuário
     * @return {@link ResponseEntity} com os dados do usuário ingressado (200 OK) ou 400 Bad Request
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinGroupWithToken(@Valid @RequestBody JoinGroupRequestDTO request) {
        try {
            GroupMemberDTO member = researchGroupService.joinGroupWithToken(request.getInviteToken(), request.getUserEmail());
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
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
    public ResponseEntity<?> setGroupLeader(
            @PathVariable("groupId") String groupId,
            @PathVariable("userEmail") String userEmail,
            @RequestParam(defaultValue = "true") boolean isLeader) {
        try {
            GroupMemberDTO member = researchGroupService.setMemberLeadership(groupId, userEmail, isLeader);
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    /**
     * DELETE /api/research-groups/{groupId}/members/{userEmail} - Remove um membro do grupo de pesquisa (Apenas Líder).
     *
     * @param groupId o ID do grupo de pesquisa
     * @param userEmail o e-mail do usuário a ser removido
     * @param requesterEmail o e-mail do solicitante enviado no Header {@code X-Requester-Email}
     * @return {@link ResponseEntity} sem conteúdo (204 No Content), 403 Forbidden ou 404 Not Found
     */
    @DeleteMapping("/{groupId}/members/{userEmail:.+}")
    public ResponseEntity<?> removeMemberFromGroup(
            @PathVariable("groupId") String groupId,
            @PathVariable("userEmail") String userEmail,
            @RequestHeader(value = "X-Requester-Email", required = false) String requesterEmail) {
        try {
            researchGroupService.removeMemberFromGroup(groupId, userEmail, requesterEmail);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<ResearchGroup> createResearchGroup(@Valid @RequestBody ResearchGroup researchGroup) {
        ResearchGroup createdGroup = researchGroupService.save(researchGroup);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdGroup);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ResearchGroup> updateResearchGroup(@PathVariable("id") String groupId, @Valid @RequestBody ResearchGroup groupDetails) {
        try {
            ResearchGroup updatedGroup = researchGroupService.update(groupId, groupDetails);
            return ResponseEntity.ok(updatedGroup);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResearchGroup(@PathVariable("id") String groupId) {
        try {
            researchGroupService.deleteById(groupId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
