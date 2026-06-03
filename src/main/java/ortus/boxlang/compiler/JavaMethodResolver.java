/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ortus.boxlang.compiler;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.loader.ClassLocation;
import ortus.boxlang.runtime.loader.ImportDefinition;

/**
 * Resolves Java methods from a class hierarchy (superclasses + interfaces) that match
 * BoxLang UDF names. Used by both the Java and ASM boxpilers to generate accurate
 * method stubs when a BoxLang class extends a Java class.
 * <p>
 * For each UDF name defined in the BoxLang class, this resolver finds all public methods
 * (including overloads) with that name from the Java class hierarchy. Each overload gets
 * its own stub that delegates to the same BoxLang UDF.
 */
public class JavaMethodResolver {

	/**
	 * Testing hook: when non-null, this function is used instead of the ClassLocator
	 * to resolve class names to Class objects. Package-private for test access.
	 */
	public static Function<String, Class<?>> classResolverOverride = null;

	/**
	 * Attempts to load a Java class by fully-qualified name using the BoxLang ClassLocator.
	 * Returns null if the class cannot be found.
	 * <p>
	 * Both the Java and ASM boxpilers should call this method to resolve the Java class
	 * being extended so the behavior is consistent and testable.
	 *
	 * @param className The fully-qualified Java class name
	 *
	 * @return The resolved Class, or null if not loadable
	 */
	public static Class<?> resolveClass( String className ) {
		return resolveClass( className, List.of() );
	}

	/**
	 * Attempts to load a Java class by fully-qualified name using the BoxLang ClassLocator,
	 * expanding import aliases if provided.
	 * Returns null if the class cannot be found.
	 *
	 * @param className The Java class name (may be an alias from an import statement)
	 * @param imports   The list of import definitions to use for alias resolution
	 *
	 * @return The resolved Class, or null if not loadable
	 */
	public static Class<?> resolveClass( String className, List<ImportDefinition> imports ) {
		// Allow tests to override the resolution
		if ( classResolverOverride != null ) {
			return classResolverOverride.apply( className );
		}
		return ( Class<?> ) RequestBoxContext.runInContext(
		    ( ctx ) -> BoxRuntime.getInstance().getClassLocator().getJavaResolver()
		        .resolve( ctx, className, imports )
		        .map( ClassLocation::clazz )
		        .orElse( null ) );
	}

	/**
	 * A resolved Java method signature that needs a stub generated.
	 */
	public record ResolvedMethod(
	    String name,
	    Class<?> returnType,
	    Class<?>[] parameterTypes,
	    Class<?>[] exceptionTypes ) {
	}

	/**
	 * Collects all public methods from the given Java class and its entire hierarchy
	 * (superclasses and interfaces), then filters to only those whose name matches
	 * one of the provided UDF names (case-insensitive).
	 *
	 * @param javaClass The Java class being extended
	 * @param udfNames  The set of UDF names defined in the BoxLang class (lowercase)
	 *
	 * @return A map from UDF name (lowercase) to the list of overloaded Java methods matching that name
	 */
	public static Map<String, List<ResolvedMethod>> resolveMatchingMethods( Class<?> javaClass, Set<String> udfNames ) {
		Map<String, List<ResolvedMethod>>	result		= new LinkedHashMap<>();

		// Collect all unique public methods from the hierarchy
		List<Method>						allMethods	= collectPublicMethods( javaClass );

		for ( Method method : allMethods ) {
			String nameLower = method.getName().toLowerCase();
			if ( udfNames.contains( nameLower ) ) {
				result.computeIfAbsent( nameLower, k -> new ArrayList<>() )
				    .add( new ResolvedMethod(
				        method.getName(),
				        method.getReturnType(),
				        method.getParameterTypes(),
				        method.getExceptionTypes() ) );
			}
		}

		return result;
	}

	/**
	 * Collects all public methods from a class and its entire hierarchy, deduplicating
	 * by exact signature (name + parameter types). Only includes methods that are
	 * overridable (public, non-static, non-final).
	 *
	 * @param javaClass The class to inspect
	 *
	 * @return List of unique overridable public methods
	 */
	private static List<Method> collectPublicMethods( Class<?> javaClass ) {
		Map<String, Method>	seen	= new LinkedHashMap<>();
		Class<?>			current	= javaClass;

		// Walk up the class hierarchy
		while ( current != null && current != Object.class ) {
			for ( Method m : current.getDeclaredMethods() ) {
				if ( isOverridable( m ) ) {
					String key = methodSignatureKey( m );
					seen.putIfAbsent( key, m );
				}
			}
			// Also check interfaces implemented by this class
			for ( Class<?> iface : current.getInterfaces() ) {
				collectInterfaceMethods( iface, seen );
			}
			current = current.getSuperclass();
		}

		return new ArrayList<>( seen.values() );
	}

	/**
	 * Recursively collects methods from an interface and its super-interfaces.
	 */
	private static void collectInterfaceMethods( Class<?> iface, Map<String, Method> seen ) {
		for ( Method m : iface.getDeclaredMethods() ) {
			if ( isOverridable( m ) ) {
				String key = methodSignatureKey( m );
				seen.putIfAbsent( key, m );
			}
		}
		for ( Class<?> superIface : iface.getInterfaces() ) {
			collectInterfaceMethods( superIface, seen );
		}
	}

	/**
	 * Returns true if a method can be overridden: public, non-static, non-final.
	 */
	private static boolean isOverridable( Method m ) {
		int mods = m.getModifiers();
		return Modifier.isPublic( mods ) && !Modifier.isStatic( mods ) && !Modifier.isFinal( mods );
	}

	/**
	 * Creates a unique key for a method signature: name + parameter type descriptors.
	 * This is used to deduplicate inherited methods.
	 */
	private static String methodSignatureKey( Method m ) {
		return m.getName() + "(" +
		    Arrays.stream( m.getParameterTypes() )
		        .map( Class::getName )
		        .collect( Collectors.joining( "," ) )
		    + ")";
	}

	/**
	 * Returns the Java source code type name for a class, handling arrays and primitives.
	 *
	 * @param type The class to get the type name for
	 *
	 * @return The fully qualified type name suitable for Java source code
	 */
	public static String toJavaSourceType( Class<?> type ) {
		if ( type.isArray() ) {
			return toJavaSourceType( type.getComponentType() ) + "[]";
		}
		return type.getName();
	}
}
