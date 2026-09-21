/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.runtime.runnables;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.VariablesScope;

/**
 * Regression coverage for BL-2628: BoxClassSupport.setSuper merges the parent's properties and accessor lookups into
 * the child's static maps on every instantiation. Without serialisation, the very first instantiations of a freshly
 * compiled subclass race the HashMap resize and can permanently drop the child's own accessor entries for the life of
 * the JVM.
 */
public class BoxClassSupportConcurrencyTest {

	static final int	ROUNDS				= 16;
	static final int	THREADS				= 64;
	static final int	PARENT_PROPERTIES	= 32;

	static BoxRuntime	instance;
	static Path			classDirectory;

	@BeforeAll
	public static void setUp() throws IOException {
		instance		= BoxRuntime.getInstance( true );
		classDirectory	= Files.createTempDirectory( "bl2628" );
		instance.getConfiguration().registerMapping( "/bl2628", classDirectory.toAbsolutePath().toString() );
	}

	@AfterAll
	public static void teardown() throws IOException {
		instance.getConfiguration().unregisterMapping( "/bl2628" );
		if ( classDirectory != null && Files.exists( classDirectory ) ) {
			try ( var paths = Files.walk( classDirectory ) ) {
				paths.sorted( Comparator.reverseOrder() ).forEach( path -> path.toFile().delete() );
			}
		}
	}

	@DisplayName( "generated accessors survive parallel first instantiation of a cold subclass" )
	@Test
	void testAccessorsSurviveParallelFirstInstantiation() throws Exception {
		List<String> failures = new CopyOnWriteArrayList<>();

		for ( int round = 0; round < ROUNDS; round++ ) {
			String			suffix		= "r" + round + "_" + System.nanoTime();
			String			child		= writeClasses( suffix );

			// Every thread races the very first instantiation of a class nothing has touched yet.
			CountDownLatch	start		= new CountDownLatch( 1 );
			CountDownLatch	finished	= new CountDownLatch( THREADS );
			ExecutorService	executor	= Executors.newFixedThreadPool( THREADS );
			try {
				for ( int i = 0; i < THREADS; i++ ) {
					executor.submit( () -> {
						try {
							start.await();
							assertProbe( child, failures, "concurrent" );
						} catch ( InterruptedException e ) {
							Thread.currentThread().interrupt();
						} finally {
							finished.countDown();
						}
					} );
				}
				start.countDown();
				assertThat( finished.await( 60, TimeUnit.SECONDS ) ).isTrue();
			} finally {
				executor.shutdownNow();
			}

			// The damage is permanent once done, so a later single-threaded call must still see the accessors.
			assertProbe( child, failures, "single-threaded" );
		}

		assertThat( failures ).isEmpty();
	}

	private void assertProbe( String child, List<String> failures, String phase ) {
		try {
			IBoxContext	context		= new ScriptingRequestBoxContext( instance.getRuntimeContext() );
			IScope		variables	= context.getScopeNearby( VariablesScope.name );
			instance.executeSource( "result = new bl2628." + child + "( table=\"orders\" ).probe()", context );
			Object result = variables.get( Key.of( "result" ) );
			if ( !"ordersinnerp17".equals( result ) ) {
				failures.add( phase + " " + child + " returned " + result );
			}
		} catch ( Throwable e ) {
			failures.add( phase + " " + child + " threw " + e.getClass().getSimpleName() + ": " + e.getMessage() );
		}
	}

	/**
	 * Writes a unique parent/child pair so each round compiles genuinely cold classes.
	 *
	 * @return the child class name
	 */
	private String writeClasses( String suffix ) throws IOException {
		String			parent	= "Parent_" + suffix;
		String			child	= "Child_" + suffix;

		StringBuilder	props	= new StringBuilder();
		for ( int i = 0; i < PARENT_PROPERTIES; i++ ) {
			props.append( "\tproperty name=\"p" ).append( i ).append( "\" type=\"string\" default=\"p" ).append( i ).append( "\";\n" );
		}

		// @formatter:off
		Files.writeString( classDirectory.resolve( parent + ".bx" ),
		    """
		    class accessors=true {
		    %s
		        function init() {
		            return this;
		        }
		    }
		    """.formatted( props.toString() ) );

		Files.writeString( classDirectory.resolve( child + ".bx" ),
		    """
		    class accessors=true extends="bl2628.%s" {
		        property name="table" type="string";
		        property name="type" type="string" default="inner";
		        property name="alias" type="string" default="";
		        property name="conditions" type="string" default="";

		        function init( required table ) {
		            variables.table = arguments.table;
		            super.init();
		            return this;
		        }

		        function probe() {
		            return getTable() & getType() & getP17();
		        }
		    }
		    """.formatted( parent ) );
		// @formatter:on

		return child;
	}

}