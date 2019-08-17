package gr.helix.httpfsproxy.domain;

import java.time.ZonedDateTime;
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

import gr.helix.httpfsproxy.model.EnumRole;
import gr.helix.httpfsproxy.model.UserInfo;

@Entity(name = "User")
@Table(name = "`user`", schema = "public")
public class UserEntity
{  
    @Id    
    @SequenceGenerator(sequenceName = "user_id_seq", name = "user_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "user_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "`_id`")
    Long id;
    
    @NotNull
    @Column(name = "`name`", unique = true)
    String name;
     
    @Column(name = "`fullname`")
    String fullname;
 
    @Column(name = "`password`")
    String password;
    
    @Column(name = "`active`")
    Boolean isActive = true;
    
    @Column(name = "`blocked`")
    Boolean isBlocked = false;
    
    @Column(name = "`registered_at`")
    ZonedDateTime registeredAt;
 
    @Column(name = "`token`", nullable = true)
    String token;
    
    @Column(name = "`email`", nullable = false)
    String email;
    
    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    Set<UserRoleEntity> memberOf;
    
    protected UserEntity() {}
    
    public UserEntity(String name, ZonedDateTime registeredAt) 
    {
        this.name = name;
        this.registeredAt = registeredAt;
    }
    
    public UserEntity(UserInfo userinfo, ZonedDateTime registeredAt) 
    {
        this.name = userinfo.getUserName();
        this.fullname = userinfo.getFullname();
        this.email = userinfo.getEmail();
        this.registeredAt = registeredAt;
    }
    
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFullname()
    {
        return fullname;
    }

    public void setFullname(String fullname)
    {
        this.fullname = fullname;
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
        return isActive;
    }

    public void setActive(Boolean isActive)
    {
        this.isActive = isActive;
    }

    public Boolean isBlocked()
    {
        return isBlocked;
    }

    public void setBlocked(Boolean isBlocked)
    {
        this.isBlocked = isBlocked;
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
        UserInfo u = new UserInfo(id, name, fullname, email);
        u.setRegisteredAt(registeredAt);
        return u;
    }

    @Override
    public String toString()
    {
        return String.format(
            "UserEntity " +
                "[id=%s, name=%s, fullname=%s, isActive=%s, isBlocked=%s, " +
                "registeredAt=%s, token=%s, email=%s, roles=%s]",
            id, name, fullname, isActive, isBlocked, registeredAt, token, email, getRoles());
    }
    
    
}