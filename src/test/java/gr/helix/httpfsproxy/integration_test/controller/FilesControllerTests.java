package gr.helix.httpfsproxy.integration_test.controller;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.Random;

import org.apache.http.protocol.HTTP;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.model.SimpleUserDetails;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@RunWith(SpringRunner.class)
@ActiveProfiles("testing")
@SpringBootTest
@WebAppConfiguration
public class FilesControllerTests
{
    @TestConfiguration
    static class Setup
    {
        private final Random random = new Random();
        
        @Autowired
        HttpFsServiceConfiguration backend;
        
        @Bean
        Random random()
        {
            return this.random;
        }
        
        @Bean
        SimpleUserDetails user1()
        {
            return SimpleUserDetails.builder()
                .username("someone")
                .usernameForHdfs(backend.getDefaultUser())
                .build();
        }
    }
    
    @org.junit.Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Autowired
    private WebApplicationContext applicationContext;
    
    /** 
     * The entry point for server-side tests 
     */
    private MockMvc mockmvc;
    
    private final FieldDescriptor restresponseStatusField = fieldWithPath("status")
        .type(JsonFieldType.STRING)
        .description("An overall status (`SUCCESS` or `FAILURE`)");
    
    private final FieldDescriptor restresponseResultField = subsectionWithPath("result")
        .type(JsonFieldType.OBJECT)
        .description("The request-specific result (on success)");
    
    private final FieldDescriptor restresponseErrorField = fieldWithPath("error")
        .type(JsonFieldType.VARIES)
        .description("An array of error messages, or `null` on success");
    
    @Autowired
    private ObjectMapper jsonMapper;
    
    @Autowired
    private SimpleUserDetails user1;
    
    @Before
    public void setup() throws Exception 
    {
        // Initialize mock MVC 
        
        this.mockmvc = MockMvcBuilders.webAppContextSetup(this.applicationContext)
            .apply(springSecurity()) // also simulate Spring-Security filter chain
            .apply(documentationConfiguration(this.restDocumentation)
                .uris()
                    .withHost("c2-httpfsproxy.hellenicdataservice.gr")
                    .withScheme("https")
                    .withPort(443))
            .alwaysDo(document("{method-name}", 
                preprocessRequest(prettyPrint()), 
                preprocessResponse(prettyPrint())))
            .build();
    }
    
    @Test
    public void getHomeDirectory() throws Exception
    {
        final FieldDescriptor resultPathField = fieldWithPath("path")
            .type(JsonFieldType.STRING)
            .description("The absolute path to the home directory");
        
        final MvcResult mvcresult = mockmvc
            .perform(get("/files/home-directory").with(user(user1)))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.result.path")
                .value(String.format("/user/%s", user1.getUsernameForHdfs())))
            .andDo(document("{method-name}",
                // Document response at a high level
                responseFields(
                    restresponseStatusField, 
                    restresponseErrorField, 
                    restresponseResultField),
                // Document nested `result` object for a /files/home-directory request
                responseFields(
                    beneathPath("result").withSubsectionId("result"), 
                    resultPathField) 
                ))
            .andReturn();
        
        System.err.printf("%n ** GET %s: HTTP %s - Received: -- %n%s%n",
            mvcresult.getRequest().getServletPath(),
            mvcresult.getResponse().getStatus(), 
            mvcresult.getResponse().getContentAsString());
    }

}
