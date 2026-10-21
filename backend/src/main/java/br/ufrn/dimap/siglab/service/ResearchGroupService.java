package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.GroupMemberDTO;
import br.ufrn.dimap.siglab.dto.InviteResponseDTO;
import br.ufrn.dimap.siglab.exception.ResourceNotFoundException;
import br.ufrn.dimap.siglab.exception.ConflictException;
import br.ufrn.dimap.siglab.exception.AccessDeniedException;
import br.ufrn.dimap.siglab.exception.GroupMustHaveLeaderException;
import br.ufrn.dimap.siglab.model.*;
import br.ufrn.dimap.siglab.repository.GroupInviteRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.repository.User_ResearchGroupRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Serviço responsável por conter a lógica de negócio relacionada a Grupos de Pesquisa,
 * atribuição de liderança, tokens de convite e gestão de membros.
 *
 */
@Service
public class ResearchGroupService {

    private final ResearchGroupRepository researchGroupRepository;
    private final User_ResearchGroupRepository userResearchGroupRepository;
    private final UserRepository userRepository;
    private final GroupInviteRepository groupInviteRepository;
    private final PasswordEncoder passwordEncoder;
    private final InterestService interests;
    private final IndexingQueue indexing;

    public ResearchGroupService(ResearchGroupRepository researchGroupRepository,
                                User_ResearchGroupRepository userResearchGroupRepository,
                                UserRepository userRepository,
                                GroupInviteRepository groupInviteRepository,
                                PasswordEncoder passwordEncoder, InterestService interests, IndexingQueue indexing) {
        this.researchGroupRepository = researchGroupRepository;
        this.userResearchGroupRepository = userResearchGroupRepository;
        this.userRepository = userRepository;
        this.groupInviteRepository = groupInviteRepository;
        this.passwordEncoder = passwordEncoder;
        this.interests = interests;
        this.indexing = indexing;
    }

    /**
     * Retorna todos os grupos de pesquisa cadastrados.
     *
     * @return lista de {@link ResearchGroup}
     */
    @Transactional(readOnly = true)
    public List<ResearchGroup> findAll() {
        return researchGroupRepository.findAll();
    }

    /**
     * Busca um grupo de pesquisa pelo seu ID em texto.
     *
     * @param groupId o ID do grupo
     * @return {@link Optional} contendo o grupo se encontrado
     */
    @Transactional(readOnly = true)
    public Optional<ResearchGroup> findById(String groupId) {
        return researchGroupRepository.findById(groupId);
    }

    /**
     * Salva ou cria um novo grupo de pesquisa.
     *
     * @param researchGroup a entidade do grupo de pesquisa
     * @return o grupo salvo
     * @throws IllegalArgumentException se já existir um grupo com o mesmo ID
     */
    @Transactional
    public ResearchGroup save(ResearchGroup researchGroup) {
        researchGroup.setResearch_fields(interests.three(researchGroup.getResearch_fields()));
        if (researchGroup.getGroup_id() != null && !researchGroup.getGroup_id().isBlank()) {
            String trimmedId = researchGroup.getGroup_id().trim();
            researchGroup.setGroup_id(trimmedId);
            if (researchGroupRepository.existsById(trimmedId)) {
                throw new ConflictException("Já existe um grupo de pesquisa cadastrado com o ID: " + trimmedId);
            }
        }
        ResearchGroup saved = researchGroupRepository.saveAndFlush(researchGroup);
        indexing.enqueue("GROUP", saved.getGroup_id());
        return saved;
    }

    @Transactional
    public ResearchGroup createForUser(ResearchGroup group, String email) {
        ResearchGroup saved = save(group);
        setMemberLeadership(saved.getGroup_id(), email, true);
        return saved;
    }

    @Transactional(readOnly = true)
    public void requireLeader(String groupId, String email) {
        if (!userResearchGroupRepository.findById(new User_ResearchGroupId(email, groupId))
                .map(User_ResearchGroup::isLeader).orElse(false))
            throw new AccessDeniedException("Apenas líderes do grupo podem executar esta ação.");
    }

    /**
     * Atualiza os dados de um grupo de pesquisa existente.
     *
     * @param groupId o ID do grupo a ser atualizado
     * @param groupDetails os novos dados do grupo
     * @return o grupo de pesquisa atualizado
     * @throws ResourceNotFoundException se o grupo não for encontrado
     */
    @Transactional
    public ResearchGroup update(String groupId, ResearchGroup groupDetails) {
        return researchGroupRepository.findById(groupId)
                .map(group -> {
                    group.setName(groupDetails.getName());
                    group.setResearch_fields(interests.three(groupDetails.getResearch_fields()));
                    if (groupDetails.getMembers() != null) {
                        group.setMembers(groupDetails.getMembers());
                    }
                    ResearchGroup saved = researchGroupRepository.save(group);
                    indexing.enqueue("GROUP", saved.getGroup_id());
                    return saved;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId));
    }

    /**
     * Exclui um grupo de pesquisa mediante confirmação de senha do orientador/líder.
     *
     * @param groupId o ID do grupo a ser removido
     * @param leaderEmail o e-mail do orientador solicitante
     * @param leaderPassword a senha de confirmação do orientador
     * @throws ResourceNotFoundException se o grupo ou usuário não forem encontrados
     * @throws SecurityException se o usuário não for líder do grupo ou a senha estiver incorreta
     */
    @Transactional
    public void deleteById(String groupId, String leaderEmail, String leaderPassword) {
        if (!researchGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId);
        }

        if (leaderEmail == null || leaderEmail.isBlank()) {
            throw new AccessDeniedException("Acesso negado: a identificação do líder/orientador é obrigatória para excluir o grupo.");
        }

        User_ResearchGroupId requesterId = new User_ResearchGroupId(leaderEmail, groupId);
        boolean isLeader = userResearchGroupRepository.findById(requesterId)
                .map(User_ResearchGroup::isLeader)
                .orElse(false);

        if (!isLeader) {
            throw new AccessDeniedException("Acesso negado: apenas orientadores/líderes do grupo podem excluí-lo.");
        }

        if (leaderPassword == null || leaderPassword.isBlank()) {
            throw new AccessDeniedException("A confirmação da senha do orientador é obrigatória para excluir o grupo de pesquisa.");
        }

        User leader = userRepository.findByEmail(leaderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário orientador não encontrado com o e-mail: " + leaderEmail));

        if (!passwordEncoder.matches(leaderPassword, leader.getPassword())) {
            throw new AccessDeniedException("Senha incorreta do orientador. Não foi possível excluir o grupo de pesquisa.");
        }

        groupInviteRepository.deleteByGroupId(groupId);
        userResearchGroupRepository.deleteAll(userResearchGroupRepository.findByGroupId(groupId));
        researchGroupRepository.deleteById(groupId);
    }

    /**
     * Busca todos os usuários membros de um grupo de pesquisa.
     *
     * @param groupId o ID do grupo de pesquisa
     * @return lista de DTOs {@link GroupMemberDTO} contendo dados dos usuários e se são líderes
     * @throws ResourceNotFoundException se o grupo não for encontrado
     */
    @Transactional(readOnly = true)
    public List<GroupMemberDTO> findMembersByGroupId(String groupId) {
        if (!researchGroupRepository.existsById(groupId)) {
            throw new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId);
        }

        List<User_ResearchGroup> joinMembers = userResearchGroupRepository.findByGroupId(groupId);
        List<GroupMemberDTO> result = new ArrayList<>();

        for (User_ResearchGroup m : joinMembers) {
            if (m.getUser() != null) {
                result.add(GroupMemberDTO.builder()
                        .id(m.getUser().getId())
                        .email(m.getUser().getEmail())
                        .name(m.getUser().getName())
                        .interest_areas(m.getUser().getInterest_areas())
                        .isLeader(m.isLeader())
                        .build());
            }
        }

        return result;
    }

    /**
     * Define ou atualiza o status de liderança de um usuário em um grupo de pesquisa.
     *
     * @param groupId o ID do grupo de pesquisa
     * @param userEmail o e-mail do usuário
     * @param isLeader {@code true} para definir como líder, {@code false} caso contrário
     * @return DTO {@link GroupMemberDTO} com o status atualizado do usuário
     * @throws ResourceNotFoundException se o grupo ou usuário não forem encontrados
     */
    @Transactional
    public GroupMemberDTO setMemberLeadership(String groupId, String userEmail, boolean isLeader) {
        ResearchGroup researchGroup = researchGroupRepository.findForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o e-mail: " + userEmail));

        User_ResearchGroupId id = new User_ResearchGroupId(userEmail, groupId);
        User_ResearchGroup userResearchGroup = userResearchGroupRepository.findById(id)
                .orElseGet(() -> {
                    User_ResearchGroup newUrg = new User_ResearchGroup();
                    newUrg.setId(id);
                    return newUrg;
                });

        if (!isLeader && userResearchGroup.isLeader()) ensureOtherLeader(groupId);

        userResearchGroup.setUser(user);
        userResearchGroup.setResearchGroup(researchGroup);
        userResearchGroup.setLeader(isLeader);

        userResearchGroupRepository.save(userResearchGroup);

        return GroupMemberDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .interest_areas(user.getInterest_areas())
                .isLeader(isLeader)
                .build();
    }

    /**
     * Remove um usuário de um grupo de pesquisa. Valida se o solicitante possui permissão de líder.
     *
     * @param groupId o ID do grupo de pesquisa
     * @param userEmail o e-mail do usuário a ser removido
     * @param requesterEmail o e-mail do usuário solicitante da remoção
     * @throws SecurityException se o solicitante não for líder do grupo
     * @throws ResourceNotFoundException se o grupo não for encontrado
     */
    @Transactional
    public void removeMemberFromGroup(String groupId, String userEmail, String requesterEmail) {
        researchGroupRepository.findForUpdate(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId));

        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new AccessDeniedException("Acesso negado: a identificação do usuário solicitante é obrigatória.");
        }

        User_ResearchGroupId requesterId = new User_ResearchGroupId(requesterEmail, groupId);
        boolean isLeader = userResearchGroupRepository.findById(requesterId)
                .map(User_ResearchGroup::isLeader)
                .orElse(false);

        if (!isLeader) {
            throw new AccessDeniedException("Acesso negado: apenas líderes do grupo podem remover membros.");
        }

        User_ResearchGroupId targetId = new User_ResearchGroupId(userEmail, groupId);
        userResearchGroupRepository.findById(targetId).ifPresent(target -> {
            if (target.isLeader()) ensureOtherLeader(groupId);
            userResearchGroupRepository.delete(target);
        });
    }

    private void ensureOtherLeader(String groupId) {
        long leaders = userResearchGroupRepository.findByGroupId(groupId).stream().filter(User_ResearchGroup::isLeader).count();
        if (leaders <= 1) throw new GroupMustHaveLeaderException("O grupo deve manter pelo menos um líder.");
    }

    /**
     * Gera uma nova hash/token de convite para um grupo de pesquisa.
     * <p>
     * Apenas líderes do grupo de pesquisa têm permissão para gerar este convite.
     * </p>
     *
     * @param groupId o ID do grupo de pesquisa
     * @param requesterEmail o e-mail do líder solicitante
     * @param expirationDays dias até a expiração da hash (padrão 7)
     * @return DTO {@link InviteResponseDTO} com os dados da hash gerada
     * @throws SecurityException se o solicitante não for líder do grupo
     * @throws ResourceNotFoundException se o grupo não for encontrado
     */
    @Transactional
    public InviteResponseDTO generateInviteToken(String groupId, String requesterEmail, Integer expirationDays) {
        ResearchGroup researchGroup = researchGroupRepository.findById(groupId)
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + groupId));

        if (requesterEmail == null || requesterEmail.isBlank()) {
            throw new AccessDeniedException("Acesso negado: a identificação do usuário solicitante é obrigatória.");
        }

        User_ResearchGroupId requesterId = new User_ResearchGroupId(requesterEmail, groupId);
        boolean isLeader = userResearchGroupRepository.findById(requesterId)
                .map(User_ResearchGroup::isLeader)
                .orElse(false);

        if (!isLeader) {
            throw new AccessDeniedException("Acesso negado: apenas líderes do grupo podem gerar tokens de convite.");
        }

        int days = expirationDays == null ? 7 : expirationDays;
        if (days < 1 || days > 30) throw new IllegalArgumentException("A validade do convite deve ser entre 1 e 30 dias.");
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

    /**
     * Permite que um usuário ingresse em um grupo de pesquisa utilizando uma hash de convite válida.
     *
     * @param inviteToken a hash de convite
     * @param userEmail o e-mail do usuário que está ingressando
     * @return DTO {@link GroupMemberDTO} do usuário adicionado ao grupo
     * @throws IllegalArgumentException se o token for inválido, já utilizado ou expirado
     * @throws ResourceNotFoundException se o usuário ou grupo não forem encontrados
     */
    @Transactional
    public GroupMemberDTO joinGroupWithToken(String inviteToken, String userEmail) {
        GroupInvite invite = groupInviteRepository.findByTokenAndUsedFalse(inviteToken)
                .orElseThrow(() -> new IllegalArgumentException("Token de convite inválido ou já utilizado: " + inviteToken));

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("O token de convite expirou em " + invite.getExpiresAt());
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com o e-mail: " + userEmail));

        ResearchGroup researchGroup = researchGroupRepository.findById(invite.getGroupId())
                .orElseThrow(() -> new ResourceNotFoundException("Grupo de pesquisa não encontrado com o ID: " + invite.getGroupId()));

        User_ResearchGroupId id = new User_ResearchGroupId(userEmail, invite.getGroupId());
        if (userResearchGroupRepository.existsById(id)) {
            throw new IllegalArgumentException("Você já é integrante deste grupo de pesquisa.");
        }

        User_ResearchGroup userResearchGroup = new User_ResearchGroup();
        userResearchGroup.setId(id);
        userResearchGroup.setUser(user);
        userResearchGroup.setResearchGroup(researchGroup);
        userResearchGroup.setLeader(false);
        userResearchGroupRepository.save(userResearchGroup);

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
