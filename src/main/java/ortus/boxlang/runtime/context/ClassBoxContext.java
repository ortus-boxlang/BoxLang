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
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.StaticScope;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Function;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.UDF;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.exceptions.ScopeNotFoundException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.util.ArgumentUtil;

/**
 * This context represents the pseduo constructor of a Box Class
 */
public class ClassBoxContext extends BaseBoxContext {

	/**
	 * The arguments scope
	 */
	protected IScope			variablesScope;

	/**
	 * The local scope
	 */
	protected IScope			thisScope;

	/**
	 * The static scope
	 */
	protected IScope			staticScope;

	/**
	 * The local scope
	 */
	protected IClassRunnable	thisClass;

	/**
	 * Creates a new execution context with a bounded function instance and parent context
	 *
	 * @param parent    The parent context
	 * @param thisClass The function instance
	 */
	public ClassBoxContext( IBoxContext parent, IClassRunnable thisClass ) {
		super( parent );
		this.variablesScope	= thisClass.getVariablesScope();
		this.thisScope		= thisClass.getThisScope();
		this.staticScope	= thisClass.getStaticScope();
		this.thisClass		= thisClass;

		if ( parent == null ) {
			throw new BoxRuntimeException( "Parent context cannot be null for ClassBoxContext" );
		}
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct getVisibleScopes( IStruct scopes, boolean nearby, boolean shallow ) {
		if ( hasParent() && !shallow ) {
			getParent().getVisibleScopes( scopes, false, false );
		}
		if ( nearby ) {
			scopes.getAsStruct( Key.contextual ).put( ThisScope.name, thisScope );
			scopes.getAsStruct( Key.contextual ).put( VariablesScope.name, variablesScope );
			scopes.getAsStruct( Key.contextual ).put( StaticScope.name, staticScope );

		}
		return scopes;
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

		// Direct Scope
		ScopeSearchResult thisSerach = scopeFindThis( key );
		if ( thisSerach != null ) {
			return thisSerach;
		}

		// Static Scope
		if ( key.equals( StaticScope.name ) ) {
			return new ScopeSearchResult( staticScope, staticScope, key, true );
		}

		// Super Class
		ScopeSearchResult superSearch = scopeFindSuper( key );
		if ( superSearch != null ) {
			return superSearch;
		}

		// Special check for $bx
		if ( key.equals( BoxMeta.key ) ) {
			return new ScopeSearchResult( getThisClass(), getThisClass().getBoxMeta(), BoxMeta.key, false );
		}

		// In query loop?
		var querySearch = queryFindNearby( key );
		if ( querySearch != null ) {
			return querySearch;
		}

		Object result = variablesScope.getRaw( key );
		// Null means not found
		if ( isDefined( result, forAssign ) ) {
			// Unwrap the value now in case it was really actually null for real
			return new ScopeSearchResult( variablesScope, Struct.unWrapNull( result ), key );
		}

		if ( shallow ) {
			return null;
		}

		// A component cannot see nearby scopes above it
		return parent.scopeFind( key, defaultScope, forAssign );

	}

	/**
	 * This scope lookup abstracted for thread context to use
	 *
	 * @param key The key to search for
	 *
	 * @return The search result or null if not foud
	 */
	protected ScopeSearchResult scopeFindThis( Key key ) {
		if ( key.equals( thisScope.getName() ) ) {
			return new ScopeSearchResult( getThisClass(), getThisClass(), key, true );
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
		if ( key.equals( Key._super ) ) {
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
		// The class context has no "global" scopes, so just defer to parent
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
		// The class context has no "global" scopes, so just defer to parent
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
		if ( name.equals( variablesScope.getName() ) ) {
			return variablesScope;
		}

		if ( name.equals( StaticScope.name ) ) {
			return staticScope;
		}

		if ( shallow ) {
			return null;
		}

		// The class context has no "global" scopes, so just defer to parent
		return parent.getScope( name );
	}

	/**
	 * Get the default variable assignment scope for this context
	 *
	 * @return The scope reference to use
	 */
	@Override
	public IScope getDefaultAssignmentScope() {
		return variablesScope;
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
	 * Get the class, if any, for a function invocation
	 *
	 * @return The class to use, or null if none
	 */
	@Override
	public IClassRunnable getFunctionClass() {
		return thisClass;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public void registerUDF( UDF udf, boolean override ) {
		if ( udf.hasModifier( BoxMethodDeclarationModifier.STATIC ) ) {
			registerUDF( staticScope, udf, override );
			return;
		}

		registerUDF( variablesScope, udf, override );

		// TODO: actually enforce this when the UDF is called.

		if ( udf.getAccess().isEffectivePublic() ) {
			registerUDF( thisScope, udf, override );
		}
	}

	/**
	 * Get the class for this context
	 */
	public IClassRunnable getThisClass() {
		return this.thisClass;
	}

	/**
	 * Flush the buffer to the output stream and then clears the local buffers
	 *
	 * @param force true, flush even if output is disabled
	 *
	 * @return This context
	 */
	@Override
	public IBoxContext flushBuffer( boolean force ) {
		if ( !canOutput() && !force ) {
			return this;
		}
		super.flushBuffer( force );
		return this;
	}

	/**
	 * A helper to look at the "output" annotation, caching the result
	 *
	 * @return Whether the function can output
	 */
	@Override
	public Boolean canOutput() {
		return getThisClass().canOutput();
	}

	/**
	 * Invoke a function call such as foo() using positional args. Will check for a
	 * registered BIF first, then search known scopes for a UDF.
	 *
	 * @return Return value of the function call
	 */
	@Override
	public Object invokeFunction( Key name, Object[] positionalArguments ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, positionalArguments, false, name );
		}

		Function function = findFunction( name );
		if ( function == null ) {

			if ( thisClass.getVariablesScope().containsKey( Key.onMissingMethod ) ) {
				return thisClass.getVariablesScope().dereferenceAndInvoke(
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
	@Override
	public Object invokeFunction( Key name, Map<Key, Object> namedArguments ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, namedArguments, false, name );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			if ( thisClass.getVariablesScope().containsKey( Key.onMissingMethod ) ) {
				Map<Key, Object> args = new HashMap<>();
				args.put( Key.missingMethodName, name.getName() );
				args.put( Key.missingMethodArguments, ArgumentUtil.createArgumentsScope( this, namedArguments ) );
				return thisClass.getVariablesScope().dereferenceAndInvoke( this, Key.onMissingMethod, args, false );
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
	@Override
	public Object invokeFunction( Key name ) {
		BIFDescriptor bif = findBIF( name );
		if ( bif != null ) {
			return bif.invoke( this, false );
		}

		Function function = findFunction( name );
		if ( function == null ) {
			if ( thisClass.getVariablesScope().containsKey( Key.onMissingMethod ) ) {
				return thisClass.getVariablesScope().dereferenceAndInvoke(
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
	 * Get the "this" scope
	 *
	 * @return The "this" scope
	 */
	public IScope getThisScope() {
		return thisScope;
	}

}
