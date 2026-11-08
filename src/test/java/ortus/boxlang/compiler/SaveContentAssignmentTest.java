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

import java.util.ArrayList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentModifier;
import ortus.boxlang.compiler.ast.expression.BoxAssignmentOperator;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxSaveContentAssignment;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.PrettyPrintBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.compiler.parser.BoxSourceType;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.context.ScriptingRequestBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.scopes.VariablesScope;

public class SaveContentAssignmentTest {

	private static BoxRuntime runtime;

	@BeforeAll
	public static void setup() {
		runtime = BoxRuntime.getInstance( true );
	}

	@Test
	public void testAssignmentAstAndVisitors() {
		var parsed = runtime.parse( "final var result = savecontent { writeOutput( 'test' ); }", BoxSourceType.CFSCRIPT );
		assertThat( parsed.isCorrect() ).isTrue();
		var captures = parsed.getRoot().getDescendantsOfType( BoxSaveContentAssignment.class );
		assertThat( captures ).hasSize( 1 );
		var assignment = captures.getFirst();
		assertThat( assignment.getOp() ).isEqualTo( BoxAssignmentOperator.Equal );
		assertThat( assignment.getModifiers() ).containsAtLeast( BoxAssignmentModifier.FINAL, BoxAssignmentModifier.VAR );
		assertThat( assignment.getComponent().getParent() ).isSameInstanceAs( assignment );
		assertThat( assignment.toMap() ).containsKey( "component" );
		assignment.accept( new VoidBoxVisitor() {
		} );
		assertThat( assignment.accept( new ReplacingBoxVisitor() {
		} ) ).isSameInstanceAs( assignment );
		String printed = parsed.getRoot().toString();
		assertThat( printed ).contains( "final var result = savecontent" );
		assertThat( printed ).doesNotContain( "variable=" );
		assertThat( runtime.parse( printed, BoxSourceType.CFSCRIPT ).isCorrect() ).isTrue();
	}

	@Test
	public void testVisitorsReachAndReplaceTargetAndBody() {
		var parsed = runtime.parse( "data[key] = savecontent { writeOutput( 'original' ); }", BoxSourceType.CFSCRIPT );
		assertThat( parsed.isCorrect() ).isTrue();
		var	assignment	= parsed.getRoot().getDescendantsOfType( BoxSaveContentAssignment.class ).getFirst();
		var	visited		= new ArrayList<BoxNode>();
		assignment.accept( new VoidBoxVisitor() {

			@Override
			public void visit( BoxIdentifier node ) {
				visited.add( node );
			}

			@Override
			public void visit( BoxStringLiteral node ) {
				visited.add( node );
			}
		} );
		var	key		= assignment.getLeft().getDescendantsOfType( BoxIdentifier.class ).stream()
		    .filter( identifier -> identifier.getName().equals( "key" ) ).findFirst().orElseThrow();
		var	content	= assignment.getComponent().getDescendantsOfType( BoxStringLiteral.class ).getFirst();
		assertThat( visited.stream().filter( node -> node == key ).count() ).isEqualTo( 1L );
		assertThat( visited.stream().filter( node -> node == content ).count() ).isEqualTo( 1L );

		assignment.accept( new ReplacingBoxVisitor() {

			@Override
			public BoxNode visit( BoxIdentifier node ) {
				return node == key ? new BoxIdentifier( "newKey", node.getPosition(), node.getSourceText() ) : node;
			}

			@Override
			public BoxNode visit( BoxStringLiteral node ) {
				return node == content ? new BoxStringLiteral( "updated", node.getPosition(), node.getSourceText() ) : node;
			}
		} );
		var	replacementKey		= assignment.getLeft().getDescendantsOfType( BoxIdentifier.class ).stream()
		    .filter( identifier -> identifier.getName().equals( "newKey" ) ).findFirst().orElseThrow();
		var	replacementContent	= assignment.getComponent().getDescendantsOfType( BoxStringLiteral.class ).getFirst();
		assertThat( replacementKey.getParent().getChildren() ).contains( replacementKey );
		assertThat( replacementContent.getValue() ).isEqualTo( "updated" );
		assertThat( replacementContent.getParent().getChildren() ).contains( replacementContent );
		String printed = parsed.getRoot().toString();
		assertThat( printed ).contains( "newKey" );
		assertThat( printed ).contains( "updated" );
		assertThat( runtime.parse( printed, BoxSourceType.CFSCRIPT ).isCorrect() ).isTrue();
	}

	@Test
	@EnabledIf( "tools.CompilerUtils#isASMBoxpiler" )
	public void testAssignmentWithProfilerEnabled() {
		var		service			= runtime.getCodeProfilerService();
		boolean	previousEnabled	= runtime.getConfiguration().codeProfilerEnabled;
		boolean	previousActive	= service.isActive();
		String	source			= "result = savecontent { writeOutput( 'profiled' ); }";
		try {
			runtime.getConfiguration().codeProfilerEnabled = true;
			service.setActive( true );
			var context = new ScriptingRequestBoxContext( runtime.getRuntimeContext() );
			runtime.executeSource( source, context, BoxSourceType.CFSCRIPT );
			assertThat( context.getScopeNearby( VariablesScope.name ).get( Key.of( "result" ) ) ).isEqualTo( "profiled" );
			var spans = service.fileSpans( IBoxpiler.MD5( BoxSourceType.CFSCRIPT.toString() + source ) );
			assertThat( spans ).isNotEmpty();
			for ( var span : spans ) {
				assertThat( span.stats().count() ).isGreaterThan( 0L );
			}
		} finally {
			runtime.getConfiguration().codeProfilerEnabled = previousEnabled;
			service.setActive( previousActive );
		}
	}

	@Test
	public void testCompoundAssignmentPrettyPrint() {
		var parsed = runtime.parse( "data[ key & 'Suffix' ] &= savecontent { writeOutput( 'test' ); }", BoxSourceType.CFSCRIPT );
		assertThat( parsed.isCorrect() ).isTrue();
		var assignment = parsed.getRoot().getDescendantsOfType( BoxSaveContentAssignment.class ).getFirst();
		assertThat( assignment.getOp() ).isEqualTo( BoxAssignmentOperator.ConcatEqual );
		String printed = parsed.getRoot().toString();
		assertThat( printed ).contains( "&= savecontent" );
		assertThat( printed ).contains( "Suffix" );
		var legacyPrinter = new PrettyPrintBoxVisitor();
		parsed.getRoot().accept( legacyPrinter );
		assertThat( legacyPrinter.getOutput() ).contains( "&= savecontent" );
		assertThat( runtime.parse( legacyPrinter.getOutput(), BoxSourceType.CFSCRIPT ).isCorrect() ).isTrue();
		assertThat( runtime.parse( printed, BoxSourceType.CFSCRIPT ).isCorrect() ).isTrue();
	}

	@ParameterizedTest
	@ValueSource( strings = {
	    "a = b = savecontent { writeOutput( 'test' ); }",
	    "function test() { return a = savecontent { writeOutput( 'test' ); }; }",
	    "if ( a = savecontent { writeOutput( 'test' ); } ) {}"
	} )
	public void testEmbeddedAssignmentsReportParseErrors( String source ) {
		// Retain the PR's standalone-assignment restriction until Adobe behavior is verified.
		assertThat( runtime.parse( source, BoxSourceType.CFSCRIPT ).isCorrect() ).isFalse();
	}

}
