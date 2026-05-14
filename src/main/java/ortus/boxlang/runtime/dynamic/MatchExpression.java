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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ortus.boxlang.runtime.context.ContainerBoxContext;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.IBoxContext.ScopeSearchResult;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.CastAttempt;
import ortus.boxlang.runtime.dynamic.casters.GenericCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.DefaultExpression;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Runtime helper for evaluating match expressions.
 */
public class MatchExpression {

	private static final Key	PATTERN_MATCH_KEY	= Key.of( "patternMatch" );
	private static final Key	MATCH_PREDICATE_KEY	= Key.of( "$matchPredicate" );
	private static final Key	MATCH_BINDINGS_KEY	= Key.of( "$matchBindings" );
	private static final Key	LABEL_KEY			= Key.of( "label" );
	private static final Key	ARITY_KEY			= Key.of( "arity" );
	private static final Key	PATTERNS_KEY		= Key.of( "patterns" );
	private static final Key	SLOT_KEY			= Key.of( "slot" );
	private static final Key	KIND_KEY			= Key.of( "kind" );
	private static final Key	BINDING_NAME_KEY	= Key.of( "bindingName" );

	public static Object invoke( IBoxContext context, Object subject, Case[] cases ) {
		Case[] safeCases = cases == null ? new Case[] {} : cases;
		for ( Case matchCase : safeCases ) {
			MatcherEngine matcher = MatcherEngine.on( new PatternMatchContext( context ) );
			if ( !matchCase.getPattern().matches( matcher, subject ) ) {
				continue;
			}
			if ( matchCase.getGuard() != null && !BooleanCaster.cast( matchCase.getGuard().evaluate( matcher.context() ) ) ) {
				continue;
			}
			return matchCase.getBody().evaluate( matcher.context() );
		}
		return null;
	}

	public static Case matchCase( Pattern pattern, DefaultExpression guard, DefaultExpression body ) {
		return new Case( pattern, guard, body );
	}

	public static Pattern literal( Object value ) {
		return new LiteralPattern( value );
	}

	public static Pattern wildcard() {
		return WildcardPattern.INSTANCE;
	}

	public static Pattern binding( Target target ) {
		return new BindingPattern( target );
	}

	public static Pattern constructor( String label, Pattern[] patterns ) {
		return new ConstructorPattern( label, patterns == null ? new Pattern[] {} : patterns );
	}

	public static Pattern or( Pattern[] patterns ) {
		return new OrPattern( patterns == null ? new Pattern[] {} : patterns );
	}

	public static Pattern and( Pattern[] patterns ) {
		return new AndPattern( patterns == null ? new Pattern[] {} : patterns );
	}

	public static Pattern not( Pattern pattern ) {
		return new NotPattern( pattern );
	}

	public static Pattern predicate( DefaultExpression predicate ) {
		return new PredicatePattern( predicate );
	}

	public static Pattern range( Object from, Object to ) {
		return new RangePattern( from, to );
	}

	public static Pattern type( String[] types, Target target ) {
		return new TypePattern( types, target );
	}

	public static Pattern type( String type, Target target ) {
		return type( new String[] { type }, target );
	}

	private static boolean containsBindingPattern( Pattern pattern ) {
		if ( pattern instanceof BindingPattern ) {
			return true;
		}
		if ( pattern instanceof TypePattern ) {
			return true;
		}
		if ( pattern instanceof ConstructorPattern constructorPattern ) {
			return Arrays.stream( constructorPattern.patterns ).anyMatch( MatchExpression::containsBindingPattern );
		}
		if ( pattern instanceof OrPattern orPattern ) {
			return Arrays.stream( orPattern.patterns ).anyMatch( MatchExpression::containsBindingPattern );
		}
		if ( pattern instanceof AndPattern andPattern ) {
			return Arrays.stream( andPattern.patterns ).anyMatch( MatchExpression::containsBindingPattern );
		}
		if ( pattern instanceof NotPattern notPattern ) {
			return containsBindingPattern( notPattern.pattern );
		}
		return false;
	}

	public static Pattern object( ObjectBinding[] bindings ) {
		return new ObjectPattern( bindings == null ? new ObjectBinding[] {} : bindings );
	}

	public static Pattern array( ArrayBinding[] bindings ) {
		return new ArrayPattern( bindings == null ? new ArrayBinding[] {} : bindings );
	}

	public static ObjectBinding objectBinding( String sourceKey, Target target, ObjectBinding[] nested, DefaultExpression defaultValue ) {
		return new ObjectBinding( Key.of( sourceKey ), target, nested == null ? new ObjectBinding[] {} : nested, defaultValue, false );
	}

	public static ObjectBinding objectRest( Target target ) {
		return new ObjectBinding( null, target, new ObjectBinding[] {}, null, true );
	}

	public static ArrayBinding arrayBinding( Target target, ArrayBinding[] nested, DefaultExpression defaultValue ) {
		return new ArrayBinding( target, nested == null ? new ArrayBinding[] {} : nested, defaultValue, false );
	}

	public static ArrayBinding arrayRest( Target target ) {
		return new ArrayBinding( target, new ArrayBinding[] {}, null, true );
	}

	public static Target target( boolean scoped, String... path ) {
		Key[] keys = Arrays.stream( path ).map( Key::of ).toArray( Key[]::new );
		return new Target( scoped, keys );
	}

	private static boolean equalsValue( Object left, Object right ) {
		if ( left == null || right == null ) {
			return left == right;
		}
		return EqualsEquals.invoke( left, right );
	}

	private static void assignTarget( IBoxContext context, Target target, Object value ) {
		if ( target == null || target.getPath().length == 0 ) {
			return;
		}

		if ( target.isScoped() ) {
			Key		scopeName	= target.getPath()[ 0 ];
			IScope	scope		= context.getScopeNearby( scopeName );
			Key[]	keys		= Arrays.copyOfRange( target.getPath(), 1, target.getPath().length );
			Referencer.setDeep( context, false, scopeName, scope, value, keys );
			return;
		}

		if ( target.getPath().length == 1 ) {
			ScopeSearchResult scope = context.scopeFindNearby( target.getPath()[ 0 ], context.getDefaultAssignmentScope(), true );
			Referencer.setDeep( context, false, null, scope, value );
		}
	}

	private static List<ConstructorDescriptor> getConstructorDescriptors( Object subject ) {
		if ( subject instanceof Attempt<?> attempt ) {
			return getAttemptConstructorDescriptors( attempt );
		}

		if ( ! ( subject instanceof IClassRunnable classRunnable ) ) {
			return List.of();
		}

		Object rawDescriptor = classRunnable.getAnnotations().get( PATTERN_MATCH_KEY );
		if ( rawDescriptor == null ) {
			return List.of();
		}

		CastAttempt<Array> attempt = ArrayCaster.attempt( rawDescriptor );
		if ( !attempt.wasSuccessful() ) {
			throw new BoxRuntimeException( "@patternMatch metadata must declare a label followed by one or more property names." );
		}

		Array descriptor = attempt.get();
		if ( descriptor.isEmpty() ) {
			throw new BoxRuntimeException( "@patternMatch metadata must declare a label followed by one or more property names." );
		}

		CastAttempt<Array> nestedAttempt = ArrayCaster.attempt( descriptor.getAt( 1 ) );
		if ( nestedAttempt.wasSuccessful() ) {
			List<ConstructorDescriptor> descriptors = new ArrayList<>();
			for ( int i = 1; i <= descriptor.size(); i++ ) {
				CastAttempt<Array> constructorAttempt = ArrayCaster.attempt( descriptor.getAt( i ) );
				if ( !constructorAttempt.wasSuccessful() ) {
					throw new BoxRuntimeException( "@patternMatch metadata must declare a label followed by one or more property names." );
				}
				descriptors.add( toConstructorDescriptor( classRunnable, constructorAttempt.get() ) );
			}
			return descriptors;
		}

		return List.of( toConstructorDescriptor( classRunnable, descriptor ) );
	}

	private static List<ConstructorDescriptor> getAttemptConstructorDescriptors( Attempt<?> attempt ) {
		if ( attempt.wasSuccessful() ) {
			return List.of(
			    new ConstructorDescriptor( "Ok", new String[] { "value" } ),
			    new ConstructorDescriptor( "Success", new String[] { "value" } )
			);
		}

		if ( attempt.hasFailurePayload() ) {
			return List.of(
			    new ConstructorDescriptor( "Err", new String[] { "error" } ),
			    new ConstructorDescriptor( "Failure", new String[] { "error" } )
			);
		}

		return List.of(
		    new ConstructorDescriptor( "Err", new String[] {} ),
		    new ConstructorDescriptor( "Failure", new String[] {} )
		);
	}

	private static Object[] getConstructorPatternValues( IBoxContext context, Object subject, ConstructorDescriptor descriptor ) {
		Object[] values = new Object[ descriptor.propertyNames().length ];

		if ( subject instanceof Attempt<?> attempt ) {
			for ( int i = 0; i < descriptor.propertyNames().length; i++ ) {
				values[ i ] = switch ( descriptor.propertyNames()[ i ] ) {
					case "value" -> attempt.get();
					case "error" -> attempt.getFailure();
					default -> null;
				};
			}
			return values;
		}

		for ( int i = 0; i < descriptor.propertyNames().length; i++ ) {
			values[ i ] = Referencer.get( context, subject, Key.of( descriptor.propertyNames()[ i ] ), false );
		}
		return values;
	}

	private static ConstructorDescriptor toConstructorDescriptor( IClassRunnable classRunnable, Array descriptor ) {
		if ( descriptor.size() < 2 ) {
			throw new BoxRuntimeException( "@patternMatch metadata must declare a label followed by one or more property names." );
		}

		String		label			= StringCaster.cast( descriptor.getAt( 1 ) );
		String[]	propertyNames	= new String[ descriptor.size() - 1 ];
		for ( int i = 2; i <= descriptor.size(); i++ ) {
			propertyNames[ i - 2 ] = StringCaster.cast( descriptor.getAt( i ) );
			if ( !classRunnable.getProperties().containsKey( Key.of( propertyNames[ i - 2 ] ) ) ) {
				throw new BoxRuntimeException(
				    "@patternMatch references unknown property [" + propertyNames[ i - 2 ] + "] on class [" + classRunnable.bxGetName().getName() + "]." );
			}
		}

		return new ConstructorDescriptor( label, propertyNames );
	}

	private static boolean hasMethod( IClassRunnable classRunnable, Key methodName ) {
		if ( classRunnable.getThisScope().get( methodName ) != null || classRunnable.getStaticScope().get( methodName ) != null ) {
			return true;
		}
		return classRunnable.getSuper() != null && hasMethod( classRunnable.getSuper(), methodName );
	}

	private static IStruct buildProtocolDescriptor( String label, Pattern[] patterns ) {
		Array patternDescriptors = new Array();
		for ( int i = 0; i < patterns.length; i++ ) {
			patternDescriptors.add( Struct.of(
			    SLOT_KEY, i + 1,
			    KIND_KEY, getPatternKind( patterns[ i ] ),
			    BINDING_NAME_KEY, getBindingName( patterns[ i ] )
			) );
		}

		return Struct.of(
		    LABEL_KEY, label,
		    ARITY_KEY, patterns.length,
		    PATTERNS_KEY, patternDescriptors
		);
	}

	private static String getPatternKind( Pattern pattern ) {
		if ( pattern instanceof BindingPattern ) {
			return "binding";
		}
		if ( pattern instanceof WildcardPattern ) {
			return "wildcard";
		}
		if ( pattern instanceof LiteralPattern ) {
			return "literal";
		}
		if ( pattern instanceof ConstructorPattern ) {
			return "constructor";
		}
		if ( pattern instanceof ObjectPattern ) {
			return "object";
		}
		if ( pattern instanceof ArrayPattern ) {
			return "array";
		}
		if ( pattern instanceof PredicatePattern ) {
			return "predicate";
		}
		if ( pattern instanceof RangePattern ) {
			return "range";
		}
		if ( pattern instanceof TypePattern ) {
			return "type";
		}
		return pattern.getClass().getSimpleName();
	}

	private static String getBindingName( Pattern pattern ) {
		Target target = null;
		if ( pattern instanceof BindingPattern bindingPattern ) {
			target = bindingPattern.target;
		} else if ( pattern instanceof TypePattern typePattern ) {
			target = typePattern.target;
		} else {
			return null;
		}
		if ( target == null || target.isScoped() || target.getPath().length != 1 ) {
			return null;
		}
		return target.getPath()[ 0 ].getName();
	}

	public static final class Case {

		private final Pattern			pattern;
		private final DefaultExpression	guard;
		private final DefaultExpression	body;

		private Case( Pattern pattern, DefaultExpression guard, DefaultExpression body ) {
			this.pattern	= pattern;
			this.guard		= guard;
			this.body		= body;
		}

		public Pattern getPattern() {
			return this.pattern;
		}

		public DefaultExpression getGuard() {
			return this.guard;
		}

		public DefaultExpression getBody() {
			return this.body;
		}
	}

	public abstract static class Pattern {

		final boolean matches( IBoxContext context, Object subject ) {
			return matches( MatcherEngine.on( context ), subject );
		}

		abstract boolean matches( MatcherEngine matcher, Object subject );
	}

	private static final class LiteralPattern extends Pattern {

		private final Object value;

		private LiteralPattern( Object value ) {
			this.value = value;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchLiteral( this.value, subject );
		}
	}

	private static final class WildcardPattern extends Pattern {

		private static final WildcardPattern INSTANCE = new WildcardPattern();

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchWildcard( subject );
		}
	}

	private static final class BindingPattern extends Pattern {

		private final Target target;

		private BindingPattern( Target target ) {
			this.target = target;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchBinding( this.target, subject );
		}
	}

	private static final class ConstructorPattern extends Pattern {

		private final String	label;
		private final Pattern[]	patterns;

		private ConstructorPattern( String label, Pattern[] patterns ) {
			this.label		= label;
			this.patterns	= patterns;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchConstructor( this.label, this.patterns, subject );
		}
	}

	private static final class OrPattern extends Pattern {

		private final Pattern[] patterns;

		private OrPattern( Pattern[] patterns ) {
			this.patterns = patterns;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchOr( this.patterns, subject );
		}
	}

	private static final class AndPattern extends Pattern {

		private final Pattern[] patterns;

		private AndPattern( Pattern[] patterns ) {
			this.patterns = patterns;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchAnd( this.patterns, subject );
		}
	}

	private static final class NotPattern extends Pattern {

		private final Pattern pattern;

		private NotPattern( Pattern pattern ) {
			if ( containsBindingPattern( pattern ) ) {
				throw new BoxRuntimeException( "not patterns cannot contain binding patterns" );
			}
			this.pattern = pattern;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchNot( this.pattern, subject );
		}
	}

	private static final class PredicatePattern extends Pattern {

		private final DefaultExpression predicate;

		private PredicatePattern( DefaultExpression predicate ) {
			this.predicate = predicate;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchPredicate( this.predicate, subject );
		}
	}

	private static final class RangePattern extends Pattern {

		private final ortus.boxlang.runtime.types.Range range;

		private RangePattern( Object from, Object to ) {
			this.range = ortus.boxlang.runtime.operators.Range.invoke( from, to );
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchRange( this.range, subject );
		}
	}

	private static final class TypePattern extends Pattern {

		private final String[]	types;
		private final Target	target;

		private TypePattern( String[] types, Target target ) {
			this.types	= types == null ? new String[] {} : types;
			this.target	= target;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchType( this.types, this.target, subject );
		}
	}

	private static final class ObjectPattern extends Pattern {

		private final ObjectBinding[] bindings;

		private ObjectPattern( ObjectBinding[] bindings ) {
			this.bindings = bindings;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchObject( this.bindings, subject );
		}
	}

	private static final class ArrayPattern extends Pattern {

		private final ArrayBinding[] bindings;

		private ArrayPattern( ArrayBinding[] bindings ) {
			this.bindings = bindings;
		}

		@Override
		boolean matches( MatcherEngine matcher, Object subject ) {
			return matcher.matchArray( this.bindings, subject );
		}
	}

	@FunctionalInterface
	private interface AtomicMatcher {

		boolean matches( MatcherEngine matcher );
	}

	private static final class MatcherEngine {

		private final IBoxContext context;

		private MatcherEngine( IBoxContext context ) {
			this.context = context;
		}

		private static MatcherEngine on( IBoxContext context ) {
			return new MatcherEngine( context );
		}

		private IBoxContext context() {
			return this.context;
		}

		private boolean matchLiteral( Object value, Object subject ) {
			return equalsValue( subject, value );
		}

		private boolean matchWildcard( Object subject ) {
			return true;
		}

		private boolean matchBinding( Target target, Object subject ) {
			assignTarget( this.context, target, subject );
			return true;
		}

		private boolean matchConstructor( String label, Pattern[] patterns, Object subject ) {
			boolean matchedTagDescriptor = false;
			for ( ConstructorDescriptor descriptor : getConstructorDescriptors( subject ) ) {
				if ( !label.equalsIgnoreCase( descriptor.label() ) ) {
					continue;
				}
				matchedTagDescriptor = true;
				if ( descriptor.propertyNames().length != patterns.length ) {
					return false;
				}

				Object[] values = getConstructorPatternValues( this.context, subject, descriptor );
				return matchNestedPatterns( patterns, values );
			}
			if ( matchedTagDescriptor ) {
				return false;
			}
			if ( subject instanceof IClassRunnable classRunnable ) {
				return matchAtomically( stagedMatcher -> stagedMatcher.matchProtocolConstructorPattern( classRunnable, label, patterns ) );
			}
			return false;
		}

		private boolean matchOr( Pattern[] patterns, Object subject ) {
			for ( Pattern pattern : patterns ) {
				if ( matchAtomically( stagedMatcher -> pattern.matches( stagedMatcher, subject ) ) ) {
					return true;
				}
			}
			return false;
		}

		private boolean matchAnd( Pattern[] patterns, Object subject ) {
			return matchAtomically( stagedMatcher -> {
				for ( Pattern pattern : patterns ) {
					if ( !pattern.matches( stagedMatcher, subject ) ) {
						return false;
					}
				}
				return true;
			} );
		}

		private boolean matchNot( Pattern pattern, Object subject ) {
			return !pattern.matches( this, subject );
		}

		private boolean matchPredicate( DefaultExpression predicate, Object subject ) {
			Object predicateFunction = predicate.evaluate( this.context );
			return BooleanCaster.cast( this.context.invokeFunction( predicateFunction, new Object[] { subject } ) );
		}

		private boolean matchRange( ortus.boxlang.runtime.types.Range range, Object subject ) {
			return range.contains( subject );
		}

		private boolean matchType( String[] types, Target target, Object subject ) {
			for ( String type : types ) {
				CastAttempt<Object> attempt = GenericCaster.attempt( this.context, subject, type );
				if ( !attempt.wasSuccessful() ) {
					continue;
				}

				assignTarget( this.context, target, attempt.get() );
				return true;
			}
			return false;
		}

		private boolean matchObject( ObjectBinding[] bindings, Object subject ) {
			CastAttempt<IStruct> structAttempt = StructCaster.attempt( subject );
			return structAttempt.wasSuccessful() && matchAtomically( stagedMatcher -> stagedMatcher.matchObjectBindings( structAttempt.get(), bindings ) );
		}

		private boolean matchArray( ArrayBinding[] bindings, Object subject ) {
			CastAttempt<Array> arrayAttempt = ArrayCaster.attempt( subject );
			return arrayAttempt.wasSuccessful() && matchAtomically( stagedMatcher -> stagedMatcher.matchArrayBindings( arrayAttempt.get(), bindings ) );
		}

		private boolean matchAtomically( AtomicMatcher matcher ) {
			PatternMatchContext	stagedContext	= new PatternMatchContext( this.context );
			MatcherEngine		stagedMatcher	= on( stagedContext );
			if ( !matcher.matches( stagedMatcher ) ) {
				return false;
			}
			stagedContext.promote();
			return true;
		}

		private boolean matchNestedPatterns( Pattern[] patterns, Object[] values ) {
			return matchAtomically( stagedMatcher -> {
				if ( patterns.length != values.length ) {
					return false;
				}
				for ( int i = 0; i < patterns.length; i++ ) {
					if ( !patterns[ i ].matches( stagedMatcher, values[ i ] ) ) {
						return false;
					}
				}
				return true;
			} );
		}

		private boolean matchProtocolConstructorPattern( IClassRunnable classRunnable, String label, Pattern[] patterns ) {
			if ( !hasMethod( classRunnable, MATCH_PREDICATE_KEY ) ) {
				return false;
			}

			IStruct	descriptor		= buildProtocolDescriptor( label, patterns );
			Object	predicateResult	= classRunnable.dereferenceAndInvoke( this.context, MATCH_PREDICATE_KEY, new Object[] { descriptor }, false );
			if ( !BooleanCaster.cast( predicateResult ) ) {
				return false;
			}

			if ( !hasMethod( classRunnable, MATCH_BINDINGS_KEY ) ) {
				throw new BoxRuntimeException( "$matchBindings must be implemented when $matchPredicate returns true." );
			}

			CastAttempt<IStruct> bindingsAttempt = StructCaster
			    .attempt( classRunnable.dereferenceAndInvoke( this.context, MATCH_BINDINGS_KEY, new Object[] { descriptor }, false ) );
			if ( !bindingsAttempt.wasSuccessful() ) {
				throw new BoxRuntimeException( "$matchBindings must return a struct of bound values." );
			}

			IStruct bindings = bindingsAttempt.get();
			for ( int i = 0; i < patterns.length; i++ ) {
				Object value = getProtocolBindingValue( bindings, patterns[ i ], i + 1 );
				if ( value == ProtocolBindingMissing.INSTANCE ) {
					throw new BoxRuntimeException( "$matchBindings must provide a value for pattern slot [" + ( i + 1 ) + "]." );
				}
				if ( !patterns[ i ].matches( this, value ) ) {
					return false;
				}
			}

			return true;
		}

		private Object getProtocolBindingValue( IStruct bindings, Pattern pattern, int slot ) {
			Key slotKey = Key.of( String.valueOf( slot ) );
			if ( bindings.containsKey( slotKey ) ) {
				return bindings.get( slotKey );
			}

			String bindingName = getBindingName( pattern );
			if ( bindingName != null ) {
				Key bindingKey = Key.of( bindingName );
				if ( bindings.containsKey( bindingKey ) ) {
					return bindings.get( bindingKey );
				}
			}

			if ( pattern instanceof WildcardPattern ) {
				return null;
			}

			return ProtocolBindingMissing.INSTANCE;
		}

		private boolean matchObjectBindings( IStruct source, ObjectBinding[] bindings ) {
			Set<Key>		consumed	= new HashSet<>();
			ObjectBinding	restBinding	= null;

			for ( ObjectBinding binding : bindings ) {
				if ( binding.isRest() ) {
					restBinding = binding;
					continue;
				}

				MatchValue inputValue = MatchValue.fromObject( source, binding.getSourceKey() );
				if ( inputValue.missing() && binding.getDefaultValue() == null ) {
					return false;
				}
				Object value = inputValue.resolve( this.context, binding.getDefaultValue() );
				consumed.add( binding.getSourceKey() );

				if ( binding.hasNested() ) {
					CastAttempt<IStruct> structAttempt = StructCaster.attempt( value );
					if ( !structAttempt.wasSuccessful() || !matchObjectBindings( structAttempt.get(), binding.getNested() ) ) {
						return false;
					}
				} else {
					assignTarget( this.context, binding.getTarget(), value );
				}
			}

			if ( restBinding != null ) {
				Struct rest = new Struct();
				source.forEach( ( key, value ) -> {
					if ( !consumed.contains( key ) ) {
						rest.put( key, value );
					}
				} );
				assignTarget( this.context, restBinding.getTarget(), rest );
			}

			return true;
		}

		private boolean matchArrayBindings( Array source, ArrayBinding[] bindings ) {
			int restIndex = -1;
			for ( int i = 0; i < bindings.length; i++ ) {
				if ( bindings[ i ].isRest() ) {
					restIndex = i;
					break;
				}
			}

			if ( restIndex == -1 && source.size() != bindings.length ) {
				return false;
			}
			if ( restIndex != -1 && source.size() < bindings.length - 1 ) {
				return false;
			}

			if ( restIndex == -1 ) {
				for ( int i = 0; i < bindings.length; i++ ) {
					if ( !matchArrayBinding( bindings[ i ], MatchValue.fromArray( true, source.getAt( i + 1 ) ) ) ) {
						return false;
					}
				}
				return true;
			}

			for ( int i = 0; i < restIndex; i++ ) {
				if ( !matchArrayBinding( bindings[ i ], MatchValue.fromArray( true, source.getAt( i + 1 ) ) ) ) {
					return false;
				}
			}

			int	leftCount			= restIndex;
			int	rightCount			= bindings.length - restIndex - 1;
			int	remainingAfterLeft	= Math.max( source.size() - leftCount, 0 );
			int	availableRight		= Math.min( remainingAfterLeft, rightCount );
			int	missingLeadingRight	= rightCount - availableRight;
			int	rightSourceStart	= source.size() - availableRight + 1;

			for ( int offset = 0; offset < rightCount; offset++ ) {
				ArrayBinding	binding		= bindings[ restIndex + 1 + offset ];
				boolean			hasValue	= offset >= missingLeadingRight;
				Object			value		= hasValue ? source.getAt( rightSourceStart + ( offset - missingLeadingRight ) ) : null;
				if ( !matchArrayBinding( binding, MatchValue.fromArray( hasValue, value ) ) ) {
					return false;
				}
			}

			Array	rest		= new Array();
			int		restStart	= leftCount + 1;
			int		restLength	= Math.max( source.size() - leftCount - availableRight, 0 );
			for ( int i = 0; i < restLength; i++ ) {
				rest.add( source.getAt( restStart + i ) );
			}
			assignTarget( this.context, bindings[ restIndex ].getTarget(), rest );
			return true;
		}

		private boolean matchArrayBinding( ArrayBinding binding, MatchValue inputValue ) {
			if ( inputValue.missing() && binding.getDefaultValue() == null ) {
				return false;
			}
			Object value = inputValue.resolve( this.context, binding.getDefaultValue() );

			if ( binding.hasNested() ) {
				CastAttempt<Array> arrayAttempt = ArrayCaster.attempt( value );
				return arrayAttempt.wasSuccessful() && matchArrayBindings( arrayAttempt.get(), binding.getNested() );
			}

			assignTarget( this.context, binding.getTarget(), value );
			return true;
		}
	}

	/**
	 * Shared matcher value contract that keeps missing slots distinct from explicit nulls.
	 */
	private record MatchValue( Object value, boolean missing ) {

		private static MatchValue fromObject( IStruct source, Key sourceKey ) {
			boolean hasKey = source.containsKey( sourceKey );
			return new MatchValue( hasKey ? source.get( sourceKey ) : null, !hasKey );
		}

		private static MatchValue fromArray( boolean hasValue, Object value ) {
			return new MatchValue( value, !hasValue );
		}

		private Object resolve( IBoxContext context, DefaultExpression defaultValue ) {
			if ( defaultValue == null || !this.missing ) {
				return this.value;
			}
			return defaultValue.evaluate( context );
		}
	}

	private static final class PatternMatchContext extends ContainerBoxContext {

		private PatternMatchContext( IBoxContext parent ) {
			super( parent );
		}

		@Override
		public ScopeSearchResult scopeFindNearby( Key key, IScope defaultScope, boolean shallow, boolean forAssign ) {
			if ( !isKeyVisibleScope( key ) ) {
				if ( !forAssign ) {
					var querySearch = queryFindNearby( key );
					if ( querySearch != null ) {
						return querySearch;
					}
				}

				Object result = this.variablesScope.getRaw( key );
				if ( isDefined( result, forAssign ) ) {
					return new ScopeSearchResult( this.variablesScope, Struct.unWrapNull( result ), key );
				}
			}

			if ( shallow ) {
				return null;
			}

			return getParent().scopeFindNearby( key, defaultScope, false, forAssign );
		}

		@Override
		public IScope getScopeNearby( Key name, boolean shallow ) {
			if ( name.equals( this.variablesScope.getName() ) ) {
				return this.variablesScope;
			}

			if ( shallow ) {
				return null;
			}

			return getParent().getScopeNearby( name, false );
		}

		private void promote() {
			IScope parentVariables = getParent().getScopeNearby( VariablesScope.name );
			parentVariables.putAll( this.variablesScope );
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
			return this.scoped;
		}

		public Key[] getPath() {
			return this.path;
		}
	}

	public static final class ObjectBinding {

		private final Key				sourceKey;
		private final Target			target;
		private final ObjectBinding[]	nested;
		private final DefaultExpression	defaultValue;
		private final boolean			rest;

		private ObjectBinding( Key sourceKey, Target target, ObjectBinding[] nested, DefaultExpression defaultValue, boolean rest ) {
			this.sourceKey		= sourceKey;
			this.target			= target;
			this.nested			= nested;
			this.defaultValue	= defaultValue;
			this.rest			= rest;
		}

		public Key getSourceKey() {
			return this.sourceKey;
		}

		public Target getTarget() {
			return this.target;
		}

		public ObjectBinding[] getNested() {
			return this.nested;
		}

		public DefaultExpression getDefaultValue() {
			return this.defaultValue;
		}

		public boolean isRest() {
			return this.rest;
		}

		public boolean hasNested() {
			return this.nested != null && this.nested.length > 0;
		}
	}

	public static final class ArrayBinding {

		private final Target			target;
		private final ArrayBinding[]	nested;
		private final DefaultExpression	defaultValue;
		private final boolean			rest;

		private ArrayBinding( Target target, ArrayBinding[] nested, DefaultExpression defaultValue, boolean rest ) {
			this.target			= target;
			this.nested			= nested;
			this.defaultValue	= defaultValue;
			this.rest			= rest;
		}

		public Target getTarget() {
			return this.target;
		}

		public ArrayBinding[] getNested() {
			return this.nested;
		}

		public DefaultExpression getDefaultValue() {
			return this.defaultValue;
		}

		public boolean isRest() {
			return this.rest;
		}

		public boolean hasNested() {
			return this.nested != null && this.nested.length > 0;
		}
	}

	private record ConstructorDescriptor( String label, String[] propertyNames ) {
	}

	private enum ProtocolBindingMissing {
		INSTANCE
	}
}