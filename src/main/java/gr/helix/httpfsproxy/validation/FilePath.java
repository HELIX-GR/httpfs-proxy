package gr.helix.httpfsproxy.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;

@Documented
@Constraint(validatedBy = { SimpleFilePathValidator.class })
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, TYPE_USE })
public @interface FilePath
{
    String message() default "invalid file path: [${validatedValue}]";
    
    Class<?>[] groups() default {};
    
    Class<?>[] payload() default {};
    
    boolean allowEmpty() default true;
}
