package gr.helix.httpfsproxy.config;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


@Configuration
@EnableWebSecurity
public class SecurityConfiguration extends WebSecurityConfigurerAdapter
{
    @Autowired
    UserDetailsService userService;
    
    @Override
    public void configure(WebSecurity security) throws Exception
    {
        security.ignoring().antMatchers("/doc/**", "/css/**");
    }
     
    @Override
    protected void configure(AuthenticationManagerBuilder builder) throws Exception
    {
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
                    "/user/me", "/files/**")
                .access("hasAuthority('USER')")
            .antMatchers(
                    "/admin/**")
                .access("hasIpAddress('127.0.0.1/8')");
                //.access("hasAuthority('ADMIN') and hasIpAddress('127.0.0.1/8')");
        
        security.sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.NEVER);
        
        security.httpBasic()
            .realmName("HDFS storage");
        
        // Exclude certain requests from CSRF protection
        
        security.csrf().requireCsrfProtectionMatcher((HttpServletRequest req) -> {
            String method = req.getMethod();
            String servletPath = req.getServletPath();
            return  (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")) && 
                !servletPath.startsWith("/files/") &&
                !servletPath.startsWith("/admin/");
        });
    }
}
