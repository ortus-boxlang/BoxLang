/**
 * [BoxLang]
 * <p>
 * Copyright [2023] [Ortus Solutions, Corp]
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package ortus.boxlang.executor;

import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.*;
import java.util.*;

/**
 * Dynamic in memory Java executor
 */
public class JavaRunner {

    final static String fqn      = "ortus.boxlang.test.TestClass";
    String              template = """
                                                          	package ortus.boxlang.test;

                                                          	import ortus.boxlang.runtime.BoxRuntime;
                                                          	import ortus.boxlang.runtime.context.IBoxContext;

                                                          	// BoxLang Auto Imports
                                                          	import ortus.boxlang.runtime.dynamic.BaseTemplate;
                                                          	import ortus.boxlang.runtime.dynamic.Referencer;
                                                          	import ortus.boxlang.runtime.interop.DynamicObject;
                                                          	import ortus.boxlang.runtime.loader.ClassLocator;
                                                          	import ortus.boxlang.runtime.operators.*;
                                                          	import ortus.boxlang.runtime.scopes.Key;
                                                          	import ortus.boxlang.runtime.scopes.IScope;
                                                          	import ortus.boxlang.runtime.dynamic.casters.*;

                                                          	import ortus.boxlang.executor.BoxJavaClass;

                                                          	public class TestClass extends BaseTemplate {

                                                          		private static TestClass instance;

                                                          		public TestClass() {
                                                          		}

                                                          		public static synchronized TestClass getInstance() {
                                                          			if ( instance == null ) {
                                                          				instance = new TestClass();
                                                          			}
                                                          			return instance;
                                                          		}
                                                          		/**
                                                          		 * Each template must implement the invoke() method which executes the template
                                                          		 *
                                                          		 * @param context The execution context requesting the execution
                                                          		 */
                                                          		public void invoke( IBoxContext context ) throws Throwable {
                                                          			// Reference to the variables scope
                                                          			IScope variablesScope = context.getScopeNearby( Key.of( "variables" ) );
                                                          			ClassLocator JavaLoader = ClassLocator.getInstance();
                                                          			${javaCode};
                                   String result = variablesScope.toString();
                                   System.out.println(result);
                                                          		}

                                                          		public static void main(String[] args) {
                                                            			BoxRuntime rt = BoxRuntime.getInstance();

                                                          			try {
                                                          				rt.executeTemplate( TestClass.getInstance() );
                                                          			} catch ( Throwable e ) {
                                                          				e.printStackTrace();
                                                          				System.exit( 1 );
                                                          			}

                                                          			// Bye bye! Ciao Bella!
                                                          			rt.shutdown();


                                                          		}
                                                          	}
                                                          """;

    Logger              logger   = LoggerFactory.getLogger( JavaRunner.class );

    public void runExpression( String javaCode ) {

        try {
            Map<String, String>                 values      = new HashMap<>() {

                                                                {
                                                                    put( "javaCode", javaCode );
                                                                }
                                                            };

            StringSubstitutor                   sub         = new StringSubstitutor( values );
            String                              javaClass   = sub.replace( template );

            JavaCompiler                        compiler    = ToolProvider.getSystemJavaCompiler();
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            JavaMemoryManager                   manager     = new JavaMemoryManager( compiler.getStandardFileManager( null, null, null ) );

            String                              javaRT      = System.getProperty( "java.class.path" );
            String                              boxRT       = "/home/madytyoo/IdeaProjects/boxlang/runtime/build/classes/java/main";
            String                              compRT      = "/home/madytyoo/IdeaProjects/boxlang1/compiler/build/classes/java/main";

            List<JavaFileObject>                sourceFiles = Collections.singletonList( new JavaSourceString( fqn, javaClass ) );
            List<String>                        options     = new ArrayList<>() {

                                                                {
                                                                    add( "-g" );
                                                                    add( "-cp" );
                                                                    add( javaRT + File.pathSeparator + boxRT + File.pathSeparator + File.pathSeparator
                                                                        + compRT );

                                                                }
                                                            };
            JavaCompiler.CompilationTask        task        = compiler.getTask( null, manager, diagnostics, options, null, sourceFiles );
            boolean                             result      = task.call();

            if ( !result ) {
                diagnostics.getDiagnostics()
                    .forEach( d -> logger.error( String.valueOf( d ) ) );
                throw new RuntimeException( "Compiler Error" );
            } else {
                // ClassLoader classLoader = manager.getClassLoader( null );
                // Class<?> clazz = ( ( JavaDynamicClassLoader ) classLoader ).define( fqn );
                // BoxJavaClass instanceOfClass = ( BoxJavaClass ) clazz.newInstance();

                JavaDynamicClassLoader classLoader = new JavaDynamicClassLoader(
                    new URL[] {
                        new File( boxRT ).toURI().toURL()
                    },
                    this.getClass().getClassLoader(),
                    manager );

                // JavaDynamicClassLoader classLoader = (JavaDynamicClassLoader) manager.getClassLoader( null );
                // classLoader.defineClass(fqn);
                Class                  cls         = Class.forName( fqn, true, classLoader );
                Method                 meth        = cls.getMethod( "main", String[].class );
                String[]               params      = null; // init params accordingly
                meth.invoke( null, ( Object ) params );

            }
        } catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        } catch ( IllegalAccessException e ) {
            throw new RuntimeException( e );
        } catch ( InvocationTargetException e ) {
            throw new RuntimeException( e );
        } catch ( NoSuchMethodException e ) {
            throw new RuntimeException( e );
        } catch ( MalformedURLException e ) {
            throw new RuntimeException( e );
        }
    }
}
