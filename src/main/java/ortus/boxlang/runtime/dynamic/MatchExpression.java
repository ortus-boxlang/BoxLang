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
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.dynamic.casters.StructCaster;
import ortus.boxlang.runtime.operators.EqualsEquals;
import ortus.boxlang.runtime.runnables.IClassRunnable;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
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
			ContainerBoxContext caseContext = new ContainerBoxContext( context );
			if ( !matchCase.getPattern().matches( caseContext, subject ) ) {
				continue;
			}
			if ( matchCase.getGuard() != null && !BooleanCaster.cast( matchCase.getGuard().evaluate( caseContext ) ) ) {
				continue;
			}
			return matchCase.getBody().evaluate( caseContext );
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

	private static boolean containsBindingPattern( Pattern pattern ) {
		if ( pattern instanceof BindingPattern ) {
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

	private static Object applyDefault( IBoxContext context, Object value, boolean isMissing, DefaultExpression defaultValue ) {
		if ( defaultValue == null ) {
			return value;
		}
		if ( isMissing || value == null ) {
			return defaultValue.evaluate( context );
		}
		return value;
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

	private static boolean matchNestedPatterns( IBoxContext context, Pattern[] patterns, Object[] values ) {
		if ( patterns.length != values.length ) {
			return false;
		}
		for ( int i = 0; i < patterns.length; i++ ) {
			if ( !patterns[ i ].matches( context, values[ i ] ) ) {
				return false;
			}
		}
		return true;
	}

	private static List<ConstructorDescriptor> getConstructorDescriptors( Object subject ) {
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
		return pattern.getClass().getSimpleName();
	}

	private static String getBindingName( Pattern pattern ) {
		if ( ! ( pattern instanceof BindingPattern bindingPattern ) ) {
			return null;
		}
		Target target = bindingPattern.target;
		if ( target == null || target.isScoped() || target.getPath().length != 1 ) {
			return null;
		}
		return target.getPath()[ 0 ].getName();
	}

	private static boolean matchProtocolConstructorPattern( IBoxContext context, IClassRunnable classRunnable, String label, Pattern[] patterns ) {
		if ( !hasMethod( classRunnable, MATCH_PREDICATE_KEY ) ) {
			return false;
		}

		IStruct	descriptor		= buildProtocolDescriptor( label, patterns );
		Object	predicateResult	= classRunnable.dereferenceAndInvoke( context, MATCH_PREDICATE_KEY, new Object[] { descriptor }, false );
		if ( !BooleanCaster.cast( predicateResult ) ) {
			return false;
		}

		if ( !hasMethod( classRunnable, MATCH_BINDINGS_KEY ) ) {
			throw new BoxRuntimeException( "$matchBindings must be implemented when $matchPredicate returns true." );
		}

		CastAttempt<IStruct> bindingsAttempt = StructCaster
		    .attempt( classRunnable.dereferenceAndInvoke( context, MATCH_BINDINGS_KEY, new Object[] { descriptor }, false ) );
		if ( !bindingsAttempt.wasSuccessful() ) {
			throw new BoxRuntimeException( "$matchBindings must return a struct of bound values." );
		}

		IStruct bindings = bindingsAttempt.get();
		for ( int i = 0; i < patterns.length; i++ ) {
			Object value = getProtocolBindingValue( bindings, patterns[ i ], i + 1 );
			if ( value == ProtocolBindingMissing.INSTANCE ) {
				throw new BoxRuntimeException( "$matchBindings must provide a value for pattern slot [" + ( i + 1 ) + "]." );
			}
			if ( !patterns[ i ].matches( context, value ) ) {
				return false;
			}
		}

		return true;
	}

	private static Object getProtocolBindingValue( IStruct bindings, Pattern pattern, int slot ) {
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

	private static boolean matchObjectBindings( IBoxContext context, IStruct source, ObjectBinding[] bindings ) {
		Set<Key>		consumed	= new HashSet<>();
		ObjectBinding	restBinding	= null;

		for ( ObjectBinding binding : bindings ) {
			if ( binding.isRest() ) {
				restBinding = binding;
				continue;
			}

			boolean	hasKey	= source.containsKey( binding.getSourceKey() );
			Object	value	= hasKey ? source.get( binding.getSourceKey() ) : null;
			if ( !hasKey && binding.getDefaultValue() == null ) {
				return false;
			}
			value = applyDefault( context, value, !hasKey, binding.getDefaultValue() );
			consumed.add( binding.getSourceKey() );

			if ( binding.hasNested() ) {
				CastAttempt<IStruct> structAttempt = StructCaster.attempt( value );
				if ( !structAttempt.wasSuccessful() || !matchObjectBindings( context, structAttempt.get(), binding.getNested() ) ) {
					return false;
				}
			} else {
				assignTarget( context, binding.getTarget(), value );
			}
		}

		if ( restBinding != null ) {
			Struct rest = new Struct();
			source.forEach( ( key, value ) -> {
				if ( !consumed.contains( key ) ) {
					rest.put( key, value );
				}
			} );
			assignTarget( context, restBinding.getTarget(), rest );
		}

		return true;
	}

	private static boolean matchArrayBindings( IBoxContext context, Array source, ArrayBinding[] bindings ) {
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
				if ( !matchArrayBinding( context, bindings[ i ], true, source.getAt( i + 1 ) ) ) {
					return false;
				}
			}
			return true;
		}

		for ( int i = 0; i < restIndex; i++ ) {
			if ( !matchArrayBinding( context, bindings[ i ], true, source.getAt( i + 1 ) ) ) {
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
			if ( !matchArrayBinding( context, binding, hasValue, value ) ) {
				return false;
			}
		}

		Array	rest		= new Array();
		int		restStart	= leftCount + 1;
		int		restLength	= Math.max( source.size() - leftCount - availableRight, 0 );
		for ( int i = 0; i < restLength; i++ ) {
			rest.add( source.getAt( restStart + i ) );
		}
		assignTarget( context, bindings[ restIndex ].getTarget(), rest );
		return true;
	}

	private static boolean matchArrayBinding( IBoxContext context, ArrayBinding binding, boolean hasValue, Object rawValue ) {
		if ( !hasValue && binding.getDefaultValue() == null ) {
			return false;
		}
		Object value = applyDefault( context, rawValue, !hasValue, binding.getDefaultValue() );

		if ( binding.hasNested() ) {
			CastAttempt<Array> arrayAttempt = ArrayCaster.attempt( value );
			return arrayAttempt.wasSuccessful() && matchArrayBindings( context, arrayAttempt.get(), binding.getNested() );
		}

		assignTarget( context, binding.getTarget(), value );
		return true;
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

		abstract boolean matches( IBoxContext context, Object subject );
	}

	private static final class LiteralPattern extends Pattern {

		private final Object value;

		private LiteralPattern( Object value ) {
			this.value = value;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			return equalsValue( subject, this.value );
		}
	}

	private static final class WildcardPattern extends Pattern {

		private static final WildcardPattern INSTANCE = new WildcardPattern();

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			return true;
		}
	}

	private static final class BindingPattern extends Pattern {

		private final Target target;

		private BindingPattern( Target target ) {
			this.target = target;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			assignTarget( context, this.target, subject );
			return true;
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
		boolean matches( IBoxContext context, Object subject ) {
			boolean matchedTagDescriptor = false;
			for ( ConstructorDescriptor descriptor : getConstructorDescriptors( subject ) ) {
				if ( !this.label.equalsIgnoreCase( descriptor.label() ) ) {
					continue;
				}
				matchedTagDescriptor = true;
				if ( descriptor.propertyNames().length != this.patterns.length ) {
					return false;
				}

				Object[] values = new Object[ descriptor.propertyNames().length ];
				for ( int i = 0; i < descriptor.propertyNames().length; i++ ) {
					values[ i ] = Referencer.get( context, subject, Key.of( descriptor.propertyNames()[ i ] ), false );
				}

				return matchNestedPatterns( context, this.patterns, values );
			}
			if ( matchedTagDescriptor ) {
				return false;
			}
			if ( subject instanceof IClassRunnable classRunnable ) {
				return matchProtocolConstructorPattern( context, classRunnable, this.label, this.patterns );
			}
			return false;
		}
	}

	private static final class OrPattern extends Pattern {

		private final Pattern[] patterns;

		private OrPattern( Pattern[] patterns ) {
			this.patterns = patterns;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			for ( Pattern pattern : this.patterns ) {
				if ( pattern.matches( context, subject ) ) {
					return true;
				}
			}
			return false;
		}
	}

	private static final class AndPattern extends Pattern {

		private final Pattern[] patterns;

		private AndPattern( Pattern[] patterns ) {
			this.patterns = patterns;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			for ( Pattern pattern : this.patterns ) {
				if ( !pattern.matches( context, subject ) ) {
					return false;
				}
			}
			return true;
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
		boolean matches( IBoxContext context, Object subject ) {
			return !this.pattern.matches( context, subject );
		}
	}

	private static final class PredicatePattern extends Pattern {

		private final DefaultExpression predicate;

		private PredicatePattern( DefaultExpression predicate ) {
			this.predicate = predicate;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			Object predicateFunction = this.predicate.evaluate( context );
			return BooleanCaster.cast( context.invokeFunction( predicateFunction, new Object[] { subject } ) );
		}
	}

	private static final class RangePattern extends Pattern {

		private final int	from;
		private final int	to;

		private RangePattern( Object from, Object to ) {
			this.from	= IntegerCaster.cast( from );
			this.to		= IntegerCaster.cast( to );
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			CastAttempt<Integer> attempt = IntegerCaster.attempt( subject );
			if ( !attempt.wasSuccessful() ) {
				return false;
			}

			int value = attempt.get();
			if ( this.from <= this.to ) {
				return value >= this.from && value <= this.to;
			}
			return value <= this.from && value >= this.to;
		}
	}

	private static final class ObjectPattern extends Pattern {

		private final ObjectBinding[] bindings;

		private ObjectPattern( ObjectBinding[] bindings ) {
			this.bindings = bindings;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			CastAttempt<IStruct> structAttempt = StructCaster.attempt( subject );
			return structAttempt.wasSuccessful() && matchObjectBindings( context, structAttempt.get(), this.bindings );
		}
	}

	private static final class ArrayPattern extends Pattern {

		private final ArrayBinding[] bindings;

		private ArrayPattern( ArrayBinding[] bindings ) {
			this.bindings = bindings;
		}

		@Override
		boolean matches( IBoxContext context, Object subject ) {
			CastAttempt<Array> arrayAttempt = ArrayCaster.attempt( subject );
			return arrayAttempt.wasSuccessful() && matchArrayBindings( context, arrayAttempt.get(), this.bindings );
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