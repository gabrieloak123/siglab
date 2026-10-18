package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class SessionUser {
    private final UserRepository users;
    public SessionUser(UserRepository users) { this.users=users; }
    public User require(HttpServletRequest request) {
        var session=request.getSession(false);
        if (session == null || !(session.getAttribute("userId") instanceof Long id)) throw new SecurityException("Entre para continuar.");
        return users.findById(id).orElseThrow(() -> new SecurityException("Sessão inválida."));
    }
}
