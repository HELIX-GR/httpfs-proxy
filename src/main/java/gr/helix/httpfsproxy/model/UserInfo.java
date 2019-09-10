package gr.helix.httpfsproxy.model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@lombok.Getter
@lombok.Setter
@lombok.ToString
public class UserInfo
{
    private Long id;
    
    private String username;
    
    private String fullname;
    
    private String hdfsUsername;
    
    private ZonedDateTime registeredAt;
    
    private String email;
    
    private boolean active;
    
    private List<EnumRole> roles = Collections.emptyList();
    
    public void setRoles(Collection<EnumRole> roles)
    {
        this.roles = Collections.unmodifiableList(new ArrayList<>(roles));
    }
}