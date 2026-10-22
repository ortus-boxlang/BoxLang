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

import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import ortus.boxlang.compiler.asmboxpiler.AsmTranspiler;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxScript;
import ortus.boxlang.compiler.ast.Source;
import ortus.boxlang.compiler.parser.BoxParser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.BoxRuntime;

/**
 * Verifies that a compile with the code profiler DISABLED never reads the source
 * text to look for profiler marks. Pass B's mark-locating helpers scan the source
 * (via {@link Source#getCode()}) to find keywords such as {@code finally}/{@code else}
 * — work that is only needed when profiling is on. When profiling is off these
 * scans are pure waste (and {@code SourceFile.getCode()} can re-read the whole file
 * each call), so the helpers must short-circuit on {@code hasProfilerId()}.
 */
// Profiling instrumentation is ASM-boxpiler-specific; skip under the Java boxpiler.
@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
class ProfilerSourceReadTest {

	@BeforeAll
	static void setUp() {
		BoxRuntime.getInstance( true );
	}

	/**
	 * A {@link Source} that counts how many times its code is read.
	 */
	private static class CountingSource extends Source {

		private final String	code;
		int						reads;

		CountingSource( String code ) {
			this.code = code;
		}

		@Override
		public String getCode() {
			this.reads++;
			return this.code;
		}

		@Override
		public Stream<String> getCodeAsStream() {
			return Stream.of( this.code.split( "\n" ) );
		}
	}

	/**
	 * Parse the source and re-tag every node's position with the given counting
	 * source so the profiler's source-scanning helpers read from it.
	 *
	 * @param code           the BOXSCRIPT source
	 * @param countingSource the source instance to tag onto every node
	 *
	 * @return the AST root
	 */
	private BoxScript parseWithCountingSource( String code, CountingSource countingSource ) throws Exception {
		BoxParser		parser	= new BoxParser();
		ParsingResult	result	= parser.parse( code );
		BoxScript		root	= ( BoxScript ) result.getRoot();
		for ( BoxNode node : root.getDescendants() ) {
			if ( node.getPosition() != null ) {
				node.getPosition().setSource( countingSource );
			}
		}
		return root;
	}

	@DisplayName( "A profiling-DISABLED compile never reads the source to locate profiler marks" )
	@Test
	void testNoSourceReadWhenProfilerDisabled() throws Exception {
		// Exercises ALL THREE Pass-B mark-locating helpers that scan the source:
		// BoxTryTransformer.finallyKeyword (try/finally), BoxIfElseTransformer
		// .findElseKeyword (if/else) and BoxFunctionDeclarationTransformer
		// .emitBodyBraceMark (function body braces). With profiling off, none of
		// them may read the source.
		String			code			= """
		                                  function foo() {
		                                      try {
		                                          a = 1;
		                                      } finally {
		                                          b = 2;
		                                      }
		                                  }
		                                  if( true ) {
		                                      foo();
		                                  } else {
		                                      c = 3;
		                                  }
		                                  """;
		CountingSource	countingSource	= new CountingSource( code );
		BoxScript		root			= parseWithCountingSource( code, countingSource );

		AsmTranspiler	transpiler		= new AsmTranspiler();
		transpiler.setProperty( "packageName", "boxgenerated.scripts" );
		transpiler.setProperty( "classname", "ProfilerSourceReadScript" );
		transpiler.setProperty( "baseclass", "BoxScript" );
		transpiler.setProperty( "returnType", "Object" );
		transpiler.setProperty( "sourceType", "BOXSCRIPT" );

		// Profiling is OFF: the transpiler has no profiler id, so no mark may be
		// emitted and no source scan should be performed.
		assertThat( transpiler.hasProfilerId() ).isFalse();

		transpiler.transpile( root );

		assertThat( countingSource.reads ).isEqualTo( 0 );
	}
}
