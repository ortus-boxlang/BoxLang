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
package ortus.boxlang.runtime.testing;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import ortus.boxlang.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.runnables.BoxTemplate;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2Lambda extends BoxTemplate {

    private static Phase2Lambda                 instance;

    private final static List<ImportDefinition> imports        = List.of();

    private static final Path                   path           = Paths.get( "runtime\\src\\main\\java\\ortus\\boxlang\\runtime\\testing\\Phase2Lambda.java" );
    private static final long                   compileVersion = 1L;
    private static final LocalDateTime          compiledOn     = LocalDateTime.parse( "2023-09-27T10:15:30" );
    private static final Object                 ast            = null;

    private Phase2Lambda() {
    }

    public static synchronized Phase2Lambda getInstance() {
        if ( instance == null ) {
            instance = new Phase2Lambda();
        }
        return instance;
    }

    /*
     * <pre>
     * <cfscript>
     * variables.greet = ( required string name='Brad' ) -> {
     * var greeting = "Hello " & name;
     * return greeting;
     * }
     *
     * variables.out = (create java.lang.System).out;
     *
     * // Positional args
     * variables.out.println( greet( 'John' ) );
     *
     * // named args
     * variables.out.println( greet( name='John' ) );
     * </cfscript>
     * </pre>
     */

    @Override
    public void _invoke( IBoxContext context ) {
        ClassLocator classLocator   = ClassLocator.getInstance();
        IScope       variablesScope = context.getScopeNearby( Key.of( "variables" ) );

        // Create instance of Lambda and set in the variables scope
        variablesScope.assign(
            context,
            Key.of( "greet" ),
            Phase2Lambda$lambda1.getInstance()
        );

        variablesScope.assign(
            context,
            Key.of( "out" ),
            Referencer.get(
                context,
                classLocator.load( context, "java:java.lang.System", imports ),
                Key.of( "out" ),
                false )
        );

        // Positional args
        Referencer.getAndInvoke(
            context,
            // Object
            variablesScope.get( Key.of( "out" ) ),
            // Method
            Key.of( "println" ),
            // Arguments
            new Object[] {
                context.invokeFunction( Key.of( "greet" ), new Object[] { "John" } )
            },
            false
        );

        // named args
        Referencer.getAndInvoke(
            context,
            // Object
            variablesScope.get( Key.of( "out" ) ),
            // Method
            Key.of( "println" ),
            // Arguments
            new Object[] {
                context.invokeFunction( Key.of( "greet" ), Map.of( Key.of( "name" ), "Bob" ) )
            },
            false
        );

    }

    // ITemplateRunnable implementation methods

    /**
     * The version of the BoxLang runtime
     */
    public long getRunnableCompileVersion() {
        return Phase2Lambda.compileVersion;
    }

    /**
     * The date the template was compiled
     */
    public LocalDateTime getRunnableCompiledOn() {
        return Phase2Lambda.compiledOn;
    }

    /**
     * The AST (abstract syntax tree) of the runnable
     */
    public Object getRunnableAST() {
        return Phase2Lambda.ast;
    }

    /**
     * The path to the template
     */
    public Path getRunnablePath() {
        return Phase2Lambda.path;
    }

    /**
     * The original source type
     */
    public BoxSourceType getSourceType() {
        return BoxSourceType.BOXSCRIPT;
    }

    /**
     * The imports for this runnable
     */
    public List<ImportDefinition> getImports() {
        return imports;
    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main( String[] args ) {
        // This is the main method, it will be invoked when the template is executed
        // You can use this
        // Get a runtime going
        BoxRuntime boxRuntime = BoxRuntime.getInstance( true );

        try {
            boxRuntime.executeTemplate( Phase2Lambda.getInstance() );
        } catch ( Throwable e ) {
            e.printStackTrace();
            System.exit( 1 );
        }

        // Bye bye! Ciao Bella!
        boxRuntime.shutdown();
    }
}
