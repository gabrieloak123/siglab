package br.ufrn.dimap.siglab.service;

import br.ufrn.dimap.siglab.integration.OrcidIdentityProvider;
import br.ufrn.dimap.siglab.exception.ConflictException;
import br.ufrn.dimap.siglab.model.User;
import br.ufrn.dimap.siglab.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class OrcidService {
    private final OrcidIdentityProvider orcid;
    private final UserRepository users;
    private final TransactionTemplate transaction;

    public OrcidService(OrcidIdentityProvider orcid, UserRepository users, TransactionTemplate transaction) {
        this.orcid = orcid; this.users = users; this.transaction = transaction;
    }

    public boolean configured() { return orcid.configured(); }
    public String authorizationUrl(String state) { return orcid.authorizationUrl(state); }

    public void link(Long userId, String code) {
        var identity = orcid.exchange(code);
        transaction.executeWithoutResult(status -> {
            User user = users.findById(userId).orElseThrow(() -> new SecurityException("Sessão inválida."));
            if (users.existsByOrcid(identity.orcid()) && !identity.orcid().equals(user.getOrcid()))
                throw new ConflictException("ORCID já vinculado a outra conta.");
            user.setOrcid(identity.orcid());
            user.setVerificationLevel("ORCID");
            users.save(user);
        });
    }
}
