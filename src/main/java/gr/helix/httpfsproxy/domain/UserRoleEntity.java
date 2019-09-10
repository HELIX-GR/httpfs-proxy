package gr.helix.httpfsproxy.domain;

import java.util.Arrays;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

import gr.helix.httpfsproxy.model.EnumRole;

@Entity(name = "UserRole")
@Table(name = "`user_role`", schema = "public",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"`member`", "`role`"}, name = "unique_user_role_member_role")
    }
)
public class UserRoleEntity
{
    @Id
    @SequenceGenerator(sequenceName = "`user_role_id_seq`", name = "user_role_id_seq", allocationSize = 1)
    @GeneratedValue(generator = "user_role_id_seq", strategy = GenerationType.SEQUENCE)
    @Column(name = "`_id`")
    Long id;
    
    @ManyToOne
    @JoinColumn(name = "`member`", foreignKey = @ForeignKey(name = "fk_user_role_member"))
    UserEntity member;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "`role`", length = 24)
    EnumRole role;
    
    public UserRoleEntity() {}
    
    public static UserRoleEntity newMember(UserEntity u, EnumRole r) 
    {
        UserRoleEntity m = new UserRoleEntity();
        m.role = r;
        m.member = u;
        return m;
    }
    
    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public UserEntity getMember()
    {
        return member;
    }

    public void setMember(UserEntity member)
    {
        this.member = member;
    }

    public EnumRole getRole()
    {
        return role;
    }

    public void setRole(EnumRole role)
    {
        this.role = role;
    }
}
