package gr.helix.httpfsproxy.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.validation.Constraint;

@Documented
@Constraint(validatedBy = { FileReplicationConstraintValidator.class })
@Retention(RUNTIME)
@Target({ FIELD, METHOD, PARAMETER, TYPE_USE })
public @interface FileReplication
{
    String message() default "the given replication factor (${validatedValue}) is out of range";
    
    Class<?>[] groups() default {};
    
    Class<?>[] payload() default {};
}
