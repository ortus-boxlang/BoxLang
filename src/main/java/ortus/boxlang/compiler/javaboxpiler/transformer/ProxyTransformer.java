/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.javaboxpiler.transformer;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ClassInfo;
import ortus.boxlang.runtime.config.util.PlaceholderHelper;

public class ProxyTransformer {

	// @formatter:off
	private static final String template = """
		package ${packageName};


		// BoxLang Auto Imports
		import ortus.boxlang.runtime.BoxRuntime;
		import ortus.boxlang.runtime.components.Component;
		import ortus.boxlang.runtime.context.*;
		import ortus.boxlang.runtime.context.ClassBoxContext;
		import ortus.boxlang.runtime.context.FunctionBoxContext;
		import ortus.boxlang.runtime.dynamic.casters.*;
		import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
		import ortus.boxlang.runtime.dynamic.IReferenceable;
		import ortus.boxlang.runtime.dynamic.Referencer;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.interop.DynamicObject;
		import ortus.boxlang.runtime.loader.ClassLocator;
		import ortus.boxlang.runtime.loader.ImportDefinition;
		import ortus.boxlang.runtime.operators.*;
		import ortus.boxlang.runtime.runnables.BoxScript;
		import ortus.boxlang.runtime.runnables.BoxTemplate;
		import ortus.boxlang.runtime.runnables.IClassRunnable;
		import ortus.boxlang.runtime.runnables.IProxyRunnable;
		import ortus.boxlang.runtime.scopes.*;
		import ortus.boxlang.runtime.scopes.Key;
		import ortus.boxlang.runtime.types.*;
		import ortus.boxlang.runtime.types.util.*;
		import ortus.boxlang.runtime.types.exceptions.*;
		import ortus.boxlang.runtime.types.exceptions.ExceptionUtil;
		import ortus.boxlang.runtime.types.meta.BoxMeta;
		import ortus.boxlang.runtime.types.meta.ClassMeta;
		import ortus.boxlang.runtime.types.Property;
		import ortus.boxlang.runtime.util.*;
		import ortus.boxlang.web.scopes.*;
		import ortus.boxlang.compiler.parser.BoxSourceType;

		// Java Imports
		import java.nio.file.Path;
		import java.nio.file.Paths;
		import java.time.LocalDateTime;
		import java.util.ArrayList;
		import java.util.Collections;
		import java.util.HashMap;
		import java.util.Iterator;
		import java.util.LinkedHashMap;
		import java.util.LinkedHashMap;
		import java.util.List;
		import java.util.Map;
		import java.util.Optional;

		public class ${className} implements IProxyRunnable, ${interfaceList} {

			private static final long					compileVersion	= ${compileVersion};

			/**
			 * The box class being proxied to
			 */
			public IClassRunnable						boxClass;

			public ${className}() {
			}

			// IProxyRunnable implementation methods

			/**
				* The version of the BoxLang runtime
			*/
			public long getBXRunnableCompileVersion() {
				return ${className}.compileVersion;
			}

			/**
			 * Set the proxy object
			 */
			public void setBXProxy( IClassRunnable boxClass ) {
				this.boxClass = boxClass;
			}

			${interfaceMethods}


		}
	""";
	// @formatter:on

	public static String transform( ClassInfo classInfo ) {
		String				packageName			= classInfo.packageName().toString();
		String				className			= classInfo.className();
		String				interfaceList		= classInfo.interfaceProxyDefinition().interfaces().stream().collect( Collectors.joining( ", " ) );
		String				interfaceMethods	= generateInterfaceMethods( classInfo.interfaceProxyDefinition().methods() );

		Map<String, String>	values				= Map.ofEntries(
		    Map.entry( "packagename", packageName ),
		    Map.entry( "className", className ),
		    Map.entry( "compileVersion", "1L" ),
		    Map.entry( "interfaceList", interfaceList ),
		    Map.entry( "interfaceMethods", interfaceMethods )
		);

		String				code				= PlaceholderHelper.resolve( template, values );
		// throw new RuntimeException( code );
		return code;
	}

	private static String generateInterfaceMethods( List<Method> methods ) {
		StringBuilder sb = new StringBuilder();
		for ( Method method : methods ) {
			sb.append( "public " );
			sb.append( method.getReturnType().getCanonicalName() );
			sb.append( " " );
			sb.append( method.getName() );
			sb.append( "(" );

			Parameter[] parameters = method.getParameters();
			for ( int i = 0; i < parameters.length; i++ ) {
				Parameter parameter = parameters[ i ];
				sb.append( parameter.getType().getCanonicalName() );
				sb.append( " " );
				sb.append( parameter.getName() );
				if ( i < parameters.length - 1 ) {
					sb.append( ", " );
				}
			}

			sb.append( ") {\n" );

			// collect method args into an array of Objects
			sb.append( "    Object[] args = new Object[] {" );
			for ( int i = 0; i < parameters.length; i++ ) {
				Parameter parameter = parameters[ i ];
				sb.append( parameter.getName() );
				if ( i < parameters.length - 1 ) {
					sb.append( ", " );
				}
			}
			sb.append( "};\n" );
			// TODO: Get actual context
			sb.append( "    IBoxContext context = new ScriptingRequestBoxContext( BoxRuntime.getInstance().getRuntimeContext() );\n" );
			sb.append( "    Object result = boxClass.dereferenceAndInvoke( context, Key.of( \"" );
			sb.append( method.getName() );
			sb.append( "\" ), args, false );\n" );

			// return only if the method is not void
			if ( !method.getReturnType().equals( void.class ) ) {
				sb.append( "    return (" );
				sb.append( method.getReturnType().getCanonicalName() );
				sb.append( ") result;\n" );
			}
			sb.append( "}\n" );
		}
		return sb.toString();
	}
}
