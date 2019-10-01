package gr.helix.httpfsproxy.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.ValidationException;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.ClientProtocolException;
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
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import gr.helix.httpfsproxy.model.RestResponse;
import gr.helix.httpfsproxy.model.SimpleUserDetails;
import gr.helix.httpfsproxy.model.controller.ContentSummaryResult;
import gr.helix.httpfsproxy.model.controller.FileChecksumResult;
import gr.helix.httpfsproxy.model.controller.FileStatusResult;
import gr.helix.httpfsproxy.model.controller.FilePathResult;
import gr.helix.httpfsproxy.model.controller.ListStatusResult;
import gr.helix.httpfsproxy.model.ops.AppendToFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.BooleanResponse;
import gr.helix.httpfsproxy.model.ops.ConcatenateFilesRequestParameters;
import gr.helix.httpfsproxy.model.ops.ContentSummary;
import gr.helix.httpfsproxy.model.ops.ContentSummaryResponse;
import gr.helix.httpfsproxy.model.ops.CreateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.DeleteFileRequestParameters;
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
import gr.helix.httpfsproxy.model.ops.RenameRequestParameters;
import gr.helix.httpfsproxy.model.ops.SetPermissionRequestParameters;
import gr.helix.httpfsproxy.model.ops.SetReplicationRequestParameters;
import gr.helix.httpfsproxy.model.ops.TruncateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.service.OperationTemplate;


@Controller
@Validated
@RequestMapping(path = { "/f/" })
public class FilesController
{
    private final static Logger logger = LoggerFactory.getLogger(FilesController.class);
    
    private static final int READ_FILE_REQUEST_BUFFER_SIZE = 2 * 4096;
    
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
 
    @Autowired
    @Qualifier("renameTemplate")
    private OperationTemplate<RenameRequestParameters, BooleanResponse> renameTemplate;
    
    @Autowired
    @Qualifier("concatenateFilesTemplate")
    private OperationTemplate<ConcatenateFilesRequestParameters, Void> concatenateFilesTemplate;
    
    @Autowired
    @Qualifier("truncateFileTemplate")
    private OperationTemplate<TruncateFileRequestParameters, BooleanResponse> truncateFileTemplate;
    
    @Autowired
    @Qualifier("deleteFileTemplate")
    private OperationTemplate<DeleteFileRequestParameters, BooleanResponse> deleteFileTemplate;
    
    @Autowired
    @Qualifier("setPermissionTemplate")
    private OperationTemplate<SetPermissionRequestParameters, Void> setPermissionTemplate;
    
    @Autowired
    @Qualifier("setReplicationTemplate")
    private OperationTemplate<SetReplicationRequestParameters, BooleanResponse> setReplicationTemplate;
    
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
        return RestResponse.error(ex.getMessage());
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
    
    /**
     * Get the home directory of current user
     */
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
    
    /**
     * Get the status of a file path
     * 
     * @param filePath A path to a file or directory
     */
    @GetMapping(path = "/file/status", produces = MediaType.APPLICATION_JSON_VALUE)
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
    
    /**
     * Get the content summary of a file path.
     * <p>This method is normally applied on a directory (where it behaves similarly to <tt>du</tt> in a 
     * Unix filesystem), but it's legal to apply also on a regular file.
     * 
     * @param filePath A path to a file or directory  
     */
    @GetMapping(path = "/file/summary", produces = MediaType.APPLICATION_JSON_VALUE)
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
    
    /**
     * Get the checksum of a regular file. This method will fail if applied on a directory.
     * 
     * @param filePath A path to a (regular) file. 
     */
    @GetMapping(path = "/file/checksum", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> getFileChecksum(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") @NotEmpty String filePath)
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
    
    /**
     * List file entries under a file path.
     * <p>This method is normally applied on a directory, but it's legal to also apply on a regular
     * file (in which case you get a result equivalent to {@link #getStatus(SimpleUserDetails, String)}).
     * 
     * @param filePath A path to a file or directory
     */
    @GetMapping(path = "/listing", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public RestResponse<?> listStatus(
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
    
    /**
     * Stream the content of a file.
     * 
     * @param filePath A path to a (regular) file
     * @param length The length (in bytes) to read
     * @param offset The offset (in bytes) to start reading from
     */
    @GetMapping(path = "/file/content")
    public ResponseEntity<StreamingResponseBody> downloadFile(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") @NotEmpty String filePath,
        @RequestParam(name = "length", required = false) @Min(0) Long length,
        @RequestParam(name = "offset", required = false) @Min(0) Long offset) 
            throws Exception
    {
        final ReadFileRequestParameters parameters = 
            new ReadFileRequestParameters(length, offset, READ_FILE_REQUEST_BUFFER_SIZE);
        final HttpUriRequest request1 = readFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("downloadFile: {}", request1);
        
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
    
    /**
     * Create a directory on a given path (with nested directories if needed).
     * <p>Note that this method will succeed if the directory already exists, but will fail if a
     * regular file exists on the given path.
     * 
     * @param filePath The path to the directory 
     * @param permission The octal permission for the newly created directory
     */
    @PutMapping(path = "/directory")
    @ResponseBody
    public ResponseEntity<?> createDirectory(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam(name = "path") @NotEmpty String filePath, 
        @RequestParam(name = "permission", required = false) String permission)
            throws Exception
    {
        final MakeDirectoryRequestParameters parameters = MakeDirectoryRequestParameters.of(permission);
        
        final HttpUriRequest request1 = makeDirectoryTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("createDirectory: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            makeDirectoryTemplate.failForStatus(response1);
            BooleanResponse r = makeDirectoryTemplate.responseFromEntity(response1.getEntity());
            Assert.state(r.getFlag(), "Expected flag to always be true!");
        }
        
        return ResponseEntity.created(uriForStatus(filePath)).<Void>build();
    }
    
    /**
     * Put content in a file.
     * 
     * @param contentType The content-type of the upload (should always be present and equal 
     *   to <tt>application/octet-stream</tt>)
     * @param filePath A path to a (regular) file
     * @param overwrite A flag that indicates if an existing file should be replaced
     * @param permission The octal permission to set on the file
     * @param replication The replication factor
     */
    @PutMapping(path = "/file/content")
    @ResponseBody
    public ResponseEntity<?> uploadFile(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestHeader(name = HttpHeaders.CONTENT_TYPE) MediaType contentType,
        @RequestParam("path") @NotEmpty String filePath, 
        @RequestParam(name  = "overwrite", required = false) Boolean overwrite,
        @RequestParam(name  = "permission", required = false) String permission,
        @RequestParam(name  = "replication", required = false) Integer replication,
        @RequestBody InputStreamResource inputStreamResource) 
            throws Exception
    {
        if (!contentType.equals(MediaType.APPLICATION_OCTET_STREAM))
            throw new IllegalArgumentException("expected content of type " + MediaType.APPLICATION_OCTET_STREAM);
        
        final CreateFileRequestParameters parameters = new CreateFileRequestParameters();
        if (permission != null) 
            parameters.setPermission(permission);
        if (overwrite != null) 
            parameters.setOverwrite(overwrite);
        if (replication != null)
            parameters.setReplication(replication);
        
        final HttpUriRequest request1 = createFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters, 
                inputStreamResource.getInputStream());
        logger.debug("uploadFile: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            createFileTemplate.failForStatus(response1);
        }
        
        return ResponseEntity.created(uriForStatus(filePath)).<Void>build();
    }
    
    /**
     * Append (i.e post) content to a file.
     * 
     * <p>There are 2 different ways to append content to a file, and which one is triggered depends
     *   on the presence of the <tt>content-type</tt> header:
     *   <ul>
     *     <li>If an upload (<tt>content-type</tt> header is present): the request body is the
     *       content to be appended to the target. In this case, a normal <tt>APPEND</tt> operation
     *       is performed.
     *     </li>
     *     <li>If not an upload (<tt>content-type</tt> header is absent): A list of source files
     *       (which must be given) are concatenated into the target file. In this case, a <tt>CONCAT</tt>
     *       operation is performed (happening entirely on the cluster side). 
     *     </li>
     *   </ul>
     * </p>
     * 
     * @param filePath A path to an existing (regular) file. This the target of an <tt>APPEND</tt> or
     *   <tt>CONCAT</tt> operation.
     * @param contentType The content-type of the upload (if an upload is taking place). If this
     *   header is present, it should always be equal to <tt>application/octet-stream</tt>.
     * @param sourceNamesAsString The list of comma-separated names of source files to be concatenated 
     *   into the target file. These names must be plain file names and will be resolved relative to parent
     *    of the target file (a limitation from the underlying <tt>CONCAT</tt> operation).
     */
    @PostMapping(path = "/file/content")
    @ResponseBody
    public ResponseEntity<?> appendToFile(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestHeader(name = HttpHeaders.CONTENT_TYPE, required = false) MediaType contentType,
        @RequestParam("path") @NotEmpty String filePath,
        @RequestParam(name = "sources", required = false) String sourceNamesAsString,
        @RequestBody(required = false) InputStreamResource inputStreamResource)
            throws Exception
    {
        if (contentType != null) {
            if (!StringUtils.isEmpty(sourceNamesAsString)) 
                throw new IllegalArgumentException("expected sources to be empty!");
            if (!contentType.equals(MediaType.APPLICATION_OCTET_STREAM))
                throw new IllegalArgumentException("expected content of type " + MediaType.APPLICATION_OCTET_STREAM);
            if (inputStreamResource == null)
                throw new IllegalArgumentException("expected a non-empty request body!");
            
            // Append input to target            
            
            final HttpUriRequest request1 = appendToFileTemplate
                .requestForPath(userDetails.getUsernameForHdfs(), filePath, 
                    inputStreamResource.getInputStream());
            logger.debug("appendToFile: {}", request1);
            
            try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
                appendToFileTemplate.failForStatus(response1);
            }
        } else {
            if (inputStreamResource != null)
                throw new IllegalArgumentException("a request body must be accompannied by a content-type header!");
            if (StringUtils.isEmpty(sourceNamesAsString))
                throw new IllegalArgumentException("no content to append, no source files to concatenate");
            
            // Concatenate sources into target
            
            final String dirPath = FilenameUtils.getFullPath(filePath); // target directory
            final String[] sourceNames = sourceNamesAsString.split(",");
            
            if (Arrays.stream(sourceNames).anyMatch(s -> s.indexOf('/') >= 0))
                throw new IllegalArgumentException("sources are expected as file names (no nested paths)");
            
            final List<String> sources = Arrays.stream(sourceNames)
                .map(s -> StringUtils.applyRelativePath(dirPath, s))
                .collect(Collectors.toList());
            
            final ConcatenateFilesRequestParameters parameters = new ConcatenateFilesRequestParameters(sources);
            final HttpUriRequest request1 = concatenateFilesTemplate
                .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
            logger.debug("appendToFile: {}", request1);
            
            try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
                concatenateFilesTemplate.failForStatus(response1);
            }
        }
       
        return ResponseEntity.created(uriForStatus(filePath)).<Void>build();
    }
    
    /**
     * Truncate a file.
     * 
     * @param filePath A path to a (regular) file
     */
    @DeleteMapping(path = "/file/content")
    @ResponseBody
    public ResponseEntity<?> truncateFile(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") @NotEmpty String filePath)
            throws Exception
    {
        final HttpUriRequest request1 = truncateFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath);
        logger.debug("truncateFile: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            truncateFileTemplate.failForStatus(response1);
            BooleanResponse r = truncateFileTemplate.responseFromEntity(response1.getEntity());
            Assert.state(r.getFlag(), "Expected flag to always be true!");
        }
        
        return ResponseEntity.noContent().<Void>build();
    }
    
    /**
     * Delete a file.
     * <p>Trying to delete a non-empty directory will fail unless <tt>recursive</tt> is set.
     * 
     * @param filePath A path to a file or directory
     * @param recursive A flag that indicates whether we should recursively delete entries of
     *   directory 
     */
    @DeleteMapping(path = "/file")
    @ResponseBody
    public ResponseEntity<?> delete(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") @NotEmpty String filePath,
        @RequestParam(name  = "recursive", required = false) Boolean recursive) 
            throws Exception
    {
        // Note: The WebHDFS API (unlike the API of a conventional filesystem) does not differentiate 
        // between deleting a (regular) file and a directory.
        
        DeleteFileRequestParameters parameters = DeleteFileRequestParameters
            .of(recursive == null? false : recursive.booleanValue());
        
        final HttpUriRequest request1 = deleteFileTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("delete: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            deleteFileTemplate.failForStatus(response1);
            BooleanResponse r = truncateFileTemplate.responseFromEntity(response1.getEntity());
            Assert.state(r.getFlag(), "Expected flag to always be true!");
        }
        
        return ResponseEntity.noContent().<Void>build();
    }
    
    /**
     * Rename a file
     * 
     * @param filePath A path to the an existing file or directory
     * @param destinationFilePath The new path to move to
     */
    @PutMapping(path = "/name")
    @ResponseBody
    public ResponseEntity<?> rename(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") @NotEmpty String filePath,
        @RequestParam("destination") @NotEmpty String destinationFilePath) 
            throws Exception
    {
        RenameRequestParameters parameters = RenameRequestParameters.of(destinationFilePath);
        
        final HttpUriRequest request1 = renameTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("rename: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            renameTemplate.failForStatus(response1);
            BooleanResponse r = renameTemplate.responseFromEntity(response1.getEntity());
            Assert.state(r.getFlag(), "Expected flag to always be true!");
        }
        
        return ResponseEntity.created(uriForStatus(destinationFilePath)).<Void>build();
    }
    
    @PutMapping(path = "/file/permission")
    @ResponseBody
    public ResponseEntity<?> setPermission(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") @NotEmpty String filePath,
        @RequestParam("permission") @NotEmpty String permission) 
            throws Exception
    {
        SetPermissionRequestParameters parameters = SetPermissionRequestParameters.of(permission);

        final HttpUriRequest request1 = setPermissionTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("setPermission: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            setPermissionTemplate.failForStatus(response1);
        }
        
        return ResponseEntity.noContent().<Void>build();
    }
    
    @PutMapping(path = "/file/replication")
    @ResponseBody
    public ResponseEntity<?> setReplication(
        @ModelAttribute("userDetails") @NotNull SimpleUserDetails userDetails,
        @RequestParam("path") @NotEmpty String filePath,
        @RequestParam("replication") @NotNull @Min(1) @Max(12) Integer replication)
            throws Exception
    {
        SetReplicationRequestParameters parameters = SetReplicationRequestParameters.of(replication);
        
        final HttpUriRequest request1 = setReplicationTemplate
            .requestForPath(userDetails.getUsernameForHdfs(), filePath, parameters);
        logger.debug("setReplication: {}", request1);
        
        try (CloseableHttpResponse response1 = httpClient.execute(request1)) {
            setReplicationTemplate.failForStatus(response1);
        }
        
        return ResponseEntity.noContent().<Void>build();
    }
}
