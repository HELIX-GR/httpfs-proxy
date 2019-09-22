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
            return String.format("temp/httpfsproxy-tests-%s-%06d/",
                Instant.now().toEpochMilli(), random.nextInt(100000));
        }
        
        @Bean
        Random random()
        {
            return this.random;
        }
    }
    
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
    
    private String userName;
    
    private final String textData1 = 
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor\n" +
        "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation\n" +
        "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit\n" +
        "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat\n" +
        "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n";
    
    private final String textData2 = 
        "Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque\n" +
        "laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto\n" +
        "beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit\n" +
        "aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt.\n" +
        "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia\n" +
        "non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem.\n" +
        "Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid\n" +
        "ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil\n" +
        "molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?\n";
    
    @Value("classpath:samples/text/Aesop.txt")
    private Resource textData3;
    
    private final String checksumForTextData1 = "0000020000000000000000005410a1b1b5ebb44f8664a3aa2dd68756";
    
    private final String checksumForTextData2 = "00000200000000000000000076a34aa557d5945ede4d1fe2d6248701";
    
    private final String checksumForTextData3 = "0000020000000000000000003d286c97e05104bfb999bce05da5bc3f";
    
    /** The checksum of the concatenation of textData1 and textData2 */
    private final String checksumForTextData12 = "000002000000000000000000f43a5e0b021b739c5f4b5190344231eb";
    
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
    
    private boolean makeDirectory(String path, String permission) throws IOException
    {
        MakeDirectoryRequestParameters parameters = new MakeDirectoryRequestParameters();
        parameters.setPermission(permission);
        
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
        parameters.setPermission("644");
        parameters.setOverwrite(false);
        
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
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters.create(false);
        
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
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters.create(false);
        
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
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters.create(true);
        
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
        makeDirectory(tempDir, "755");
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
        String path = createFileInDirectory(tempDir, "data1-a.txt", textData1.getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData1)));
    }
    
    @Test
    public void test04a_d2_createTextInTempDirectory() throws IOException
    {        
        String path = createFileInDirectory(tempDir, "data2-a.txt", textData2.getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData2)));
    }
    
    @Test
    public void test04a_d3_createTextInTempDirectory() throws IOException
    {        
        String text = null;
        try (InputStream in = textData3.getInputStream()) { 
            text = IOUtils.toString(in, Charset.forName("UTF-8"));
        }
        String path = createFileInDirectory(tempDir, "data3-a.txt", text.getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData3)));
    }
    
    @Test
    public void test04b_d1_createTextInTempDirectory() throws IOException
    {        
        ByteArrayInputStream in = new ByteArrayInputStream(textData1.getBytes());
        String path = createFileInDirectory(tempDir, "data1-b.txt", in);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData1)));
    }
    
    @Test
    public void test04b_d2_createTextInTempDirectory() throws IOException
    {        
        ByteArrayInputStream in = new ByteArrayInputStream(textData2.getBytes());
        String path = createFileInDirectory(tempDir, "data2-b.txt", in);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData2)));
    }
 
    @Test
    public void test04b_d3_createTextInTempDirectory() throws IOException
    {   
        String path = null;
        try (InputStream in = textData3.getInputStream()) { 
            path = createFileInDirectory(tempDir, "data3-b.txt", in);
        }
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData3)));
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
        assertThat(r, hasProperty("length", equalTo(Long.valueOf(textData1.length()))));
        assertThat(r, hasProperty("replication", greaterThanOrEqualTo(2)));
    }
    
    @Test
    public void test04z_d2_getFileStatus() throws IOException
    {
        FileStatus r = getFileStatus(StringUtils.applyRelativePath(tempDir, "data2-a.txt"));
        assertThat(r, hasProperty("type", equalTo(FileStatus.EnumType.FILE)));
        assertThat(r, hasProperty("ownerName", equalTo(userName)));
        assertThat(r, hasProperty("path", isEmptyString()));
        assertThat(r, hasProperty("length", equalTo(Long.valueOf(textData2.length()))));
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
        String path = createFileInDirectory(tempDir, name, textData1.getBytes());
        appendToFile(path, textData2.getBytes());
    }
    
    @Test 
    public void test05b_d12_appendTextToFile() throws IOException
    {
        String name = "data12-b.txt";
        ByteArrayInputStream in1 = new ByteArrayInputStream(textData1.getBytes());
        String path = createFileInDirectory(tempDir, name, in1);
        ByteArrayInputStream in2 = new ByteArrayInputStream(textData2.getBytes());
        appendToFile(path, in2);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData12)));
    }
    
    @Test 
    public void test05z_appendTextToNonExistingFile() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "i-dont-exist.txt");
        appendToNonExistingFile(path, textData1.getBytes());
    }
    
    @Test // Note: must run after test4a_d1_createTextInTempDirectory
    public void test06a_d1_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data1-a.txt");
        String textData = readTextFile(path);
        assertEquals(textData1, textData);
    }
    
    @Test // Note: must run after test4a_d2_createTextInTempDirectory
    public void test06a_d2_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data2-a.txt");
        String textData = readTextFile(path);
        assertEquals(textData2, textData);
    }
    
    @Test // Note: must run after test4a_d3_createTextInTempDirectory
    public void test06a_d3_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data3-a.txt");
        String textData = readTextFile(path);
        
        String expectedTextData = null;
        try (InputStream in = textData3.getInputStream()) {
            expectedTextData = IOUtils.toString(in, Charset.forName("UTF-8"));
        }
        assertEquals(expectedTextData, textData);
    }
    
    @Test // Note: must run after test4a_d12_createTextInTempDirectory
    public void test06a_d12_readText() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "data12-a.txt");
        String textData = readTextFile(path);
        assertEquals(textData1 + textData2, textData);
    }
    
    @Test
    public void test07a_d1_concatenateTextFiles() throws IOException
    {
        String sourcePath1 = createFileInDirectory(tempDir, "data-part-1.txt", textData1.getBytes());
        // CONCAT expects the destination path to exist: create an empty file
        String path = createFileInDirectory(tempDir, "concat-data-1-a.txt", new byte[0]);
        concatenateFiles(path, sourcePath1);
        String textData = readTextFile(path);
        assertEquals(textData1, textData);
    }
    
    @Test
    public void test07a_d12_concatenateTextFiles() throws IOException
    {
        String sourcePath1 = createFileInDirectory(tempDir, "data-part-1.txt", textData1.getBytes());
        String sourcePath2 = createFileInDirectory(tempDir, "data-part-2.txt", textData2.getBytes());
        // CONCAT expects the destination path to exist: create an empty file
        String path = createFileInDirectory(tempDir, "concat-data-1-2-a.txt", new byte[0]);
        concatenateFiles(path, sourcePath1, sourcePath2);
        String textData = readTextFile(path);
        String expectedTextData = textData1 + textData2;
        assertEquals(expectedTextData, textData);
    }
    
    @Test
    public void test08a_d1_truncateFile() throws IOException
    {
        String path = createFileInDirectory(tempDir, "data1-a-to-be-truncated.txt", textData1.getBytes());
        FileStatus st0 = getFileStatus(path);
        assertThat(st0, hasProperty("length", equalTo(Long.valueOf(textData1.length()))));
        
        truncateFile(path);
        FileStatus st1 = getFileStatus(path);
        assertThat(st1, hasProperty("length", equalTo(0L)));
    }
    
    @Test
    public void test09a_d1_deleteFile() throws IOException
    {
        String path = createFileInDirectory(tempDir, "data1-a-to-be-deleted.txt", textData1.getBytes());
        getFileStatus(path);
        deleteFile(path);
        getFileStatusOfNonExisitingFile(path);
    }
    
    @Test
    public void test09z_deleteNonEmptyDirectory() throws IOException
    {
        String dirName = String.format("sub-%05d", random.nextInt(100000));
        String dirPath = StringUtils.applyRelativePath(tempDir, dirName) + "/";
        makeDirectory(dirPath, "775");
        createFileInDirectory(dirPath, "timestamp", new byte[0]);
        deleteNonEmptyDirectory(dirPath);
    }
    
    @Test
    public void test09z_deleteRecursiveNonEmptyDirectory() throws IOException
    {
        String dirName = String.format("sub-%05d", random.nextInt(100000));
        String dirPath = StringUtils.applyRelativePath(tempDir, dirName) + "/";
        makeDirectory(dirPath, "775");
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
}
