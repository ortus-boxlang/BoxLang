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
package ortus.boxlang.runtime.dynamic;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Runtime helper for object destructuring assignment.
 */
public class ObjectDestructurer {

	/**
	 * Destructure a source object into assignment targets.
	 *
	 * @param context         execution context
	 * @param sourceValue     source value to destructure
	 * @param isFinal         whether to assign as final
	 * @param mustBeScopeName required scope for assignment (var/static) or null
	 * @param bindings        destructuring binding descriptors
	 *
	 * @return original source value
	 */
	public static Object destructure( IBoxContext context, Object sourceValue, boolean isFinal, Key mustBeScopeName, Binding[] bindings ) {
		IStruct source = toStructOrThrow( sourceValue, "root" );
		applyBindings( context, source, isFinal, mustBeScopeName, bindings == null ? new Binding[] {} : bindings, "root" );
		return sourceValue;
	}

	/**
	 * binding.
	 */
	public static Binding binding( String sourceKey, Target target, Binding[] nested, Function<Object, Object> defaultValueSupplier ) {
		return new Binding( Key.of( sourceKey ), target, nested == null ? new Binding[] {} : nested, defaultValueSupplier, false );
	}

	/**
	 * rest.
	 */
	public static Binding rest( Target target ) {
		return new Binding( null, target, new Binding[] {}, null, true );
	}

	/**
	 * target.
	 */
	public static Target target( boolean scoped, String... path ) {
		Key[] keys = Arrays.stream( path ).map( Key::of ).toArray( Key[]::new );
		return new Target( scoped, keys );
	}

	/**
	 * applyBindings.
	 */
	private static void applyBindings( IBoxContext context, IStruct source, boolean isFinal, Key mustBeScopeName, Binding[] bindings, String pathPrefix ) {
		Set<Key>	consumed	= new HashSet<>();
		Binding		restBinding	= null;

		for ( Binding binding : bindings ) {
			if ( binding.isRest() ) {
				restBinding = binding;
				continue;
			}

			Key		sourceKey	= binding.getSourceKey();
			String	keyPath		= joinPath( pathPrefix, sourceKey.getName() );
			consumed.add( sourceKey );

			boolean	hasKey	= source.containsKey( sourceKey );
			Object	value	= hasKey ? source.get( sourceKey ) : null;

			if ( binding.hasNested() ) {
				if ( !hasKey && binding.getDefaultValueSupplier() == null ) {
					for ( Binding nestedBinding : binding.getNested() ) {
						applyMissingBinding( context, nestedBinding, isFinal, mustBeScopeName, keyPath );
					}
					continue;
				}

				if ( !hasKey ) {
					value = binding.getDefaultValueSupplier().apply( null );
				}

				IStruct nestedSource = toStructOrThrow( value, keyPath );
				applyBindings( context, nestedSource, isFinal, mustBeScopeName, binding.getNested(), keyPath );
				continue;
			}

			if ( !hasKey && binding.getDefaultValueSupplier() != null ) {
				value = binding.getDefaultValueSupplier().apply( null );
			}

			assignTarget( context, binding.getTarget(), value, isFinal, mustBeScopeName );
		}

		if ( restBinding != null ) {
			Struct rest = new Struct();
			source.forEach( ( key, value ) -> {
				if ( !consumed.contains( key ) ) {
					rest.put( key, value );
				}
			} );
			assignTarget( context, restBinding.getTarget(), rest, isFinal, mustBeScopeName );
		}
	}

	/**
	 * applyMissingBinding.
	 */
	private static void applyMissingBinding( IBoxContext context, Binding binding, boolean isFinal, Key mustBeScopeName, String pathPrefix ) {
		if ( binding.isRest() ) {
			assignTarget( context, binding.getTarget(), new Struct(), isFinal, mustBeScopeName );
			return;
		}

		String	keyPath	= joinPath( pathPrefix, binding.getSourceKey().getName() );
		Object	value	= binding.getDefaultValueSupplier() != null ? binding.getDefaultValueSupplier().apply( null ) : null;

		if ( binding.hasNested() ) {
			if ( binding.getDefaultValueSupplier() == null ) {
				for ( Binding nestedBinding : binding.getNested() ) {
					applyMissingBinding( context, nestedBinding, isFinal, mustBeScopeName, keyPath );
				}
				return;
			}

			IStruct nestedSource = toStructOrThrow( value, keyPath );
			applyBindings( context, nestedSource, isFinal, mustBeScopeName, binding.getNested(), keyPath );
			return;
		}

		assignTarget( context, binding.getTarget(), value, isFinal, mustBeScopeName );
	}

	/**
	 * assignTarget.
	 */
	private static void assignTarget( IBoxContext context, Target target, Object value, boolean isFinal, Key mustBeScopeName ) {
		if ( target == null || target.getPath().length == 0 ) {
			throw new BoxRuntimeException( "Invalid destructuring target" );
		}

		if ( target.isScoped() ) {
			if ( mustBeScopeName != null ) {
				throw new BoxRuntimeException( "Scoped targets are not allowed in var/final/static destructuring declarations." );
			}
			if ( target.getPath().length < 2 ) {
				throw new BoxRuntimeException( "Scoped destructuring targets must include at least one member key." );
			}

			Key		scopeName	= target.getPath()[ 0 ];
			IScope	scope		= context.getScopeNearby( scopeName );
			Key[]	keys		= Arrays.copyOfRange( target.getPath(), 1, target.getPath().length );
			Referencer.setDeep( context, isFinal, scopeName, scope, value, keys );
			return;
		}

		if ( target.getPath().length != 1 ) {
			throw new BoxRuntimeException( "Unscoped destructuring targets must be a single identifier." );
		}

		if ( mustBeScopeName != null ) {
			ScopeSearchResult scope = context.scopeFindNearby( mustBeScopeName, null, true );
			Referencer.setDeep( context, isFinal, mustBeScopeName, scope.scope(), value, target.getPath() );
			return;
		}

		ScopeSearchResult scope = context.scopeFindNearby( target.getPath()[ 0 ], context.getDefaultAssignmentScope(), true );
		Referencer.setDeep( context, isFinal, null, scope, value );
	}

	/**
	 * toStructOrThrow.
	 */
	private static IStruct toStructOrThrow( Object value, String path ) {
		CastAttempt<IStruct> casted = StructCaster.attempt( value );
		if ( !casted.wasSuccessful() ) {
			throw new BoxRuntimeException(
			    "Cannot destructure path [" + path + "] from non-struct value of type [" + describeType( value ) + "]." );
		}
		return casted.get();
	}

	/**
	 * describeType.
	 */
	private static String describeType( Object value ) {
		return value == null ? "null" : value.getClass().getName();
	}

	/**
	 * joinPath.
	 */
	private static String joinPath( String prefix, String key ) {
		if ( prefix == null || prefix.isEmpty() ) {
			return key;
		}
		return prefix + "." + key;
	}

	public static final class Binding {

		private final Key						sourceKey;
		private final Target					target;
		private final Binding[]					nested;
		private final Function<Object, Object>	defaultValueSupplier;
		private final boolean					rest;

		private Binding( Key sourceKey, Target target, Binding[] nested, Function<Object, Object> defaultValueSupplier, boolean rest ) {
			this.sourceKey				= sourceKey;
			this.target					= target;
			this.nested					= nested;
			this.defaultValueSupplier	= defaultValueSupplier;
			this.rest					= rest;
		}

			/**
			 * @return source key read from the source struct.
			 */
			public Key getSourceKey() {
				return sourceKey;
			}

			/**
			 * @return assignment target descriptor.
			 */
			public Target getTarget() {
				return target;
			}

			/**
			 * @return nested destructuring bindings, or null when none.
			 */
			public Binding[] getNested() {
				return nested;
			}

			/**
			 * @return default value supplier, or null when no default is configured.
			 */
			public Function<Object, Object> getDefaultValueSupplier() {
				return defaultValueSupplier;
			}

			/**
			 * @return true when this binding captures the rest segment.
			 */
			public boolean isRest() {
				return rest;
			}

			/**
			 * @return true when this binding has nested bindings.
			 */
			public boolean hasNested() {
				return nested != null && nested.length > 0;
			}
	}

	public static final class Target {

		private final boolean	scoped;
		private final Key[]		path;

		private Target( boolean scoped, Key[] path ) {
			this.scoped	= scoped;
			this.path	= path;
		}

			/**
			 * @return true when assignment targets an explicit scope path.
			 */
			public boolean isScoped() {
				return scoped;
			}

			/**
			 * @return key path representing the assignment target.
			 */
			public Key[] getPath() {
				return path;
			}
	}
}
