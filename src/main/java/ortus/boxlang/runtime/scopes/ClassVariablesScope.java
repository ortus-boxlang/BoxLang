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
package ortus.boxlang.runtime.scopes;

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.FunctionBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ArgumentUtil;

/**
 * I'm just like a normal Variables scope, but I know I belong to a class
 */
public class ClassVariablesScope extends VariablesScope {

	private final IClassRunnable	thisClass;
	/**
	 * --------------------------------------------------------------------------
	 * Public Properties
	 * --------------------------------------------------------------------------
	 */
	public static final Key			name	= Key.of( "variables" );

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	public ClassVariablesScope( IClassRunnable thisClass ) {
		super();
		this.thisClass = thisClass;
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */
	public IClassRunnable getThisClass() {
		return this.thisClass;
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {

		MemberDescriptor	memberDescriptor	= BoxRuntime.getInstance().getFunctionService().getMemberMethod( name, BoxLangType.STRUCT );

		Object				value				= get( name );
		if ( value != null ) {

			if ( value instanceof Function function ) {
				FunctionBoxContext fContext = Function.generateFunctionContext(
				    function,
				    context.getFunctionParentContext(),
				    name,
				    positionalArguments,
				    getFunctionContextThisClassForInvoke( context ),
				    getFunctionContextThisInterfaceForInvoke()
				);
				return function.invoke( fContext );
			} else if ( memberDescriptor == null ) {
				throw new BoxRuntimeException(
				    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function " );
			}
		}

		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		if ( containsKey( Key.onMissingMethod ) && !DynamicInteropService.hasMethod( this.getClass(), name.getName() ) ) {
			return thisClass.dereferenceAndInvoke(
			    context,
			    Key.onMissingMethod,
			    new Object[] { name.getName(), ArgumentUtil.createArgumentsScope( context, positionalArguments ) },
			    safe
			);
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		MemberDescriptor	memberDescriptor	= BoxRuntime.getInstance().getFunctionService().getMemberMethod( name, BoxLangType.STRUCT );

		Object				value				= get( name );
		if ( value != null ) {
			if ( value instanceof Function function ) {
				FunctionBoxContext fContext = Function.generateFunctionContext(
				    function,
				    context.getFunctionParentContext(),
				    name,
				    namedArguments,
				    getFunctionContextThisClassForInvoke( context ),
				    getFunctionContextThisInterfaceForInvoke()
				);
				return function.invoke( fContext );
			} else if ( memberDescriptor == null ) {
				throw new BoxRuntimeException(
				    "key '" + name.getName() + "' of type  '" + value.getClass().getName() + "'  is not a function "
				);
			}
		}
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		if ( containsKey( Key.onMissingMethod ) && !DynamicInteropService.hasMethod( this.getClass(), name.getName() ) ) {
			Map<Key, Object> args = new HashMap<>();
			args.put( Key.missingMethodName, name.getName() );
			args.put( Key.missingMethodArguments, ArgumentUtil.createArgumentsScope( context, namedArguments ) );
			return dereferenceAndInvoke( context, Key.onMissingMethod, args, safe );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

	public IClassRunnable getFunctionContextThisClassForInvoke( IBoxContext context ) {
		return thisClass;
	}

}
