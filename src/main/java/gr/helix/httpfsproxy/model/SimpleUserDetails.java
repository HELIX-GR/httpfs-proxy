package gr.helix.httpfsproxy.model;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.collect.Lists;

@lombok.Data
@lombok.Builder
@lombok.ToString(exclude = {"password"})
public class SimpleUserDetails implements UserDetails
{
    private static final long serialVersionUID = 1L;
    
    @lombok.NonNull
    private final String username;
    
    @lombok.NonNull
    private final List<EnumRole> roles;
    
    private final String usernameForHdfs;
    
    private String password;
    
    private final boolean enabled;
    
    private final boolean blocked;
    
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities()
    {
        return Lists.transform(roles, t -> new SimpleGrantedAuthority(t.name()));
    }

    @Override
    public boolean isAccountNonExpired()
    {
        return enabled;
    }

    @Override
    public boolean isAccountNonLocked()
    {
        return !blocked;
    }

    @Override
    public boolean isCredentialsNonExpired()
    {
        return enabled;
    }

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }
}
