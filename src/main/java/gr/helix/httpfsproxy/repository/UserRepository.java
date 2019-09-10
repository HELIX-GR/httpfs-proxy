package gr.helix.httpfsproxy.repository;

import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import gr.helix.httpfsproxy.domain.UserEntity;
import gr.helix.httpfsproxy.model.UserInfo;

@Repository
@Transactional(readOnly = true)
public interface UserRepository extends JpaRepository<UserEntity, Long>
{    
    @Query("FROM User u WHERE u.username = :username")
    Optional<UserEntity> findByUsername(@Param("username") String username);
    
    @Query("SELECT u.id FROM User u WHERE u.username = :username")
    OptionalLong idByUsername(@Param("username") String username);
    
    @Query("FROM User u WHERE u.registeredAt > :start")
    List<UserEntity> findByRegisteredAfter(@Param("start") ZonedDateTime start);
    
    @Modifying
    @Query("UPDATE User u SET u.active = :active WHERE u.id IN (:uids)")
    @Transactional(readOnly = false)
    int setActive(@Param("uids") Collection<Long> uids, @Param("active") boolean active);
    
    @Modifying
    @Transactional(readOnly = false)
    default UserEntity createWith(UserInfo userInfo) 
    {
        return saveAndFlush(new UserEntity(userInfo));
    };
    
    // Todo update from UserInfo
    
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :uid")
    @Transactional(readOnly = false)
    int updatePassword(@Param("uid") long uid, @Param("password") String digestedPassword);
}
