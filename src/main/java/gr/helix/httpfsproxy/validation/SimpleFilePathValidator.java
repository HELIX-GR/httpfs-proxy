package gr.helix.httpfsproxy.validation;

import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Validate simple Unix-style file paths. 
 * <p>Accepts only name components that contain characters from: the alphanumeric set, 
 * a dash (<tt>-</tt>), an underscore (<tt>_</tt>), a dot (<tt>.</tt>) or circumflex (<tt>~<tt>).  
 */
public class SimpleFilePathValidator implements ConstraintValidator<FilePath, String>
{
    /**
     * A simple pattern matching a path with at least 1 name component.
     * Note that it does not match a root path (<tt>/</tt>).
     */
    private static final Pattern patternForNonRoot = Pattern.compile(
        "^[/]?([-_~.a-z0-9]+[/])*([-_~.a-z0-9]+)[/]?$", Pattern.CASE_INSENSITIVE);
    
    private boolean allowEmpty = false;
    
    @Override
    public void initialize(FilePath constraintAnnotation)
    {
        this.allowEmpty = constraintAnnotation.allowEmpty();
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context)
    {
        if (value == null) {
            return true; // do not examine nulls
        } else if (value.isEmpty()) {
            return allowEmpty;
        } else if (value.equals("/")) {
            return true; // a root path is considered valid
        } else {
            return patternForNonRoot.matcher(value).matches();
        }
    }
}