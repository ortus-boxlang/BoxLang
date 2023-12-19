package ortus.boxlang.runtime.bifs;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import ortus.boxlang.runtime.types.BoxLangType;

@Retention( RetentionPolicy.RUNTIME )
@Target( ElementType.TYPE )
@Repeatable( BoxMembers.class )
public @interface BoxMember {

    BoxLangType type();

    // If not provided, the name will be the name of the BIF with the BoxType replaced. So arrayAppend() would be append()
    String name() default "";

    // If not provided, the argument will be the first argument of the BIF
    String objectArgument() default "";
}