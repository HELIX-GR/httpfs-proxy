package gr.helix.httpfsproxy.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import gr.helix.httpfsproxy.domain.UserEntity;

@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<UserEntity, Long>
{    
    @Query("FROM User u WHERE u.name = :name")
    Optional<UserEntity> findByUsername(@Param("name") String username);
    
    @Query("FROM User u WHERE u.registeredAt > :start")
    List<UserEntity> findByRegisteredAfter(@Param("start") ZonedDateTime start);
    
    @Modifying
    @Query("UPDATE User u SET u.isActive = :active WHERE u.id IN (:uids)")
    @Transactional(readOnly = false)
    void setActive(@Param("uids") Collection<Long> uids, @Param("active") boolean active);
}
