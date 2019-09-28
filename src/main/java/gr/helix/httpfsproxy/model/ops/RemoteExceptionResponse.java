package gr.helix.httpfsproxy.model.ops;

import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

@lombok.Value
public class RemoteExceptionResponse
{
    @JsonProperty("RemoteException")
    @NotNull
    @lombok.NonNull
    final RemoteExceptionInfo exceptionInfo;
    
    @JsonCreator
    public static RemoteExceptionResponse of(@JsonProperty("RemoteException") RemoteExceptionInfo exceptionInfo)
    {
        return new RemoteExceptionResponse(exceptionInfo);
    }
}
