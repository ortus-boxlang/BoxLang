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
import java.util.function.Function;

import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Runtime helper for array destructuring assignment.
 */
public class ArrayDestructurer {

	/**
	 * Destructure a source array into assignment targets.
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
		Array source = toArrayOrThrow( sourceValue, "root" );
		applyBindings( context, source, isFinal, mustBeScopeName, bindings == null ? new Binding[] {} : bindings, "root" );
		return sourceValue;
	}

	public static Binding binding( Target target, Binding[] nested, Function<Object, Object> defaultValueSupplier ) {
		return new Binding( target, nested == null ? new Binding[] {} : nested, defaultValueSupplier, false );
	}

	public static Binding rest( Target target ) {
		return new Binding( target, new Binding[] {}, null, true );
	}

	public static Target target( boolean scoped, String... path ) {
		Key[] keys = Arrays.stream( path ).map( Key::of ).toArray( Key[]::new );
		return new Target( scoped, keys );
	}

	private static void applyBindings( IBoxContext context, Array source, boolean isFinal, Key mustBeScopeName, Binding[] bindings, String pathPrefix ) {
		Binding[]	safeBindings	= bindings == null ? new Binding[] {} : bindings;

		int			restIndex		= -1;
		for ( int i = 0; i < safeBindings.length; i++ ) {
			if ( safeBindings[ i ].isRest() ) {
				if ( restIndex != -1 ) {
					throw new BoxRuntimeException( "Array destructuring patterns may only contain one rest binding." );
				}
				restIndex = i;
			}
		}

		if ( restIndex == -1 ) {
			for ( int i = 0; i < safeBindings.length; i++ ) {
				int patternIndex = i + 1;
				applyBinding(
				    context,
				    source,
				    isFinal,
				    mustBeScopeName,
				    safeBindings[ i ],
				    patternIndex,
				    joinPath( pathPrefix, String.valueOf( patternIndex ) )
				);
			}
			return;
		}

		Binding	restBinding	= safeBindings[ restIndex ];
		int		leftCount	= restIndex;
		int		rightCount	= safeBindings.length - restIndex - 1;

		for ( int i = 0; i < leftCount; i++ ) {
			int patternIndex = i + 1;
			applyBinding(
			    context,
			    source,
			    isFinal,
			    mustBeScopeName,
			    safeBindings[ i ],
			    patternIndex,
			    joinPath( pathPrefix, String.valueOf( patternIndex ) )
			);
		}

		int	remainingAfterLeft	= Math.max( source.size() - leftCount, 0 );
		int	availableRight		= Math.min( remainingAfterLeft, rightCount );
		int	missingLeadingRight	= rightCount - availableRight;
		int	rightSourceStart	= source.size() - availableRight + 1;

		for ( int offset = 0; offset < rightCount; offset++ ) {
			Binding	binding		= safeBindings[ restIndex + 1 + offset ];
			Integer	sourceIndex	= null;
			if ( offset >= missingLeadingRight ) {
				sourceIndex = rightSourceStart + ( offset - missingLeadingRight );
			}
			int patternIndex = leftCount + offset + 1;
			applyBinding(
			    context,
			    source,
			    isFinal,
			    mustBeScopeName,
			    binding,
			    sourceIndex,
			    joinPath( pathPrefix, String.valueOf( patternIndex ) )
			);
		}

		Array	rest		= new Array();
		int		restStart	= leftCount + 1;
		int		restLength	= Math.max( source.size() - leftCount - availableRight, 0 );
		for ( int i = 0; i < restLength; i++ ) {
			rest.add( source.getAt( restStart + i ) );
		}
		assignTarget( context, restBinding.getTarget(), rest, isFinal, mustBeScopeName );
	}

	private static void applyBinding( IBoxContext context, Array source, boolean isFinal, Key mustBeScopeName, Binding binding, Integer sourceIndex,
	    String keyPath ) {
		boolean	hasValue	= sourceIndex != null && sourceIndex >= 1 && sourceIndex <= source.size();
		Object	value		= hasValue ? source.getAt( sourceIndex ) : null;

		if ( binding.hasNested() ) {
			if ( !hasValue && binding.getDefaultValueSupplier() == null ) {
				for ( Binding nestedBinding : binding.getNested() ) {
					applyMissingBinding( context, nestedBinding, isFinal, mustBeScopeName, keyPath );
				}
				return;
			}

			if ( ( !hasValue || value == null ) && binding.getDefaultValueSupplier() != null ) {
				value = binding.getDefaultValueSupplier().apply( null );
			}

			Array nestedSource = toArrayOrThrow( value, keyPath );
			applyBindings( context, nestedSource, isFinal, mustBeScopeName, binding.getNested(), keyPath );
			return;
		}

		if ( ( !hasValue || value == null ) && binding.getDefaultValueSupplier() != null ) {
			value = binding.getDefaultValueSupplier().apply( null );
		}

		assignTarget( context, binding.getTarget(), value, isFinal, mustBeScopeName );
	}

	private static void applyMissingBinding( IBoxContext context, Binding binding, boolean isFinal, Key mustBeScopeName, String pathPrefix ) {
		if ( binding.isRest() ) {
			assignTarget( context, binding.getTarget(), new Array(), isFinal, mustBeScopeName );
			return;
		}

		Object value = binding.getDefaultValueSupplier() != null ? binding.getDefaultValueSupplier().apply( null ) : null;

		if ( binding.hasNested() ) {
			if ( binding.getDefaultValueSupplier() == null ) {
				for ( Binding nestedBinding : binding.getNested() ) {
					applyMissingBinding( context, nestedBinding, isFinal, mustBeScopeName, pathPrefix );
				}
				return;
			}

			Array nestedSource = toArrayOrThrow( value, pathPrefix );
			applyBindings( context, nestedSource, isFinal, mustBeScopeName, binding.getNested(), pathPrefix );
			return;
		}

		assignTarget( context, binding.getTarget(), value, isFinal, mustBeScopeName );
	}

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

	private static Array toArrayOrThrow( Object value, String path ) {
		CastAttempt<Array> casted = ArrayCaster.attempt( value );
		if ( !casted.wasSuccessful() ) {
			throw new BoxRuntimeException(
			    "Cannot destructure path [" + path + "] from non-array value of type [" + describeType( value ) + "]." );
		}
		return casted.get();
	}

	private static String describeType( Object value ) {
		return value == null ? "null" : value.getClass().getName();
	}

	private static String joinPath( String prefix, String key ) {
		if ( prefix == null || prefix.isEmpty() ) {
			return key;
		}
		return prefix + "." + key;
	}

	public static final class Binding {

		private final Target					target;
		private final Binding[]					nested;
		private final Function<Object, Object>	defaultValueSupplier;
		private final boolean					rest;

		private Binding( Target target, Binding[] nested, Function<Object, Object> defaultValueSupplier, boolean rest ) {
			this.target					= target;
			this.nested					= nested;
			this.defaultValueSupplier	= defaultValueSupplier;
			this.rest					= rest;
		}

		public Target getTarget() {
			return target;
		}

		public Binding[] getNested() {
			return nested;
		}

		public Function<Object, Object> getDefaultValueSupplier() {
			return defaultValueSupplier;
		}

		public boolean isRest() {
			return rest;
		}

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

		public boolean isScoped() {
			return scoped;
		}

		public Key[] getPath() {
			return path;
		}
	}
}
