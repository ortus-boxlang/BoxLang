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
package ortus.boxlang.compiler;

import static com.google.common.truth.Truth.assertThat;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.visitor.ClassMetadataVisitor;
import ortus.boxlang.compiler.parser.BoxScriptParser;
import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.ParseException;

public class ClassMetadataVisitorTest {

	@Test
	public void testMetadataVisitor() {
		try {
			ParsingResult result = new BoxScriptParser()
			    .parse( Paths.get( "src/test/java/ortus/boxlang/compiler/MyClassMDVistor.bx" ).toAbsolutePath().toFile() );
			if ( !result.isCorrect() ) {
				throw new ParseException( result.getIssues(), "" );
			}
			ClassMetadataVisitor visitor = new ClassMetadataVisitor();
			result.getRoot().accept( visitor );

			var meta = visitor.getMetadata();
			assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "class" );
			assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "MyClassMDVistor" );
			assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.ortus.boxlang.compiler.MyClassMDVistor" );
			assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClassMDVistor.bx" ) ).isTrue();
			assertThat( meta.get( Key.of( "properties" ) ) instanceof Array ).isTrue();
			assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();

			assertThat( meta.get( Key.of( "extends" ) ) instanceof IStruct ).isTrue();

			assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 7 );
			var fun1 = meta.getAsArray( Key.of( "functions" ) ).get( 0 );
			assertThat( fun1 ).isInstanceOf( Struct.class );
			assertThat( ( ( IStruct ) fun1 ).containsKey( Key.of( "name" ) ) ).isTrue();

			assertThat( meta.get( Key.of( "documentation" ) ) instanceof IStruct ).isTrue();
			var docs = meta.getAsStruct( Key.of( "documentation" ) );
			assertThat( docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
			assertThat( docs.get( Key.of( "luis" ) ) ).isEqualTo( "" );
			assertThat( docs.getAsString( Key.of( "hint" ) ).trim() )
			    .isEqualTo( "This is my class description continued on this line \nand this one as well." );

			assertThat( meta.get( Key.of( "annotations" ) ) instanceof IStruct ).isTrue();
			var annos = meta.getAsStruct( Key.of( "annotations" ) );
			assertThat( annos.getAsString( Key.of( "foo" ) ).trim() ).isEqualTo( "bar" );
			// assertThat( annos.getAsString( Key.of( "implements" ) ).trim() ).isEqualTo( "Luis,Jorge" );
			assertThat( annos.getAsString( Key.of( "singleton" ) ).trim() ).isEqualTo( "" );
			assertThat( annos.getAsString( Key.of( "gavin" ) ).trim() ).isEqualTo( "pickin" );
			assertThat( annos.getAsString( Key.of( "inject" ) ).trim() ).isEqualTo( "" );

			assertThat( annos.get( Key.of( "multiAnno" ) ) ).isInstanceOf( Array.class );
			assertThat( annos.getAsArray( Key.of( "multiAnno" ) ).size() ).isEqualTo( 3 );
			assertThat( annos.getAsArray( Key.of( "multiAnno" ) ).get( 0 ) ).isEqualTo( "one" );
			assertThat( annos.getAsArray( Key.of( "multiAnno" ) ).get( 1 ) ).isEqualTo( "two" );
			assertThat( annos.getAsArray( Key.of( "multiAnno" ) ).get( 2 ) ).isEqualTo( "three" );

			assertThat( annos.get( Key.of( "arrayAnno" ) ) ).isInstanceOf( Array.class );
			assertThat( annos.getAsArray( Key.of( "arrayAnno" ) ).size() ).isEqualTo( 3 );
			assertThat( annos.getAsArray( Key.of( "arrayAnno" ) ).get( 0 ) ).isEqualTo( "one" );
			assertThat( annos.getAsArray( Key.of( "arrayAnno" ) ).get( 1 ) ).isEqualTo( "two" );
			assertThat( annos.getAsArray( Key.of( "arrayAnno" ) ).get( 2 ) ).isEqualTo( "three" );

			assertThat( annos.get( Key.of( "structAnno" ) ) ).isInstanceOf( IStruct.class );
			assertThat( annos.getAsStruct( Key.of( "structAnno" ) ).get( Key.of( "one" ) ) ).isEqualTo( "two" );
			assertThat( annos.getAsStruct( Key.of( "structAnno" ) ).get( Key.of( "three" ) ) ).isEqualTo( "four" );
		} catch ( IOException e ) {
			throw new RuntimeException( e );
		}
	}

	@Test
	public void testMetadataVisitorCF() {
		ParsingResult result = new Parser().parse( Paths.get( "src/test/java/TestCases/phase3/MyClassCF.cfc" ).toAbsolutePath().toFile() );
		if ( !result.isCorrect() ) {
			throw new ParseException( result.getIssues(), "" );
		}
		ClassMetadataVisitor visitor = new ClassMetadataVisitor();
		result.getRoot().accept( visitor );

		var meta = visitor.getMetadata();
		System.out.println( meta );
		assertThat( meta.get( Key.of( "type" ) ) ).isEqualTo( "class" );
		assertThat( meta.get( Key.of( "fullname" ) ) ).isEqualTo( "src.test.java.testcases.phase3.MyClassCF" );
		assertThat( meta.get( Key.of( "name" ) ) ).isEqualTo( "MyClassCF" );
		assertThat( meta.getAsString( Key.of( "path" ) ).contains( "MyClassCF.cfc" ) ).isTrue();
		assertThat( meta.get( Key.of( "properties" ) ) instanceof Array ).isTrue();
		assertThat( meta.get( Key.of( "functions" ) ) instanceof Array ).isTrue();

		assertThat( meta.get( Key.of( "extends" ) ) instanceof IStruct ).isTrue();

		assertThat( meta.getAsArray( Key.of( "functions" ) ).size() ).isEqualTo( 5 );
		var fun1 = meta.getAsArray( Key.of( "functions" ) ).get( 0 );
		assertThat( fun1 ).isInstanceOf( Struct.class );
		assertThat( ( ( IStruct ) fun1 ).containsKey( Key.of( "name" ) ) ).isTrue();

		assertThat( meta.get( Key.of( "documentation" ) ) instanceof IStruct ).isTrue();
		var docs = meta.getAsStruct( Key.of( "documentation" ) );
		assertThat( docs.getAsString( Key.of( "brad" ) ).trim() ).isEqualTo( "wood" );
		assertThat( docs.get( Key.of( "luis" ) ) ).isEqualTo( "" );
		assertThat( docs.getAsString( Key.of( "hint" ) ).trim() )
		    .isEqualTo( "This is my class description" );

		assertThat( meta.get( Key.of( "annotations" ) ) instanceof IStruct ).isTrue();
		var annos = meta.getAsStruct( Key.of( "annotations" ) );
		assertThat( annos.getAsString( Key.of( "foo" ) ).trim() ).isEqualTo( "bar" );
		// assertThat( annos.getAsString( Key.of( "implements" ) ).trim() ).isEqualTo( "Luis,Jorge" );
		assertThat( annos.getAsString( Key.of( "singleton" ) ).trim() ).isEqualTo( "" );
		assertThat( annos.getAsString( Key.of( "gavin" ) ).trim() ).isEqualTo( "pickin" );
		assertThat( annos.getAsString( Key.of( "inject" ) ).trim() ).isEqualTo( "" );
	}

}
