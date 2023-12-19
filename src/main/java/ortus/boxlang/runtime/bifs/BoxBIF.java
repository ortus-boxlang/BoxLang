package ortus.boxlang.runtime.bifs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Repeatable( BoxBIFs.class )
public @interface BoxBIF {

    String alias() default "";

}