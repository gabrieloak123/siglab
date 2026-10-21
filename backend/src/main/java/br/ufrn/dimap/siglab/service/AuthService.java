package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.dto.GoogleLoginRequest;
import br.ufrn.dimap.siglab.dto.RegistrationRequest;
import br.ufrn.dimap.siglab.dto.GooglePreviewResponse;
import br.ufrn.dimap.siglab.integration.GoogleIdentityProvider;
import br.ufrn.dimap.siglab.exception.AuthenticationRequiredException;
import br.ufrn.dimap.siglab.exception.ConflictException;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository users;
    private final UserService userService;
    private final InterestService interests;
    private final GoogleIdentityProvider google;
    private final PasswordEncoder encoder;

    public AuthService(UserRepository users, UserService userService, InterestService interests,
                       GoogleIdentityProvider google, PasswordEncoder encoder) {
        this.users=users; this.userService=userService; this.interests=interests; this.google=google; this.encoder=encoder;
    }
    public User register(RegistrationRequest request) {
        if (request.authorId() != null && !request.authorId().isBlank())
            throw new IllegalArgumentException("Vincule o ORCID antes de informar o author_id.");
        String[] areas = interests.three(request.interestAreas());
        User user = userService.save(User.builder().name(request.name().trim()).email(request.email().trim())
                .password(request.password()).interest_areas(areas).build());
        return user;
    }
    public User login(String email, String password) {
        User user = users.findByEmailIgnoreCase(email.trim()).orElseThrow(() -> new AuthenticationRequiredException("Credenciais inválidas."));
        if (!encoder.matches(password, user.getPassword())) throw new AuthenticationRequiredException("Credenciais inválidas.");
        return user;
    }
    public User google(GoogleLoginRequest request) {
        var identity = google.verify(request.credential());
        return users.findByGoogleSubject(identity.subject()).orElseGet(() -> {
            if (users.existsByEmailIgnoreCase(identity.email()))
                throw new ConflictException("Já existe uma conta com este e-mail. Entre com senha antes de vincular Google.");
            if (request.authorId() != null && !request.authorId().isBlank())
                throw new IllegalArgumentException("Vincule o ORCID antes de informar o author_id.");
            String[] areas = interests.three(request.interestAreas());
            String level=identity.hostedDomain() == null || identity.hostedDomain().isBlank() ? "NAO_VERIFICADO" : "INSTITUCIONAL";
            User created = userService.save(User.builder().name(identity.name()).email(identity.email())
                    .password(UUID.randomUUID().toString())
                    .googleSubject(identity.subject()).institutionalDomain(identity.hostedDomain())
                    .verificationLevel(level).interest_areas(areas).build());
            return created;
        });
    }
    public GooglePreviewResponse previewGoogle(String credential) {
        var identity=google.verify(credential);
        boolean existing=users.findByGoogleSubject(identity.subject()).isPresent();
        return new GooglePreviewResponse(existing, identity.name(), identity.email(), identity.hostedDomain());
    }
    public User linkGoogle(User current, String credential) {
        var identity = google.verify(credential);
        if (!current.getEmail().equalsIgnoreCase(identity.email()))
            throw new SecurityException("A conta Google deve usar o mesmo e-mail da conta atual.");
        users.findByGoogleSubject(identity.subject()).ifPresent(existing -> {
            if (!existing.getId().equals(current.getId())) throw new ConflictException("Conta Google já vinculada a outro usuário.");
        });
        current.setGoogleSubject(identity.subject());
        current.setInstitutionalDomain(identity.hostedDomain());
        if (!"ORCID".equals(current.getVerificationLevel()) && identity.hostedDomain() != null && !identity.hostedDomain().isBlank())
            current.setVerificationLevel("INSTITUCIONAL");
        return users.save(current);
    }
}
