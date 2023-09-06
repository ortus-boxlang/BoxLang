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

import java.util.List;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
// BoxLang Auto Imports
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2UDF extends BaseTemplate {

    private static Phase2UDF                    instance;

    private final static List<ImportDefinition> imports = List.of();

    private Phase2UDF() {
    }

    public static synchronized Phase2UDF getInstance() {
        if ( instance == null ) {
            instance = new Phase2UDF();
        }
        return instance;
    }

    /**
     * <pre>
    <cfscript>
        function greet( required string name='Brad' ) hint="My Function Hint" {
            var greeting = "Hello " & name;
            return greeting;
        }

        new java.lang.System.out.println( greet( 'John' ) );
    </cfscript>
     * </pre>
     */

    @Override
    public void invoke( IBoxContext context ) throws Throwable {
        ClassLocator classLocator = ClassLocator.getInstance();

        // Create instance of UDF and register in the variables scope
        context.regsiterUDF( Phase2UDF$foo.getInstance() );

        Referencer.getAndInvoke(
            // Object
            Referencer.get(
                classLocator.load( context, "java:java.lang.System", imports ),
                Key.of( "out" ),
                false ),
            // Method
            Key.of( "println" ),
            // Arguments
            new Object[] {
                context.invokeFunction( Key.of( "foo" ), new Object[] { "John" } )
            },
            false
        );

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
            boxRuntime.executeTemplate( Phase2UDF.getInstance() );
        } catch ( Throwable e ) {
            e.printStackTrace();
            System.exit( 1 );
        }

        // Bye bye! Ciao Bella!
        boxRuntime.shutdown();
    }
}
