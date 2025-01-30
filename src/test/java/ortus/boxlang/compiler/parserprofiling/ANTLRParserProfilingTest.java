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
package ortus.boxlang.compiler.parserprofiling;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.parser.AbstractParser;
import ortus.boxlang.compiler.parser.BoxParser;
import ortus.boxlang.compiler.parser.CFParser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.IScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class ANTLRParserProfilingTest {

	static BoxRuntime	instance;
	IBoxContext			context;
	IScope				variables;
	static Key			result	= new Key( "result" );

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

	@Test
	@Disabled
	public void generateBenchMarks() {
		record BenchMarkRecord(
		    Class<? extends AbstractParser> parserClass,
		    String fileName,
		    String csvName,
		    String benchMarkName ) {

		}
		List<BenchMarkRecord> benchMarks = new ArrayList<BenchMarkRecord>();
		benchMarks.add( new BenchMarkRecord(
		    CFParser.class,
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA.cfc",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA.csv",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_benchmark.csv"
		)
		);

		benchMarks.add( new BenchMarkRecord(
		    CFParser.class,
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang.bx",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang.csv",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang_benchmark.csv"
		)
		);

		for ( BenchMarkRecord rec : benchMarks ) {
			try {
				AbstractParser parser = ( AbstractParser ) rec.parserClass.getConstructor().newInstance();
				parser.setDebugMode( true );
				ParsingResult res = parser.parse( new File( rec.fileName ), true );
				parser.getProfilingResults().writeCSV( "src/test/java/ortus/boxlang/compiler/parserprofiling" );

				File	csvFile			= new File( rec.csvName );
				File	benchMarkFile	= new File( rec.benchMarkName );

				csvFile.renameTo( benchMarkFile );
			} catch ( Exception e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	@Test
	public void testUnquotedAttribute4() throws IOException {
		CFParser parser = new CFParser();
		parser.setDebugMode( true );
		ParsingResult res = parser.parse( new File( "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA.cfc" ), true );
		parser.getProfilingResults().writeCSV( "src/test/java/ortus/boxlang/compiler/parserprofiling" );

		assertThat( res.isCorrect() ).isTrue();
		boolean areEqual = FileUtils.contentEquals( new File( "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_benchmark.csv" ),
		    new File( "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA.csv" ) );
		assertThat( areEqual ).isTrue();
	}

	@Test
	public void testBoxLangClassA() throws IOException {
		BoxParser parser = new BoxParser();
		parser.setDebugMode( true );
		ParsingResult res = parser.parse( new File( "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang.bx" ), true );
		parser.getProfilingResults().writeCSV( "src/test/java/ortus/boxlang/compiler/parserprofiling" );

		assertThat( res.isCorrect() ).isTrue();
		boolean areEqual = FileUtils.contentEquals( new File( "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_benchmark.csv" ),
		    new File( "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang.csv" ) );
		assertThat( areEqual ).isTrue();
	}

}
