package gr.helix.httpfsproxy.service;

import java.io.IOException;
import java.io.InputStream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.ops.BaseRequestParameters;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.model.ops.OperationFailedException;
import gr.helix.httpfsproxy.model.ops.VoidRequestParameters;
import gr.helix.httpfsproxy.validation.FilePath;

/**
 * A template for operations on the HttpFS backend
 * 
 * @param <P> The type for request parameters. It may be {@link VoidRequestParameters} if no
 *   parameters are expected
 * @param <R> The type for response (DTO). It may be {@link Void}, if no response is expected.
 */
public interface OperationTemplate <P extends BaseRequestParameters, R>
{
    EnumOperation operation();
    
    /**
     * Return the name of the HTTP method
     */
    String methodName();
    
    /**
     * Check the response status and throw a proper exception if the operation was unsuccessful.
     * In the later case, this method may attempt to read (i.e. consume) the response body to extract 
     * details on what has happened.
     * 
     * @param response An HTTP response
     * 
     * @throws OperationFailedException if the back-end server reports the operation has failed
     * @throws IllegalStateException if the operation results in an unexpected state  
     */
    void failForStatus(@NotNull HttpResponse response)
        throws OperationFailedException;
    
    /**
     * Parse the response entity to a DTO object. This method assumes that the revelant HTTP request 
     * was successful.
     * 
     * <p>Note: This method is meaningful only for REST-like operations returning objects
     * (and not for operations like <tt>OPEN</tt> returning a binary stream of data). 
     * 
     * @param e The HTTP response entity (never <tt>null</tt>, even when it carries a zero-length body)
     * 
     * @see {@link #failForStatus(HttpResponse)}
     */
    R responseFromEntity(@NotNull HttpEntity e) 
        throws JsonProcessingException, IOException;
    
    /**
     * Build a {@link HttpUriRequest} for an operation on given path.
     * 
     * @param userName The name of the HDFS user we act on behalf 
     * @param filePath
     * @param parameters
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters);
    
    /**
     * Build a {@link HttpUriRequest} carrying a request entity for an operation on given path.
     * 
     * @param userName The name of the HDFS user we act on behalf 
     * @param filePath The file path on the HDFS filesystem (an empty path corresponds to
     *   the user's home directory)
     * @param parameters An object that models operation-specific parameters
     * @param in The input stream to form the request HTTP entity
     * @param contentType THe content-type of the input stream
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull InputStream in, @NotNull ContentType contentType);
    
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull InputStream in);
    
    /**
     * Build a {@link HttpUriRequest} for an operation on given path (no extra parameters)
     * @see {@link OperationTemplate#requestForPath(String, String, BaseRequestParameters)}
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath);
    
    /**
     * Build a {@link HttpUriRequest} carrying a request entity for an operation on given path
     * (no extra parameters).
     * @see {@link OperationTemplate#requestForPath(String, String, BaseRequestParameters, InputStream, ContentType)}
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath,
        @NotNull InputStream in, @NotNull ContentType contentType);
    
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath,
        @NotNull InputStream in);
    
    /**
     * Build a {@link HttpUriRequest} carrying a request entity for an operation on given path.
     * @see {@link OperationTemplate#requestForPath(String, String, InputStream, ContentType)}
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath,
        @NotNull byte[] data, @NotNull ContentType contentType);
    
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath,
        @NotNull byte[] data);
    
    /**
     * Build a {@link HttpUriRequest} carrying a request entity for an operation on given path.
     * @see {@link OperationTemplate#requestForPath(String, String, BaseRequestParameters, InputStream, ContentType)}
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull byte[] data, @NotNull ContentType contentType);
    
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, @Valid P parameters, 
        @NotNull byte[] data);
}
