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
package ortus.boxlang.runtime.context;

import java.util.Map;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.LocalScope;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.KeyNotFoundException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.util.ArgumentUtil;

/**
 * This context represents the context of any function execution in BoxLang
 * It encapsulates the arguments scope and local scope and has a reference to the function being invoked.
 * This context is extended for use with both UDFs and Closures as well
 */
public class FunctionBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	protected ArgumentsScope	argumentsScope;

	/**
	 * The local scope
	 */
	protected IScope			localScope;

	/**
	 * The Function being invoked with this context
	 */
	protected Function			function;

	/**
	 * The class in which this function is executing, if any
	 */
	protected IClassRunnable	enclosingBoxClass		= null;

	/**
	 * The class in which this function is executing, if any
	 */
	protected DynamicObject		enclosingStaticBoxClass	= null;

	/**
	 * The Function name being invoked with this context. Note this may or may not be the name the function was declared as.
	 */
	protected Key				functionCalledName;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent   The parent context
	 * @param function The function being invoked with this context
	 */
	public FunctionBoxContext( IBoxContext parent, Function function ) {
		this( parent, function, function.getName() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 *
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName ) {
		this( parent, function, functionCalledName, new ArgumentsScope() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent         The parent context
	 * @param function       The function being invoked with this context
	 * @param argumentsScope The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, ArgumentsScope argumentsScope ) {
		this( parent, function, function.getName(), argumentsScope );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param argumentsScope     The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName, ArgumentsScope argumentsScope ) {
		super( parent );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for FunctionBoxContext" );
		}
		if ( function == null ) {
			throw new BoxRuntimeException( "function cannot be null for FunctionBoxContext" );
		}
		this.localScope			= new LocalScope();
		this.argumentsScope		= argumentsScope;
		this.function			= function;
		this.functionCalledName	= functionCalledName;
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent              The parent context
	 * @param function            The function being invoked with this context
	 * @param functionCalledName  The name of the function being invoked
	 * @param positionalArguments The arguments
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName, Object[] positionalArguments, IClassRunnable thisClass ) {
		super( parent );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for FunctionBoxContext" );
		}
		if ( function == null ) {
			throw new BoxRuntimeException( "function cannot be null for FunctionBoxContext" );
		}
		this.localScope			= new LocalScope();
		this.argumentsScope		= new ArgumentsScope();
		this.function			= function;
		this.functionCalledName	= functionCalledName;
		setThisClass( thisClass );
		pushTemplate( function );
		try {
			ArgumentUtil.createArgumentsScope( this, positionalArguments, function.getArguments(), this.argumentsScope, function.getName() );
		} finally {
			popTemplate();
		}
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param namedArguments     The arguments
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName, Map<Key, Object> namedArguments, IClassRunnable thisClass ) {
		super( parent );
		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for FunctionBoxContext" );
		}
		if ( function == null ) {
			throw new BoxRuntimeException( "function cannot be null for FunctionBoxContext" );
		}
		this.localScope			= new LocalScope();
		this.argumentsScope		= new ArgumentsScope();
		this.function			= function;
		this.functionCalledName	= functionCalledName;
		setThisClass( thisClass );
		pushTemplate( function );
		try {
			ArgumentUtil.createArgumentsScope( this, namedArguments, function.getArguments(), this.argumentsScope, function.getName() );
		} finally {
			popTemplate();
		}
	}

	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() ) {
			getParent().getVisibleScopes( scopes, true, shallow );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( ArgumentsScope.name, argumentsScope );
			scopes.getAsStruct( Key.contextual ).put( LocalScope.name, localScope );
		}
		if ( isInClass() ) {
			// A function executing in a class can see the class variables
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, getThisClass().getBottomClass().getVariablesScope() );
			scopes.getAsStruct( Key.contextual ).put( StaticScope.name, getThisClass().getStaticScope() );
		}
		return scopes;
	}

	/**
	 * Returns the function being invoked with this context
	 */
	public Function getFunction() {
		return function;
	}

	/**
	 * Search for a variable in "nearby" scopes
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 * @param shallow      Whether to search only the "nearby" scopes or all scopes
	 *
	 * @return The search result
	 */
	@Override
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow ) {

		if ( key.equals( localScope.getName() ) ) {
			return new ScopeSearchResult( localScope, localScope, key, true );
		}

		if ( key.equals( argumentsScope.getName() ) ) {
			return new ScopeSearchResult( argumentsScope, argumentsScope, key, true );
		}

		if ( key.equals( ThisScope.name ) && isInClass() ) {
			return new ScopeSearchResult( getThisClass().getBottomClass(), getThisClass().getBottomClass(), key, true );
		}
		if ( key.equals( Key._super ) && getThisClass() != null ) {
			if ( getThisClass().getSuper() != null ) {
				return new ScopeSearchResult( getThisClass().getSuper(), getThisClass().getSuper(), key, true );
			} else if ( getThisClass().isJavaExtends() ) {
				var jSuper = DynamicObject.of( getThisClass() ).setTargetClass( getThisClass().getClass().getSuperclass() );
				return new ScopeSearchResult( jSuper, jSuper, key, true );
			}
		}

		if ( key.equals( StaticScope.name ) && isInClass() ) {
			return new ScopeSearchResult( getThisClass().getStaticScope(), getThisClass().getStaticScope(), key, true );
		}

		if ( key.equals( StaticScope.name ) && isInStaticClass() ) {
			IScope staticScope = BoxClassSupport.getStaticScope( getThisStaticClass() );
			return new ScopeSearchResult( staticScope, staticScope, key, true );
		}

		Object result = localScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( localScope, Struct.unWrapNull( result ), key );
		}

		result = argumentsScope.getRaw( key );
		// Null means not found
		if ( result != null ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( argumentsScope, Struct.unWrapNull( result ), key );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		if ( isInClass() ) {
			// A function executing in a class can see the class variables
			IScope classVariablesScope = getThisClass().getBottomClass().getVariablesScope();
			result = classVariablesScope.getRaw( key );
			// Null means not found
			if ( result != null ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( classVariablesScope, Struct.unWrapNull( result ), key );
			}

			if ( shallow ) {
				return null;
			}

			// A component cannot see nearby scopes above it
			return parent.scopeFind( key, defaultScope );
		} else {

			if ( shallow ) {
				return parent.scopeFindNearby( key, defaultScope, true );
			}

			// A UDF is "transparent" and can see everything in the parent scope as a "local" observer
			return parent.scopeFindNearby( key, defaultScope );
		}

	}

	/**
	 * Search for a variable in scopes
	 *
	 * @param key          The key to search for
	 * @param defaultScope The default scope to use if the key is not found
	 *
	 * @return The search result
	 */
	@Override
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope ) {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.scopeFind( key, defaultScope );
	}

	/**
	 * Look for a scope by name
	 *
	 * @param name The name of the scope to look for
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getScope( Key name ) throws ScopeNotFoundException {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.getScope( name );
	}

	/**
	 * Look for a "nearby" scope by name
	 *
	 * @param name The name of the scope to look for
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getScopeNearby( Key name, boolean shallow ) throws ScopeNotFoundException {
		// Check the scopes I know about
		if ( name.equals( localScope.getName() ) ) {
			return localScope;
		}
		if ( name.equals( argumentsScope.getName() ) ) {
			return argumentsScope;
		}

		if ( isInClass() ) {
			if ( name.equals( VariablesScope.name ) ) {
				return getThisClass().getBottomClass().getVariablesScope();
			}

			if ( shallow ) {
				return null;
			}

			// We don't have a check for "this" here because this.foo transpiles to a direct reference to the class itself

			// A component cannot see nearby scopes above it
			return parent.getScope( name );
		} else {

			if ( shallow ) {
				return null;
			}

			// The FunctionBoxContext has no "global" scopes, so just defer to parent
			return parent.getScopeNearby( name );
		}
	}

	/**
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not called from a function
	 */
	@Override
	public Key findClosestFunctionName() {
		return functionCalledName;
	};

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		// CF Source sets into variable scope. BoxLang defaults to local scope
		return getFunction().getSourceType().equals( BoxSourceType.CFSCRIPT ) || getFunction().getSourceType().equals( BoxSourceType.CFTEMPLATE )
		    ? getScopeNearby( VariablesScope.name )
		    : localScope;
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	@Override
	public IBoxContext getFunctionParentContext() {
		// If a function is executed inside another function, it uses the parent since there is nothing a function can "see" from inside it's calling function
		return getParent();
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 */
	public boolean isInClass() {
		return enclosingBoxClass != null;
	}

	/**
	 * Get the class instance this function is inside of
	 */
	public IClassRunnable getThisClass() {
		return enclosingBoxClass;
	}

	/**
	 * Set the enclosing box class
	 *
	 * @param enclosingBoxClass The class in which this function is executing
	 */
	public FunctionBoxContext setThisClass( IClassRunnable enclosingBoxClass ) {
		this.enclosingBoxClass = enclosingBoxClass;
		return this;
	}

	/**
	 * Detects of this Function is executing in the context of a static class *
	 */
	public boolean isInStaticClass() {
		return enclosingStaticBoxClass != null;
	}

	/**
	 * et the static class this function is inside of
	 */
	public DynamicObject getThisStaticClass() {
		return enclosingStaticBoxClass;
	}

	/**
	 * Set the enclosing static box class
	 *
	 * @param enclosingStaticBoxClass The static class in which this function is executing
	 */
	public FunctionBoxContext setThisStaticClass( DynamicObject enclosingStaticBoxClass ) {
		this.enclosingStaticBoxClass = enclosingStaticBoxClass;
		return this;
	}

	/**
	 * Flush the buffer to the output stream and then clears the local buffers
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	public FunctionBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}
		super.flushBuffer( force );
		return this;
	}

	/**
	 * Invoke a function call such as foo() using positional args. Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		if ( isInClass() ) {
			IClassRunnable boxClass = getThisClass();
			if ( boxClass.getSetterLookup().containsKey( name ) || boxClass.getGetterLookup().containsKey( name ) ) {
				return boxClass.dereferenceAndInvoke( this, name, positionalArguments, false );
			}
		}
		return super.invokeFunction( name, positionalArguments );
	}

	/**
	 * Invoke a function call such as foo() using named args. Will check for a registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		if ( isInClass() ) {
			IClassRunnable boxClass = getThisClass();
			if ( boxClass.getSetterLookup().containsKey( name ) || boxClass.getGetterLookup().containsKey( name ) ) {
				return boxClass.dereferenceAndInvoke( this, name, namedArguments, false );
			}
		}
		return super.invokeFunction( name, namedArguments );
	}

	/**
	 * Find a function in the corrent context. Will search known scopes for a UDF.
	 *
	 * @param name The name of the function to find
	 *
	 * @return The function instance
	 */
	protected Function findFunction( Key name ) {
		ScopeSearchResult result = null;
		try {
			result = scopeFindNearby( name, null );
		} catch ( KeyNotFoundException e ) {
			// Ignore
		}
		if ( result != null ) {
			Object value = result.value();
			if ( value instanceof Function fun ) {
				return fun;
			} else {
				throw new BoxRuntimeException(
				    "Variable '" + name + "' of type  '" + value.getClass().getName() + "'  is not a function." );
			}
		}

		if ( isInStaticClass() ) {
			Object staticResult = BoxClassSupport.dereferenceStatic( getThisStaticClass(), this, name, true );
			if ( staticResult != null && staticResult instanceof Function fun ) {
				return fun;
			}
		}
		if ( isInClass() ) {
			Object staticResult = getThisClass().getStaticScope().get( name );
			if ( staticResult != null && staticResult instanceof Function fun ) {
				return fun;
			}

		}
		throw new BoxRuntimeException( "Function '" + name.getName() + "' not found" );
	}

	/**
	 * If this function is executing inside of a BoxClass, register a UDF in the class's variables scope
	 * OTherise, defer to the parent context, which is probably a scripting context
	 *
	 * @param udf The UDF to register
	 */
	public void registerUDF( UDF udf ) {
		if ( isInClass() ) {
			IClassRunnable boxClass = getThisClass();
			if ( udf.hasModifier( BoxMethodDeclarationModifier.STATIC ) ) {
				boxClass.getStaticScope().put( udf.getName(), udf );
				return;
			}
			boxClass.getVariablesScope().put( udf.getName(), udf );
		}
		getParent().registerUDF( udf );
	}

	/**
	 * Can the current context output to the response stream?
	 * Contexts tied to a specific object like a function or class may override this to return false based on their own logic.
	 */
	public Boolean canOutput() {
		return getFunction().canOutput( this );
	}

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	public IClassRunnable getFunctionClass() {
		return isInClass() ? getThisClass().getBottomClass() : null;
	}

}
