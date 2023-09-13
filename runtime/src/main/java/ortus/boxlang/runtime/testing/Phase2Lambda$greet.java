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

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.operators.Concat;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.types.Lambda;

/**
 * Phase 2 BoxLang
 * Example of UDF delcaration and execution
 */
public class Phase2Lambda$greet extends Lambda {

    private static Phase2Lambda$greet instance;

    private Phase2Lambda$greet() {
        super(
            new Argument[] {
                new Argument( true, "String", Key.of( "name" ), "Brad", "" )
            }
        );
    }

    public static synchronized Phase2Lambda$greet getInstance() {
        if ( instance == null ) {
            instance = new Phase2Lambda$greet();
        }
        return instance;
    }

    /**
     * <pre>
        ( required string name='Brad' ) -> {
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

        return context.scopeFindNearby( Key.of( "greeting" ), null ).value();
    }

}
