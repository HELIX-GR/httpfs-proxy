package gr.helix.httpfsproxy.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.NoOpPasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter
{
    @Value("${gr.helix.httpfsproxy.admin.admin-password}")
    String adminPassword;
    
    @Autowired
    UserDetailsService userService;
    
    private RequestMatcher csrfProtectionRequestMatcher = new RequestMatcher()
    {
        final Set<String> allowedMethods = new HashSet<>(Arrays.asList("GET", "OPTIONS", "HEAD", "TRACE"));
        
        final RequestMatcher excludePathMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/f/**"), new AntPathRequestMatcher("/admin/**"));
        
        @Override
        public boolean matches(HttpServletRequest request)
        {
            return !allowedMethods.contains(request.getMethod()) && !excludePathMatcher.matches(request);
        }
    };
    
    @Override
    public void configure(WebSecurity security) throws Exception
    {
        security.ignoring().antMatchers("/doc/**", "/css/**", "/js/**");
    }
     
    @Override
    protected void configure(AuthenticationManagerBuilder builder) throws Exception
    {
        builder.inMemoryAuthentication()
            .withUser("admin")
                .authorities("ADMIN")
                .password("{noop}" + adminPassword);
        
        builder.userDetailsService(userService)
            .passwordEncoder(new BCryptPasswordEncoder());
        
        builder.eraseCredentials(true);
    }
    
    @Override
    protected void configure(HttpSecurity security) throws Exception
    {        
        security.authorizeRequests()
            .antMatchers(
                    "/index", "/", "/about")
                .permitAll()
            .antMatchers(
                    "/users/me", "/files/**")
                .access("hasAuthority('USER')")
            .antMatchers(
                    "/admin/**")
                .access("hasAuthority('ADMIN') and hasIpAddress('127.0.0.1/8')");
        
        security.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER);
        
        security.httpBasic()
            .realmName("HttpFS-Proxy");
        
        security.csrf()
            .requireCsrfProtectionMatcher(csrfProtectionRequestMatcher);
    }
}
