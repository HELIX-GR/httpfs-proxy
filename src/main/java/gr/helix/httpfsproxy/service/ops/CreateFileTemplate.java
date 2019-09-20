package gr.helix.httpfsproxy.service.ops;

import java.io.IOException;
import java.io.InputStream;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;

import com.fasterxml.jackson.core.JsonProcessingException;

import gr.helix.httpfsproxy.model.ops.CreateFileRequestParameters;
import gr.helix.httpfsproxy.model.ops.EnumOperation;
import gr.helix.httpfsproxy.service.AbstractPutOperationTemplate;
import gr.helix.httpfsproxy.validation.FilePath;

@Service("createFileTemplate")
@Validated
public class CreateFileTemplate extends AbstractPutOperationTemplate<CreateFileRequestParameters, Void>
{
    @Override
    public EnumOperation operation()
    {
        return EnumOperation.CREATE;
    }

    @Override
    protected Class<Void> responseType()
    {
        return Void.class;
    }

    @Override
    protected boolean requireParameters()
    {
        return true;
    }
    
    @Override
    public Void responseFromHttpEntity(HttpEntity e)
        throws JsonProcessingException, IOException
    {
        return null; // this operation returns no response body
    }
    
    @Override
    protected boolean requireBody()
    {
        return true;
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, 
        @Valid CreateFileRequestParameters parameters,
        @NotNull byte[] data, @NotNull ContentType contentType)
    {
        Assert.isTrue(ContentType.APPLICATION_OCTET_STREAM.equals(contentType), 
            "This operation (CREATE) expects a request body of type application/octet-stream");
        return super.requestForPath(userName, filePath, parameters, data, contentType);
    }
    
    @Override
    public HttpUriRequest requestForPath(
        @NotEmpty String userName, @NotNull @FilePath String filePath, 
        @Valid CreateFileRequestParameters parameters,
        @NotNull InputStream inputStream, @NotNull ContentType contentType)
    {
        Assert.isTrue(ContentType.APPLICATION_OCTET_STREAM.equals(contentType), 
            "This operation (CREATE) expects a request body of type application/octet-stream");
        return super.requestForPath(userName, filePath, parameters, inputStream, contentType);
    }
}
