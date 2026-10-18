package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByGoogleSubject(String googleSubject);
    boolean existsByOrcid(String orcid);
    boolean existsByAuthorId(String authorId);
    boolean existsByEmail(String email);
    void deleteByEmail(String email);
}
