/**
 *
 */
package org.unicode.cldr.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to mark CLDR Tools that are runnable by users.
 * All CLDR Tools should be so annotated.
 * Running "java -jar cldr.jar" will list all annotated tools.
 *
 * @author srl
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface CLDRTool {

    /**
     * Short name for this tool. Required.
     * @return
     */
    String alias();

    /**
     * Long description of the purpose of this tool.
     * @return
     */
    String description() default "";

    /**
     * If non-empty, a description of why this tool should be hidden from user view.
     * Example: hidden="BROKEN"  or hidden="one-off testing tool"
     * or hidden="" for visible
     * @return
     */
    String hidden() default "";

    /**
     * If non-empty, URL to further docs on this tool.
     */
    String url() default "";
}
