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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.runnables.RunnableLoader;
import ortus.boxlang.runtime.util.ResolvedFilePath;

public class BoxpilerClassPoolIndexTest {

	static BoxRuntime	instance;

	@TempDir
	Path				mappingDir;

	@BeforeAll
	public static void setUp() {
		instance = BoxRuntime.getInstance( true );
	}

	private ResolvedFilePath resolve( String name ) {
		return ResolvedFilePath.of( "/poolindex", this.mappingDir.toString(), name, this.mappingDir.resolve( name ) );
	}

	private Boxpiler boxpiler() {
		return ( Boxpiler ) RunnableLoader.getInstance().getBoxpiler();
	}

	@DisplayName( "Repeated compileClass calls for a file return the same class via the path index" )
	@Test
	public void testIndexedLookupReturnsSameClass() throws Exception {
		Files.writeString( this.mappingDir.resolve( "Indexed.bx" ), "class { function value() { return 1; } }" );
		var	first	= boxpiler().compileClass( resolve( "Indexed.bx" ) );
		var	second	= boxpiler().compileClass( resolve( "Indexed.bx" ) );

		assertThat( second ).isSameInstanceAs( first );
		assertThat( boxpiler().pathIndex.get( this.mappingDir.toString() ) ).containsKey( resolve( "Indexed.bx" ) );
	}

	@DisplayName( "An indexed class is still recompiled when its source changes" )
	@Test
	public void testIndexedLookupStillRecompiles() throws Exception {
		Path file = this.mappingDir.resolve( "Changing.bx" );
		Files.writeString( file, "class { function value() { return 1; } }" );
		var first = boxpiler().compileClass( resolve( "Changing.bx" ) );
		boxpiler().compileClass( resolve( "Changing.bx" ) );

		Files.writeString( file, "class { function value() { return 2; } }" );
		Files.setLastModifiedTime( file, FileTime.from( Instant.now().plusSeconds( 5 ) ) );
		var changed = boxpiler().compileClass( resolve( "Changing.bx" ) );

		assertThat( changed ).isNotSameInstanceAs( first );
		assertThat( boxpiler().compileClass( resolve( "Changing.bx" ) ) ).isSameInstanceAs( changed );
	}

	@DisplayName( "A hint naming a missing or different pool entry falls back to the full lookup" )
	@Test
	public void testStaleHintFallsBack() throws Exception {
		Files.writeString( this.mappingDir.resolve( "Stale.bx" ), "class {}" );
		var first = boxpiler().compileClass( resolve( "Stale.bx" ) );

		boxpiler().pathIndex.get( this.mappingDir.toString() ).put( resolve( "Stale.bx" ), "no.such.Fqn" );
		assertThat( boxpiler().compileClass( resolve( "Stale.bx" ) ) ).isSameInstanceAs( first );

		Files.writeString( this.mappingDir.resolve( "Other.bx" ), "class {}" );
		var		other		= boxpiler().compileClass( resolve( "Other.bx" ) );
		String	otherFQN	= boxpiler().pathIndex.get( this.mappingDir.toString() ).get( resolve( "Other.bx" ) );
		boxpiler().pathIndex.get( this.mappingDir.toString() ).put( resolve( "Stale.bx" ), otherFQN );
		assertThat( boxpiler().compileClass( resolve( "Stale.bx" ) ) ).isSameInstanceAs( first );
		assertThat( boxpiler().compileClass( resolve( "Stale.bx" ) ) ).isNotSameInstanceAs( other );
	}

	@DisplayName( "Clearing the page pool also clears the path index" )
	@Test
	public void testClearPagePoolClearsIndex() throws Exception {
		Files.writeString( this.mappingDir.resolve( "Cleared.bx" ), "class {}" );
		boxpiler().compileClass( resolve( "Cleared.bx" ) );
		boxpiler().clearPagePool();

		assertThat( boxpiler().pathIndex ).isEmpty();
	}
}
