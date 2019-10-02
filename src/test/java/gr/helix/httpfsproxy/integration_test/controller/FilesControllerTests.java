package gr.helix.httpfsproxy.integration_test.controller;

import static org.junit.Assert.*;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.restdocs.payload.SubsectionDescriptor;
import org.springframework.restdocs.request.ParameterDescriptor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
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
import static org.springframework.restdocs.operation.preprocess.Preprocessors.removeHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyParameters;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.headers.HeaderDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.*;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@RunWith(SpringRunner.class)
@ActiveProfiles("testing")
@SpringBootTest
@WebAppConfiguration
@TestPropertySource(locations = { "FilesControllerTests.properties" })
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
    
    private MockMvc mockmvc;
    
    @Value("${gr.helix.httpfsproxy.files-controller-tests.documentation.uri:https://httpfsproxy.example.net:8443/}")
    private URI documentationUri;
    
    @Value("${gr.helix.httpfsproxy.files-controller-tests.sample-text-files:temp/1.txt,temp/2.txt}")
    private List<String> sampleTextFiles;
    
    @Value("${gr.helix.httpfsproxy.files-controller-tests.sample-directory:temp}")
    private String sampleDirectory;
    
    private final FieldDescriptor restresponseStatusField = fieldWithPath("status")
        .type(JsonFieldType.STRING)
        .description("An overall status (`SUCCESS` or `FAILURE`)");
    
    private final FieldDescriptor restresponseResultField = subsectionWithPath("result")
        .type(JsonFieldType.OBJECT)
        .description("The request-specific result (on success)");
    
    private final FieldDescriptor restresponseErrorField = fieldWithPath("error")
        .type(JsonFieldType.VARIES)
        .description("An array of error messages, or `null` on success");
    
    private final ParameterDescriptor querystringPathParam = parameterWithName("path")
        .description("A user-relative or absolute path on the HDFS filesystem");
    
    private final FieldDescriptor fieldstatusLengthField = fieldWithPath("length")
        .type(JsonFieldType.NUMBER)
        .description("The length of a file (in bytes), or zero if a directory");
    
    private final FieldDescriptor fieldstatusTypeField = fieldWithPath("type")
        .type(JsonFieldType.STRING)
        .description("The type of this enty. One of: `FILE`, `DIRECTORY`, `SYMLINK`");
    
    private final FieldDescriptor fieldstatusPathField = fieldWithPath("pathSuffix")
        .type(JsonFieldType.STRING)
        .description("A target-relative path");
    
    private final FieldDescriptor fieldstatusPermissionField = fieldWithPath("permission")
        .type(JsonFieldType.STRING)
        .description("The octal permission, e.g `644`");
    
    private final FieldDescriptor fieldstatusBlockSizeField = fieldWithPath("blockSize")
        .type(JsonFieldType.NUMBER)
        .description("The block size (in bytes)");
    
    private final FieldDescriptor fieldstatusModificationTimeField = fieldWithPath("modificationTime")
        .type(JsonFieldType.NUMBER)
        .description("The timestamp of last modification (in Epoch milliseconds)");
    
    private final FieldDescriptor fieldstatusAccessTimeField = fieldWithPath("accessTime")
        .type(JsonFieldType.NUMBER)
        .description("The timestamp of last access (in Epoch milliseconds)");
    
    private final FieldDescriptor fieldstatusReplicationField = fieldWithPath("replication")
        .type(JsonFieldType.NUMBER)
        .description("The replication factor for a file, or zero if a directory");
    
    private final FieldDescriptor fieldstatusOwnerField = fieldWithPath("owner")
        .type(JsonFieldType.STRING)
        .description("The owning user");
    
    private final FieldDescriptor fieldstatusGroupField = fieldWithPath("group")
        .type(JsonFieldType.STRING)
        .description("The owning group");
    
    private static final String[] ignoredResponseHeaders = new String[] {
        "Pragma", "X-XSS-Protection", "X-Frame-Options", "X-Content-Type-Options",
        "Strict-Transport-Security", "Cache-Control", "Expires"
    };
    
    private static final String documentationSnippetNameTemplate = "{class-name}/{method-name}";
    
    @Autowired
    private ObjectMapper jsonMapper;
    
    @Autowired
    private SimpleUserDetails user1;
    
    @Before
    public void setup() throws Exception 
    {
        // Initialize mock MVC 
        
        this.mockmvc = MockMvcBuilders.webAppContextSetup(this.applicationContext)
            .apply(springSecurity()) // add Spring-Security filter chain
            .apply(documentationConfiguration(this.restDocumentation)
                .uris()
                    .withHost(documentationUri.getHost())
                    .withScheme(documentationUri.getScheme())
                    .withPort(documentationUri.getPort()))
            .build();
    }
   
    //
    // Helpers
    //
    
    private MvcResult getHomeDirectory() throws Exception
    {
        final FieldDescriptor resultPathField = fieldWithPath("path")
            .type(JsonFieldType.STRING)
            .description("The absolute path to the home directory");
        
        final MvcResult mvcresult = mockmvc
            .perform(get("/f/home-directory")
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.result.path")
                .value(String.format("/user/%s", user1.getUsernameForHdfs())))
            .andDo(document(documentationSnippetNameTemplate,
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders)),
                // Document response at a high level
                responseFields(
                    restresponseStatusField, restresponseErrorField, restresponseResultField),
                // Document nested `result` object for a /f/home-directory request
                responseFields(
                    beneathPath("result").withSubsectionId("result"), resultPathField) 
                ))
            .andReturn();
        
        return mvcresult;
    }
    
    private MvcResult getFileStatus(String filePath) throws Exception
    {
        FieldDescriptor resultStatusField = subsectionWithPath("status")
            .type(JsonFieldType.OBJECT)
            .description("An object holding the <<resources-filestatus,file status>> of target path");
        
        final MvcResult mvcresult = mockmvc
            .perform(get("/f/file/status").param("path", filePath)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.result.status").isNotEmpty())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders)),
                requestParameters(querystringPathParam),
                // Document response at a high level
                responseFields(
                    restresponseStatusField, restresponseErrorField, restresponseResultField),
                // Document nested `result` object for a /f/file/status request
                responseFields(
                    beneathPath("result").withSubsectionId("result"), resultStatusField),
                responseFields(
                    beneathPath("result.status").withSubsectionId("result-status"),
                    fieldstatusTypeField, 
                    fieldstatusLengthField, 
                    fieldstatusPathField,
                    fieldstatusPermissionField, 
                    fieldstatusBlockSizeField, 
                    fieldstatusReplicationField,
                    fieldstatusAccessTimeField, 
                    fieldstatusModificationTimeField,
                    fieldstatusOwnerField, 
                    fieldstatusGroupField)
                ))
            .andReturn();
        
        return mvcresult;
    }
    
    private MvcResult getFileChecksum(String filePath) throws Exception
    {
        FieldDescriptor resultChecksumField = fieldWithPath("checksum")
            .type(JsonFieldType.OBJECT)
            .description("An object holding the details on a computed cheksum");
        
        FieldDescriptor resultChecksumAlgorithmField = fieldWithPath("checksum.algorithm")
            .type(JsonFieldType.STRING)
            .description("The name of the checksum algorithm (an MD5 variation)");
        
        FieldDescriptor resultChecksumBytesField = fieldWithPath("checksum.bytes")
            .type(JsonFieldType.STRING)
            .description("The checksum as a hex-encoded string");
        
        FieldDescriptor resultChecksumLengthField = fieldWithPath("checksum.length")
            .type(JsonFieldType.NUMBER)
            .description("The length (in bytes) of the checksum");
        
        final MvcResult mvcresult = mockmvc
            .perform(get("/f/file/checksum").param("path", filePath)
                .with(user(user1)))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.result.checksum").isMap())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders)),
                requestParameters(querystringPathParam),
                // Document response at a high level
                responseFields(
                    restresponseStatusField, restresponseErrorField, restresponseResultField),
                // Document nested `result` object for a /f/file/checksum request
                responseFields(
                    beneathPath("result").withSubsectionId("result"), 
                    resultChecksumField,
                    resultChecksumAlgorithmField, 
                    resultChecksumBytesField, 
                    resultChecksumLengthField)
                ))
            .andReturn();
        
        return mvcresult;
    }
    
    private MvcResult listStatus(String filePath) throws Exception
    {
        FieldDescriptor resultStatusesField = subsectionWithPath("statuses")
            .type(JsonFieldType.ARRAY)
            .description("An array of <<resources-filestatus,file status>> objects");
        
        final MvcResult mvcresult = mockmvc
            .perform(get("/f/listing").param("path", filePath)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.result.statuses").isArray())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders)),
                requestParameters(querystringPathParam),
                // Document response at a high level
                responseFields(
                    restresponseStatusField, restresponseErrorField, restresponseResultField),
                // Document nested `result` object for a /f/listing request
                responseFields(
                    beneathPath("result").withSubsectionId("result"), resultStatusesField)
             ))
            .andReturn();
        
        return mvcresult;
    }
    
    private MvcResult getContentSummary(String filePath) throws Exception
    {
        FieldDescriptor resultSummaryField = fieldWithPath("summary")
            .type(JsonFieldType.OBJECT)
            .description("An object representing a usage summary of a subtree");
        
        FieldDescriptor resultSummaryDirectoryCountField = fieldWithPath("summary.directoryCount")
            .type(JsonFieldType.NUMBER)
            .description("The number of directories");
        
        FieldDescriptor resultSummaryFileCountField = fieldWithPath("summary.fileCount")
            .type(JsonFieldType.NUMBER)
            .description("The number of regular files");
        
        FieldDescriptor resultSummaryLengthField = fieldWithPath("summary.length")
            .type(JsonFieldType.NUMBER)
            .description("The number of bytes used by the content");
        
        FieldDescriptor resultSummaryQuotaField = fieldWithPath("summary.quota")
            .type(JsonFieldType.NUMBER)
            .description("The quota on the number of entries under this directory");
        
        FieldDescriptor resultSummarySpaceConsumedField = fieldWithPath("summary.spaceConsumed")
            .type(JsonFieldType.NUMBER)
            .description("The disk space consumed by the content");
        
        FieldDescriptor resultSummarySpaceQuotaField = fieldWithPath("summary.spaceQuota")
            .type(JsonFieldType.NUMBER)
            .description("The quota on the total disk space");
        
        final MvcResult mvcresult = mockmvc
            .perform(get("/f/file/summary").param("path", filePath)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value("SUCCESS"))
            .andExpect(jsonPath("$.error").isEmpty())
            .andExpect(jsonPath("$.result.summary").isMap())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders)),
                requestParameters(querystringPathParam),
                // Document response at a high level
                responseFields(
                    restresponseStatusField, restresponseErrorField, restresponseResultField),
                // Document nested `result` object for a /f/file/summary request
                responseFields(
                    beneathPath("result").withSubsectionId("result"),
                    resultSummaryField,
                    resultSummaryDirectoryCountField,
                    resultSummaryFileCountField,
                    resultSummaryLengthField,
                    resultSummaryQuotaField,
                    resultSummarySpaceConsumedField,
                    resultSummarySpaceQuotaField)
                ))    
            .andReturn();
        
        return mvcresult;
    }
    
    private MvcResult downloadFile(String filePath) throws Exception
    {
        final MvcResult mvcresult = mockmvc
            .perform(get("/f/file/content").param("path", filePath)
                .with(user(user1)))
            // Todo
            .andReturn();
        
        // Todo
        return mvcresult;
    }
    
    //
    // Tests
    //
    
    @Test
    public void testGetHomeDirectory() throws Exception
    {
        getHomeDirectory();
    }

    @Test
    public void testGetFileStatusX1() throws Exception
    {
        getFileStatus(sampleTextFiles.get(0));
    }
    
    @Test
    public void testGetFileChecksumX1() throws Exception
    {
        getFileChecksum(sampleTextFiles.get(0));
    }
    
    @Test
    public void testListStatus() throws Exception
    {
        listStatus(sampleDirectory);
    }
    
    @Test
    public void testGetContentSummary() throws Exception
    {
        getContentSummary(sampleDirectory);
    }
}
