package gr.helix.httpfsproxy.integration_test.service;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import com.google.common.base.CaseFormat;

import gr.helix.httpfsproxy.config.HttpFsServiceConfiguration;
import gr.helix.httpfsproxy.model.backend.VoidRequestParameters;
import gr.helix.httpfsproxy.model.backend.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.backend.ops.FileStatus;
import gr.helix.httpfsproxy.model.backend.ops.GetHomeDirectoryResponse;
import gr.helix.httpfsproxy.model.backend.ops.ListStatusResponse;
import gr.helix.httpfsproxy.model.backend.ops.MakeDirectoryRequestParameters;
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
            return String.format("httpfsproxy-tests-%s-%06d",
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
    
    private String userName;
    
    @PostConstruct
    private void setup()
    {
        this.userName = backend.getDefaultUser();
    }
    
    List<FileStatus> testListStatus(String path) throws IOException
    {
        final HttpUriRequest request = listStatusTemplate.requestForPath(userName, path);
        
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
    
    boolean testMakeDirectory(String path, String permission) throws IOException
    {
        MakeDirectoryRequestParameters parameters = new MakeDirectoryRequestParameters();
        parameters.setPermission(permission);
        
        HttpUriRequest request = makeDirectoryTemplate.requestForPath(userName, path, parameters);
        
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
        List<FileStatus> r = testListStatus("");
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test2b_listStatusInRootDirectory() throws IOException
    {
        List<FileStatus> r = testListStatus("/");
        assertThat(r, not(empty()));
    }
    
    @Test
    public void test3_makeTempDirectory() throws IOException
    {
        testMakeDirectory(tempDir, "755");
    }
    
    @Test
    public void test3a_listStatusInTempDirectory() throws IOException
    {
        List<FileStatus> r = testListStatus(tempDir);
        assertThat(r, empty());
    }
    
    @Test
    public void test4_createTextInTempDirectory() throws IOException
    {
        fail("Not implemented yet");
    }
    
    @Test
    public void test4_appendTextInTempDirectory() throws IOException
    {
        fail("Not implemented yet");
    }
}
