package gr.helix.httpfsproxy.validation;

import org.springframework.beans.factory.annotation.Value;

public class FileReplicationConstraintValidator extends AbstractIntRangeConstraintValidator<FileReplication>
{
    @Value("${gr.helix.httpfsproxy.hdfs.min-replication:2}")
    Integer minValue;
    
    @Value("${gr.helix.httpfsproxy.hdfs.max-replication:12}")
    Integer maxValue;

    @Override
    protected Integer minValue() { return minValue; }

    @Override
    protected Integer maxValue() { return maxValue; }
}
