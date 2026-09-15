package br.ufrn.dimap.siglab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteResponseDTO {
    private String inviteToken;
    private String groupId;
    private String createdByEmail;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
}
