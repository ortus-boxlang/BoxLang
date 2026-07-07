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
package ortus.boxlang.runtime.modules;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.RequestBoxContext;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.ThisScope;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.services.InterceptorService;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Adapter that wraps a BoxLang {@link IClassRunnable} module descriptor ({@code ModuleConfig.bx})
 * and exposes it as an {@link IModuleConfig}.
 *
 * <p>
 * This lets {@link ModuleRecord} treat BX- and Java-based module configs identically
 * through the {@link IModuleConfig} contract, with no {@code instanceof} branching in
 * the lifecycle methods.
 * </p>
 *
 * <p>
 * Interceptor registration uses the {@link InterceptorService#register(IClassRunnable)} overload
 * so that BoxLang-metadata-based {@code @interceptionPoint} discovery is preserved.
 * </p>
 */
public class BoxModuleConfig implements IModuleConfig {

	/**
	 * The underlying compiled BoxLang module descriptor class.
	 */
	private final IClassRunnable bxClass;

	/**
	 * @param bxClass The compiled and instantiated {@code ModuleConfig.bx} class
	 */
	public BoxModuleConfig( IClassRunnable bxClass ) {
		this.bxClass = bxClass;
	}

	/**
	 * @return the underlying BoxLang class (needed by {@link ModuleRecord#activate} to
	 *         register per-module BX interceptors declared in {@code variables.interceptors})
	 */
	public IClassRunnable getBxClass() {
		return this.bxClass;
	}

	/**
	 * Invokes the BX {@code configure()} method if present, then extracts
	 * {@code variables.settings}, {@code variables.interceptors}, and
	 * {@code variables.customInterceptionPoints} into the module record.
	 */
	@Override
	public void configure( IBoxContext context, ModuleRecord record ) {
		ThisScope		thisScope		= this.bxClass.getThisScope();
		VariablesScope	variablesScope	= this.bxClass.getVariablesScope();

		if ( thisScope.containsKey( Key.configure ) ) {
			RequestBoxContext.runInContext( context,
			    ctx -> this.bxClass.dereferenceAndInvoke( ctx, Key.configure, DynamicObject.EMPTY_ARGS, false ) );
		}

		record.settings					= ( Struct ) variablesScope.getAsStruct( Key.settings );
		record.interceptors				= variablesScope.getAsArray( Key.interceptors );
		record.customInterceptionPoints	= variablesScope.getAsArray( Key.customInterceptionPoints );
	}

	/**
	 * Invokes the BX {@code onLoad()} method if present.
	 */
	@Override
	public void onLoad( IBoxContext context, ModuleRecord record ) {
		if ( this.bxClass.getThisScope().containsKey( Key.onLoad ) ) {
			RequestBoxContext.runInContext( context,
			    ctx -> this.bxClass.dereferenceAndInvoke( ctx, Key.onLoad, DynamicObject.EMPTY_ARGS, false ) );
		}
	}

	/**
	 * Invokes the BX {@code onUnload()} method if present.
	 */
	@Override
	public void onUnload( IBoxContext context, ModuleRecord record ) {
		if ( this.bxClass.getThisScope().containsKey( Key.onUnload ) ) {
			RequestBoxContext.runInContext(
			    context,
			    ctx -> this.bxClass.dereferenceAndInvoke( ctx, Key.onUnload, DynamicObject.EMPTY_ARGS, false )
			);
		}
	}

	/**
	 * Invokes the BX {@code main()} method, converting {@code String[]} args to a
	 * BoxLang {@link Array}. Throws if the method is not defined.
	 */
	@Override
	public void main( IBoxContext context, String[] args ) {
		if ( !this.bxClass.getThisScope().containsKey( Key.main ) ) {
			throw new BoxRuntimeException( "Module is not executable. It must have a 'main' method in its ModuleConfig.bx descriptor." );
		}
		RequestBoxContext.runInContext(
		    context,
		    ctx -> this.bxClass.dereferenceAndInvoke( ctx, Key.main, new Object[] { Array.fromArray( args ) }, false )
		);
	}

	/**
	 * Registers the underlying BX class via the {@link IClassRunnable} overload so
	 * that {@code @interceptionPoint} annotations are discovered from BX metadata
	 * rather than Java reflection.
	 */
	@Override
	public void registerInterceptor( InterceptorService interceptorService, IStruct settings ) {
		interceptorService.register( this.bxClass );
	}

	/**
	 * Unregisters the underlying BX class from all interception states.
	 */
	@Override
	public void unregisterInterceptor( InterceptorService interceptorService ) {
		interceptorService.unregister( DynamicObject.of( this.bxClass ) );
	}

}
