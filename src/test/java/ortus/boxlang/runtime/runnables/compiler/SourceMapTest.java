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
package ortus.boxlang.runtime.runnables.compiler;

import static com.google.common.truth.Truth.assertThat;

import org.junit.jupiter.api.Test;

import ortus.boxlang.runtime.util.JSONUtil;

public class SourceMapTest {

	@Test
	public void testItCanSFindLineNums() {
		String		json		= """
		                          {
		                          	"sourceMapRecords" : [ {
		                          	  "javaSourceLineStart" : 70,
		                          	  "javaSourceLineEnd" : 74,
		                          	  "originSourceLineStart" : 1,
		                          	  "originSourceLineEnd" : 1,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 75,
		                          	  "javaSourceLineEnd" : 90,
		                          	  "originSourceLineStart" : 2,
		                          	  "originSourceLineEnd" : 8,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 91,
		                          	  "javaSourceLineEnd" : 100,
		                          	  "originSourceLineStart" : 10,
		                          	  "originSourceLineEnd" : 10,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 101,
		                          	  "javaSourceLineEnd" : 105,
		                          	  "originSourceLineStart" : 12,
		                          	  "originSourceLineEnd" : 12,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 68,
		                          	  "javaSourceLineEnd" : 68,
		                          	  "originSourceLineStart" : 14,
		                          	  "originSourceLineEnd" : 17,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxFunctionDeclaration"
		                          	}, {
		                          	  "javaSourceLineStart" : 516,
		                          	  "javaSourceLineEnd" : 519,
		                          	  "originSourceLineStart" : 15,
		                          	  "originSourceLineEnd" : 15,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1.Func_greet",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 520,
		                          	  "javaSourceLineEnd" : 530,
		                          	  "originSourceLineStart" : 16,
		                          	  "originSourceLineEnd" : 16,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1.Func_greet",
		                          	  "javaSourceNode" : "IfStmt",
		                          	  "originSourceNode" : "BoxReturn"
		                          	}, {
		                          	  "javaSourceLineStart" : 106,
		                          	  "javaSourceLineEnd" : 110,
		                          	  "originSourceLineStart" : 19,
		                          	  "originSourceLineEnd" : 21,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 111,
		                          	  "javaSourceLineEnd" : 115,
		                          	  "originSourceLineStart" : 23,
		                          	  "originSourceLineEnd" : 23,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 116,
		                          	  "javaSourceLineEnd" : 120,
		                          	  "originSourceLineStart" : 25,
		                          	  "originSourceLineEnd" : 25,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	}, {
		                          	  "javaSourceLineStart" : 121,
		                          	  "javaSourceLineEnd" : 125,
		                          	  "originSourceLineStart" : 27,
		                          	  "originSourceLineEnd" : 27,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "MethodCallExpr",
		                          	  "originSourceNode" : "BoxFunctionInvocation"
		                          	}, {
		                          	  "javaSourceLineStart" : 121,
		                          	  "javaSourceLineEnd" : 125,
		                          	  "originSourceLineStart" : 27,
		                          	  "originSourceLineEnd" : 27,
		                          	  "javaSourceClassName" : "boxgenerated.templates.c.users.jacob.dev.boxlangbattleship.Test$cfs1",
		                          	  "javaSourceNode" : "ExpressionStmt",
		                          	  "originSourceNode" : "BoxExpression"
		                          	} ],
		                          	"source" : "c:\\\\Users\\\\jacob\\\\Dev\\\\boxlang-battleship\\\\test.cfs"
		                            }
		                          	""";
		SourceMap	sourceMap	= JSONUtil.fromJSON( SourceMap.class, json );

		assertThat( sourceMap.convertSourceLineToJavaLine( 15 ) ).isEqualTo( 516 );
		assertThat( sourceMap.convertSourceLineToJavaLine( 16 ) ).isEqualTo( 520 );
		assertThat( sourceMap.convertSourceLineToJavaLine( 17 ) ).isEqualTo( 68 );

		assertThat( sourceMap.convertJavaLineToSourceLine( 525 ) ).isEqualTo( 16 );
	}

}
