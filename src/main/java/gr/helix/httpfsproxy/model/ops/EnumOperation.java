package gr.helix.httpfsproxy.model.ops;

/**
 * @see https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-hdfs/WebHDFS.html
 */
public enum EnumOperation
{
    GETHOMEDIRECTORY,
    
    LISTSTATUS,
    
    GETFILESTATUS,
    
    OPEN,
    
    CREATE,
    
    APPEND,
    
    CONCAT,
    
    MKDIRS,
    
    CREATESYMLINK,
    
    RENAME,
    
    DELETE,
    
    GETFILECHECKSUM,
    
    SETPERMISSION,
    
    SETOWNER,
    
    SETREPLICATION,

    ;
}
