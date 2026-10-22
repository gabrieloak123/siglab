package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.ResearchGroup;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.FollowRepository;
import br.ufrn.dimap.siglab.repository.ResearchGroupRepository;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Notifica por e-mail os seguidores de um grupo de pesquisa e de um pesquisador
 * quando um post é publicado.
 * <p>
 * São notificados quem segue o grupo do post e quem segue o pesquisador que o
 * publicou. Quem segue um pesquisador com áreas de interesse só recebe o e-mail
 * quando o post corresponde a alguma dessas áreas; quem segue sem escolher áreas
 * recebe todas as publicações. O autor do post nunca é notificado e cada
 * destinatário aparece uma única vez, mesmo que siga o grupo e o pesquisador.
 * </p>
 * <p>
 * O envio é best-effort: a falha de um destinatário é registrada em log e não
 * interrompe os demais, porque a notificação é acessória à publicação.
 * </p>
 *
 */
@Service
public class FollowerNotificationService {

    private static final Logger log = LoggerFactory.getLogger(FollowerNotificationService.class);
    private static final int EXCERPT_LIMIT = 300;

    private final FollowRepository follows;
    private final ResearchGroupRepository groups;
    private final UserRepository users;
    private final EmailService emails;
    private final String appBaseUrl;

    public FollowerNotificationService(FollowRepository follows, ResearchGroupRepository groups, UserRepository users,
                                       EmailService emails, @Value("${APP_BASE_URL:http://localhost:3000}") String appBaseUrl) {
        this.follows = follows;
        this.groups = groups;
        this.users = users;
        this.emails = emails;
        this.appBaseUrl = appBaseUrl;
    }

    /**
     * Envia o e-mail de publicação para todos os seguidores elegíveis.
     *
     * @param event o evento de publicação do post
     */
    public void notifyFollowers(PostPublishedEvent event) {
        User publisher = users.findById(event.publisherUserId()).orElse(null);
        String publisherName = publisher == null ? "um pesquisador do SIGLab" : publisher.getName();
        String publisherEmail = publisher == null ? null : publisher.getEmail();
        List<FollowRepository.Follower> recipients = recipients(event, publisherEmail);
        if (recipients.isEmpty()) {
            log.info("Post {} não possui seguidores elegíveis para notificação.", event.postId());
            return;
        }
        String groupName = groups.findById(event.groupId()).map(ResearchGroup::getName).orElse(event.groupId());
        String subject = "Novo post em " + groupName + ": " + event.title();
        int sent = 0;
        int failed = 0;
        for (FollowRepository.Follower recipient : recipients) {
            try {
                emails.sendEmail(recipient.email(), subject, body(event, groupName, publisherName, recipient.name()));
                sent++;
            } catch (RuntimeException ex) {
                failed++;
                log.warn("Falha ao notificar {} sobre o post {}: {}", recipient.email(), event.postId(), ex.getMessage());
            }
        }
        log.info("Post {} notificado: {} e-mails enviados, {} falhas.", event.postId(), sent, failed);
    }

    /**
     * Junta os seguidores do grupo e do pesquisador, sem duplicatas e sem o autor.
     */
    private List<FollowRepository.Follower> recipients(PostPublishedEvent event, String publisherEmail) {
        Map<String, FollowRepository.Follower> unique = new LinkedHashMap<>();
        for (FollowRepository.Follower follower : follows.groupFollowers(event.groupId())) {
            add(unique, follower, publisherEmail);
        }
        for (FollowRepository.Follower follower : follows.researcherFollowers(event.publisherUserId())) {
            if (matchesInterest(follower, event)) add(unique, follower, publisherEmail);
        }
        return new ArrayList<>(unique.values());
    }

    private void add(Map<String, FollowRepository.Follower> unique, FollowRepository.Follower follower, String publisherEmail) {
        if (follower == null || follower.email() == null || follower.email().isBlank()) return;
        String key = follower.email().trim().toLowerCase(Locale.ROOT);
        if (key.equals(publisherEmail == null ? "" : publisherEmail.trim().toLowerCase(Locale.ROOT))) return;
        unique.putIfAbsent(key, follower);
    }

    /**
     * Verifica se o post corresponde a alguma das áreas de interesse do seguidor.
     * Quem segue sem escolher áreas recebe todas as publicações do pesquisador.
     */
    private boolean matchesInterest(FollowRepository.Follower follower, PostPublishedEvent event) {
        String[] areas = follower.interestAreas();
        if (areas == null || areas.length == 0) return true;
        Set<String> tokens = tokens(searchableText(event));
        if (tokens.isEmpty()) return false;
        for (String area : areas) {
            if (matchesArea(area, tokens)) return true;
        }
        return false;
    }

    /**
     * Uma área corresponde quando todas as suas palavras aparecem no texto do post,
     * ignorando maiúsculas, acentos e separadores.
     */
    private boolean matchesArea(String area, Set<String> tokens) {
        if (area == null || area.isBlank()) return true;
        List<String> words = words(area);
        if (words.isEmpty()) return false;
        for (String word : words) {
            boolean found = false;
            for (String token : tokens) {
                if (token.startsWith(word)) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    private String searchableText(PostPublishedEvent event) {
        StringBuilder text = new StringBuilder();
        append(text, event.title());
        append(text, event.description());
        append(text, event.content());
        append(text, event.tags());
        append(text, event.keywords());
        return text.toString();
    }

    private void append(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) text.append(value).append(' ');
    }

    private void append(StringBuilder text, String[] values) {
        if (values == null) return;
        for (String value : values) append(text, value);
    }

    private String body(PostPublishedEvent event, String groupName, String publisherName, String followerName) {
        StringBuilder body = new StringBuilder();
        body.append("Olá, ").append(followerName == null || followerName.isBlank() ? "tudo bem?" : followerName).append("!\n\n");
        body.append("O grupo ").append(groupName).append(" publicou um novo post: \"").append(event.title()).append("\".\n\n");
        body.append("Publicado por ").append(publisherName).append(".\n\n");
        String excerpt = excerpt(event);
        if (!excerpt.isBlank()) body.append(excerpt).append("\n\n");
        body.append("Acesse: ").append(link(event)).append("\n\n");
        body.append("Equipe SIGLab");
        return body.toString();
    }

    private String excerpt(PostPublishedEvent event) {
        String source = event.description() != null && !event.description().isBlank() ? event.description() : event.content();
        if (source == null) return "";
        String text = source.strip();
        if (text.length() <= EXCERPT_LIMIT) return text;
        return text.substring(0, EXCERPT_LIMIT).strip() + "...";
    }

    private String link(PostPublishedEvent event) {
        String group = URLEncoder.encode(event.groupId(), StandardCharsets.UTF_8).replace("+", "%20");
        return appBaseUrl.replaceAll("/+$", "") + "/groups/" + group + "/posts/" + event.postId();
    }

    private Set<String> tokens(String text) {
        Set<String> tokens = new TreeSet<>();
        for (String word : words(text)) tokens.add(word);
        return tokens;
    }

    private List<String> words(String text) {
        if (text == null) return List.of();
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
        List<String> words = new ArrayList<>();
        for (String part : normalized.split("[^\\p{L}\\p{N}]+")) {
            if (!part.isBlank()) words.add(part);
        }
        return words;
    }
}
