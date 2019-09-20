package gr.helix.httpfsproxy.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;

@Documented
@Constraint(validatedBy = { FileBlockSizeConstraintValidator.class })
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, TYPE_USE })
public @interface FileBlockSize
{
    String message() default "the given block-size (${validatedValue}) is too small";
    
    Class<?>[] groups() default {};
    
    Class<?>[] payload() default {};
}
