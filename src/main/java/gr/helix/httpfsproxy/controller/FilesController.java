package gr.helix.httpfsproxy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import gr.helix.httpfsproxy.model.RestResponse;
import gr.helix.httpfsproxy.model.SimpleUserDetails;
import gr.helix.httpfsproxy.model.controller.ContentSummaryResult;
import gr.helix.httpfsproxy.model.controller.FileChecksumResult;
import gr.helix.httpfsproxy.model.controller.FileStatusResult;
import gr.helix.httpfsproxy.model.controller.FilePathResult;
import gr.helix.httpfsproxy.model.controller.ListStatusResult;
import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.ContentSummary;
import gr.helix.httpfsproxy.model.ops.ContentSummaryResponse;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.FileChecksum;
import gr.helix.httpfsproxy.model.ops.FileNotExistsException;
import gr.helix.httpfsproxy.model.ops.FileStatus;
import gr.helix.httpfsproxy.model.ops.GetFileChecksumResponse;
import gr.helix.httpfsproxy.model.ops.GetFileStatusResponse;
import gr.helix.httpfsproxy.model.ops.GetHomeDirectoryResponse;
import gr.helix.httpfsproxy.model.ops.InvalidParameterException;
import gr.helix.httpfsproxy.model.ops.ListStatusResponse;
import gr.helix.httpfsproxy.model.ops.MakeDirectoryRequestParameters;
import gr.helix.httpfsproxy.model.ops.OperationFailedException;
import gr.helix.httpfsproxy.model.ops.PermissionDeniedException;
import gr.helix.httpfsproxy.model.ops.ReadFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.OperationTemplate;
import lombok.NonNull;


@Controller
@Validated
@RequestMapping(path = { "/files", "/f" })
public class FilesController
{
    private final static Logger logger = LoggerFactory.getLogger(FilesController.class);
    
    private static final int READ_FILE_REQUEST_BUFFER_SIZE = 4096;
    
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
    
    @Autowired
    @Qualifier("getFileChecksumTemplate")
    private OperationTemplate<VoidRequestParameters, GetFileChecksumResponse> getFileChecksumTemplate;
    
    @Autowired
    @Qualifier("readFileTemplate")
    private OperationTemplate<ReadFileRequestParameters, ?> readFileTemplate;
    
    @Autowired
    @Qualifier("makeDirectoryTemplate")
    private OperationTemplate<MakeDirectoryRequestParameters, BooleanResponse> makeDirectoryTemplate;
    
    @ModelAttribute("userDetails")
    SimpleUserDetails userDetails(Authentication authentication)
    {
        return Optional.ofNullable(authentication)
            .map(Authentication::getPrincipal)
            .filter(SimpleUserDetails.class::isInstance)
            .map(SimpleUserDetails.class::cast)
            .orElse(null);
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
        return RestResponse.error("invalid argument: " + ex.getMessage());
    }
    
    @ExceptionHandler({PermissionDeniedException.class})
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ResponseBody
    public RestResponse<?> handleException(PermissionDeniedException ex)
    {
        return RestResponse.error("operation fails because it lacks permission: " + ex.getMessage());
    }
    
    @ExceptionHandler({FileNotExistsException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestResponse<?> handleException(FileNotExistsException ex)
    {
        return RestResponse.error("operation fails because file does not exist: " + ex.getMessage());
    }
    
    @ExceptionHandler({InvalidParameterException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestResponse<?> handleException(InvalidParameterException ex)
    {
        return RestResponse.error("operation fails due to invalid parameter: " + ex.getMessage());
    }
    
    @ExceptionHandler({OperationFailedException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    public RestResponse<?> handleException(OperationFailedException ex)
    {
        return RestResponse.error("operation failed: " + ex.getMessage());
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

    @GetMapping(path = "/home-directory", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getHomeDirectory(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails)
            throws Exception
    {
        final HttpUriRequest request1 = getHomeDirectoryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), "/");
        logger.debug("getHomeDirectory: {}", request1);
        
        FilePathResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            getHomeDirectoryTemplate.failForStatus(response1);
            GetHomeDirectoryResponse r1 = getHomeDirectoryTemplate.responseFromEntity(response1.getEntity());
            result = FilePathResult.of(r1.getPath());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getStatus(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") String filePath)
            throws Exception  
    {
        final HttpUriRequest request1 = getFileStatusTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath);
        logger.debug("getStatus: {}", request1);
        
        FileStatusResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            getFileStatusTemplate.failForStatus(response1);
            GetFileStatusResponse r1 = getFileStatusTemplate.responseFromEntity(response1.getEntity());
            result = FileStatusResult.of(r1.getFileStatus());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getSummary(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") String filePath)
            throws Exception  
    {
        final HttpUriRequest request1 = getContentSummaryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath);
        logger.debug("getSummary: {}", request1);
        
        ContentSummaryResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            getContentSummaryTemplate.failForStatus(response1);
            ContentSummaryResponse r1 = getContentSummaryTemplate.responseFromEntity(response1.getEntity());
            result = ContentSummaryResult.of(r1.getSummary());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/checksum", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getFileChecksum(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") String filePath)
            throws Exception  
    {
        final HttpUriRequest request1 = getFileChecksumTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath);
        logger.debug("getFileChecksum: {}", request1);
        
        FileChecksumResult result = null;
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            getFileChecksumTemplate.failForStatus(response1);
            GetFileChecksumResponse r1 = getFileChecksumTemplate.responseFromEntity(response1.getEntity());
            result = FileChecksumResult.of(r1.getChecksum()); 
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/list-status", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> listStatus(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") String filePath)
            throws Exception
    {
        final HttpUriRequest request1 = listStatusTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath);
        logger.debug("listStatus: {}", request1);
        
        ListStatusResult result = null; 
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            listStatusTemplate.failForStatus(response1);
            ListStatusResponse r1 = listStatusTemplate.responseFromEntity(response1.getEntity());
            result = ListStatusResult.of(r1.getStatusList());
        }
        
        return RestResponse.result(result);
    }
    
    @GetMapping(path = "/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> download(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") String filePath,
        @RequestParam(name = "length", required = false) @Min(0) Long length,
        @RequestParam(name = "offset", required = false) @Min(0) Long offset) 
            throws Exception
    {
        final ReadFileRequestParameters parameters = 
            new ReadFileRequestParameters(length, offset, READ_FILE_REQUEST_BUFFER_SIZE);
        final HttpUriRequest request1 = readFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("download: {}", request1);
        
        final CloseableHttpResponse response1 = httpClient.execute(request1);
        
        try {
            readFileTemplate.failForStatus(response1);
        } catch (Exception ex) {
            response1.close(); // must always be closed!
            throw ex;
        }

        // The request was successful: Stream data from back-end directly to client
        
        final StreamingResponseBody body = new StreamingResponseBody()
        {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException
            {                
                logger.debug("Copying data from {}", filePath);
                long nbytes = -1L;
                try {
                    final org.apache.http.HttpEntity e = response1.getEntity();
                    Assert.state(e != null, "expected a response HTTP entity!");
                    nbytes = IOUtils.copyLarge(e.getContent(), outputStream);
                } finally {
                    response1.close();
                }
                logger.debug("Copied {} bytes from {}", nbytes, filePath);
            }
        };
        
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(body);
    }
    
    @PostMapping(path = "/directory", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> makeDirectory(
        @NonNull Authentication authentication, 
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") String filePath, 
        @RequestParam(name  = "permission", defaultValue = "775") String permission)
            throws Exception
    {
        final MakeDirectoryRequestParameters parameters = MakeDirectoryRequestParameters.of(permission);
        
        final HttpUriRequest request1 = makeDirectoryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            makeDirectoryTemplate.failForStatus(response1);
            BooleanResponse r = makeDirectoryTemplate.responseFromEntity(response1.getEntity());
            Assert.state(r.getFlag(), "Expected flag to always be true!");
        }
        
        // Redirect to the status URI of this directory
        
        final FilesController controller = MvcUriComponentsBuilder.controller(FilesController.class);
        final URI redirectUri = MvcUriComponentsBuilder
            .fromMethodCall(controller.getStatus(null, null, filePath))
            .build().toUri();
        
        return ResponseEntity.created(redirectUri).<Void>build();
    }
}
