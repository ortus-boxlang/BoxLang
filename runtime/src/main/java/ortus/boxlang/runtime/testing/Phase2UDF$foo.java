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

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;

// BoxLang Auto Imports
import ortus.boxlang.runtime.dynamic.BaseTemplate;
import ortus.boxlang.runtime.dynamic.Referencer;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.loader.ClassLocator;
import ortus.boxlang.runtime.loader.ImportDefinition;
import ortus.boxlang.runtime.operators.*;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.scopes.IScope;

// Classes Auto-Imported on all Templates and Classes by BoxLang
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2UDF$foo extends UDF {

    private static Phase2UDF$foo instance;

    private Phase2UDF$foo() {
        super(
            Access.PUBLIC,
            Key.of( "foo" ),
            "String",
            new Argument[] {
                new Argument( true, "String", Key.of( "name" ), "Brad", "" )
            },
            "My Function Hint",
            true
        );
    }

    public static synchronized Phase2UDF$foo getInstance() {
        if ( instance == null ) {
            instance = new Phase2UDF$foo();
        }
        return instance;
    }

    /**
     * <pre>
        function greet( required string name='Brad' ) hint="My Function Hint" {
            var greeting = "Hello " & name;
            return greeting;
        }
     * </pre>
     */
    @Override
    public Object invoke( FunctionBoxContext context ) {

        context.getScopeNearby( LocalScope.name ).assign(
            Key.of( "Greeting" ),
            Concat.invoke(
                "Hello ",
                context.scopeFindNearby( Key.of( "name" ), null ).value()
            )
        );

        // TODO: check return type before returning
        return context.scopeFindNearby( Key.of( "greeting" ), null ).value();

    }

}
