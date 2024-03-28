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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.stmt.BlockStmt;

import ortus.boxlang.compiler.parser.Parser;
import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.compiler.transpiler.JavaTranspiler;

public class TestBase {

	protected String	testboxDirectory	= System.getProperty( "testboxdir" );
	protected String	contentboxDirectory	= System.getProperty( "contentboxdir" );
	protected String	coldboxDirectory	= System.getProperty( "coldboxdir" );

	protected List<Path> scanForFiles( String path, Set<String> extensions ) {
		List<Path> fileList = new ArrayList<Path>();

		try {
			Files.walkFileTree( Paths.get( path ), new HashSet<>(), Integer.MAX_VALUE, new FileVisitor<>() {

				private boolean match( String fileName ) {
					int index = fileName.lastIndexOf( '.' );
					if ( index > 0 ) {
						String ext = fileName.substring( index + 1 );
						return extensions.contains( ext );
					}
					return false;
				}

				@Override
				public FileVisitResult preVisitDirectory( Path dir, BasicFileAttributes attrs ) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile( Path file, BasicFileAttributes attrs ) throws IOException {
					if ( match( file.getFileName().toString() ) )
						fileList.add( file );
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed( Path file, IOException exc ) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory( Path dir, IOException exc ) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			} );
		} catch ( Exception e ) {

		}
		return fileList;
	}

	protected void assertEqualsNoWhiteSpaces( String expected, String actual ) {
		assertEquals( expected.replaceAll( "[ \\t\\r\\n]", "" ), actual.replaceAll( "[ \\t\\r\\n]", "" ) );
	}

	protected Node extractFromBlockStmt( Node stmt ) {
		if ( stmt instanceof BlockStmt s ) {
			return s.getStatements().get( 0 );
		}
		return stmt;

	}

	public ParsingResult parseExpression( String statement ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( statement );
		assertTrue( result.isCorrect() );
		return result;
	}

	public String transformExpression( String statement ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseExpression( statement );
		assertTrue( result.isCorrect() );

		return new JavaTranspiler().transform( result.getRoot() ).toString();
	}

	public ParsingResult parseStatement( String statement ) throws IOException {
		Parser			parser	= new Parser();
		ParsingResult	result	= parser.parseStatement( statement );
		assertTrue( result.isCorrect() );
		return result;
	}
}
