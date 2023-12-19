package ortus.boxlang.runtime.interop;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;

/**
 * This immutable record represents an executable method handle and it's metadata.
 * This record is the one that is cached in the {@link DynamicObject#methodCache} map.
 *
 * @param methodName    The name of the method
 * @param method        The method representation
 * @param methodHandle  The method handle to use for invocation
 * @param isStatic      Whether the method is static or not
 * @param argumentCount The number of arguments the method takes
 */
public record MethodRecord(
    String methodName,
    Method method,
    MethodHandle methodHandle,
    boolean isStatic,
    int argumentCount ) {
    // A beautiful java record of our method handle
}
