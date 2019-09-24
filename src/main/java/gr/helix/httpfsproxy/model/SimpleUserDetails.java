package gr.helix.httpfsproxy.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.google.common.collect.Lists;

import lombok.AccessLevel;

@lombok.Getter
@lombok.Builder
public class SimpleUserDetails implements UserDetails
{
    private static final long serialVersionUID = 1L;
    
    @lombok.NonNull
    private final String username;
    
    @lombok.NonNull
    @lombok.Builder.Default
    private final List<EnumRole> roles = Collections.singletonList(EnumRole.USER);
    
    private final String usernameForHdfs;
    
    private String password;
    
    @lombok.Builder.Default
    private final boolean enabled = true;
    
    @lombok.Builder.Default
    private final boolean blocked = false;
    
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
