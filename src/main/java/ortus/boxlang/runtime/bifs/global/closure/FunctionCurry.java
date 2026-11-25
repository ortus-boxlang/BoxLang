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
package ortus.boxlang.runtime.bifs.global.closure;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.CurriedFunction;
import ortus.boxlang.runtime.types.Function;

@BoxBIF( description = "Returns a new function with the given leading arguments already bound, to be supplied the rest of the arguments later" )
@BoxMember( type = BoxLangType.FUNCTION, name = "curry" )
public class FunctionCurry extends BIF {

	/**
	 * Constructor
	 */
	public FunctionCurry() {
		super();
		declaredArguments = new Argument[] {
		    new Argument( true, Argument.ANY, Key.function )
		};
	}

	/**
	 * Binds any number of leading arguments to the given function, returning a new function that,
	 * when called, runs the original with the bound arguments first, followed by whatever
	 * arguments the new function is actually called with.
	 * <p>
	 * Groovy's own {@code closure.curry(a, b)} member syntax (equally usable from any BoxLang
	 * dialect as {@code curry(fn, a, b)}) is the primary motivating idiom - a genuinely runtime
	 * operation on an arbitrary Function/Closure/Lambda value, not something resolvable at parse
	 * time the way most of this Groovy-parser effort's other desugarings are.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.function The function/closure/lambda to curry. Any further positional arguments
	 *                    passed to this BIF are bound as its leading arguments.
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		Function		target		= arguments.getAsFunction( Key.function );
		// A raw values()/asNativeArray() dump of a BIF's own ArgumentsScope also picks up the
		// __isMemberExecution/__functionName bookkeeping entries BIFDescriptor#invoke stuffs into
		// this SAME scope for every BIF call (confirmed empirically - a real bug caught while
		// testing this) - so the extra (undeclared) positional arguments beyond "function" itself
		// are collected explicitly by key instead, skipping both that self-argument and the two
		// reserved bookkeeping keys.
		List<Object>	boundArgs	= new ArrayList<>();
		for ( Map.Entry<Key, Object> entry : arguments.entrySet() ) {
			Key key = entry.getKey();
			if ( key.equals( Key.function ) || key.equals( BIF.__isMemberExecution ) || key.equals( BIF.__functionName ) ) {
				continue;
			}
			boundArgs.add( entry.getValue() );
		}
		return new CurriedFunction( target, boundArgs.toArray() );
	}

}
