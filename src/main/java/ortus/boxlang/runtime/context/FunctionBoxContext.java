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

import java.util.HashMap;
import java.util.Map;

import ortus.boxlang.compiler.ast.statement.BoxMethodDeclarationModifier;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.BoxClassSupport;
import ortus.boxlang.runtime.runnables.BoxInterface;
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
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.util.ArgumentUtil;

/**
 * This context represents the context of any function execution in BoxLang
 * <p>
 * It encapsulates the arguments scope and local scope and has a reference to
 * the function being invoked.
 * <p>
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
	 * The class in which this function is executing in, if any
	 */
	protected DynamicObject		enclosingStaticBoxClass	= null;

	/**
	 * The interface this static function is executing in, if any
	 */
	protected BoxInterface		enclosingBoxInterface	= null;

	/**
	 * The Function name being invoked with this context. Note this may or may not
	 * be the name the function was declared as.
	 */
	protected Key				functionCalledName;

	/**
	 * Creates a new execution context with a bounded function instance and parent
	 * context
	 *
	 * @param parent   The parent context
	 * @param function The function being invoked with this context
	 */
	public FunctionBoxContext( IBoxContext parent, Function function ) {
		this( parent, function, function.getName() );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent
	 * context
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
	 * Creates a new execution context with a bounded function instance and parent
	 * context and arguments scope
	 *
	 * @param parent         The parent context
	 * @param function       The function being invoked with this context
	 * @param argumentsScope The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, ArgumentsScope argumentsScope ) {
		this( parent, function, function.getName(), argumentsScope );
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent
	 * context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param argumentsScope     The arguments scope
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName,
	    ArgumentsScope argumentsScope ) {
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
	 * Creates a new execution context with a bounded function instance and parent
	 * context and arguments scope
	 *
	 * @param parent              The parent context
	 * @param function            The function being invoked with this context
	 * @param functionCalledName  The name of the function being invoked
	 * @param positionalArguments The arguments
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName,
	    Object[] positionalArguments, IClassRunnable thisClass, DynamicObject thisStaticClass, BoxInterface thisInterface ) {
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

		setThisClass( BoxClassSupport.resolveClassForUDF( thisClass, function ) );
		setThisStaticClass( thisStaticClass );
		setThisInterface( thisInterface );
		pushTemplate( function );
		try {
			ArgumentUtil.createArgumentsScope( this, positionalArguments, function.getArguments(), this.argumentsScope,
			    function.getName() );
		} finally {
			popTemplate();
		}
	}

	/**
	 * Creates a new execution context with a bounded function instance and parent
	 * context and arguments scope
	 *
	 * @param parent             The parent context
	 * @param function           The function being invoked with this context
	 * @param functionCalledName The name of the function being invoked
	 * @param namedArguments     The arguments
	 */
	public FunctionBoxContext( IBoxContext parent, Function function, Key functionCalledName,
	    Map<Key, Object> namedArguments, IClassRunnable thisClass, DynamicObject thisStaticClass, BoxInterface thisInterface ) {
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
		setThisClass( BoxClassSupport.resolveClassForUDF( thisClass, function ) );
		setThisStaticClass( thisStaticClass );
		setThisInterface( thisInterface );
		pushTemplate( function );
		try {
			ArgumentUtil.createArgumentsScope( this, namedArguments, function.getArguments(), this.argumentsScope,
			    function.getName() );
		} finally {
			popTemplate();
		}
	}

	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() ) {
			getParent().getVisibleScopes( scopes, true && nearby, shallow );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( ArgumentsScope.name, argumentsScope );
			scopes.getAsStruct( Key.contextual ).put( LocalScope.name, localScope );
		}
		if ( isInClass() ) {
			// A function executing in a class can see the class variables
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name,
			    getThisClass().getBottomClass().getVariablesScope() );
			scopes.getAsStruct( Key.contextual ).put( StaticScope.name, getThisClass().getStaticScope() );
		}
		return scopes;
	}

	/**
	 * Check if a key is visible in the current context as a scope name.
	 * This allows us to "reserve" known scope names to ensure arguments.foo
	 * will always look in the proper arguments scope and never in
	 * local.arguments.foo for example
	 * 
	 * @param key     The key to check for visibility
	 * @param nearby  true, check only scopes that are nearby to the current execution context
	 * @param shallow true, do not delegate to parent or default scope if not found
	 * 
	 * @return True if the key is visible in the current context, else false
	 */
	@Override
	public boolean isKeyVisibleScope( Key key, boolean nearby, boolean shallow ) {
		if ( nearby && ( key.equals( ArgumentsScope.name ) || key.equals( LocalScope.name ) ) ) {
			return true;
		}
		if ( isInClass() ) {
			if ( key.equals( VariablesScope.name ) || key.equals( StaticScope.name ) || key.equals( ThisScope.name ) ) {
				return true;
			}
			if ( key.equals( Key._super ) && ( getThisClass().getSuper() != null || getThisClass().isJavaExtends() ) ) {
				return true;
			}
		}

		return super.isKeyVisibleScope( key, true && nearby, shallow );
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
	public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) && isInClass() ) {
			return new ScopeSearchResult( getThisClass().getBottomClass(), getThisClass().getBottomClass().getBoxMeta(),
			    BoxMeta.key, false );
		}

		// Look in the local scope first
		if ( key.equals( localScope.getName() ) ) {
			return new ScopeSearchResult( localScope, localScope, key, true );
		}

		// Look in the arguments scope next
		if ( key.equals( argumentsScope.getName() ) ) {
			return new ScopeSearchResult( argumentsScope, argumentsScope, key, true );
		}

		ScopeSearchResult thisSerach = scopeFindThis( key );
		if ( thisSerach != null ) {
			return thisSerach;
		}

		ScopeSearchResult superSearch = scopeFindSuper( key );
		if ( superSearch != null ) {
			return superSearch;
		}

		// Look in the static scope next
		if ( key.equals( StaticScope.name ) && isInClass() ) {
			return new ScopeSearchResult( getThisClass().getStaticScope(), getThisClass().getStaticScope(), key, true );
		}

		// Look in the static scope next for a static class
		if ( key.equals( StaticScope.name ) && isInStaticClass() ) {
			IScope staticScope = BoxClassSupport.getStaticScope( this, getThisStaticClass() );
			return new ScopeSearchResult( staticScope, staticScope, key, true );
		}
		// Look in the static scope next for an interface
		if ( key.equals( StaticScope.name ) && isInInterface() ) {
			IScope staticScope = getThisInterface().getStaticScope();
			return new ScopeSearchResult( staticScope, staticScope, key, true );
		}

		Object	result;
		boolean	isKeyVisibleScope	= isKeyVisibleScope( key );
		if ( !isKeyVisibleScope ) {
			result = localScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( localScope, Struct.unWrapNull( result ), key );
			}

			result = argumentsScope.getRaw( key );
			// Null means not found
			if ( isDefined( result, forAssign ) ) {
				// Unwrap the value now in case it was really actually null for real
				return new ScopeSearchResult( argumentsScope, Struct.unWrapNull( result ), key );
			}

			// In query loop?
			var querySearch = queryFindNearby( key );
			if ( querySearch != null ) {
				return querySearch;
			}
		}

		if ( isInClass() ) {
			if ( !isKeyVisibleScope ) {
				// A function executing in a class can see the class variables
				IScope classVariablesScope = getThisClass().getBottomClass().getVariablesScope();
				result = classVariablesScope.getRaw( key );
				// Null means not found
				if ( isDefined( result, forAssign ) ) {
					// Unwrap the value now in case it was really actually null for real
					return new ScopeSearchResult( classVariablesScope, Struct.unWrapNull( result ), key );
				}
			}

			if ( shallow ) {
				return null;
			}

			// A component cannot see nearby scopes above it
			// "skip over" other parent function calls which are all part of this class. We've already looked in the scopes like variables, this, and static here, so
			// there are no other visible scopes in this class. At this point, it would need to be a scope like request, or application.
			return getParentContextNotInSameClass().scopeFind( key, defaultScope, forAssign );
		} else {

			if ( shallow ) {
				return parent.scopeFindNearby( key, defaultScope, true );
			}

			// A UDF is "transparent" and can see everything in the parent scope as a
			// "local" observer
			return parent.scopeFindNearby( key, defaultScope, forAssign );
		}

	}

	/**
	 * This scope lookup abstracted for thread context to use
	 * 
	 * @param key The key to search for
	 * 
	 * @return The search result or null if not foud
	 */
	protected ScopeSearchResult scopeFindThis( Key key ) {
		// Look in the "this" scope next
		if ( key.equals( ThisScope.name ) && isInClass() ) {
			return new ScopeSearchResult( getThisClass().getBottomClass(), getThisClass().getBottomClass(), key, true );
		}
		return null;
	}

	/**
	 * Super scope lookup abstracted for thread context to use
	 * 
	 * @param key The key to search for
	 * 
	 * @return The search result or null if not foud
	 */
	protected ScopeSearchResult scopeFindSuper( Key key ) {
		// Look in the "super" scope next
		if ( key.equals( Key._super ) && isInClass() ) {
			if ( getThisClass().getSuper() != null ) {
				return new ScopeSearchResult( getThisClass().getSuper(), getThisClass().getSuper(), key, true );
			} else if ( getThisClass().isJavaExtends() ) {
				var jSuper = DynamicObject.of( getThisClass() ).setTargetClass( getThisClass().getClass().getSuperclass() );
				return new ScopeSearchResult( jSuper, jSuper, key, true );
			}
		}
		return null;
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
	public ScopeSearchResult scopeFind( Key key, IScope defaultScope, boolean forAssign ) {
		// The FunctionBoxContext has no "global" scopes, so just defer to parent
		return parent.scopeFind( key, defaultScope, forAssign );
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

			// We don't have a check for "this" here because this.foo transpiles to a direct
			// reference to the class itself

			// A component cannot see nearby scopes above it
			return parent.getScope( name );
		} else {

			// functions not in a class, are sort of see-through and we'll defer to the parent context (which could be a script, custom tag, etc)
			// Pass along the shallow flag
			return parent.getScopeNearby( name, shallow );
		}
	}

	/**
	 * Finds the closest function call name
	 *
	 * @return The called name of the function if found, null if this code is not
	 *         called from a function
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
		var sourceType = getFunction().getSourceType();
		if ( sourceType.equals( BoxSourceType.CFSCRIPT ) || sourceType.equals( BoxSourceType.CFTEMPLATE ) ) {
			// If we are in a static initializer or a function not in a class at all, defer to the parent
			if ( getParent() instanceof StaticClassBoxContext || !isInClass() ) {
				return getParent().getDefaultAssignmentScope();
			} else {
				// Otherwise, non-static functions in a class use the closest variables scope
				return getScopeNearby( VariablesScope.name );
			}
		} else {
			return localScope;
		}
	}

	/**
	 * Get parent context for a function execution happening in this context
	 *
	 * @return The context to use
	 */
	@Override
	public IBoxContext getFunctionParentContext() {
		return this;
	}

	/**
	 * Detects of this Function is executing in the context of a class
	 */
	public boolean isInClass() {
		return this.enclosingBoxClass != null;
	}

	/**
	 * Get the class instance this function is inside of
	 */
	public IClassRunnable getThisClass() {
		return this.enclosingBoxClass;
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
	 * @param enclosingStaticBoxClass The static class in which this function is
	 *                                executing
	 */
	public FunctionBoxContext setThisStaticClass( DynamicObject enclosingStaticBoxClass ) {
		this.enclosingStaticBoxClass = enclosingStaticBoxClass;
		return this;
	}

	/**
	 * Detects of this Function is executing in the context of a static class *
	 */
	public boolean isInInterface() {
		return enclosingBoxInterface != null;
	}

	/**
	 * et the static class this function is inside of
	 */
	public BoxInterface getThisInterface() {
		return enclosingBoxInterface;
	}

	/**
	 * Set the enclosing static box class
	 *
	 * @param enclosingBoxInterface The static class in which this function is
	 *                              executing
	 */
	public FunctionBoxContext setThisInterface( BoxInterface enclosingBoxInterface ) {
		this.enclosingBoxInterface = enclosingBoxInterface;
		return this;
	}

	/**
	 * Flush the buffer to the output stream and then clears the local buffers
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	@Override
	public FunctionBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}
		super.flushBuffer( force );
		return this;
	}

	/**
	 * Invoke a function call such as foo() using positional args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, positionalArguments, false, name );
		}

		Function function = findFunction( name );
		if ( function == null ) {

			if ( isInClass() && getThisClass().getBottomClass().getVariablesScope().containsKey( Key.onMissingMethod ) ) {
				return getThisClass().getBottomClass().getVariablesScope().dereferenceAndInvoke(
				    this,
				    Key.onMissingMethod,
				    new Object[] { name.getName(), ArgumentUtil.createArgumentsScope( this, positionalArguments ) },
				    false
				);
			} else {
				throw new BoxRuntimeException( "Function [" + name + "] not found" );
			}

		}
		return invokeFunction( function, name, positionalArguments );
	}

	/**
	 * Invoke a function call such as foo() using named args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, namedArguments, false, name );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			if ( isInClass() && getThisClass().getBottomClass().getVariablesScope().containsKey( Key.onMissingMethod ) ) {
				Map<Key, Object> args = new HashMap<>();
				args.put( Key.missingMethodName, name.getName() );
				args.put( Key.missingMethodArguments, ArgumentUtil.createArgumentsScope( this, namedArguments ) );
				return getThisClass().getBottomClass().getVariablesScope().dereferenceAndInvoke( this, Key.onMissingMethod, args, false );
			} else {
				throw new BoxRuntimeException( "Function [" + name + "] not found" );
			}
		}
		return invokeFunction( function, name, namedArguments );
	}

	/**
	 * Invoke a function call such as foo() using no args.
	 *
	 * @return Return value of the function call
	 */
	public Object invokeFunction( Key name ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, false );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			if ( isInClass() && getThisClass().getBottomClass().getVariablesScope().containsKey( Key.onMissingMethod ) ) {
				return getThisClass().getBottomClass().getVariablesScope().dereferenceAndInvoke(
				    this,
				    Key.onMissingMethod,
				    new Object[] { name.getName(), ArgumentUtil.createArgumentsScope( this, new Object[] {} ) },
				    false
				);
			} else {
				throw new BoxRuntimeException( "Function [" + name + "] not found" );
			}
		}
		return invokeFunction( function, name, new Object[] {} );
	}

	/**
	 * Find a function in the corrent context. Will search known scopes for a UDF.
	 *
	 * @param name The name of the function to find
	 *
	 * @return The function instance
	 */
	@Override
	protected Function findFunction( Key name ) {
		ScopeSearchResult result = null;
		try {
			result = scopeFindNearby( name, null, false );
		} catch ( KeyNotFoundException e ) {
			// Ignore
		}
		// Did we find a function in a nearby scope?
		if ( result != null ) {
			Object value = result.value();
			if ( value instanceof Function fun ) {
				return fun;
			} else if ( value == null ) {
				throw new BoxRuntimeException(
				    "Variable '" + name + "' is null and cannot be used as a function." );
			} else {
				throw new BoxRuntimeException(
				    "Variable '" + name + "' of type  '" + value.getClass().getName() + "'  is not a function." );
			}
		}

		// Check for a function if it's in an interface
		if ( isInInterface() ) {
			Object staticResult = getThisInterface().dereference( this, name, true );
			if ( staticResult != null && staticResult instanceof Function fun ) {
				return fun;
			}
		}
		// Check for a function if it's in a static class
		if ( isInStaticClass() ) {
			Object staticResult = BoxClassSupport.dereferenceStatic( getThisStaticClass(), this, name, true );
			if ( staticResult != null && staticResult instanceof Function fun ) {
				return fun;
			}
		}
		// Check for a function if it's in a class
		if ( isInClass() ) {
			// Check for a static function
			Object staticResult = getThisClass().getStaticScope().get( name );
			if ( staticResult != null && staticResult instanceof Function fun ) {
				return fun;
			}
		}

		return null;
	}

	/**
	 * If this function is executing inside of a BoxClass, register a UDF in the
	 * class's variables scope
	 * OTherise, defer to the parent context, which is probably a scripting context
	 *
	 * @param udf The UDF to register
	 */
	@Override
	public void registerUDF( UDF udf, boolean override ) {
		// If we're in a class, register it there
		if ( isInClass() ) {
			IClassRunnable boxClass = getThisClass();
			if ( udf.hasModifier( BoxMethodDeclarationModifier.STATIC ) ) {
				registerUDF( boxClass.getStaticScope(), udf, override );
				return;
			}
			// Register in variables (private)
			registerUDF( boxClass.getBottomClass().getVariablesScope(), udf, override );

			// if public, put there as well
			if ( udf.getAccess().isEffectivePublic() ) {
				registerUDF( boxClass.getBottomClass().getThisScope(), udf, override );
			}
		} else {
			// else, defer to parent context
			getParent().registerUDF( udf, override );
		}
	}

	/**
	 * Can the current context output to the response stream?
	 * Contexts tied to a specific object like a function or class may override this
	 * to return false based on their own logic.
	 */
	@Override
	public Boolean canOutput() {
		return getFunction().canOutput( this );
	}

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	@Override
	public IClassRunnable getFunctionClass() {
		return isInClass() ? getThisClass().getBottomClass() : null;
	}

	/**
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	@Override
	public DynamicObject getFunctionStaticClass() {
		return isInStaticClass() ? getThisStaticClass() : null;
	}

	/**
	 * Get the static class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	@Override
	public BoxInterface getFunctionInterface() {
		return isInInterface() ? getThisInterface() : null;
	}

	/**
	 * Get the arguments scope
	 *
	 * @return The arguments scope
	 */
	public ArgumentsScope getArgumentsScope() {
		return argumentsScope;
	}

	/**
	 * Climb the parents until we reach one that is not a function call in the same class.
	 * 
	 * @return The parent context
	 */
	private IBoxContext getParentContextNotInSameClass() {
		IBoxContext parent = getParent();
		while ( parent instanceof FunctionBoxContext fbc && fbc.isInClass() && fbc.getThisClass().equals( getThisClass() ) ) {
			parent = parent.getParent();
		}
		return parent;
	}

}
