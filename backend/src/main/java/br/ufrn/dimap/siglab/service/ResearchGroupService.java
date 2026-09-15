package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.GroupMemberDTO;
import br.ufrn.dimap.siglab.dto.InviteResponseDTO;
import br.ufrn.dimap.siglab.model.*;
import br.ufrn.dimap.siglab.repository.GroupInviteRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ResearchGroupService {

    private final ResearchGroupRepository researchGroupRepository;
    private final User_ResearchGroupRepository userResearchGroupRepository;
    private final UserRepository userRepository;
    private final GroupInviteRepository groupInviteRepository;

    public ResearchGroupService(ResearchGroupRepository researchGroupRepository,
                                User_ResearchGroupRepository userResearchGroupRepository,
                                UserRepository userRepository,
                                GroupInviteRepository groupInviteRepository) {
        this.researchGroupRepository = researchGroupRepository;
        this.userResearchGroupRepository = userResearchGroupRepository;
        this.userRepository = userRepository;
        this.groupInviteRepository = groupInviteRepository;
    }

    public List<ResearchGroup> findAll() {
        return researchGroupRepository.findAll();
    }

    public Optional<ResearchGroup> findById(String groupId) {
        return researchGroupRepository.findById(groupId);
    }

    public ResearchGroup save(ResearchGroup researchGroup) {
        return researchGroupRepository.save(researchGroup);
    }

    public ResearchGroup update(String groupId, ResearchGroup groupDetails) {
        return researchGroupRepository.findById(groupId)
                .map(group -> {
                    group.setName(groupDetails.getName());
                    group.setResearch_fields(groupDetails.getResearch_fields());
                    if (groupDetails.getMembers() != null) {
                        group.setMembers(groupDetails.getMembers());
                    }
                    return researchGroupRepository.save(group);
                })
                .orElseThrow(() -> new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + groupId));
    }

    public void deleteById(String groupId) {
        if (!researchGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + groupId);
        }
        researchGroupRepository.deleteById(groupId);
    }

    public List<GroupMemberDTO> findMembersByGroupId(String groupId) {
        if (!researchGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + groupId);
        }

        List<User_ResearchGroup> joinMembers = userResearchGroupRepository.findByGroupId(groupId);
        Map<String, Boolean> leaderMap = joinMembers.stream()
                .filter(m -> m.getUser() != null)
                .collect(Collectors.toMap(m -> m.getUser().getEmail(), User_ResearchGroup::isLeader, (a, b) -> a || b));

        Set<String> processedEmails = new HashSet<>();
        List<GroupMemberDTO> result = new ArrayList<>();

        for (User_ResearchGroup m : joinMembers) {
            if (m.getUser() != null) {
                processedEmails.add(m.getUser().getEmail());
                result.add(GroupMemberDTO.builder()
                        .id(m.getUser().getId())
                        .email(m.getUser().getEmail())
                        .name(m.getUser().getName())
                        .interest_areas(m.getUser().getInterest_areas())
                        .isLeader(m.isLeader())
                        .build());
            }
        }

        List<User> arrayUsers = userRepository.findByGroupIdInArray(groupId);
        for (User u : arrayUsers) {
            if (!processedEmails.contains(u.getEmail())) {
                processedEmails.add(u.getEmail());
                result.add(GroupMemberDTO.builder()
                        .id(u.getId())
                        .email(u.getEmail())
                        .name(u.getName())
                        .interest_areas(u.getInterest_areas())
                        .isLeader(leaderMap.getOrDefault(u.getEmail(), false))
                        .build());
            }
        }

        return result;
    }

    @Transactional
    public GroupMemberDTO setMemberLeadership(String groupId, String userEmail, boolean isLeader) {
        ResearchGroup researchGroup = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + groupId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o e-mail: " + userEmail));

        User_ResearchGroupId id = new User_ResearchGroupId(userEmail, groupId);
        User_ResearchGroup userResearchGroup = userResearchGroupRepository.findById(id)
                .orElseGet(() -> {
                    User_ResearchGroup newUrg = new User_ResearchGroup();
                    newUrg.setId(id);
                    return newUrg;
                });

        userResearchGroup.setUser(user);
        userResearchGroup.setResearchGroup(researchGroup);
        userResearchGroup.setLeader(isLeader);

        userResearchGroupRepository.save(userResearchGroup);

        if (user.getGroups() == null) {
            user.setGroups(new String[]{groupId});
            userRepository.save(user);
        } else if (!Arrays.asList(user.getGroups()).contains(groupId)) {
            List<String> groupList = new ArrayList<>(Arrays.asList(user.getGroups()));
            groupList.add(groupId);
            user.setGroups(groupList.toArray(new String[0]));
            userRepository.save(user);
        }

        return GroupMemberDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .interest_areas(user.getInterest_areas())
                .isLeader(isLeader)
                .build();
    }

    @Transactional
    public void removeMemberFromGroup(String groupId, String userEmail, String requesterEmail) {
        if (!researchGroupRepository.existsById(groupId)) {
            throw new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + groupId);
        }

        if (requesterEmail != null && !requesterEmail.isBlank()) {
            User_ResearchGroupId requesterId = new User_ResearchGroupId(requesterEmail, groupId);
            boolean isLeader = userResearchGroupRepository.findById(requesterId)
                    .map(User_ResearchGroup::isLeader)
                    .orElse(false);

            if (!isLeader) {
                throw new SecurityException("Acesso negado: apenas líderes do grupo podem remover membros.");
            }
        }

        User_ResearchGroupId targetId = new User_ResearchGroupId(userEmail, groupId);
        if (userResearchGroupRepository.existsById(targetId)) {
            userResearchGroupRepository.deleteById(targetId);
        }

        userRepository.findByEmail(userEmail).ifPresent(user -> {
            if (user.getGroups() != null) {
                List<String> remainingGroups = Arrays.stream(user.getGroups())
                        .filter(g -> !g.equals(groupId))
                        .toList();
                user.setGroups(remainingGroups.toArray(new String[0]));
                userRepository.save(user);
            }
        });
    }

    @Transactional
    public InviteResponseDTO generateInviteToken(String groupId, String requesterEmail, Integer expirationDays) {
        ResearchGroup researchGroup = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + groupId));

        if (requesterEmail != null && !requesterEmail.isBlank()) {
            User_ResearchGroupId requesterId = new User_ResearchGroupId(requesterEmail, groupId);
            boolean isLeader = userResearchGroupRepository.findById(requesterId)
                    .map(User_ResearchGroup::isLeader)
                    .orElse(false);

            if (!isLeader) {
                throw new SecurityException("Acesso negado: apenas líderes do grupo podem gerar tokens de convite.");
            }
        }

        int days = (expirationDays != null && expirationDays > 0) ? expirationDays : 7;
        String token = "INV-" + groupId + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(days);

        GroupInvite invite = GroupInvite.builder()
                .token(token)
                .groupId(groupId)
                .createdByEmail(requesterEmail != null ? requesterEmail : "admin")
                .createdAt(now)
                .expiresAt(expiresAt)
                .used(false)
                .build();

        groupInviteRepository.save(invite);

        return InviteResponseDTO.builder()
                .inviteToken(token)
                .groupId(groupId)
                .createdByEmail(invite.getCreatedByEmail())
                .createdAt(now)
                .expiresAt(expiresAt)
                .build();
    }

    @Transactional
    public GroupMemberDTO joinGroupWithToken(String inviteToken, String userEmail) {
        GroupInvite invite = groupInviteRepository.findByTokenAndUsedFalse(inviteToken)
                .orElseThrow(() -> new RuntimeException("Token de convite inválido ou já utilizado: " + inviteToken));

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("O token de convite expirou em " + invite.getExpiresAt());
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com o e-mail: " + userEmail));

        ResearchGroup researchGroup = researchGroupRepository.findById(invite.getGroupId())
                .orElseThrow(() -> new RuntimeException("Grupo de pesquisa não encontrado com o ID: " + invite.getGroupId()));

        // Criar a associação do usuário no grupo como membro comum (isLeader = false)
        User_ResearchGroupId id = new User_ResearchGroupId(userEmail, invite.getGroupId());
        User_ResearchGroup userResearchGroup = userResearchGroupRepository.findById(id)
                .orElseGet(() -> {
                    User_ResearchGroup newUrg = new User_ResearchGroup();
                    newUrg.setId(id);
                    return newUrg;
                });

        userResearchGroup.setUser(user);
        userResearchGroup.setResearchGroup(researchGroup);
        userResearchGroup.setLeader(false);
        userResearchGroupRepository.save(userResearchGroup);

        if (user.getGroups() == null) {
            user.setGroups(new String[]{invite.getGroupId()});
            userRepository.save(user);
        } else if (!Arrays.asList(user.getGroups()).contains(invite.getGroupId())) {
            List<String> groupList = new ArrayList<>(Arrays.asList(user.getGroups()));
            groupList.add(invite.getGroupId());
            user.setGroups(groupList.toArray(new String[0]));
            userRepository.save(user);
        }

        invite.setUsed(true);
        groupInviteRepository.save(invite);

        return GroupMemberDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .interest_areas(user.getInterest_areas())
                .isLeader(false)
                .build();
    }
}
