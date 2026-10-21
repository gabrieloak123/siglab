package br.ufrn.dimap.siglab.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO de uma conferência sugerida ao usuário, com os dados exibidos no painel
 * e o link {@code detail_page} (atribuição obrigatória pelos termos de uso da
 * Conference Partner — todo card deve linkar de volta à página da conferência).
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConferenceSuggestionDTO {

    /** Identificador da conferência na Conference Partner. */
    private Long id;

    /** Sigla curta (ex.: ICML). */
    private String shortName;

    /** Nome completo da conferência. */
    private String fullName;

    /** Data de início do evento; nas sugestões, nunca no passado. */
    private LocalDate conferenceDate;

    /** Prazo de submissão (CFP), quando conhecido. */
    private LocalDate submissionDate;

    /** Local do evento, quando conhecido. */
    private String location;

    /** Ranking CCF (A/B/C), vazio quando não ranqueada. */
    private String ccfRank;

    /** Pontuação Conference Partner Index (CP-I), quando disponível. */
    private Double cpIndexScore;

    /** URL canônica da conferência na Conference Partner (obrigatória para atribuição). */
    private String detailPage;
}
