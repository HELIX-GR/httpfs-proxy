package gr.helix.httpfsproxy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Optional;

import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
import gr.helix.httpfsproxy.model.ops.AppendToFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.ContentSummary;
import gr.helix.httpfsproxy.model.ops.ContentSummaryResponse;
import gr.helix.httpfsproxy.model.ops.CreateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.FileAlreadyExistsException;
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
@RequestMapping(path = { "/fs" })
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
    
    @Autowired
    @Qualifier("createFileTemplate")
    private OperationTemplate<CreateFileRequestParameters, Void> createFileTemplate;
    
    @Autowired
    @Qualifier("appendToFileTemplate")
    private OperationTemplate<AppendToFileRequestParameters, Void> appendToFileTemplate;
    
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
    RestResponse<?> handleException(ValidationException ex)
    {
        return RestResponse.error("constraint validation has failed: " + ex.getMessage());
    }
    
    @ExceptionHandler({IllegalArgumentException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    RestResponse<?> handleException(IllegalArgumentException ex)
    {
        return RestResponse.error("invalid argument: " + ex.getMessage());
    }
    
    @ExceptionHandler({PermissionDeniedException.class})
    @ResponseStatus(code = HttpStatus.FORBIDDEN)
    @ResponseBody
    RestResponse<?> handleException(PermissionDeniedException ex)
    {
        return RestResponse.error("operation fails because it lacks permission: " + ex.getMessage());
    }
    
    @ExceptionHandler({FileNotExistsException.class})
    @ResponseStatus(code = HttpStatus.NOT_FOUND)
    @ResponseBody
    RestResponse<?> handleException(FileNotExistsException ex)
    {
        return RestResponse.error("operation fails because file does not exist: " + ex.getMessage());
    }
    
    @ExceptionHandler({FileAlreadyExistsException.class})
    @ResponseStatus(code = HttpStatus.CONFLICT)
    @ResponseBody
    RestResponse<?> handleException(FileAlreadyExistsException ex)
    {
        return RestResponse.error("operation fails because file already exists: " + ex.getMessage());
    }
    
    @ExceptionHandler({InvalidParameterException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    RestResponse<?> handleException(InvalidParameterException ex)
    {
        return RestResponse.error("operation fails due to invalid parameter: " + ex.getMessage());
    }
    
    @ExceptionHandler({OperationFailedException.class})
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    @ResponseBody
    RestResponse<?> handleException(OperationFailedException ex)
    {
        return RestResponse.error("operation failed: " + ex.getMessage());
    }
    
    @ExceptionHandler({IllegalStateException.class})
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    RestResponse<?> handleException(IllegalStateException ex)
    {
        return RestResponse.error("server encountered an unexpected state: " + ex.getMessage());
    }
    
    @ExceptionHandler({IOException.class})
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    RestResponse<?> handleException(IOException ex)
    {
        return RestResponse.error("server encountered an i/o error: " + ex.getMessage());
    }

    URI uriForStatus(String filePath) throws Exception
    {
        FilesController controller = MvcUriComponentsBuilder.controller(FilesController.class);
        return MvcUriComponentsBuilder.fromMethodCall(controller.getStatus(null, filePath))
            .build().toUri();
    }
    
    @GetMapping(path = "/home-directory", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getHomeDirectory(
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
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") String filePath)
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
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") String filePath)
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
    
    @GetMapping(path = "/file/checksum", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getFileChecksum(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") String filePath)
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
        @RequestParam(name = "path") String filePath)
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
    
    @GetMapping(path = "/file/content", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> download(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") String filePath,
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
    
    @PutMapping(path = "/directory", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> makeDirectory(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") String filePath, 
        @RequestParam(name = "permission", required = false) String permission)
            throws Exception
    {
        final MakeDirectoryRequestParameters parameters = MakeDirectoryRequestParameters.of(permission);
        
        final HttpUriRequest request1 = makeDirectoryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("makeDirectory: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            makeDirectoryTemplate.failForStatus(response1);
            BooleanResponse r = makeDirectoryTemplate.responseFromEntity(response1.getEntity());
            Assert.state(r.getFlag(), "Expected flag to always be true!");
        }
        
        return ResponseEntity.created(uriForStatus(filePath)).<Void>build();
    }
    
    @PutMapping(path = "/file/content", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> createFile(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestHeader(name = HttpHeaders.CONTENT_TYPE) MediaType contentType,
        @RequestParam("path") String filePath, 
        @RequestParam(name  = "overwrite", required = false) Boolean overwrite,
        @RequestParam(name  = "permission", required = false) String permission,
        @RequestBody InputStreamResource inputStreamResource) 
            throws Exception
    {
        if (!contentType.equals(MediaType.APPLICATION_OCTET_STREAM))
            throw new IllegalArgumentException(
                "Expected content of type " + MediaType.APPLICATION_OCTET_STREAM);
        
        final CreateFileRequestParameters parameters = new CreateFileRequestParameters();
        if (permission != null) 
            parameters.setPermission(permission);
        if (overwrite != null) 
            parameters.setOverwrite(overwrite);
        
        final HttpUriRequest request1 = createFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters, 
                inputStreamResource.getInputStream());
        logger.debug("createFile: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            createFileTemplate.failForStatus(response1);
        }
        
        return ResponseEntity.created(uriForStatus(filePath)).<Void>build();
    }
    
    @PostMapping(path = "/file/content", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> appendToFile(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestHeader(name = HttpHeaders.CONTENT_TYPE) MediaType contentType,
        @RequestParam("path") String filePath,
        @RequestBody InputStreamResource inputStreamResource)
            throws Exception
    {
        if (!contentType.equals(MediaType.APPLICATION_OCTET_STREAM))
            throw new IllegalArgumentException(
                "Expected content of type " + MediaType.APPLICATION_OCTET_STREAM);
        
        final HttpUriRequest request1 = appendToFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, 
                inputStreamResource.getInputStream());
        logger.debug("appendToFile: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            appendToFileTemplate.failForStatus(response1);
        }
        
        return ResponseEntity.created(uriForStatus(filePath)).<Void>build();
    }
}
