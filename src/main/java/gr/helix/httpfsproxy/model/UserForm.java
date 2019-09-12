package gr.helix.httpfsproxy.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.constraints.Email;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.springframework.util.StringUtils;

@lombok.Getter
@lombok.Setter
@lombok.ToString(exclude = {"password"})
public class UserForm
{
    @Min(0)
    private Long id;
    
    @NotNull
    @Length(min = 3, max = 127)
    private String username;
    
    @NotNull
    @Length(min = 3, max = 127)
    private String fullname;
    
    @NotNull
    @Length(min = 3, max = 127)
    private String hdfsUsername;
    
    @NotNull
    @Email
    private String email;
    
    private boolean active = true;
    
    @NotNull
    private List<EnumRole> roles = Collections.singletonList(EnumRole.USER);
    
    @Length(min = 6, max = 127)
    private String password;
    
    public void setPassword(String password)
    {
        this.password = StringUtils.isEmpty(password)? null : password;
    }
    
    public UserInfo toUserInfo()
    {
        UserInfo userInfo = new UserInfo();
        userInfo.setId(id);
        userInfo.setUsername(username);
        userInfo.setHdfsUsername(hdfsUsername);
        userInfo.setFullname(fullname);
        userInfo.setEmail(email);
        userInfo.setRoles(roles);
        userInfo.setActive(active);
        return userInfo;
    }
    
    public void copyUserInfo(UserInfo userInfo)
    {
        this.id = userInfo.getId();
        this.username = userInfo.getUsername();
        this.hdfsUsername = userInfo.getHdfsUsername();
        this.email = userInfo.getEmail();
        this.fullname = userInfo.getFullname();
        this.roles = new ArrayList<>(userInfo.getRoles());
        this.active = userInfo.isActive();
    }
    
    public static UserForm from(UserInfo userInfo)
    {
        UserForm form = new UserForm();
        form.copyUserInfo(userInfo);
        return form;
    }
}
