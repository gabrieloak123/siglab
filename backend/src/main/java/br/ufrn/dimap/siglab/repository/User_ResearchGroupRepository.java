package br.ufrn.dimap.siglab.repository;

import br.ufrn.dimap.siglab.model.User_ResearchGroup;
import br.ufrn.dimap.siglab.model.User_ResearchGroupId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface User_ResearchGroupRepository extends JpaRepository<User_ResearchGroup, User_ResearchGroupId> {

    @Query("SELECT urg FROM User_ResearchGroup urg WHERE urg.researchGroup.group_id = :groupId")
    List<User_ResearchGroup> findByGroupId(@Param("groupId") String groupId);

    @Query("""
            SELECT CASE WHEN COUNT(urg) > 0 THEN true ELSE false END
            FROM User_ResearchGroup urg
            WHERE urg.user.id = :userId
              AND urg.researchGroup.group_id = :groupId
            """)
    boolean existsMemberByUserIdAndGroupId(@Param("userId") Long userId,
                                            @Param("groupId") String groupId);
}
