package gr.helix.httpfsproxy.domain;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.Assert;

import gr.helix.httpfsproxy.model.EnumRole;
import gr.helix.httpfsproxy.model.SimpleUserDetails;
import gr.helix.httpfsproxy.model.UserInfo;

@Entity(name = "User")
@Table(name = "`user`", schema = "public")
public class UserEntity
{  
    @Id    
    @SequenceGenerator(sequenceName = "`user_id_seq`", name = "user_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "user_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "`_id`")
    Long id;
    
    @NotNull
    @Column(name = "`username`", unique = true, updatable = false)
    String username;
     
    @Column(name = "`fullname`")
    String fullname;
 
    @Column(name = "`hdfs_username`", nullable = false)
    String hdfsUsername;
    
    @Column(name = "`password`")
    String password;
    
    @Column(name = "`active`")
    Boolean active = true;
    
    @Column(name = "`blocked`")
    Boolean blocked = false;
    
    @Column(name = "`registered_at`", updatable = false)
    ZonedDateTime registeredAt;
 
    @Column(name = "`token`", nullable = true)
    String token;
    
    @Column(name = "`email`", nullable = false)
    String email;
    
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    List<UserRoleEntity> memberOf = new ArrayList<>();
    
    protected UserEntity() {}
    
    public UserEntity(String name, ZonedDateTime registeredAt) 
    {
        this.username = name;
        this.registeredAt = registeredAt;
    }
    
    /**
     * Create an entity from a DTO object
     * @param userinfo
     */
    public UserEntity(UserInfo userinfo) 
    {
        Assert.notNull(userinfo, "A userinfo object is required");
        
        this.username = userinfo.getUsername();
        this.fullname = userinfo.getFullname();
        this.hdfsUsername = userinfo.getHdfsUsername();
        this.email = userinfo.getEmail();
        this.active = userinfo.isActive();
        this.registeredAt = userinfo.getRegisteredAt();
        
        for (EnumRole role: userinfo.getRoles())
            this.memberOf.add(UserRoleEntity.newMember(this, role));
    }
    
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String name)
    {
        this.username = name;
    }

    public String getFullname()
    {
        return fullname;
    }

    public void setFullname(String fullname)
    {
        this.fullname = fullname;
    }
    
    public String getHdfsUsername()
    {
        return hdfsUsername;
    }
    
    public void setHdfsUsername(String hdfsUsername)
    {
        this.hdfsUsername = hdfsUsername;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    public Boolean isActive()
    {
        return active;
    }

    public void setActive(Boolean isActive)
    {
        this.active = isActive;
    }

    public Boolean isBlocked()
    {
        return blocked;
    }

    public void setBlocked(Boolean isBlocked)
    {
        this.blocked = isBlocked;
    }

    public ZonedDateTime getRegisteredAt()
    {
        return registeredAt;
    }

    public void setRegisteredAt(ZonedDateTime registeredAt)
    {
        this.registeredAt = registeredAt;
    }

    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }
    
    public String getEmail()
    {
        return email;
    }

    public void setEmail(String email)
    {
        this.email = email;
    }
    
    public void addRole(EnumRole role)
    {
        memberOf.add(UserRoleEntity.newMember(this, role));
    }
    
    public List<EnumRole> getRoles()
    {
        return memberOf.stream()
            .map(UserRoleEntity::getRole)
            .collect(Collectors.toList());
    }

    /**
     * Return a DTO from this entity
     * @return
     */
    public UserInfo toUserInfo()
    {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setUsername(hdfsUsername);
        userInfo.setFullname(fullname);
        userInfo.setEmail(email);
        userInfo.setHdfsUsername(hdfsUsername);
        userInfo.setRegisteredAt(registeredAt);
        userInfo.setActive(active);
        userInfo.setRoles(getRoles());
        return userInfo;
    }
    
    /**
     * Return a {@link UserDetails} object needed for authentication
     * @return
     */
    public UserDetails toUserDetails()
    {
        return new SimpleUserDetails(getRoles(), username, password, active, blocked);
    }

    @Override
    public String toString()
    {
        return String.format(
            "UserEntity [id=%s, name=%s, active=%s, blocked=%s, email=%s, roles=%s]",
            id, username, active, blocked, email, getRoles());
    }
    
    
}