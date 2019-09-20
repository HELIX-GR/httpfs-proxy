package gr.helix.httpfsproxy.validation;

import org.springframework.beans.factory.annotation.Value;

public class FileBlockSizeConstraintValidator extends AbstractIntRangeConstraintValidator<FileBlockSize> 
{
    @Value("${gr.helix.httpfsproxy.hdfs.min-block-size:524288}")
    Integer minValue;

    @Override
    protected Integer minValue() { return minValue; }
    
    @Override
    protected Integer maxValue() { return null; }
}
