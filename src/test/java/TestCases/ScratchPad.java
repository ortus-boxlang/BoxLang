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

package TestCases;

import java.io.File;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;
import ortus.boxlang.runtime.types.Argument;

public class ScratchPad {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			resultKey	= new Key( "result" );

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	@AfterAll
	public static void teardown() {
	}

	@BeforeEach
	public void setupEach() {
		context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
		variables	= context.getScopeNearby( VariablesScope.name );
	}

	@DisplayName( "Test it" )
	@Test
	@Disabled
	void testIt() {
		Method[] targets = File.class.getDeclaredMethods();
		Arrays
		    .stream( targets )
		    // Sort it
		    .sorted( Comparator.comparing( Method::getName ) )
		    // To List
		    .toList();

		// @formatter:off
		instance.executeSource(
			"""
	for ( var x = 1; x lte cacheKeysLength; x++ ) { }
  """, context, BoxSourceType.CFSCRIPT );
		// @formatter:on
	}

	@Test
	public void testMathSpeePlusInteger() {
		Integer		a			= Integer.valueOf( 1 );
		Integer		b			= Integer.valueOf( 2 );
		BigDecimal	Ba			= BigDecimal.ONE;
		BigDecimal	Bb			= BigDecimal.TWO;
		MathContext	mc			= MathContext.DECIMAL128;

		long		startTime1	= System.nanoTime();
		for ( int i = 0; i < 1000000; i++ ) {
			var result = a + b;
		}
		long	endTime1		= System.nanoTime();
		long	executionTime1	= endTime1 - startTime1;

		long	startTime2		= System.nanoTime();
		for ( int i = 0; i < 1000000; i++ ) {
			var result = Ba.add( Bb, mc );
		}
		long	endTime2		= System.nanoTime();
		long	executionTime2	= endTime2 - startTime2;

		System.out.println( "Execution time for a + b: " + executionTime1 + " nanoseconds" );
		System.out.println( "Execution time for Ba.add( Bb ): " + executionTime2 + " nanoseconds" );

	}

	@Test
	public void testMathSpeedTimes() {
		Double		a			= Double.valueOf( 12.34 );
		Double		b			= Double.valueOf( 56.78 );
		BigDecimal	Ba			= new BigDecimal( "12.34" );
		BigDecimal	Bb			= new BigDecimal( "56.78" );
		MathContext	mc			= MathContext.DECIMAL128;

		long		startTime1	= System.nanoTime();
		for ( int i = 0; i < 1000000; i++ ) {
			var result = a * b;
		}
		long	endTime1		= System.nanoTime();
		long	executionTime1	= endTime1 - startTime1;

		long	startTime2		= System.nanoTime();
		for ( int i = 0; i < 1000000; i++ ) {
			var result = Ba.multiply( Bb, mc );
		}
		long	endTime2		= System.nanoTime();
		long	executionTime2	= endTime2 - startTime2;

		System.out.println( "Execution time for a * b: " + executionTime1 + " nanoseconds" );
		System.out.println( "Execution time for Ba.multiply( Bb ): " + executionTime2 + " nanoseconds" );

	}

	@Test
	public void testMathSpeedPlusDecimal() {
		Double		a			= Double.valueOf( 12.34 );
		Double		b			= Double.valueOf( 56.78 );
		BigDecimal	Ba			= new BigDecimal( "12.34" );
		BigDecimal	Bb			= new BigDecimal( "56.78" );
		MathContext	mc			= MathContext.DECIMAL128;

		long		startTime1	= System.nanoTime();
		for ( int i = 0; i < 1; i++ ) {
			var result = a + b;
		}
		long	endTime1		= System.nanoTime();
		long	executionTime1	= endTime1 - startTime1;

		long	startTime2		= System.nanoTime();
		for ( int i = 0; i < 1; i++ ) {
			var result = Ba.add( Bb, mc );
		}
		long	endTime2		= System.nanoTime();
		long	executionTime2	= endTime2 - startTime2;

		System.out.println( "Execution time for a + b: " + executionTime1 + " nanoseconds" );
		System.out.println( "Execution time for Ba.add( Bb ): " + executionTime2 + " nanoseconds" );

	}

	@Test
	@Disabled
	public void testbrad() {
		System.out.println( "BIF Name,Member Name,Argument Name,Argument Position" );
		ServiceLoader
		    .load( BIF.class, BoxRuntime.class.getClassLoader() )
		    .stream()
		    .parallel()
		    .map( ServiceLoader.Provider::type )
		    .filter( t -> !t.getName().equals( "ortus.boxlang.runtime.bifs.global.encryption.Hash" )
		        && !t.getName().equals( "ortus.boxlang.runtime.bifs.global.io.FileInfo" )
		        && !t.getName().equals( "ortus.boxlang.runtime.bifs.global.system.Dump" )
		        && !t.getName().equals( "ortus.boxlang.runtime.bifs.global.temporal.CreateODBCDateTime" ) )
		    .forEach( targetClass -> processBIFRegistration( targetClass ) );

	}

	public void processBIFRegistration( Class<? extends BIF> BIFClass ) {
		BoxBIF[]	bifAnnotations			= BIFClass.getAnnotationsByType( BoxBIF.class );
		BoxMember[]	boxMemberAnnotations	= BIFClass.getAnnotationsByType( BoxMember.class );

		if ( bifAnnotations.length > 0 && boxMemberAnnotations.length > 0 ) {
			List<BoxMember> boxMembers = new ArrayList<>( Arrays.asList( boxMemberAnnotations ) );
			// System.out.println( BIFClass.getName() );
			for ( BoxBIF b : bifAnnotations ) {
				String BIFName = b.alias().isEmpty() ? BIFClass.getSimpleName().toLowerCase() : b.alias().toLowerCase();
				// System.out.print( " BIF: " + BIFName + "() --> " );
				System.out.print( BIFName + "()," );
				// find matching member
				boolean	found	= false;
				int		i		= 0;
				for ( BoxMember m : boxMembers ) {
					String	type		= m.type().toString().toLowerCase();
					String	memberName	= m.name().isEmpty() ? BIFName.replace( m.type().toString().toLowerCase(), "" ) : m.name().toLowerCase();
					String	argName		= m.objectArgument().isEmpty() ? getFirstArgName( BIFClass ) : m.objectArgument();
					String	argPos		= m.objectArgument().isEmpty() ? "1" : getArgPosition( m.objectArgument(), BIFClass );

					if ( ( type + memberName ).equals( BIFName )
					    || ( type.equals( "datetime" ) && ( "date" + memberName ).equals( BIFName ) )
					    || ( ( "gettimezone" ).equals( BIFName ) && ( memberName ).equals( "timezone" ) )
					    || memberName.equals( BIFName )
					    || BIFName.equals( "jsonserialize" )
					    || BIFName.equals( "lsnumberformat" )
					    || BIFName.equals( "lscurrencyformat" )
					    || BIFName.equals( "structget" ) ) {

						// System.out.println( " BoxMember: " + type + "." + memberName + "() - use arg name \"" + argName + "\" (pos: " + argPos + ")" );
						System.out.println( type + "." + memberName + "()," + argName + "," + argPos );
						if ( !memberName.equals( "numberformat" ) && !memberName.equals( "currencyformat" ) ) {
							boxMembers.remove( i );
						}
						found = true;
						break;
					}
					i++;
				}
				if ( !found ) {
					throw new IllegalArgumentException( "BIF " + BIFName + " has no matching BoxMember. Remaining unused members are: " + boxMembers.stream()
					    .map( bm -> bm.type().toString() + "."
					        + ( bm.name().isEmpty() ? BIFName.toLowerCase().replace( bm.type().toString().toLowerCase(), "" ) : bm.name() ) + "()" )
					    .collect( Collectors.joining( ", " ) ) );
				}
			}

			/*
			 * System.out.println( "  BoxMember: " + Arrays.asList( boxMemberAnnotations ).stream()
			 * .map( b -> b.type().toString() + "."
			 * + ( b.name().isEmpty() ? BIFClass.getSimpleName().toLowerCase().replace( b.type().toString().toLowerCase(), "" ) : b.name() ) + "()"
			 * + ( b.objectArgument().isEmpty() ? " - use arg name \"" + getFirstArgName( BIFClass ) + "\" (pos: 1)"
			 * : " - use arg name \"" + b.objectArgument() + "\" (pos: " + getArgPosition( b.objectArgument(), BIFClass ) + ")" ) )
			 * .collect( Collectors.joining( ", " ) ) );
			 */
			// System.out.println();
		}

	}

	private String getArgPosition( String name, Class<? extends BIF> BIFClass ) {
		int i = 1;
		try {
			for ( Argument arg : BIFClass.newInstance().getDeclaredArguments() ) {
				if ( arg.name().equals( Key.of( name ) ) ) {
					return String.valueOf( i );
				}
				i++;
			}
		} catch ( InstantiationException | IllegalAccessException e ) {
			throw new RuntimeException( e );
		}
		throw new IllegalArgumentException( "Argument " + name + " not found in " + BIFClass.getName() );
	}

	private String getFirstArgName( Class<? extends BIF> BIFClass ) {
		try {
			return BIFClass.newInstance().getDeclaredArguments()[ 0 ].name().getName();
		} catch ( InstantiationException | IllegalAccessException e ) {
			throw new RuntimeException( e );
		}
	}

}
