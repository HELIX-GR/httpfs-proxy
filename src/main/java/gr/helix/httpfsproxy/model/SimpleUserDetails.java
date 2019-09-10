package gr.helix.httpfsproxy.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.collect.Lists;

public class SimpleUserDetails implements UserDetails
{
    private static final long serialVersionUID = 1L;
    
    private final List<EnumRole> roles;
    
    private final String username;
    
    private final String password;
    
    private final boolean active;
    
    private final boolean blocked;
    
    public SimpleUserDetails(
        List<EnumRole> roles, String username, String password, boolean active, boolean blocked)
    {
        this.roles = roles;
        this.username = username;
        this.password = password;
        this.active = active;
        this.blocked = blocked;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return Lists.transform(roles, t -> new SimpleGrantedAuthority(t.name()));
    }

    @Override
    public String getPassword()
    {
        return password;
    }

    @Override
    public String getUsername()
    {
        return username;
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return active;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return !blocked;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return active;
    }

    @Override
    public boolean isEnabled()
    {
        return active;
    }   

}
