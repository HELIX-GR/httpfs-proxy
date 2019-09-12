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
import org.springframework.validation.annotation.Validated;
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
        final ModelAndView r = new ModelAndView();
        
        // Check
        if (userForm.getId() != null) {
            r.addObject("errorMessage", "Did not expect a user ID");
            r.setViewName("error-400");
            return r;
        } else if (bindingResult.hasErrors()) {
            r.setViewName("admin/user-edit");
            return r;
        }
        
        // Save to repository
        final UserEntity userEntity = userRepository.createFrom(userForm.toUserInfo());
        final long uid = userEntity.getId();
        logger.info("addUser(): Created user #{}: username={}", uid, userEntity.getUsername());
        
        // Update password (if a non-empty password is given)
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
        @PathVariable("userId") Long userId,
        @ModelAttribute("userForm") UserForm userForm)
    {
        final UserInfo userInfo = userRepository.findById(userId.longValue())
            .map(UserEntity::toUserInfo)
            .orElse(null);
        final ModelAndView r = new ModelAndView();
        if (userInfo == null) {
            r.setStatus(HttpStatus.NOT_FOUND);
            r.setViewName("/error-404");
        } else {
            userForm.copyUserInfo(userInfo);
            r.addObject("userId", userId);
            r.setViewName("admin/user-edit");
        }
        return r;
    }
    
    @PostMapping(path = {"/admin/users/{userId}/edit"})
    public ModelAndView editUser(
        @PathVariable("userId") Long userId,
        @ModelAttribute("userForm") @Valid UserForm userForm,
        BindingResult bindingResult,
        RedirectAttributes redirectAttrs)
    {
        final ModelAndView r = new ModelAndView();
        if (!userId.equals(userForm.getId())) {
            r.addObject("errorMessage", "Expected a specific user ID");
            r.setViewName("error-400");
            return r;
        } else if (bindingResult.hasErrors()) {
            r.setViewName("admin/user-edit");
            return r;
        }
        
        // Save to repository
        userRepository.updateFrom(userForm.toUserInfo());
        logger.info("editUser(): Updated info for user #{}", userId);
        
        // Update password (if a non-empty password is given)
        if (!StringUtils.isEmpty(userForm.getPassword())) {
            userRepository.updatePassword(userId, passwordEncoder.encode(userForm.getPassword()));
            logger.info("addUser(): Updated password for user #{}", userId);
        }
        
        // Redirect 
        redirectAttrs.addFlashAttribute("infoMessage",
            String.format("The user `%s` is updated successfully!", userForm.getUsername()));
        r.setViewName("redirect:/admin/users");
        return r;
    }
    
    @GetMapping(path = {"/admin/users/{userId}/delete"})
    public ModelAndView showUserFormForDelete(
        @PathVariable("userId") Long userId,
        @ModelAttribute("userForm") UserForm userForm)
    {
        UserInfo userInfo = userRepository.findById(userId.longValue())
            .map(UserEntity::toUserInfo)
            .orElse(null);
        ModelAndView r = new ModelAndView();
        if (userInfo == null) {
            r.setStatus(HttpStatus.NOT_FOUND);
            r.setViewName("/error-404");
        } else {
            userForm.copyUserInfo(userInfo);
            r.addObject("userId", userId);
            r.setViewName("/admin/user-delete");
        }
        return r;
    }
    
    @PostMapping(path = {"/admin/users/{userId}/delete"})
    public ModelAndView deleteUser(
        @PathVariable("userId") Long userId,
        RedirectAttributes redirectAttrs)
    {
        UserInfo userInfo = userRepository.findById(userId.longValue())
            .map(UserEntity::toUserInfo)
            .orElse(null);
        ModelAndView r = new ModelAndView();
        if (userInfo == null) {
            r.setStatus(HttpStatus.NOT_FOUND);
            r.setViewName("/error-404");
        }
        
        // Delete from repository
        userRepository.deleteById(userId);
        
        // Redirect
        redirectAttrs.addFlashAttribute("infoMessage",
            String.format("The user `%s` is deleted", userInfo.getUsername()));
        r.setViewName("redirect:/admin/users");
        return r;
    }
}
