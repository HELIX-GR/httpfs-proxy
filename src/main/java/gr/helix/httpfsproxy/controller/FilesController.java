package gr.helix.httpfsproxy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

import gr.helix.httpfsproxy.model.RestResponse;
import gr.helix.httpfsproxy.model.SimpleUserDetails;
import gr.helix.httpfsproxy.model.controller.ContentSummaryResult;
import gr.helix.httpfsproxy.model.controller.FileStatusResult;
import gr.helix.httpfsproxy.model.controller.HomeDirectoryResult;
import gr.helix.httpfsproxy.model.controller.ListStatusResult;
import gr.helix.httpfsproxy.model.ops.ContentSummary;
import gr.helix.httpfsproxy.model.ops.ContentSummaryResponse;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.FileStatus;
import gr.helix.httpfsproxy.model.ops.GetFileStatusResponse;
import gr.helix.httpfsproxy.model.ops.GetHomeDirectoryResponse;
import gr.helix.httpfsproxy.model.ops.ListStatusResponse;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.OperationTemplate;
import gr.helix.httpfsproxy.validation.FilePath;
import lombok.NonNull;

@Controller
@RequestMapping(path = "/files")
public class FilesController
{
    private final static Logger logger = LoggerFactory.getLogger(FilesController.class);
    
    @Autowired
    @Qualifier("httpClient")
    CloseableHttpClient httpClient;
    
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
    @Qualifier("getContentSummaryTemplate")
    private OperationTemplate<VoidRequestParameters, ContentSummaryResponse> getContentSummaryTemplate;
    
    @ModelAttribute("userDetails")
    SimpleUserDetails userDetails(Authentication authn)
    {
        final Object principal = authn.getPrincipal();
        return principal instanceof SimpleUserDetails? ((SimpleUserDetails) principal) : null;
    }
    
    IllegalStateException wrapFailureAsException(
        EnumOperation operation, HttpUriRequest request, org.apache.http.StatusLine statusLine)
    {
        String errMessage = String.format(
            "The backend server failed on an operation of [%s]: %s", operation.name(), statusLine);
        return new IllegalStateException(errMessage);
    }
    
    @ExceptionHandler({ValidationException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestResponse<?> handleException(ValidationException ex)
    {
        return RestResponse.error("constraint validation has failed: " + ex.getMessage());
    }
    
    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestResponse<?> handleException(IllegalArgumentException ex)
    {
        return RestResponse.error("got an invalid argument: " + ex.getMessage());
    }
    
    @ExceptionHandler({FileNotFoundException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestResponse<?> handleException(FileNotFoundException ex)
    {
        return RestResponse.error("file does not exist: " + ex.getMessage());
    }
    
    @ExceptionHandler({IllegalStateException.class})
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public RestResponse<?> handleException(IllegalStateException ex)
    {
        return RestResponse.error("server encountered an unexpected state: " + ex.getMessage());
    }
    
    @ExceptionHandler({IOException.class})
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public RestResponse<?> handleException(IOException ex)
    {
        return RestResponse.error("server encountered an i/o error: " + ex.getMessage());
    }

    @GetMapping(path = "/get-home-directory", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getHomeDirectory(
        Authentication authn, 
        @ModelAttribute("userDetails") @NonNull SimpleUserDetails userDetails)
            throws IOException
    {
        final HttpUriRequest request1 = getHomeDirectoryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), "/");
        logger.debug("getHomeDirectory(): {}", request1);
        
        HomeDirectoryResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            final org.apache.http.StatusLine statusLine = response1.getStatusLine();
            final HttpStatus status = HttpStatus.valueOf(statusLine.getStatusCode());
            if (!status.equals(HttpStatus.OK)) {
                throw wrapFailureAsException(getHomeDirectoryTemplate.operation(), request1, statusLine);
            }
            final org.apache.http.HttpEntity e1 = response1.getEntity();
            Assert.state(e1 != null, "expected an HTTP entity from a successful response!");
            final GetHomeDirectoryResponse r1 = getHomeDirectoryTemplate.responseFromHttpEntity(e1);
            result = HomeDirectoryResult.of(r1.getPath());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/get-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getStatus(
        Authentication authn, 
        @ModelAttribute("userDetails") @NonNull SimpleUserDetails userDetails,
        @RequestParam("path") String path)
            throws IOException  
    {
        final HttpUriRequest request1 = getFileStatusTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), path);
        logger.debug("getStatus(): {}", request1);
        
        FileStatusResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            final org.apache.http.StatusLine statusLine = response1.getStatusLine();
            final HttpStatus status = HttpStatus.valueOf(statusLine.getStatusCode());
            if (status.equals(HttpStatus.NOT_FOUND)) {
                throw new FileNotFoundException(path);
            } else if (!status.equals(HttpStatus.OK)) {
                throw wrapFailureAsException(getFileStatusTemplate.operation(), request1, statusLine);
            }
            final org.apache.http.HttpEntity e1 = response1.getEntity();
            Assert.state(e1 != null, "expected an HTTP entity from a successful response!");
            final GetFileStatusResponse r1 = getFileStatusTemplate.responseFromHttpEntity(e1);
            result = FileStatusResult.of(r1.getFileStatus());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/get-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getSummary(
        Authentication authn, 
        @ModelAttribute("userDetails") @NonNull SimpleUserDetails userDetails,
        @RequestParam("path") String path)
            throws IOException  
    {
        final HttpUriRequest request1 = getContentSummaryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), path);
        logger.debug("getSummary(): {}", request1);
        
        ContentSummaryResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            final org.apache.http.StatusLine statusLine = response1.getStatusLine();
            final HttpStatus status = HttpStatus.valueOf(statusLine.getStatusCode());
            if (status.equals(HttpStatus.NOT_FOUND)) {
                throw new FileNotFoundException(path);
            } else if (!status.equals(HttpStatus.OK)) {
                throw wrapFailureAsException(getContentSummaryTemplate.operation(), request1, statusLine);
            }
            final org.apache.http.HttpEntity e1 = response1.getEntity();
            Assert.state(e1 != null, "expected an HTTP entity from a successful response!");
            final ContentSummaryResponse r1 = getContentSummaryTemplate.responseFromHttpEntity(e1);
            result = ContentSummaryResult.of(r1.getSummary());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/list-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> listStatus(
        Authentication authn, 
        @ModelAttribute("userDetails") @NonNull SimpleUserDetails userDetails,
        @RequestParam("path") String path)
            throws IOException
    {
        final HttpUriRequest request1 = listStatusTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), path);
        logger.debug("listStatus(): {}", request1);
        
        ListStatusResult result = null; 
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            final org.apache.http.StatusLine statusLine = response1.getStatusLine();
            final HttpStatus status = HttpStatus.valueOf(statusLine.getStatusCode());
            if (status.equals(HttpStatus.NOT_FOUND)) {
                throw new FileNotFoundException(path);
            } else if (!status.equals(HttpStatus.OK)) {
                throw wrapFailureAsException(listStatusTemplate.operation(), request1, statusLine);
            }
            final org.apache.http.HttpEntity e1 = response1.getEntity();
            Assert.state(e1 != null, "expected an HTTP entity from a successful response!");
            final ListStatusResponse r1 = listStatusTemplate.responseFromHttpEntity(e1);
            result = ListStatusResult.of(r1.getStatusList());
        }
        
        return RestResponse.result(result);
    }
    
}
