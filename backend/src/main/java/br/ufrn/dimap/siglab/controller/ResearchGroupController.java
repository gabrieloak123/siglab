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

    @PostMapping("/join")
    public ResponseEntity<?> joinGroupWithToken(@Valid @RequestBody JoinGroupRequestDTO request) {
        try {
            GroupMemberDTO member = researchGroupService.joinGroupWithToken(request.getInviteToken(), request.getUserEmail());
            return ResponseEntity.ok(member);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

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
