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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

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

	record BenchMarkRecord(
	    Class<? extends AbstractParser> parserClass,
	    String fileName,
	    String csvName,
	    String benchMarkName ) {

	}

	static Map<String, BenchMarkRecord>	benchMarks	= new HashMap<String, BenchMarkRecord>();
	static BoxRuntime					instance;
	IBoxContext							context;
	IScope								variables;
	static Key							result		= new Key( "result" );

	static {
		benchMarks.put( "ClassA", new BenchMarkRecord(
		    CFParser.class,
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA.cfc",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA.csv",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_benchmark.csv"
		)
		);

		benchMarks.put( "ClassABoxLang", new BenchMarkRecord(
		    BoxParser.class,
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang.bx",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang.csv",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ClassA_BoxLang_benchmark.csv"
		)
		);

		benchMarks.put( "ControllerCF", new BenchMarkRecord(
		    CFParser.class,
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ControllerCF.cfc",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ControllerCF.csv",
		    "src/test/java/ortus/boxlang/compiler/parserprofiling/ControllerCF_benchmark.csv"
		)
		);
	}

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

		for ( BenchMarkRecord rec : benchMarks.values() ) {
			try {
				AbstractParser parser = ( AbstractParser ) rec.parserClass.getConstructor().newInstance();
				parser.setDebugMode( true );
				ParsingResult res = parser.parse( new File( rec.fileName ), true );
				parser.getProfilingResults().writeCSV( "src/test/java/ortus/boxlang/compiler/parserprofiling" );

				File	csvFile			= new File( rec.csvName );
				File	benchMarkFile	= new File( rec.benchMarkName );

				try {

					Files.delete( benchMarkFile.toPath() );
				} catch ( Exception e ) {
					// pass
				}

				csvFile.renameTo( benchMarkFile );
			} catch ( Exception e ) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private boolean runTest( BenchMarkRecord benchmark ) {
		try {
			AbstractParser parser = ( AbstractParser ) benchmark.parserClass.getConstructor().newInstance();
			parser.setDebugMode( true );
			long			start		= System.currentTimeMillis();
			ParsingResult	res			= parser.parse( new File( benchmark.fileName ), true );
			long			end			= System.currentTimeMillis();
			long			duration	= end - start;
			parser.getProfilingResults().writeCSV( "src/test/java/ortus/boxlang/compiler/parserprofiling" );

			assertThat( res.isCorrect() ).isTrue();
			boolean areEqual = FileUtils.contentEquals( new File( benchmark.benchMarkName ),
			    new File( benchmark.csvName ) );

			return areEqual;
		} catch ( Exception e ) {
			return false;
		}
	}

	@Test
	@Disabled
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
	@Disabled
	public void testBoxLangClassA() throws IOException {
		boolean areEqual = runTest( benchMarks.get( "ClassABoxLang" ) );
		assertThat( areEqual ).isTrue();
	}

	@Test
	public void testControllerCF() throws IOException {
		boolean areEqual = runTest( benchMarks.get( "ControllerCF" ) );
		assertThat( areEqual ).isTrue();
	}

}
