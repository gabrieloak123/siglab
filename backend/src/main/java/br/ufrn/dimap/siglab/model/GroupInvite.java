package br.ufrn.dimap.siglab.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entidade que representa um Token/Hash de convite para um {@link ResearchGroup}.
 * <p>
 * Gerado exclusivamente por líderes do grupo de pesquisa para permitir o ingresso controlado
 * de novos membros.
 * </p>
 *
 */
@Entity
@Table(name = "group_invites")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupInvite {

    /**
     * Hash/Token único do convite (chave primária).
     */
    @Id
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    /**
     * Identificador do grupo de pesquisa para o qual o convite dá acesso.
     */
    @Column(name = "group_id", nullable = false)
    private String groupId;

    /**
     * E-mail do líder que criou a hash de convite.
     */
    @Column(name = "created_by_email", nullable = false)
    private String createdByEmail;

    /**
     * Data e hora de criação da hash de convite.
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * Data e hora limite de expiração da hash de convite.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Indica se a hash de convite já foi utilizada por um usuário.
     */
    @Column(name = "used", nullable = false)
    private boolean used;
}
