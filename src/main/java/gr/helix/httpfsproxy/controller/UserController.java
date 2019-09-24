package gr.helix.httpfsproxy.controller;

import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Validated
public class UserController
{
    private final static Logger logger = LoggerFactory.getLogger(UserController.class);
    
    @GetMapping(path = {"/users/me"})
    public String me(@NotNull Authentication authn, ModelMap model)
    {
        logger.info("Generating user page for `{}`; principal={}", 
            authn.getName(), authn.getPrincipal());
        
        model.addAttribute("username", authn.getName());
        return "me";
    }
}
