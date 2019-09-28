package gr.helix.httpfsproxy.model.ops;

import org.apache.http.StatusLine;

public class FileAlreadyExistsException extends OperationFailedException
{
    private static final long serialVersionUID = 1L;
    
    FileAlreadyExistsException(String message, StatusLine statusLine)
    {
        super(message, statusLine);
    }
}