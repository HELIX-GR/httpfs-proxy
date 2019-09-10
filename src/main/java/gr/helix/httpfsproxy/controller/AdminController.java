package gr.helix.httpfsproxy.controller;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.config.HttpFsServiceProperties;
import gr.helix.httpfsproxy.domain.UserEntity;
import gr.helix.httpfsproxy.model.UserForm;
import gr.helix.httpfsproxy.model.UserInfo;
import gr.helix.httpfsproxy.repository.UserRepository;

@Controller
public class AdminController
{
    private final static Logger logger = LoggerFactory.getLogger(AdminController.class);
    
    private final static BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    
    @Autowired
    private HttpFsServiceProperties httpfsServiceProperties;
    
    @Autowired
    private UserRepository userRepository;
    
    static class ServiceInfo
    {
        URI uri;
        
        boolean operational;
        
        public ServiceInfo(URI uri, boolean operational)
        {
            this.uri = uri;
            this.operational = operational;
        }
        
        @JsonProperty
        public URI getUri()
        {
            return uri;
        }
        
        @JsonProperty
        public boolean isOperational()
        {
            return operational;
        }
    }

    @GetMapping(path = {"/admin/", "/admin/index"})
    public String index(ModelMap model)
    {
        // Todo check service status
        
        model.addAttribute("backendServices", Collections.singletonList(
            new ServiceInfo(httpfsServiceProperties.getBaseUri(), true)));
        
        return "admin/index";
    }
    
    @GetMapping(path = {"/admin/status"})
    public String status(ModelMap model)
    {
        return "admin/status";
    }
    
    @GetMapping(path = {"/admin/users"})
    public String showUsers(ModelMap model, 
        @RequestParam(name = "page", defaultValue = "0") Integer pageNumber,
        @RequestParam(name = "size", defaultValue = "12") Integer pageSize)
    {
        final PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
        List<UserInfo> users = userRepository.findAll(pageRequest).stream()
            .collect(Collectors.mapping(UserEntity::toUserInfo, Collectors.toList()));
        
        model.addAttribute("users", users);
        
        // Todo Add pagination
        return "admin/users";
    }
    
    @GetMapping(path = {"/admin/users/new"})
    public String showUserForm(
        @ModelAttribute("userForm") UserForm userForm)
    {
        return "admin/user-edit";
    }
    
    @PostMapping(path = {"/admin/users/new"})
    public ModelAndView addUser(
        @ModelAttribute("userForm") @Valid UserForm userForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttrs)
    {
        logger.debug("addUser(): Got {}", userForm);
        
        final ModelAndView r = new ModelAndView();
        if (bindingResult.hasErrors()) {
            r.setViewName("admin/user-edit");
            return r;
        }
        
        // Save to repository
        final UserInfo userInfo = userForm.toUserInfo();
        final UserEntity userEntity = userRepository.createWith(userInfo);
        final long uid = userEntity.getId();
        logger.info("addUser(): Created user #{}: username={} ", uid, userEntity.getUsername());
        
        // Update password
        if (!StringUtils.isEmpty(userForm.getPassword())) {
            userRepository.updatePassword(uid, passwordEncoder.encode(userForm.getPassword()));
            logger.info("addUser(): Updated password for user #{}", uid);
        }
        
        // Redirect
        redirectAttrs.addFlashAttribute("infoMessage",
            String.format("The user `%s` is created successfully!", userForm.getUsername()));
        r.setViewName("redirect:/admin/users");
        return r;
    }
    
    @GetMapping(path = {"/admin/users/{userId}/edit"})
    public ModelAndView showUserForm(
        @PathVariable("userId") Integer userId,
        @ModelAttribute("userForm") UserForm userForm)
    {
        final UserInfo user = userRepository.findById(userId.longValue())
            .map(UserEntity::toUserInfo)
            .orElse(null);
        final ModelAndView r = new ModelAndView();
        if (user == null) {
            r.setStatus(HttpStatus.NOT_FOUND);
            r.setViewName("/error-404");
        } else {
            // Todo set fields for userForm
            r.addObject("userId", userId);
            r.setViewName("/admin/user-edit");
        }
        return null;
    }
    
    @PostMapping(path = {"/admin/users/{userId}/edit"})
    public String editUser(
        ModelMap model, @PathVariable("userId") Integer userId)
    {
        return "redirect:/admin/users";
    }
    
    @GetMapping(path = {"/admin/users/{userId}/delete"})
    public ModelAndView showUserDeleteForm(
        @PathVariable("userId") Integer userId)
    {
        UserInfo user = userRepository.findById(userId.longValue())
            .map(UserEntity::toUserInfo)
            .orElse(null);
        ModelAndView r = new ModelAndView();
        if (user == null) {
            r.setStatus(HttpStatus.NOT_FOUND);
            r.setViewName("/error-404");
        } else {
            r.addObject("user", user);
            r.setViewName("/admin/user-delete");
        }
        return r;
    }
    
    @PostMapping(path = {"/admin/users/{userId}/delete"})
    public String deleteUser(
        ModelMap model, @PathVariable("userId") Integer userId)
    {
        return "redirect:/admin/users";
    }
    
}
