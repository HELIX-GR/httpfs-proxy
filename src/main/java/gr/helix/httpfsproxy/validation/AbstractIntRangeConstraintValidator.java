package gr.helix.httpfsproxy.validation;

import java.lang.annotation.Annotation;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public abstract class AbstractIntRangeConstraintValidator <A extends Annotation> 
    implements ConstraintValidator<A, Integer>
{
    protected abstract Integer minValue();
    
    protected abstract Integer maxValue();
    
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context)
    {
        if (value == null)
            return true; // ignore nulls
        
        final int v = value.intValue();
        
        final Integer n0 = minValue();
        if (n0 != null && v < n0)
            return false; // too small
        
        final Integer n1 = maxValue();
        if (n1 != null && v > n1)
            return false; // too big
        
        return true;
    }
}
