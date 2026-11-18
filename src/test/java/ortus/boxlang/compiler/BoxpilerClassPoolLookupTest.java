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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.nio.file.Path;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.util.ResolvedFilePath;

/**
 * Covers the class pool lookup used by {@link Boxpiler#compileClass(ResolvedFilePath)} on every `new` and
 * `createObject`. The lookup is keyed by FQN first so its cost does not grow with the number of classes in the
 * mapping's pool.
 */
public class BoxpilerClassPoolLookupTest {

	static BoxRuntime	instance;
	static Boxpiler		boxpiler;

	static final String	MAPPING_NAME	= "/lookupbench";
	static final String	MAPPING_PATH	= "C:/lookupbench/gen/";
	static final int	CALLS			= 10_000;
	static final int	RUNS			= 3;

	@BeforeAll
	public static void setUp() {
		instance	= BoxRuntime.getInstance( true );
		boxpiler	= ( Boxpiler ) RunnableLoader.getInstance().getBoxpiler();
	}

	@AfterAll
	public static void tearDown() {
		// Our pool is only used by this test, don't leave it behind
		boxpiler.getClassPools().remove( MAPPING_PATH );
	}

	@DisplayName( "It finds a pooled class by its FQN key" )
	@Test
	public void testFindByFqn() {
		ResolvedFilePath	rfp			= rfp( 1 );
		ClassInfo			classInfo	= ClassInfo.forClass( rfp, BoxSourceType.BOXSCRIPT, boxpiler );
		boxpiler.getClassPool( rfp.mappingPath() ).put( classInfo.fqn().toString(), classInfo );

		assertThat( boxpiler.findClassInfo( rfp ) ).isSameInstanceAs( classInfo );
	}

	@DisplayName( "It finds a pooled class by path when it is registered under another key" )
	@Test
	public void testFindByPathFallback() {
		ResolvedFilePath	rfp			= rfp( 2 );
		// A template is pooled under boxgenerated.templates.*, so the class FQN lookup will miss
		ClassInfo			template	= ClassInfo.forTemplate( rfp, BoxSourceType.BOXSCRIPT, boxpiler );
		boxpiler.getClassPool( rfp.mappingPath() ).put( template.fqn().toString(), template );

		assertThat( template.fqn().toString() ).isNotEqualTo( rfp.getFQN( "boxgenerated.boxclass" ).toString() );
		assertThat( boxpiler.findClassInfo( rfp ) ).isSameInstanceAs( template );
	}

	@DisplayName( "It returns null when the path is not pooled" )
	@Test
	public void testNotPooled() {
		assertThat( boxpiler.findClassInfo( rfp( 3 ) ) ).isNull();
	}

	@DisplayName( "It does not look up paths without a mapping path" )
	@Test
	public void testNoMappingPath() {
		ResolvedFilePath rfp = new ResolvedFilePath( null, null, "C:/lookupbench/genC4.bx", Path.of( "C:/lookupbench/genC4.bx" ) );

		assertThat( boxpiler.findClassInfo( rfp ) ).isNull();
	}

	@DisplayName( "Lookup cost stays flat as the class pool grows" )
	@Test
	@Disabled( "Manual timing probe, results vary by machine. Run by hand to compare the FQN lookup against the path scan." )
	public void testLookupTimings() {
		int[]		sizes		= { 1, 50, 500, 4_000 };
		double[]	newTimes	= new double[ sizes.length ];
		double[]	legacyTimes	= new double[ sizes.length ];

		System.out.println();
		System.out.println( "Class pool lookup cost in us per call (" + CALLS + " calls, best of " + RUNS + ")" );
		System.out.println( "--------------------------------------------------------------------------------" );
		System.out.printf( "%-12s %14s %14s %12s%n", "pool size", "FQN first", "path scan", "speedup" );

		// Warm both code paths up so the first row isn't timing the JIT
		Map<String, ClassInfo>	warmPool	= populatePool( 500 );
		ResolvedFilePath		warmTarget	= rfp( 499 );
		measure( rfp -> boxpiler.findClassInfo( rfp ), warmTarget, CALLS );
		measure( rfp -> legacyLookup( warmPool, rfp ), warmTarget, CALLS );
		boxpiler.getClassPools().remove( MAPPING_PATH );

		for ( int i = 0; i < sizes.length; i++ ) {
			Map<String, ClassInfo>	classPool	= populatePool( sizes[ i ] );
			ResolvedFilePath		target		= rfp( sizes[ i ] - 1 );

			newTimes[ i ]		= measure( rfp -> boxpiler.findClassInfo( rfp ), target, CALLS );
			legacyTimes[ i ]	= measure( rfp -> legacyLookup( classPool, rfp ), target, CALLS );

			System.out.printf( "%-12d %14.3f %14.3f %11.1fx%n", sizes[ i ], newTimes[ i ], legacyTimes[ i ],
			    legacyTimes[ i ] / newTimes[ i ] );

			boxpiler.getClassPools().remove( MAPPING_PATH );
		}
		System.out.println();

		// The scan is linear in the pool size, the FQN lookup is not
		assertThat( newTimes[ 3 ] ).isLessThan( newTimes[ 1 ] * 4.0 );
		assertThat( legacyTimes[ 3 ] ).isGreaterThan( legacyTimes[ 1 ] * 5.0 );

		// Which also means the flat lookup wins big once a mapping holds hundreds of classes
		assertThat( newTimes[ 3 ] ).isLessThan( legacyTimes[ 3 ] );
	}

	/**
	 * Create a mapping's worth of pooled classes. Each ClassInfo is registered under the FQN that
	 * {@code compileClass()} would register it under.
	 */
	private Map<String, ClassInfo> populatePool( int size ) {
		Map<String, ClassInfo> classPool = boxpiler.getClassPool( MAPPING_PATH );
		for ( int i = 0; i < size; i++ ) {
			ResolvedFilePath	rfp			= rfp( i );
			ClassInfo			classInfo	= ClassInfo.forClass( rfp, BoxSourceType.BOXSCRIPT, boxpiler );
			classPool.put( classInfo.fqn().toString(), classInfo );
		}
		return classPool;
	}

	/**
	 * The pre-existing lookup: copy the pool values, then compare every resolved file path
	 */
	private ClassInfo legacyLookup( Map<String, ClassInfo> classPool, ResolvedFilePath resolvedFilePath ) {
		ClassInfo[] snapshot = classPool.values().toArray( new ClassInfo[ classPool.size() + 50 ] );
		for ( ClassInfo entry : snapshot ) {
			if ( entry == null ) {
				break;
			}
			if ( entry.resolvedFilePath().equals( resolvedFilePath ) ) {
				return entry;
			}
		}
		return classPool.get( resolvedFilePath.getFQN( "boxgenerated.boxclass" ).toString() );
	}

	private double measure( Function<ResolvedFilePath, ClassInfo> lookup, ResolvedFilePath target, int calls ) {
		double	best	= Double.MAX_VALUE;
		int		found	= 0;
		for ( int run = 0; run < RUNS; run++ ) {
			long start = System.nanoTime();
			for ( int i = 0; i < calls; i++ ) {
				// A fresh ResolvedFilePath (and fresh path strings) per call, like a real resolution produces
				ResolvedFilePath fresh = ResolvedFilePath.of(
				    target.mappingName(),
				    target.mappingPath(),
				    new String( target.relativePath() ),
				    Path.of( target.absolutePath().toString() )
				);
				found += lookup.apply( fresh ) == null ? 0 : 1;
			}
			best = Math.min( best, ( System.nanoTime() - start ) / 1_000.0 / calls );
		}
		assertThat( found ).isEqualTo( calls * RUNS );
		return best;
	}

	private static ResolvedFilePath rfp( int index ) {
		String	relativePath	= MAPPING_NAME + "/C" + index + ".bx";
		String	absolutePath	= MAPPING_PATH + "C" + index + ".bx";
		return ResolvedFilePath.of( MAPPING_NAME, MAPPING_PATH, relativePath, Path.of( absolutePath ) );
	}
}
