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
package ortus.boxlang.runtime.types;

import java.util.HashMap;

import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;

public class SampleClosure extends Closure {

    Object returnVal = null;

    static {
        arguments = new Argument[] {};
        metadata  = new HashMap<Key, Object>();
    }

    public SampleClosure( Argument[] arguments, IBoxContext declaringContext, Object returnVal ) {
        super( declaringContext );
        this.returnVal = returnVal;
        // This is not how "real" closures will work. I'm just doing this to re-use this sample class for testing.
        this.arguments = arguments;
    }

    @Override
    public Object invoke( FunctionBoxContext context ) {
        return returnVal;
    }
}