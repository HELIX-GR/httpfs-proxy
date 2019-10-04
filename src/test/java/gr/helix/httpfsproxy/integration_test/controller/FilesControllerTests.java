package gr.helix.httpfsproxy.integration_test.controller;

import static org.junit.Assert.*;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

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
import org.springframework.http.MediaType;
import org.springframework.restdocs.JUnitRestDocumentation;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.restdocs.operation.OperationRequest;
import org.springframework.restdocs.operation.OperationRequestFactory;
import org.springframework.restdocs.operation.OperationResponse;
import org.springframework.restdocs.operation.Parameters;
import org.springframework.restdocs.operation.preprocess.ContentModifier;
import org.springframework.restdocs.operation.preprocess.ContentModifyingOperationPreprocessor;
import org.springframework.restdocs.operation.preprocess.OperationPreprocessor;
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
import org.springframework.util.StringUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.model.SimpleUserDetails;

import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.*;
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
        
        @Bean
        String tempDir()
        {
            return String.format("temp/%d/", Instant.now().toEpochMilli());
        }
        
        @Bean
        String sampleTextData()
        {
            return "Hello Hadoop!";
        }
    }
    
    @org.junit.Rule
    public JUnitRestDocumentation restDocumentation = new JUnitRestDocumentation("target/generated-snippets");

    @Autowired
    private WebApplicationContext applicationContext;
    
    private MockMvc mockmvc;
    
    @Value("${gr.helix.httpfsproxy.files-controller-tests.documentation.uri:https://httpfsproxy.example.net:8443/}")
    private URI documentationUri;
    
    @Autowired
    private String tempDir;
    
    @Autowired
    private String sampleTextData;
    
    private String sampleTextFile;
    
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
     
    private final OperationPreprocessor ignoreParameters = new OperationPreprocessor()
    {

        @Override
        public OperationRequest preprocess(OperationRequest request)
        {
            return new OperationRequestFactory().createFrom(request, new Parameters());
        }

        @Override
        public OperationResponse preprocess(OperationResponse response)
        {
            return response;
        }
    };
    
    private static final String[] ignoredResponseHeaders = new String[] {
        "Pragma", "X-XSS-Protection", "X-Frame-Options", "X-Content-Type-Options",
        "Strict-Transport-Security", "Cache-Control", "Expires"
    };
    
    private static final String documentationSnippetNameTemplate = "{ClassName}/{methodName}";
    
    @Autowired
    private ObjectMapper jsonMapper;
    
    @Autowired
    private SimpleUserDetails user1;
    
    @Before
    public void setup() throws Exception 
    {
        this.sampleTextFile = StringUtils.applyRelativePath(tempDir, "hello.txt");
        
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
    
    private MvcResult getHomeDirectoryAndThenDocument() throws Exception
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
    
    private MvcResult getFileStatusAndThenDocument(String filePath) throws Exception
    {
        FieldDescriptor resultStatusField = subsectionWithPath("status")
            .type(JsonFieldType.OBJECT)
            .description("An object holding the <<resources-filestatus,file status>> of target path");
        
        String uri = UriComponentsBuilder.fromPath("/f/file/status")
            .queryParam("path", filePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(get(uri)
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
    
    private MvcResult getFileChecksumAndThenDocument(String filePath) throws Exception
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
        
        String uri = UriComponentsBuilder.fromPath("/f/file/checksum")
            .queryParam("path", filePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(get(uri)
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
    
    private MvcResult listStatusAndThenDocument(String filePath) throws Exception
    {
        FieldDescriptor resultStatusesField = subsectionWithPath("statuses")
            .type(JsonFieldType.ARRAY)
            .description("An array of <<resources-filestatus,file status>> objects");
        
        String uri = UriComponentsBuilder.fromPath("/f/listing")
            .queryParam("path", filePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(get(uri)
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
    
    private MvcResult getContentSummaryAndThenDocument(String filePath) throws Exception
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
        
        String uri = UriComponentsBuilder.fromPath("/f/file/summary")
            .queryParam("path", filePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(get(uri)
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
    
    private MvcResult downloadFileAndThenDocument(String filePath) throws Exception
    {
        ParameterDescriptor querystringLengthParam = parameterWithName("length")
            .optional()
            .description("The number of bytes to be returned");
        
        ParameterDescriptor querystringOffsetParam = parameterWithName("offset")
            .optional()
            .description("The starting byte position");
        
        String uri = UriComponentsBuilder.fromPath("/f/file/content")
            .queryParam("path", filePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(get(uri)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM))
            .andDo(document(documentationSnippetNameTemplate,
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders)),
                requestParameters(querystringPathParam, querystringLengthParam, querystringOffsetParam)
            ))
            .andReturn();
        
        return mvcresult;
    }
    
    private MvcResult createDirectoryAndThenDocument(String filePath) throws Exception
    {
        ParameterDescriptor querystringPermissionParam = parameterWithName("permission")
            .optional()
            .description("The octal permission for this directory (default is `775`)");
        
        String uri = UriComponentsBuilder.fromPath("/f/directory")
            .queryParam("path", filePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(put(uri) 
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isCreated())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
        
        return mvcresult; 
    }
    
    private MvcResult createDirectory(String filePath) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/directory")
            .queryParam("path", filePath)
            .toUriString();
        return mockmvc.perform(put(uri).with(user(user1))).andReturn();
    }
    
    private MvcResult renameAndThenDocument(String filePath, String destinationFilePath) throws Exception
    {
        ParameterDescriptor querystringDestinationParam = parameterWithName("destination")
            .optional()
            .description("The new name for the file");
        
        String uri = UriComponentsBuilder.fromPath("/f/name")
            .queryParam("path", filePath)
            .queryParam("destination", destinationFilePath)
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(put(uri) 
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isCreated())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
        
        return mvcresult; 
    }
    
    private MvcResult uploadTextDataAndThenDocument(String filePath, String textData) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/content")
            .queryParam("path", filePath)
            .queryParam("overwrite", "true")
            .toUriString();
        
        final MvcResult mvcresult = mockmvc
            .perform(put(uri)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(textData.getBytes())
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isCreated())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
        
        return mvcresult; 
    }
    
    private MvcResult uploadTextData(String filePath, String textData) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/content")
            .queryParam("path", filePath)
            .queryParam("overwrite", "true")
            .toUriString();
        
        return mockmvc
            .perform(put(uri)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(textData.getBytes())
                .with(user(user1)))
            .andReturn();
    }
    
    private MvcResult appendTextDataAndThenDocument(String filePath, String textData) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/content")
            .queryParam("path", filePath)
            .toUriString();
        
        return mockmvc
            .perform(post(uri)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .content(textData.getBytes())
                .with(user(user1)))
            .andDo(print())
            .andExpect(status().isCreated())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
    }
    
    private MvcResult concatenateFilesAndDocument(String filePath, String ...sourceNames) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/content")
            .queryParam("path", filePath)
            .queryParam("sources", String.join(",", sourceNames))
            .toUriString();
        
        return mockmvc
            .perform(post(uri)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isCreated())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
    }
    
    private MvcResult truncateFileAndThenDocument(String filePath) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/content")
            .queryParam("path", filePath)
            .toUriString();
        
        return mockmvc
            .perform(delete(uri)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
    }
    
    private MvcResult deleteFileAndThenDocument(String filePath) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file")
            .queryParam("path", filePath)
            .toUriString();
        
        return mockmvc
            .perform(delete(uri)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
    }
    
    private MvcResult setPermissionAndThenDocument(String filePath, String permission) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/permission")
            .queryParam("path", filePath)
            .queryParam("permission", permission)
            .toUriString();
        
        return mockmvc
            .perform(put(uri)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
    }
    
    private MvcResult setReplicationAndThenDocument(String filePath, int replication) throws Exception
    {
        String uri = UriComponentsBuilder.fromPath("/f/file/replication")
            .queryParam("path", filePath)
            .queryParam("replication", replication)
            .toUriString();
        
        return mockmvc
            .perform(put(uri)
                .with(user(user1)))
            //.andDo(print())
            .andExpect(status().isNoContent())
            .andDo(document(documentationSnippetNameTemplate,
                preprocessRequest(ignoreParameters),
                preprocessResponse(prettyPrint(), removeHeaders(ignoredResponseHeaders))
            ))
            .andReturn();
    }
    
    //
    // Tests
    //
        
    @Test
    public void testGetHomeDirectory() throws Exception
    {
        getHomeDirectoryAndThenDocument();
    }
    
    @Test
    public void testCreateTempDirectory() throws Exception
    {
        createDirectoryAndThenDocument(tempDir);
    }
    
    @Test
    public void testUploadFileInTempDirectory() throws Exception
    {
        createDirectory(tempDir);
        uploadTextDataAndThenDocument(sampleTextFile, sampleTextData);
    }
    
    @Test
    public void testGetFileStatus() throws Exception
    {
        uploadTextData(sampleTextFile, sampleTextData);
        getFileStatusAndThenDocument(sampleTextFile);
    }
    
    @Test
    public void testGetFileChecksum() throws Exception
    {
        uploadTextData(sampleTextFile, sampleTextData);
        getFileChecksumAndThenDocument(sampleTextFile);
    }
    
    @Test
    public void testListStatus() throws Exception
    {
        createDirectory(StringUtils.applyRelativePath(tempDir, "sub1"));
        listStatusAndThenDocument(tempDir);
    }
    
    @Test
    public void testGetContentSummary() throws Exception
    {
        getContentSummaryAndThenDocument(tempDir);
    }
    
    @Test
    public void testDownloadFile() throws Exception
    {
        uploadTextData(sampleTextFile, sampleTextData);
        downloadFileAndThenDocument(sampleTextFile);
    }
        
    @Test
    public void testRenameDirectory() throws Exception
    {
        String dirPath = StringUtils.applyRelativePath(tempDir, "foo");
        createDirectory(dirPath);
        renameAndThenDocument(dirPath, StringUtils.applyRelativePath(tempDir, "baz"));
    }
    
    @Test
    public void testAppendTextToFile() throws Exception
    {
        String targetName = "hello-and-then-goodbye.txt";
        String path = StringUtils.applyRelativePath(tempDir, targetName);
        uploadTextData(path, "I say hello!");
        appendTextDataAndThenDocument(path, "You say goodbye!");
    }
    
    @Test
    public void testConcatenateTextSourcesToFile() throws Exception
    {
        String targetName = "hello-goodbye.txt";
        String sourceName1 = "hello-part.txt";
        String sourceName2 = "goodbye-part.txt";
        String path = StringUtils.applyRelativePath(tempDir, targetName);
        String source1 = StringUtils.applyRelativePath(tempDir, sourceName1);
        String source2 = StringUtils.applyRelativePath(tempDir, sourceName2);
        uploadTextData(path, "You say yes, i say no!\n");
        uploadTextData(source1, "I say hello!");
        uploadTextData(source2, "And you say goodbye!");
        concatenateFilesAndDocument(path, sourceName1, sourceName2);
    }
    
    @Test
    public void testTruncateFile() throws Exception
    {
        String path = StringUtils.applyRelativePath(tempDir, "hello.txt");
        uploadTextData(path, "I say hello!");
        truncateFileAndThenDocument(path);
    }
    
    @Test
    public void testDeleteFile() throws Exception
    {
        String path = StringUtils.applyRelativePath(tempDir, "hello.txt");
        uploadTextData(path, "I say hello!");
        deleteFileAndThenDocument(path);
    }
    
    @Test
    public void testSetPermission() throws Exception
    {
        String path = StringUtils.applyRelativePath(tempDir, "hello.txt");
        uploadTextData(path, "I say hello!");
        setPermissionAndThenDocument(path, "640");
    }
    
    @Test
    public void testSetReplication() throws Exception
    {
        String path = StringUtils.applyRelativePath(tempDir, "hello.txt");
        uploadTextData(path, "I say hello!");
        setReplicationAndThenDocument(path, 3);
    }
}
