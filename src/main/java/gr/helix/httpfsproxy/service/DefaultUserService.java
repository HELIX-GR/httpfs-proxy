package gr.helix.httpfsproxy.service;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import gr.helix.httpfsproxy.domain.UserEntity;
import gr.helix.httpfsproxy.repository.UserRepository;

@Service
public class DefaultUserService implements UserDetailsService
{    
    @Autowired
    UserRepository userRepository;
    
    private UserDetails extractUserDetails(final UserEntity user)
    {
        final String username = user.getName();
        final String password = user.getPassword();
        
        final boolean active = user.isActive();
        final boolean blocked = user.isBlocked();
        
        final List<SimpleGrantedAuthority> roles = user.getRoles().stream()
            .map(r -> new SimpleGrantedAuthority(r.name()))
            .collect(Collectors.toList());
        
        return new UserDetails()
        {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isEnabled()
            {
                return active;
            }
            
            @Override
            public boolean isCredentialsNonExpired()
            {
                return active;
            }
            
            @Override
            public boolean isAccountNonLocked()
            {
                return !blocked;
            }
            
            @Override
            public boolean isAccountNonExpired()
            {
                return active;
            }
            
            @Override
            public String getUsername()
            {
                return username;
            }
            
            @Override
            public String getPassword()
            {
                return password;
            }
            
            @Override
            public Collection<? extends GrantedAuthority> getAuthorities()
            {
                return roles;
            }
        };
    }
    
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException
    {
        return userRepository.findByUsername(username)
            .map(this::extractUserDetails)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }  
}
