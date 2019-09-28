package gr.helix.httpfsproxy.model.ops;

import org.apache.http.StatusLine;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A general exception that represents a failure during an HttpFS/WebHDFS operation.
 * 
 * <p>It basically encapsulates the message from the <tt>RemoteException</tt> reported by the
 * HttpFS back-end.
 * 
 * @see https://hadoop.apache.org/docs/r2.9.2/hadoop-project-dist/hadoop-hdfs/WebHDFS.html#Error_Responses 
 */
public class OperationFailedException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * The response HTTP status of the failed operation 
     */
    final StatusLine statusLine;
    
    OperationFailedException(String message, StatusLine statusLine)
    {
        super(message);
        this.statusLine = statusLine;
    }
    
    public StatusLine getStatusLine()
    {
        return statusLine;
    }
    
    @Override
    public String toString()
    {
        return String.format("%s(status=%d, message=%s)",
            this.getClass().getName(),
            statusLine == null? -1 : statusLine.getStatusCode(), this.getMessage());
    }

    public static OperationFailedException fromMessage(String message, StatusLine statusLine)
    {
        Assert.isTrue(!StringUtils.isEmpty(message), "Expected a non-empty message!");
        return new OperationFailedException(message, statusLine);
    }
    
    public static OperationFailedException fromRemoteException(
        RemoteExceptionInfo exceptionResponse, StatusLine statusLine)
    {
        Assert.notNull(exceptionResponse, "Expected an instance of RemoteExceptionResponse");
        
        final String exceptionName = exceptionResponse.getExceptionName(); // as reported from the remote side
        final String exceptionMessage = exceptionResponse.getMessage();
        
        Assert.isTrue(!StringUtils.isEmpty(exceptionName), "Expected a non-empty name for the remote exception!");
        Assert.isTrue(!StringUtils.isEmpty(exceptionMessage), "Expected a non-empty message from the remote exception!");
        
        if (exceptionName.equalsIgnoreCase("IllegalArgumentException")) {
            return new InvalidParameterException(exceptionMessage, statusLine);
        } else if (exceptionName.equalsIgnoreCase("SecurityException") 
                || exceptionName.equalsIgnoreCase("AccessControlException")) {
            return new PermissionDeniedException(exceptionMessage, statusLine);
        } else if (exceptionName.equalsIgnoreCase("FileNotFoundException")) {
            return new FileNotExistsException(exceptionMessage, statusLine);
        } else if (exceptionName.equalsIgnoreCase("FileAlreadyExistsException")) {
            return new FileAlreadyExistsException(exceptionMessage, statusLine);
        } else {
            return new OperationFailedException(exceptionMessage, statusLine);
        }
    }
}
