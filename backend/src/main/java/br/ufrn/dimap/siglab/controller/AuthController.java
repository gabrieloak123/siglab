package br.ufrn.dimap.siglab.controller;

import br.ufrn.dimap.siglab.dto.*;
import br.ufrn.dimap.siglab.integration.ScholarClient;
import br.ufrn.dimap.siglab.integration.OrcidClient;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import br.ufrn.dimap.siglab.service.AuthService;
import br.ufrn.dimap.siglab.service.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.view.RedirectView;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final UserRepository users;
    private final ScholarClient scholar;
    private final SessionUser sessionUser;
    private final OrcidClient orcid;
    private final String appBaseUrl;
    public AuthController(AuthService auth, UserRepository users, ScholarClient scholar, SessionUser sessionUser,
                          OrcidClient orcid, @Value("${APP_BASE_URL:http://localhost:3000}") String appBaseUrl) {
        this.auth=auth; this.users=users; this.scholar=scholar; this.sessionUser=sessionUser; this.orcid=orcid; this.appBaseUrl=appBaseUrl;
    }
    private User session(HttpServletRequest request, User user) {
        HttpSession old = request.getSession(false);
        if (old != null) old.invalidate();
        request.getSession(true).setAttribute("userId", user.getId());
        return user;
    }
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public User register(@Valid @RequestBody RegistrationRequest body, HttpServletRequest request) {
        return session(request, auth.register(body));
    }
    @PostMapping("/login")
    public User login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        return session(request, auth.login(body.email(), body.password()));
    }
    @PostMapping("/google")
    public User google(@Valid @RequestBody GoogleLoginRequest body, HttpServletRequest request) {
        return session(request, auth.google(body));
    }
    @PostMapping("/google/preview")
    public GooglePreviewResponse googlePreview(@RequestBody java.util.Map<String,String> body) {
        String credential=body.get("credential");
        if (credential == null || credential.isBlank()) throw new IllegalArgumentException("Credencial Google é obrigatória.");
        return auth.previewGoogle(credential);
    }
    @PostMapping("/google/link")
    public User linkGoogle(@RequestBody java.util.Map<String, String> body, HttpServletRequest request) {
        String credential = body.get("credential");
        if (credential == null || credential.isBlank()) throw new IllegalArgumentException("Credencial Google é obrigatória.");
        return auth.linkGoogle(sessionUser.require(request), credential);
    }
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) session.invalidate();
    }
    @GetMapping("/me")
    public User me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Long id)) throw new SecurityException("Entre para continuar.");
        return users.findById(id).orElseThrow(() -> new SecurityException("Sessão inválida."));
    }
    @GetMapping("/scholar/{authorId}")
    public List<String> scholar(@PathVariable String authorId) { return scholar.interests(authorId); }
    @GetMapping("/orcid/status")
    public java.util.Map<String,Boolean> orcidStatus() { return java.util.Map.of("configured", orcid != null && orcid.configured()); }
    @GetMapping("/orcid/start")
    public RedirectView orcidStart(HttpServletRequest request) {
        sessionUser.require(request);
        String state=java.util.UUID.randomUUID().toString();
        request.getSession().setAttribute("orcidState", state);
        return new RedirectView(orcid.authorizationUrl(state));
    }
    @GetMapping("/orcid/callback")
    public RedirectView orcidCallback(@RequestParam String code, @RequestParam String state, HttpServletRequest request) {
        Object expected=request.getSession().getAttribute("orcidState");
        request.getSession().removeAttribute("orcidState");
        if (!(expected instanceof String value) || !java.security.MessageDigest.isEqual(value.getBytes(), state.getBytes()))
            throw new SecurityException("Estado OAuth do ORCID inválido.");
        User user=sessionUser.require(request);
        var identity=orcid.exchange(code);
        if (users.existsByOrcid(identity.orcid()) && !identity.orcid().equals(user.getOrcid()))
            throw new IllegalArgumentException("ORCID já vinculado a outra conta.");
        user.setOrcid(identity.orcid()); user.setVerificationLevel("ORCID"); users.save(user);
        return new RedirectView(appBaseUrl + "/?orcid=linked");
    }
}
