package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.GoogleLoginRequest;
import br.ufrn.dimap.siglab.dto.RegistrationRequest;
import br.ufrn.dimap.siglab.dto.GooglePreviewResponse;
import br.ufrn.dimap.siglab.dto.ScholarCandidate;
import br.ufrn.dimap.siglab.integration.GoogleIdentityClient;
import br.ufrn.dimap.siglab.integration.ScholarClient;
import br.ufrn.dimap.siglab.exception.ExternalServiceException;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserService userService;
    private final InterestService interests;
    private final GoogleIdentityClient google;
    private final PasswordEncoder encoder;
    private final ScholarClient scholar;

    public AuthService(UserRepository users, UserService userService, InterestService interests,
                       GoogleIdentityClient google, PasswordEncoder encoder, ScholarClient scholar) {
        this.users=users; this.userService=userService; this.interests=interests; this.google=google; this.encoder=encoder; this.scholar=scholar;
    }
    @Transactional
    public User register(RegistrationRequest request) {
        String authorId = normalizedAuthorId(request.authorId());
        if (authorId != null && users.existsByAuthorId(authorId)) throw new IllegalArgumentException("author_id já cadastrado.");
        String[] areas = areas(request.authorId(), request.interestAreas());
        User user = userService.save(User.builder().name(request.name().trim()).email(request.email().trim())
                .password(request.password()).authorId(authorId).interest_areas(areas).build());
        return user;
    }
    public User login(String email, String password) {
        User user = users.findByEmail(email.trim()).orElseThrow(() -> new SecurityException("Credenciais inválidas."));
        if (!encoder.matches(password, user.getPassword())) throw new SecurityException("Credenciais inválidas.");
        return user;
    }
    @Transactional
    public User google(GoogleLoginRequest request) {
        var identity = google.verify(request.credential());
        return users.findByGoogleSubject(identity.subject()).orElseGet(() -> {
            if (users.existsByEmail(identity.email()))
                throw new IllegalArgumentException("Já existe uma conta com este e-mail. Entre com senha antes de vincular Google.");
            String authorId = normalizedAuthorId(request.authorId());
            if (authorId != null && users.existsByAuthorId(authorId)) throw new IllegalArgumentException("author_id já cadastrado.");
            String[] areas = areas(request.authorId(), request.interestAreas());
            String level=verificationLevel(identity, authorId);
            User created = userService.save(User.builder().name(identity.name()).email(identity.email())
                    .password(UUID.randomUUID().toString()).authorId(authorId)
                    .googleSubject(identity.subject()).institutionalDomain(identity.hostedDomain())
                    .verificationLevel(level).interest_areas(areas).build());
            return created;
        });
    }
    public GooglePreviewResponse previewGoogle(String credential) {
        var identity=google.verify(credential);
        boolean existing=users.findByGoogleSubject(identity.subject()).isPresent();
        java.util.List<ScholarCandidate> candidates=java.util.List.of();
        if (!existing) try { candidates=scholar.searchProfiles(identity.name()); }
        catch (ExternalServiceException ignored) { /* descoberta é opcional; o cadastro manual permanece disponível */ }
        return new GooglePreviewResponse(existing, identity.name(), identity.email(), identity.hostedDomain(), candidates);
    }
    @Transactional
    public User linkGoogle(User current, String credential) {
        var identity = google.verify(credential);
        if (!current.getEmail().equalsIgnoreCase(identity.email()))
            throw new SecurityException("A conta Google deve usar o mesmo e-mail da conta atual.");
        users.findByGoogleSubject(identity.subject()).ifPresent(existing -> {
            if (!existing.getId().equals(current.getId())) throw new IllegalArgumentException("Conta Google já vinculada a outro usuário.");
        });
        current.setGoogleSubject(identity.subject());
        current.setInstitutionalDomain(identity.hostedDomain());
        if (identity.hostedDomain() != null && !identity.hostedDomain().isBlank()) current.setVerificationLevel("INSTITUCIONAL");
        return users.save(current);
    }
    public String[] areas(String authorId, String[] extra) {
        String normalized = normalizedAuthorId(authorId);
        return normalized == null ? interests.three(extra) : interests.withScholar(normalized, extra);
    }

    private String normalizedAuthorId(String authorId) {
        return authorId == null || authorId.isBlank() ? null : authorId.trim();
    }
    private String verificationLevel(GoogleIdentityClient.Identity identity, String authorId) {
        if (identity.hostedDomain() == null || identity.hostedDomain().isBlank()) return "NAO_VERIFICADO";
        if (authorId == null) return "INSTITUCIONAL";
        ScholarCandidate profile=scholar.profile(authorId);
        return identity.hostedDomain().equalsIgnoreCase(profile.verifiedDomain()) ? "INSTITUCIONAL" : "NAO_VERIFICADO";
    }
}
