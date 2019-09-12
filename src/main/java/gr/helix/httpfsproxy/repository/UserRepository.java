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
import org.springframework.util.Assert;

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
    default UserEntity createFrom(UserInfo userInfo) 
    {
        Assert.notNull(userInfo, "Expected a userInfo object!");
        Assert.isNull(userInfo.getId(), "Did not expect a user ID");
        return save(UserEntity.fromUserInfo(userInfo));
    };
    
    @Modifying
    @Transactional(readOnly = false)
    default UserEntity updateFrom(UserInfo userInfo) 
    {
        Assert.notNull(userInfo, "Expected a userInfo object!");
        final Long uid = userInfo.getId();
        Assert.notNull(uid, "Expected a non-null user ID!");
        return getOne(uid).withUserInfo(userInfo);
    };
    
    @Modifying
    @Query("UPDATE User u SET u.password = :password WHERE u.id = :uid")
    @Transactional(readOnly = false)
    int updatePassword(@Param("uid") long uid, @Param("password") String digestedPassword);
}
