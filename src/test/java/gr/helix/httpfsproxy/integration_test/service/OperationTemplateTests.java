package gr.helix.httpfsproxy.integration_test.service;

import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
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
import gr.helix.httpfsproxy.model.ops.CreateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.FileChecksum;
import gr.helix.httpfsproxy.model.ops.FileStatus;
import gr.helix.httpfsproxy.model.ops.GetFileChecksumResponse;
import gr.helix.httpfsproxy.model.ops.GetHomeDirectoryResponse;
import gr.helix.httpfsproxy.model.ops.ListStatusResponse;
import gr.helix.httpfsproxy.model.ops.MakeDirectoryRequestParameters;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.OperationTemplate;

@RunWith(SpringRunner.class)
@SpringBootTest
@ActiveProfiles({"testing"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OperationTemplateTests
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
    }
    
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
    @Qualifier("makeDirectoryTemplate")
    private OperationTemplate<MakeDirectoryRequestParameters, BooleanResponse> makeDirectoryTemplate;
    
    @Autowired
    @Qualifier("createFileTemplate")
    private OperationTemplate<CreateFileRequestParameters, Void> createFileTemplate;
    
    @Autowired
    @Qualifier("appendToFileTemplate")
    private OperationTemplate<AppendToFileRequestParameters, Void> appendToFileTemplate;
    
    @Autowired
    @Qualifier("getFileChecksumTemplate")
    private OperationTemplate<VoidRequestParameters, GetFileChecksumResponse> getFileChecksumTemplate;
    
    private String userName;
    
    private final String textData1 = 
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor\n" +
        "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation\n" +
        "ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit\n" +
        "in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat\n" +
        "non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.\n";
    
    private final String checksumForTextData1 = "0000020000000000000000005410a1b1b5ebb44f8664a3aa2dd68756";
    
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
    
    private final String checksumForTextData2 = "00000200000000000000000076a34aa557d5945ede4d1fe2d6248701";
    
    /** The checksum of the concatenation of textData1 and textData2 */
    private final String checksumForTextData12 = "000002000000000000000000f43a5e0b021b739c5f4b5190344231eb";
    
    @Value("classpath:samples/text/Aesop.txt")
    private Resource textData3;
    
    private final String checksumForTextData3 = "0000020000000000000000003d286c97e05104bfb999bce05da5bc3f";
    
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
        parameters.setOverwrite(true);
        
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
    
    //
    // Tests
    //
    
    @Test
    public void test1_getHomeDirectory() throws IOException
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
    public void test2a_listStatusInHomeDirectory() throws IOException
    {
        List<FileStatus> r = listStatus("");
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test2b_listStatusInRootDirectory() throws IOException
    {
        List<FileStatus> r = listStatus("/");
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test3_makeTempDirectory() throws IOException
    {
        makeDirectory(tempDir, "755");
    }
    
    @Test
    public void test3a_listStatusInTempDirectory() throws IOException
    {
        List<FileStatus> r = listStatus(tempDir);
        assertThat(r, empty());
    }
    
    @Test
    public void test4a_d1_createTextInTempDirectory() throws IOException
    {        
        String path = createFileInDirectory(tempDir, "data1-a.txt", textData1.getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData1)));
    }
    
    @Test
    public void test4a_d2_createTextInTempDirectory() throws IOException
    {        
        String path = createFileInDirectory(tempDir, "data2-a.txt", textData2.getBytes());
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData2)));
    }
    
    @Test
    public void test4a_d3_createTextInTempDirectory() throws IOException
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
    public void test4b_d1_createTextInTempDirectory() throws IOException
    {        
        ByteArrayInputStream in = new ByteArrayInputStream(textData1.getBytes());
        String path = createFileInDirectory(tempDir, "data1-b.txt", in);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData1)));
    }
    
    @Test
    public void test4b_d2_createTextInTempDirectory() throws IOException
    {        
        ByteArrayInputStream in = new ByteArrayInputStream(textData2.getBytes());
        String path = createFileInDirectory(tempDir, "data2-b.txt", in);
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData2)));
    }
 
    @Test
    public void test4b_d3_createTextInTempDirectory() throws IOException
    {   
        String path = null;
        try (InputStream in = textData3.getInputStream()) { 
            path = createFileInDirectory(tempDir, "data3-b.txt", in);
        }
        FileChecksum checksum = getFileChecksum(path);
        assertThat(checksum, hasProperty("checksumAsHexString", equalTo(checksumForTextData3)));
    }
    
    @Test 
    public void test5a_d12_appendTextToFile() throws IOException
    {
        String name = "data12-a.txt";
        String path = createFileInDirectory(tempDir, name, textData1.getBytes());
        appendToFile(path, textData2.getBytes());
    }
    
    @Test 
    public void test5b_d12_appendTextToFile() throws IOException
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
    public void test5_appendTextToNonExistingFile() throws IOException
    {
        String path = StringUtils.applyRelativePath(tempDir, "i-dont-exist.txt");
        appendToNonExistingFile(path, textData1.getBytes());
    }
}
