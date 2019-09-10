package gr.helix.httpfsproxy.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import gr.helix.httpfsproxy.domain.UserEntity;
import gr.helix.httpfsproxy.repository.UserRepository;

@Service
public class DefaultUserService implements UserDetailsService
{    
    @Autowired
    UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String username) 
        throws UsernameNotFoundException
    {
        return userRepository.findByUsername(username)
            .map(UserEntity::toUserDetails)
            .orElseThrow(() -> new UsernameNotFoundException(username));
    }  
}
