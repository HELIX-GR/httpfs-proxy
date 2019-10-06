package gr.helix.httpfsproxy.config;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import com.google.common.net.InetAddresses;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter
{
    private static final Pattern CIDR_NETWORK_PATTERN = 
        Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/(\\d{1,2})");
    
    @Value("${gr.helix.httpfsproxy.admin.admin-password}")
    private String adminPassword;
    
    @Value("${gr.helix.httpfsproxy.admin.remote-addresses:127.0.0.1/8}")
    private String[] adminRemoteAddresses; 
        
    @Autowired
    private UserDetailsService userService;
    
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    private RequestMatcher csrfProtectionRequestMatcher = new RequestMatcher()
    {
        final Set<String> allowedMethods = new HashSet<>(Arrays.asList("GET", "OPTIONS", "HEAD", "TRACE"));
        
        final RequestMatcher excludePathMatcher = new OrRequestMatcher(
            new AntPathRequestMatcher("/f/**"), new AntPathRequestMatcher("/admin/**")
        );
        
        @Override
        public boolean matches(HttpServletRequest request)
        {
            return !allowedMethods.contains(request.getMethod()) && !excludePathMatcher.matches(request);
        }
    };
    
    @PostConstruct
    private void checkAdminRemoteAddresses()
    {
        for (String r: this.adminRemoteAddresses) {
            Matcher matcher = CIDR_NETWORK_PATTERN.matcher(r);
            if (!matcher.matches() || !InetAddresses.isInetAddress(matcher.group(1)) ||
                    Integer.parseUnsignedInt(matcher.group(2)) > 32) {
                throw new IllegalStateException("address is not a CIDR network: " + r);
            }
        }
    }
    
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
            .passwordEncoder(passwordEncoder);
        
        builder.eraseCredentials(true);
    }
    
    @Override
    protected void configure(HttpSecurity security) throws Exception
    {        
        final String userAccessExpression = "hasAuthority('USER')";
        
        final String adminAccessExpression = "hasAuthority('ADMIN') and " +
            Arrays.stream(adminRemoteAddresses).map(r -> "hasIpAddress(" + "'" + r + "'" + ")")
                .collect(Collectors.joining(" or ", "(", ")"));
        
        security.authorizeRequests()
            .antMatchers(
                    "/index", "/", "/about")
                .permitAll()
            .antMatchers(
                    "/users/me", "/files/**")
                .access(userAccessExpression)
            .antMatchers(
                    "/admin/**")
                .access(adminAccessExpression);
        
        security.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER);
        
        security.httpBasic()
            .realmName("HttpFS-Proxy");
        
        security.csrf()
            .requireCsrfProtectionMatcher(csrfProtectionRequestMatcher);
    }
}
