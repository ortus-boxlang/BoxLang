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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Point;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.expression.BoxBinaryOperation;
import ortus.boxlang.compiler.ast.expression.BoxComparisonOperation;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
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
	void parserCanonicalizesEqualSemanticStringsWithinOneAst() throws IOException {
		var identifiers = new BoxParser().parseExpression( "alpha + alpha + Alpha" ).getRoot().getDescendantsOfType( BoxIdentifier.class );

		assertThat( identifiers ).hasSize( 3 );
		assertThat( identifiers.get( 0 ).getName() ).isSameInstanceAs( identifiers.get( 1 ).getName() );
		assertThat( identifiers.get( 0 ).getName() ).isNotSameInstanceAs( identifiers.get( 2 ).getName() );
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
	void primitiveCoordinatesCreateEquivalentPosition() {
		Position position = new Position( 1, 2, 3, 4 );

		assertThat( position.getStart().getLine() ).isEqualTo( 1 );
		assertThat( position.getStart().getColumn() ).isEqualTo( 2 );
		assertThat( position.getEnd().getLine() ).isEqualTo( 3 );
		assertThat( position.getEnd().getColumn() ).isEqualTo( 4 );
	}

	@Test
	void primitiveCoordinatesRetainSourceRangeAcrossLines() {
		SourceCode	source		= new SourceCode( "first\nvalue" );
		Position	position	= new Position( 1, 0, 2, 5, source, 6, 11 );

		assertThat( position.getSource() ).isSameInstanceAs( source );
		assertThat( position.getStart().getLine() ).isEqualTo( 1 );
		assertThat( position.getEnd().getLine() ).isEqualTo( 2 );
		assertThat( position.getSourceText() ).isEqualTo( "value" );
	}

	@Test
	void positionMutationDoesNotDiscardLazySourceText() throws IOException {
		var node = new BoxParser().parseExpression( "value + 1" ).getRoot();

		node.getPosition().getStart().setLine( 10 );

		assertThat( node.getSourceText() ).isEqualTo( "value + 1" );
	}

	@Test
	void compactNodePositionProvidesLiveViews() throws IOException {
		var			node			= new BoxParser().parseExpression( "value + 1" ).getRoot();
		Position	positionView	= node.getPosition();
		Point		startView		= positionView.getStart();

		node.getPosition().getStart().setLine( 10 ).setColumn( 20 );
		node.getPosition().getEnd().setLine( 30 ).setColumn( 40 );

		assertThat( positionView.getStart().getLine() ).isEqualTo( 10 );
		assertThat( positionView.getEnd().getColumn() ).isEqualTo( 40 );
		assertThat( startView.getLine() ).isEqualTo( 10 );
		assertThat( startView.getColumn() ).isEqualTo( 20 );
		assertThat( node.getPosition().toString() ).endsWith( "10,20 - 30,40" );
		assertThat( node.getPosition().toMap().get( "start" ) ).isEqualTo( java.util.Map.of( "line", 10, "column", 20 ) );
	}

	@Test
	void nodePositionFallsBackForLargeCoordinates() throws IOException {
		var node = new BoxParser().parseExpression( "value" ).getRoot();

		node.setPosition( Position.compact( 70_000, 80_000, 90_000, 100_000, null ) );
		node.getPosition().getStart().setLine( -1 );

		assertThat( node.getPosition().getStart().getLine() ).isEqualTo( -1 );
		assertThat( node.getPosition().getStart().getColumn() ).isEqualTo( 80_000 );
		assertThat( node.getPosition().getEnd().getLine() ).isEqualTo( 90_000 );
		assertThat( node.getPosition().getEnd().getColumn() ).isEqualTo( 100_000 );
	}

	@Test
	void compactNodePositionPromotionPreservesSourceRange() throws IOException {
		var			node	= new BoxParser().parseExpression( "value" ).getRoot();
		SourceCode	source	= new SourceCode( "value" );
		node.setPosition( Position.compact( 1, 0, 1, 5, source, 0, 5 ) );

		node.getPosition().getStart().setLine( 65_536 );

		assertThat( node.getPosition().getStart().getLine() ).isEqualTo( 65_536 );
		assertThat( node.getPosition().getSource() ).isSameInstanceAs( source );
		assertThat( node.getPosition().getSourceText() ).isEqualTo( "value" );
	}

	@Test
	void promotedPositionViewDoesNotAliasAnotherNode() throws IOException {
		BoxNode		first	= new BoxParser().parseExpression( "first" ).getRoot();
		BoxNode		second	= new BoxParser().parseExpression( "second" ).getRoot();
		Position	view	= first.getPosition();
		view.getStart().setLine( 65_536 );

		second.setPosition( view );
		second.getPosition().getStart().setLine( 70_000 );

		assertThat( first.getPosition().getStart().getLine() ).isEqualTo( 65_536 );
		assertThat( second.getPosition().getStart().getLine() ).isEqualTo( 70_000 );
	}

	@Test
	void callerSuppliedPositionRetainsIdentityAndSharedMutation() throws IOException {
		var			node		= new BoxParser().parseExpression( "value" ).getRoot();
		Position	position	= new Position( 1, 2, 3, 4 );

		node.setPosition( position );
		position.getStart().setLine( 10 );
		node.getPosition().getEnd().setColumn( 20 );

		assertThat( node.getPosition() ).isSameInstanceAs( position );
		assertThat( node.getPosition().getStart().getLine() ).isEqualTo( 10 );
		assertThat( position.getEnd().getColumn() ).isEqualTo( 20 );
	}

	@Test
	void compactNodePositionSerializesWithoutRetainingNode() throws Exception {
		var						node	= new BoxParser().parseExpression( "value" ).getRoot();

		ByteArrayOutputStream	bytes	= new ByteArrayOutputStream();
		try ( ObjectOutputStream output = new ObjectOutputStream( bytes ) ) {
			output.writeObject( node.getPosition() );
		}

		Position restored;
		try ( ObjectInputStream input = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) {
			restored = ( Position ) input.readObject();
		}
		assertThat( restored.getStart().toMap() ).isEqualTo( node.getPosition().getStart().toMap() );
		assertThat( restored.getEnd().toMap() ).isEqualTo( node.getPosition().getEnd().toMap() );
		assertThat( restored.getSourceText() ).isEqualTo( node.getSourceText() );
	}

	@Test
	void compactNodePositionSnapshotIsIndependent() throws IOException {
		var			node		= new BoxParser().parseExpression( "value" ).getRoot();
		Position	snapshot	= node.getPosition().snapshot();

		node.getPosition().getStart().setLine( 10 );
		snapshot.getEnd().setLine( 20 );

		assertThat( snapshot.getStart().getLine() ).isEqualTo( 1 );
		assertThat( node.getPosition().getEnd().getLine() ).isEqualTo( 1 );
		assertThat( new Position( 1, 2, 3, 4 ).snapshot().getStart().toMap() ).isEqualTo( java.util.Map.of( "line", 1, "column", 2 ) );
	}

	@Test
	void compactNodePositionViewDoesNotDirectlyRetainNode() throws IOException {
		Position position = new BoxParser().parseExpression( "value" ).getRoot().getPosition();

		assertThat( Arrays.stream( position.getClass().getDeclaredFields() ).anyMatch( field -> field.getType() == WeakReference.class ) ).isTrue();
		assertThat( Arrays.stream( position.getClass().getDeclaredFields() ).noneMatch( field -> BoxNode.class.isAssignableFrom( field.getType() ) ) ).isTrue();
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
