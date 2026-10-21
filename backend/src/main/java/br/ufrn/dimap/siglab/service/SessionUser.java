package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.exception.AuthenticationRequiredException;

import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SessionUser {
    private final UserRepository users;
    public SessionUser(UserRepository users) { this.users=users; }
    /**
     * Busca o usuário da sessão atual sem exigir autenticação.
     * Retorna {@link Optional#empty()} para visitantes anônimos, permitindo
     * endpoints de leitura públicos sem expor as operações que exigem sessão.
     */
    public Optional<User> find(HttpServletRequest request) {
        var session=request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Long id)) return Optional.empty();
        return users.findById(id);
    }
    public User require(HttpServletRequest request) {
        var session=request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Long id)) throw new AuthenticationRequiredException("Entre para continuar.");
        return users.findById(id).orElseThrow(() -> new AuthenticationRequiredException("Sessão inválida."));
    }
}
