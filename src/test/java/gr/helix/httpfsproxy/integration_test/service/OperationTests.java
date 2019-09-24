package gr.helix.httpfsproxy.integration_test.service;

import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.integration_test.TextAsResourceFixture;
import gr.helix.httpfsproxy.integration_test.TextAsStringFixture;
import gr.helix.httpfsproxy.model.ops.AppendToFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.ConcatenateFilesRequestParameters;
import gr.helix.httpfsproxy.model.ops.ContentSummary;
import gr.helix.httpfsproxy.model.ops.ContentSummaryResponse;
import gr.helix.httpfsproxy.model.ops.CreateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.DeleteFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.FileChecksum;
import gr.helix.httpfsproxy.model.ops.FileStatus;
import gr.helix.httpfsproxy.model.ops.GetFileChecksumResponse;
import gr.helix.httpfsproxy.model.ops.GetFileStatusResponse;
import gr.helix.httpfsproxy.model.ops.GetHomeDirectoryResponse;
import gr.helix.httpfsproxy.model.ops.ListStatusResponse;
import gr.helix.httpfsproxy.model.ops.MakeDirectoryRequestParameters;
import gr.helix.httpfsproxy.model.ops.ReadFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.SetOwnerRequestParameters;
import gr.helix.httpfsproxy.model.ops.SetPermissionRequestParameters;
import gr.helix.httpfsproxy.model.ops.SetReplicationRequestParameters;
import gr.helix.httpfsproxy.model.ops.TruncateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.OperationTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"testing"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OperationTests
{
    @TestConfiguration
    static class Setup
    {
        private final Random random = new Random();
        
        @Bean("tempDir")
        String tempDir()
        {
            return String.format("temp/httpfsproxy-tests-%09d/", random.nextInt(10000000));
        }
        
        @Bean
        Random random()
        {
            return this.random;
        }
        
        @Bean
        TextAsStringFixture fixture01(
            @Value("classpath:samples/text/Lorem-Ipsum-p1.txt") Resource textResource,
            @Value("classpath:samples/text/Lorem-Ipsum-p1.txt.crc") Resource checksumResource)
                throws IOException
        {
            return TextAsStringFixture.from(textResource, checksumResource);
        }
        
        @Bean
        TextAsStringFixture fixture02(
            @Value("classpath:samples/text/Lorem-Ipsum-p2.txt") Resource textResource,
            @Value("classpath:samples/text/Lorem-Ipsum-p2.txt.crc") Resource checksumResource)
                throws IOException
        {
            return TextAsStringFixture.from(textResource, checksumResource);
        }
        
        @Bean
        TextAsResourceFixture fixture03(
            @Value("classpath:samples/text/Aesop.txt") Resource textResource,
            @Value("classpath:samples/text/Aesop.txt.crc") Resource checksumResource)
                throws IOException
        {
            return TextAsResourceFixture.from(textResource, checksumResource);
        }
        
        // This is the text resource produced by the concatenation of fixture01 and fixture02
        @Bean
        TextAsStringFixture fixture01plus02(
            @Value("classpath:samples/text/Lorem-Ipsum.txt") Resource textResource,
            @Value("classpath:samples/text/Lorem-Ipsum.txt.crc") Resource checksumResource)
                throws IOException
        {
            return TextAsStringFixture.from(textResource, checksumResource);
        }
    }
    
    static final String DEFAULT_FILE_PERMISSION = "664";
    
    static final String RESTRICTIVE_FILE_PERMISSION = "640";
    
    static final String DEFAULT_DIRECTORY_PERMISSION = "775";
    
    static final int DEFAULT_REPLICATION = 2;
    
    @Autowired
    Random random;
    
    @Autowired
    HttpFsServiceConfiguration backend;
    
    @Autowired
    AsyncTaskExecutor taskExecutor;
    
    @Autowired
    @Qualifier("httpClient")
    private CloseableHttpClient httpClient;
    
    @Autowired
    @Qualifier("tempDir")
    private String tempDir;
    
    @Autowired
    @Qualifier("getHomeDirectoryTemplate")
    private OperationTemplate<VoidRequestParameters, GetHomeDirectoryResponse> getHomeDirectoryTemplate;
    
    @Autowired
    @Qualifier("listStatusTemplate")
    private OperationTemplate<VoidRequestParameters, ListStatusResponse > listStatusTemplate;
    
    @Autowired
    @Qualifier("getFileStatusTemplate")
    private OperationTemplate<VoidRequestParameters, GetFileStatusResponse> getFileStatusTemplate;
    
    @Autowired
    @Qualifier("makeDirectoryTemplate")
    private OperationTemplate<MakeDirectoryRequestParameters, BooleanResponse> makeDirectoryTemplate;
    
    @Autowired
    @Qualifier("createFileTemplate")
    private OperationTemplate<CreateFileRequestParameters, Void> createFileTemplate;
    
    @Autowired
    @Qualifier("appendToFileTemplate")
    private OperationTemplate<AppendToFileRequestParameters, Void> appendToFileTemplate;
    
    @Autowired
    @Qualifier("truncateFileTemplate")
    private OperationTemplate<TruncateFileRequestParameters, BooleanResponse> truncateFileTemplate;
    
    @Autowired
    @Qualifier("getFileChecksumTemplate")
    private OperationTemplate<VoidRequestParameters, GetFileChecksumResponse> getFileChecksumTemplate;
    
    @Autowired
    @Qualifier("readFileTemplate")
    private OperationTemplate<ReadFileRequestParameters, ?> readFileTemplate;
    
    @Autowired
    @Qualifier("concatenateFilesTemplate")
    private OperationTemplate<ConcatenateFilesRequestParameters, Void> concatenateFilesTemplate;
    
    @Autowired
    @Qualifier("deleteFileTemplate")
    private OperationTemplate<DeleteFileRequestParameters, BooleanResponse> deleteFileTemplate;
    
    @Autowired
    @Qualifier("getContentSummaryTemplate")
    private OperationTemplate<VoidRequestParameters, ContentSummaryResponse> getContentSummaryTemplate;
    
    @Autowired
    @Qualifier("setPermissionTemplate")
    private OperationTemplate<SetPermissionRequestParameters, Void> setPermissionTemplate;
    
    @Autowired
    @Qualifier("setReplicationTemplate")
    private OperationTemplate<SetReplicationRequestParameters, BooleanResponse> setReplicationTemplate;
    
    @Autowired
    @Qualifier("setOwnerTemplate")
    private OperationTemplate<SetOwnerRequestParameters, Void> setOwnerTemplate;
    
    @Autowired
    private TextAsStringFixture fixture01;
    
    @Autowired
    private TextAsStringFixture fixture02;
    
    @Autowired
    private TextAsStringFixture fixture01plus02;
    
    @Autowired
    private TextAsResourceFixture fixture03;
    
    private String userName;
    
    @PostConstruct
    private void setup()
    {
        this.userName = backend.getDefaultUser();
    }
    
    private List<FileStatus> listStatus(String path) throws IOException
    {
        HttpUriRequest request = listStatusTemplate.requestForPath(userName, path);
        System.err.println(" * " + request);
        
        ListStatusResponse r = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = listStatusTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("statusList", notNullValue()));
        }
        
        return r.getStatusList();
    }

    private FileStatus getFileStatus(String path) throws IOException
    {
        HttpUriRequest request = getFileStatusTemplate.requestForPath(userName, path);
        System.err.println(" * " + request);
        
        GetFileStatusResponse r = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = getFileStatusTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("fileStatus", notNullValue()));
        }
        
        return r.getFileStatus();
    }
    
    private void getFileStatusOfNonExisitingFile(String path) throws IOException
    {
        HttpUriRequest request = getFileStatusTemplate.requestForPath(userName, path);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_NOT_FOUND, responseStatus.getStatusCode());
        }
    }
    
    private boolean makeDirectory(String path) throws IOException
    {
        MakeDirectoryRequestParameters parameters = 
            MakeDirectoryRequestParameters.of(DEFAULT_DIRECTORY_PERMISSION);
        
        HttpUriRequest request = makeDirectoryTemplate.requestForPath(userName, path, parameters);
        System.err.println(" * " + request);
        
        BooleanResponse r = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = makeDirectoryTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("flag", equalTo(Boolean.TRUE)));
        }
        
        return r.getFlag();
    }
    
    private String createFileInDirectory(String dirPath, String filename, Object data) throws IOException
    {
        final String path = StringUtils.applyRelativePath(dirPath, filename);
        
        final CreateFileRequestParameters parameters = new CreateFileRequestParameters();
        parameters.setPermission(DEFAULT_FILE_PERMISSION);
        parameters.setOverwrite(false);
        parameters.setReplication(DEFAULT_REPLICATION);
        
        HttpUriRequest request = null; 
        if (data instanceof InputStream) 
            request = createFileTemplate.requestForPath(userName, path, parameters, (InputStream) data);
        else if (data instanceof byte[])
            request = createFileTemplate.requestForPath(userName, path, parameters, (byte[]) data);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_CREATED, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            assertNull(createFileTemplate.responseFromHttpEntity(e));
        }
        
        return path;
    }
    
    private void appendToFile(String path, Object data) throws IOException
    {
        final AppendToFileRequestParameters parameters = new AppendToFileRequestParameters();
        
        HttpUriRequest request = null; 
        if (data instanceof InputStream) 
            request = appendToFileTemplate.requestForPath(userName, path, parameters, (InputStream) data);
        else if (data instanceof byte[])
            request = appendToFileTemplate.requestForPath(userName, path, parameters, (byte[]) data);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            assertNull(appendToFileTemplate.responseFromHttpEntity(e));
        }
    }
    
    private void appendToNonExistingFile(String path, Object data) throws IOException
    {
        final AppendToFileRequestParameters parameters = new AppendToFileRequestParameters();
        
        HttpUriRequest request = null; 
        if (data instanceof InputStream) 
            request = appendToFileTemplate.requestForPath(userName, path, parameters, (InputStream) data);
        else if (data instanceof byte[])
            request = appendToFileTemplate.requestForPath(userName, path, parameters, (byte[]) data);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_NOT_FOUND, responseStatus.getStatusCode());
        }
    }
    
    private FileChecksum getFileChecksum(String path) throws IOException
    {
        GetFileChecksumResponse r = null;
        HttpUriRequest request = getFileChecksumTemplate.requestForPath(userName, path);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = getFileChecksumTemplate.responseFromHttpEntity(e);
            assertThat(r, hasProperty("checksum", notNullValue()));
            assertThat(r.getChecksum(), hasProperty("checksumAsHexString", notNullValue()));
        }
        return r.getChecksum();
    }
    
    private String readTextFile(String path) throws IOException
    {
        HttpUriRequest request = readFileTemplate.requestForPath(userName, path);
        System.err.println(" * " + request);
        
        String text = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            final HttpEntity e = response.getEntity();
            assertNotNull(e);
            final ContentType t = ContentType.getOrDefault(e);
            assertEquals(ContentType.APPLICATION_OCTET_STREAM.getMimeType(), t.getMimeType());
            text = IOUtils.toString(e.getContent(), Charset.forName("UTF-8"));
        }
        
        return text;
    }
    
    private String concatenateFiles(String path, String ...sourcePaths) throws IOException
    {
        ConcatenateFilesRequestParameters parameters = new ConcatenateFilesRequestParameters();
        parameters.setSources(Arrays.asList(sourcePaths));
        
        HttpUriRequest request =  concatenateFilesTemplate.requestForPath(userName, path, parameters);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            final HttpEntity e = response.getEntity();
            assertNotNull(e);
            assertNull(concatenateFilesTemplate.responseFromHttpEntity(e));
        }
        
        return path;
    }
    
    private boolean truncateFile(String path) throws IOException
    {
        TruncateFileRequestParameters parameters = new TruncateFileRequestParameters();
        
        HttpUriRequest request =  truncateFileTemplate.requestForPath(userName, path, parameters);
        System.err.println(" * " + request);
        
        BooleanResponse r = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            final HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = truncateFileTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("flag", equalTo(Boolean.TRUE)));
        }
        
        return r.getFlag();
    }
    
    private boolean deleteFile(String path) throws IOException
    {
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters.of(false);
        
        HttpUriRequest request =  deleteFileTemplate.requestForPath(userName, path, parameters);
        System.err.println(" * " + request);
        
        BooleanResponse r = null;
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            final HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = deleteFileTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("flag", equalTo(Boolean.TRUE)));
        }
        
        return r.getFlag();
    }
    
    private void deleteNonEmptyDirectory(String path) throws IOException
    {
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters.of(false);
        
        HttpUriRequest request =  deleteFileTemplate.requestForPath(userName, path, parameters);
        System.err.println(" * " + request);
        
        // Trying to delete an non-empty directory results to HTTP 500
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertNotEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
        }
    }
    
    private void deleteRecursiveNonEmptyDirectory(String path) throws IOException
    {
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters.of(true);
        
        HttpUriRequest request =  deleteFileTemplate.requestForPath(userName, path, parameters);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
        }
    }
    
    private ContentSummary getSummary(String path) throws IOException
    {
        ContentSummaryResponse r = null;
        HttpUriRequest request = getContentSummaryTemplate.requestForPath("user", path);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            final HttpEntity e = response.getEntity();
            assertNotNull(e);
            r = getContentSummaryTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("summary", notNullValue()));
        }
        
        return r.getSummary();
    }
    
    private void getSummaryOfNonExistingPath(String path) throws IOException
    {
        ContentSummaryResponse r = null;
        HttpUriRequest request = getContentSummaryTemplate.requestForPath("user", path);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_NOT_FOUND, responseStatus.getStatusCode());
        }
    }
    
    private void setPermission(String path, String permission) throws IOException
    {
        SetPermissionRequestParameters parameters = SetPermissionRequestParameters.of(permission);
        
        HttpUriRequest request = setPermissionTemplate.requestForPath("user", path, parameters);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
        }
    }
    
    private void setReplication(String path, int replication) throws IOException
    {
        SetReplicationRequestParameters parameters = SetReplicationRequestParameters.of(replication);
        
        HttpUriRequest request = setReplicationTemplate.requestForPath("user", path, parameters);
        System.err.println(" * " + request);
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            final StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
        }
    }
    
    //
    // Tests
    //
    
    @Test
    public void test01a_getHomeDirectory() throws IOException
    {
        final HttpUriRequest request = getHomeDirectoryTemplate.requestForPath(userName, "/");
        
        try (CloseableHttpResponse response = httpClient.execute(request)) {
            StatusLine responseStatus = response.getStatusLine();
            assertEquals(HttpStatus.SC_OK, responseStatus.getStatusCode());
            HttpEntity e = response.getEntity();
            assertNotNull(e);
            GetHomeDirectoryResponse r = getHomeDirectoryTemplate.responseFromHttpEntity(e);
            assertNotNull(r);
            assertThat(r, hasProperty("path", equalTo(String.format("/user/%s", userName))));
        }   
    }
    
    @Test
    public void test02a_listStatusInHomeDirectory() throws IOException
    {
        List<FileStatus> r = listStatus("");
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test02b_listStatusInRootDirectory() throws IOException
    {
        List<FileStatus> r = listStatus("/");
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test03a_makeTempDirectory() throws IOException
    {
        makeDirectory(tempDir);
    }
    
    @Test
    public void test03b_listStatusInTempDirectory() throws IOException
    {
        List<FileStatus> r = listStatus(tempDir);
        assertThat(r, empty());
    }
    
    @Test
    public void test03c_getStatusInTempDirectory() throws IOException
    {
        FileStatus r = getFileStatus(tempDir);
        assertThat(r, hasProperty("type", equalTo(FileStatus.EnumType.DIRECTORY)));
        assertThat(r, hasProperty("ownerName", equalTo(userName)));
        assertThat(r, hasProperty("path", isEmptyString()));
    }
    
    @Test
    public void test04a_d1_createTextInTempDirectory() throws IOException
    {        
        String path = createFileInDirectory(tempDir, "data1-a.txt", fixture01.getText().getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture01.getChecksum())));
    }
    
    @Test
    public void test04a_d2_createTextInTempDirectory() throws IOException
    {        
        String path = createFileInDirectory(tempDir, "data2-a.txt", fixture02.getText().getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture02.getChecksum())));
    }
    
    @Test
    public void test04a_d3_createTextInTempDirectory() throws IOException
    {        
        String path = createFileInDirectory(tempDir, "data3-a.txt", fixture03.readText().getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture03.getChecksum())));
    }
    
    @Test
    public void test04b_d1_createTextInTempDirectory() throws IOException
    {        
        ByteArrayInputStream in = new ByteArrayInputStream(fixture01.getText().getBytes());
        String path = createFileInDirectory(tempDir, "data1-b.txt", in);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture01.getChecksum())));
    }
    
    @Test
    public void test04b_d2_createTextInTempDirectory() throws IOException
    {        
        ByteArrayInputStream in = new ByteArrayInputStream(fixture02.getText().getBytes());
        String path = createFileInDirectory(tempDir, "data2-b.txt", in);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture02.getChecksum())));
    }
 
    @Test
    public void test04b_d3_createTextInTempDirectory() throws IOException
    {   
        String path = null;
        try (InputStream in = fixture03.getResource().getInputStream()) { 
            path = createFileInDirectory(tempDir, "data3-b.txt", in);
        }
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture03.getChecksum())));
    }
    
    @Test
    public void test04z_listStatusInTempDirectory() throws IOException
    {
        List<FileStatus> r = listStatus(tempDir);
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test04z_d1_getFileStatus() throws IOException
    {
        FileStatus r = getFileStatus(StringUtils.applyRelativePath(tempDir, "data1-a.txt"));
        assertThat(r, hasProperty("type", equalTo(FileStatus.EnumType.FILE)));
        assertThat(r, hasProperty("ownerName", equalTo(userName)));
        assertThat(r, hasProperty("path", isEmptyString()));
        assertThat(r, hasProperty("length", equalTo(Long.valueOf(fixture01.getText().length()))));
        assertThat(r, hasProperty("replication", greaterThanOrEqualTo(2)));
    }
    
    @Test
    public void test04z_d2_getFileStatus() throws IOException
    {
        FileStatus r = getFileStatus(StringUtils.applyRelativePath(tempDir, "data2-a.txt"));
        assertThat(r, hasProperty("type", equalTo(FileStatus.EnumType.FILE)));
        assertThat(r, hasProperty("ownerName", equalTo(userName)));
        assertThat(r, hasProperty("path", isEmptyString()));
        assertThat(r, hasProperty("length", equalTo(Long.valueOf(fixture02.getText().length()))));
        assertThat(r, hasProperty("replication", greaterThanOrEqualTo(2)));
    }
    
    @Test
    public void test04z_getFileStatusOfNonExistingFile() throws IOException
    {
        getFileStatusOfNonExisitingFile(StringUtils.applyRelativePath(tempDir, "i-dont-exist.txt"));
    }
    
    @Test 
    public void test05a_d12_appendTextToFile() throws IOException
    {
        String name = "data12-a.txt";
        String path = createFileInDirectory(tempDir, name, fixture01.getText().getBytes());
        appendToFile(path, fixture02.getText().getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture01plus02.getChecksum())));
    }
    
    @Test 
    public void test05b_d12_appendTextToFile() throws IOException
    {
        String name = "data12-b.txt";
        ByteArrayInputStream in1 = new ByteArrayInputStream(fixture01.getText().getBytes());
        String path = createFileInDirectory(tempDir, name, in1);
        ByteArrayInputStream in2 = new ByteArrayInputStream(fixture02.getText().getBytes());
        appendToFile(path, in2);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(fixture01plus02.getChecksum())));
    }
    
    @Test 
    public void test05z_appendTextToNonExistingFile() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "i-dont-exist.txt");
        appendToNonExistingFile(path, fixture01.getText().getBytes());
    }
    
    @Test // Note: must run after test04a_d1_createTextInTempDirectory
    public void test06a_d1_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data1-a.txt");
        String textData = readTextFile(path);
        assertEquals(fixture01.getText(), textData);
    }
    
    @Test // Note: must run after test04a_d2_createTextInTempDirectory
    public void test06a_d2_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data2-a.txt");
        String textData = readTextFile(path);
        assertEquals(fixture02.getText(), textData);
    }
    
    @Test // Note: must run after test04a_d3_createTextInTempDirectory
    public void test06a_d3_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data3-a.txt");
        String textData = readTextFile(path);
        assertEquals(fixture03.readText(), textData);
    }
    
    @Test // Note: must run after test04a_d12_createTextInTempDirectory
    public void test06a_d12_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data12-a.txt");
        String textData = readTextFile(path);
        String expectedTextData = fixture01.getText() + fixture02.getText();
        assertEquals(expectedTextData, textData);
    }
    
    @Test
    public void test07a_d1_concatenateTextFiles() throws IOException
    {
        String sourcePath1 = createFileInDirectory(tempDir, "data-part-1.txt", fixture01.getText().getBytes());
        // CONCAT expects the destination path to exist: create an empty file
        String path = createFileInDirectory(tempDir, "concat-data-1-a.txt", new byte[0]);
        concatenateFiles(path, sourcePath1);
        String textData = readTextFile(path);
        assertEquals(fixture01.getText(), textData);
    }
    
    @Test
    public void test07a_d12_concatenateTextFiles() throws IOException
    {
        String sourcePath1 = createFileInDirectory(tempDir, "data-part-1.txt", fixture01.getText().getBytes());
        String sourcePath2 = createFileInDirectory(tempDir, "data-part-2.txt", fixture02.getText().getBytes());
        // CONCAT expects the destination path to exist: create an empty file
        String path = createFileInDirectory(tempDir, "concat-data-1-2-a.txt", new byte[0]);
        concatenateFiles(path, sourcePath1, sourcePath2);
        String textData = readTextFile(path);
        String expectedTextData = fixture01.getText() + fixture02.getText();
        assertEquals(expectedTextData, textData);
    }
    
    @Test
    public void test08a_d1_truncateFile() throws IOException
    {
        String path = createFileInDirectory(tempDir, "data1-a-to-be-truncated.txt", fixture01.getText().getBytes());
        FileStatus st0 = getFileStatus(path);
        assertThat(st0, hasProperty("length", equalTo(Long.valueOf(fixture01.getText().length()))));
        
        truncateFile(path);
        FileStatus st1 = getFileStatus(path);
        assertThat(st1, hasProperty("length", equalTo(0L)));
    }
    
    @Test
    public void test09a_d1_deleteFile() throws IOException
    {
        String path = createFileInDirectory(tempDir, "data1-a-to-be-deleted.txt", fixture01.getText().getBytes());
        getFileStatus(path);
        deleteFile(path);
        getFileStatusOfNonExisitingFile(path);
    }
    
    @Test
    public void test09z_deleteNonEmptyDirectory() throws IOException
    {
        String dirName = String.format("sub-%05d", random.nextInt(100000));
        String dirPath = StringUtils.applyRelativePath(tempDir, dirName) + "/";
        makeDirectory(dirPath);
        createFileInDirectory(dirPath, "timestamp", new byte[0]);
        deleteNonEmptyDirectory(dirPath);
    }
    
    @Test
    public void test09z_deleteRecursiveNonEmptyDirectory() throws IOException
    {
        String dirName = String.format("sub-%05d", random.nextInt(100000));
        String dirPath = StringUtils.applyRelativePath(tempDir, dirName) + "/";
        makeDirectory(dirPath);
        createFileInDirectory(dirPath, "timestamp", new byte[0]);
        deleteRecursiveNonEmptyDirectory(dirPath);
    }
    
    @Test // Note: must run after test04* tests that create content inside temporary directory
    public void test10a_getSummaryInTempDirectory() throws IOException
    {
        ContentSummary summary = getSummary(tempDir);
        assertThat(summary, hasProperty("fileCount", greaterThan(0)));
        assertThat(summary, hasProperty("directoryCount", greaterThan(0)));
        assertThat(summary, hasProperty("length", greaterThan(0L)));
        assertThat(summary, hasProperty("spaceConsumed", greaterThan(0L)));
    }
    
    @Test
    public void test10b_getSummaryOfNonExistingDirectory() throws IOException
    {
        getSummaryOfNonExistingPath(StringUtils.applyRelativePath(tempDir, "i-dont-exist"));
    }
    
    @Test // Note: must run after test04_d1_* which creates examined file
    public void test11a_d1_setPermission() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data1-a.txt");
        FileStatus st0 = getFileStatus(path);
        assertThat(st0, hasProperty("permission", equalTo(DEFAULT_FILE_PERMISSION)));
        
        setPermission(path, RESTRICTIVE_FILE_PERMISSION);
        FileStatus st1 = getFileStatus(path);
        assertThat(st1, hasProperty("permission", equalTo(RESTRICTIVE_FILE_PERMISSION)));
    }
    
    @Test // Note: must run after test04_d1_* which creates examined file
    public void test12a_d1_setReplication() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data1-a.txt");
        FileStatus st0 = getFileStatus(path);
        assertThat(st0, hasProperty("replication", equalTo(DEFAULT_REPLICATION)));
        
        setReplication(path, DEFAULT_REPLICATION + 1);
        FileStatus st1 = getFileStatus(path);
        assertThat(st1, hasProperty("replication", equalTo(DEFAULT_REPLICATION + 1)));
    }
}
