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

import java.io.IOException;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.parser.BoxParser;
import ortus.boxlang.compiler.parser.CFParser;

class ParserMemoryRepresentationTest {

	@Test
	void sourceTextRemainsAvailableFromSharedSourceRange() throws IOException {
		String	code	= "answer = variables.value + 1;";
		var		result	= new BoxParser().parse( code, false, true );

		assertThat( result.isCorrect() ).isTrue();
		assertThat( result.getRoot().getSourceText() ).isEqualTo( code );
		assertThat( result.getRoot().getDescendantsOfType( BoxBinaryOperation.class ).getFirst().getSourceText() )
		    .isEqualTo( "variables.value + 1" );
	}

	@Test
	void explicitlyChangedSourceTextOverridesOriginalRange() throws IOException {
		var node = new BoxParser().parseExpression( "value + 1" ).getRoot();

		node.setSourceText( "replacement" );

		assertThat( node.getSourceText() ).isEqualTo( "replacement" );
	}

	@Test
	void astMapResolvesLazySourceText() throws IOException {
		var node = new BoxParser().parseExpression( "value + 1" ).getRoot();

		assertThat( node.toMap().get( "sourceText" ) ).isEqualTo( "value + 1" );
	}

	@Test
	void subparserSourceTextUsesExpressionInsteadOfOuterSourceOffset() throws IOException {
		String	code	= "<cfloop condition=\"counter LT 5\"></cfloop>";
		var		result	= new CFParser().parse( code, false, false );

		assertThat( result.isCorrect() ).isTrue();
		assertThat( result.getRoot().getDescendantsOfType( BoxComparisonOperation.class ).getFirst().getSourceText() )
		    .isEqualTo( "counter LT 5" );
	}

	@Test
	void pointMutationUpdatesCompactPosition() {
		Position	position			= new Position( new Point( 1, 2 ), new Point( 3, 4 ) );
		Point		previousStartView	= position.getStart();

		position.getStart().setLine( 5 ).setColumn( 6 );
		position.getEnd().setLine( 7 ).setColumn( 8 );

		assertThat( position.getStart().getLine() ).isEqualTo( 5 );
		assertThat( position.getStart().getColumn() ).isEqualTo( 6 );
		assertThat( position.getEnd().getLine() ).isEqualTo( 7 );
		assertThat( position.getEnd().getColumn() ).isEqualTo( 8 );
		assertThat( previousStartView.getLine() ).isEqualTo( 5 );
		assertThat( previousStartView.getColumn() ).isEqualTo( 6 );
	}

	@Test
	void positionMutationDoesNotDiscardLazySourceText() throws IOException {
		var node = new BoxParser().parseExpression( "value + 1" ).getRoot();

		node.getPosition().getStart().setLine( 10 );

		assertThat( node.getSourceText() ).isEqualTo( "value + 1" );
	}

	@Test
	void sourceRangesConvertAntlrCodePointIndexesToJavaCharIndexes() {
		SourceCode	source		= new SourceCode( "\uD83D\uDE80value" );
		Position	position	= new Position( new Point( 1, 1 ), new Point( 1, 6 ), source, 1, 6 );

		assertThat( position.sourceTextEquals( "value" ) ).isTrue();
		assertThat( position.getSourceText() ).isEqualTo( "value" );
	}

	@Test
	void parentRegistrationPromotesSharedEmptyChildrenList() throws IOException {
		BoxBinaryOperation operation = ( BoxBinaryOperation ) new BoxParser().parseExpression( "left + right" ).getRoot();

		assertThat( operation.getChildren() ).hasSize( 2 );
		assertThat( operation.getChildren().getFirst().getChildren() ).isEmpty();
	}

}
