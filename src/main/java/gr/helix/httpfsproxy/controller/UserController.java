package gr.helix.httpfsproxy.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserController
{
    @GetMapping(path = {"/users/me"})
    public String me(Authentication authn, ModelMap model)
    {
        model.addAttribute("username", authn.getName());
        return "me";
    }
}
