package gr.helix.httpfsproxy.model.ops;

import org.apache.http.StatusLine;

public class PermissionDeniedException extends OperationFailedException
{
    private static final long serialVersionUID = 1L;

    PermissionDeniedException(String message, StatusLine statusLine)
    {
        super(message, statusLine);
    }
}
