package gr.helix.httpfsproxy.service;

import java.io.IOException;
import java.io.InputStream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.backend.BaseRequestParameters;
import gr.helix.httpfsproxy.model.backend.EnumOperation;

/**
 * A template for operations on the HttpFs backend
 * 
 * @param <P> The type for request parameters
 * @param <R> The type for response
 */
public interface OperationTemplate <P extends BaseRequestParameters, R>
{
    EnumOperation operation();
    
    /**
     * Return the name of the HTTP method
     */
    String methodName();
    
    /**
     * Parse the response entity
     * 
     * @param e
     */
    R responseFromHttpEntity(HttpEntity e) 
        throws JsonProcessingException, IOException;
    
    /**
     * Build a {@link HttpUriRequest} for an operation on given path.
     * 
     * @param userName The name of the HDFS user we act on behalf 
     * @param filepath
     * @param parameters
     * @return
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull String filepath, @Valid P parameters);
    
    
    /**
     * Build a {@link HttpUriRequest} carrying a request entity for an operation on given path.
     * 
     * @param userName The name of the HDFS user we act on behalf 
     * @param filepath
     * @param parameters
     * @param in The input stream to form the request HTTP entity
     * @param contentType THe content-type of the input stream
     * @return
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull String filepath, @Valid P parameters, 
        InputStream in, ContentType contentType);
    
    /**
     * Build a {@link HttpUriRequest} for an operation on given path (no extra parameters)
     * @see {@link OperationTemplate#requestForPath(String, String, BaseRequestParameters)}
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull String filepath);
    
    /**
     * Build a {@link HttpUriRequest} carrying a request entity for an operation on given path
     * (no extra parameters).
     * @see {@link OperationTemplate#requestForPath(String, String, BaseRequestParameters, InputStream, ContentType)}
     */
    HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull String filepath,
        InputStream in, ContentType contentType);
}
