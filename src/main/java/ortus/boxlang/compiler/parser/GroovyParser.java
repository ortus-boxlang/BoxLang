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
package ortus.boxlang.compiler.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.SourceCode;
import ortus.boxlang.compiler.ast.SourceFile;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.compiler.toolchain.GroovyExpressionVisitor;
import ortus.boxlang.compiler.toolchain.GroovyVisitor;
import ortus.boxlang.parser.antlr.GroovyGrammar;
import ortus.boxlang.parser.antlr.GroovyGrammar.CompilationUnitContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.ImportStatementContext;
import ortus.boxlang.parser.antlr.GroovyGrammar.TopLevelDeclarationContext;
import ortus.boxlang.parser.antlr.GroovyLexer;

/**
 * Parses Groovy source into the shared BoxLang AST, following the same overall shape as
 * {@code CFParser}: an ANTLR lexer/parser pair produces a parse tree, which a visitor walks
 * into {@code ortus.boxlang.compiler.ast} nodes.
 * <p>
 * Phase 2 of the Groovy parser/transpiler effort. A source is treated as a "class file" (its AST
 * root is a {@code BoxClass}, the shape {@code RunnableLoader#loadClass} expects) only when it
 * contains exactly one top-level {@code class}/{@code interface}/{@code trait} declaration and
 * nothing else besides package/import statements. Otherwise it is treated as a script - and a
 * class declaration mixed in among other top-level statements (which real Groovy allows) becomes
 * a {@code BoxLocalClass} peer statement in that script, the same mechanism a named nested class
 * or a hoisted anonymous class already use (see GroovyVisitor#visitClassDeclaration).
 */
public class GroovyParser extends AbstractParser {

	private final GroovyVisitor statementVisitor = new GroovyVisitor( this );

	public GroovyParser() {
		super();
	}

	public GroovyParser( int startLine, int startColumn ) {
		super( startLine, startColumn );
	}

	@Override
	public ParsingResult parse( File file, boolean isScript ) throws IOException {
		this.file = file;
		setSource( new SourceFile( file ) );
		BOMInputStream	inputStream	= getInputStream( file );
		BoxNode			ast			= parserFirstStage( inputStream, false, isScript );
		return new ParsingResult( ast, issues, comments );
	}

	@Override
	public ParsingResult parse( String code, boolean classOrInterface, boolean isScript ) throws IOException {
		this.sourceCode = code;
		setSource( new SourceCode( code ) );
		InputStream	inputStream	= IOUtils.toInputStream( code, StandardCharsets.UTF_8 );
		BoxNode		ast			= parserFirstStage( inputStream, classOrInterface, isScript );
		return new ParsingResult( ast, issues, comments );
	}

	@Override
	protected BoxNode parserFirstStage( InputStream stream, boolean classOrInterface, boolean isScript ) throws IOException {
		GroovyLexer		lexer	= new GroovyLexer( CharStreams.fromStream( stream, StandardCharsets.UTF_8 ) );
		GroovyGrammar	parser	= new GroovyGrammar( new CommonTokenStream( lexer ) );

		addErrorListeners( lexer, parser );

		CompilationUnitContext parseTree;
		try {
			parseTree = parser.compilationUnit();
		} catch ( Exception e ) {
			errorListener.semanticError( e.getClass().getName() + " " + e.getMessage(), createOffsetPosition( 1, 0, 1, 0 ) );
			return null;
		}

		if ( !issues.isEmpty() ) {
			return null;
		}

		BoxNode rootNode;
		try {
			rootNode = toAst( parseTree );
		} catch ( Exception e ) {
			if ( issues.isEmpty() ) {
				throw e;
			}
			return null;
		}

		if ( isSubParser() ) {
			return rootNode;
		}
		return rootNode;
	}

	/**
	 * Decides whether the parsed compilation unit is a single class/interface/trait file or a
	 * script, and builds the corresponding {@code BoxClass}/{@code BoxScript} root node.
	 */
	private BoxNode toAst( CompilationUnitContext ctx ) {
		var								pos					= getPosition( ctx );
		var								src					= getSourceText( ctx );

		// "import static" is partitioned out from ordinary imports entirely - "import static
		// java.lang.Math.PI" doesn't name an importable class at all ("PI" is a field, not a
		// class), so unlike a regular import, it's never added to the emitted BoxImport list.
		// Its only effect is populating knownStaticClassNames (with the OWNING class, "Math" -
		// not "PI") and, for a specific (non-wildcard) member, staticImportedMembers so bare
		// references to that member can be rewritten to a qualified static access/invocation -
		// see GroovyExpressionVisitor#visitIdentifierExpr/#buildCallExpression.
		List<ImportStatementContext>	staticImportCtxs	= ctx.importStatement().stream().filter( i -> i.STATIC() != null ).toList();
		List<ImportStatementContext>	regularImportCtxs	= ctx.importStatement().stream().filter( i -> i.STATIC() == null ).toList();

		List<BoxImport>					userImports			= buildImports( regularImportCtxs );
		List<BoxImport>					imports				= new ArrayList<>( defaultImports( pos ) );
		imports.addAll( userImports );

		// Bare, unqualified references to common java.lang/java.util/java.math classes (e.g.
		// "Math.max(...)", "new BigDecimal(...)") need to be recognized as static class access
		// rather than an ordinary instance dot-access, so the GroovyExpressionVisitor is told
		// which simple names to treat that way: the classes Groovy always auto-imports, plus
		// whatever this file explicitly imports by name (wildcard imports aside - those would
		// require enumerating a package's members, which isn't attempted here).
		Set<String> knownStaticClassNames = new java.util.HashSet<>( GroovyExpressionVisitor.DEFAULT_STATIC_CLASS_NAMES );
		knownStaticClassNames.addAll( userImports.stream()
		    .filter( i -> !i.getExpression().getSourceText().endsWith( ".*" ) )
		    .map( i -> {
			    String fqn = i.getExpression().getSourceText();
			    return fqn.substring( fqn.lastIndexOf( '.' ) + 1 );
		    } )
		    .collect( Collectors.toSet() ) );

		java.util.Map<String, String> staticImportedMembers = new java.util.HashMap<>();
		for ( ImportStatementContext staticImportCtx : staticImportCtxs ) {
			String qualifiedName = staticImportCtx.qualifiedName().getText();
			if ( staticImportCtx.STAR() != null ) {
				// "import static java.lang.Math.*" - only the owning class is recognizable
				// ("Math.max(...)" still resolves); enumerating every static member of an
				// arbitrary class to support fully bare references isn't attempted, matching the
				// same documented limitation regular wildcard imports already have.
				knownStaticClassNames.add( qualifiedName.substring( qualifiedName.lastIndexOf( '.' ) + 1 ) );
				continue;
			}
			String	owner		= qualifiedName.substring( 0, qualifiedName.lastIndexOf( '.' ) );
			String	ownerSimple	= owner.substring( owner.lastIndexOf( '.' ) + 1 );
			String	member		= qualifiedName.substring( qualifiedName.lastIndexOf( '.' ) + 1 );
			String	boundName	= staticImportCtx.AS() != null ? staticImportCtx.IDENTIFIER().getText() : member;
			knownStaticClassNames.add( ownerSimple );
			staticImportedMembers.put( boundName, ownerSimple );
		}

		statementVisitor.getExpressionVisitor().setKnownStaticClassNames( knownStaticClassNames );
		statementVisitor.getExpressionVisitor().setStaticImportedMembers( staticImportedMembers );

		List<TopLevelDeclarationContext>	declarations	= ctx.topLevelDeclarations() == null
		    ? new ArrayList<>()
		    : ctx.topLevelDeclarations().topLevelDeclaration();

		long								classCount		= declarations.stream().filter( d -> d.classDeclaration() != null ).count();

		// A file that is ENTIRELY a single class declaration (no other top-level statements) is
		// compiled as a true class-file (BoxClass), the same "the whole file's AST root IS the
		// class" mode RunnableLoader#loadClass expects - distinct from a script that merely
		// DEFINES a class among other statements (handled below via a BoxLocalClass, the same
		// mechanism a named nested class or a hoisted anonymous class already use).
		if ( classCount == 1 && declarations.size() == 1 ) {
			return statementVisitor.buildClass( declarations.get( 0 ).classDeclaration(), imports );
		}

		// Push the script's own top-level hoist-scope frame before building any statement, so an
		// anonymous class discovered anywhere below (even nested inside a top-level function's own
		// body) is attributed to the script itself, isolated from any class-shaped body built
		// within it - see GroovyExpressionVisitor#pushHoistScope for the full reasoning.
		statementVisitor.getExpressionVisitor().pushHoistScope();
		List<BoxStatement> statements = new ArrayList<>( imports );
		for ( TopLevelDeclarationContext decl : declarations ) {
			if ( decl.methodDeclaration() != null ) {
				statements.add( ( BoxStatement ) decl.methodDeclaration().accept( statementVisitor ) );
			} else if ( decl.enumDeclaration() != null ) {
				statements.add( ( BoxStatement ) decl.enumDeclaration().accept( statementVisitor ) );
			} else if ( decl.classDeclaration() != null ) {
				// A class declaration mixed in with other top-level script statements - built as a
				// BoxLocalClass (the exact same AST shape a nested classDeclaration inside another
				// class's body already produces via GroovyVisitor#visitClassDeclaration), landing
				// as a peer statement in the script's own statement list. AsmTranspiler's existing
				// preCompileLocalClasses already scans a BoxScript's statements for BoxLocalClass
				// entries and compiles each as an auxiliary class, so no new compiler machinery is
				// needed for this - only lifting the restriction that used to reject this shape.
				statements.add( ( BoxStatement ) decl.classDeclaration().accept( statementVisitor ) );
			} else {
				statements.add( ( BoxStatement ) decl.statement().accept( statementVisitor ) );
			}
		}
		// Any anonymous inner class discovered while building the above (even nested inside a
		// top-level function's own body) is hoisted here, at the script's own top level - see
		// GroovyExpressionVisitor#visitNewInstanceExpr for why it can never stay where it's written.
		statements.addAll( statementVisitor.getExpressionVisitor().popHoistScope() );
		return new ortus.boxlang.compiler.ast.BoxScript( statements, pos, src, BoxSourceType.GROOVYSCRIPT );
	}

	// Groovy implicitly imports these packages/classes into every file, with no "import"
	// statement required - unlike BoxLang/CFML, where every Java class reference must be either
	// fully qualified or explicitly imported. Only the plain-Java subset is mirrored here
	// (groovy.lang.*/groovy.util.* are out of scope - see the class header).
	private static final List<String> DEFAULT_IMPORT_SPECS = List.of(
	    "java.lang.*",
	    "java.util.*",
	    "java.io.*",
	    "java.math.BigInteger",
	    "java.math.BigDecimal" );

	private List<BoxImport> defaultImports( Position pos ) {
		List<BoxImport> result = new ArrayList<>();
		for ( String spec : DEFAULT_IMPORT_SPECS ) {
			result.add( new BoxImport( new BoxFQN( spec, pos, spec ), null, pos, spec ) );
		}
		return result;
	}

	private List<BoxImport> buildImports( List<ImportStatementContext> importContexts ) {
		List<BoxImport> imports = new ArrayList<>();
		for ( ImportStatementContext importCtx : importContexts ) {
			var		pos			= getPosition( importCtx );
			var		src			= getSourceText( importCtx );
			String	fqnText		= importCtx.qualifiedName().getText() + ( importCtx.STAR() != null ? ".*" : "" );
			var		expression	= new BoxFQN( fqnText, pos, src );
			var		alias		= importCtx.AS() != null
			    ? new BoxIdentifier( importCtx.IDENTIFIER().getText(), getPosition( importCtx.IDENTIFIER().getSymbol() ), importCtx.IDENTIFIER().getText() )
			    : null;
			imports.add( new BoxImport( expression, alias, pos, src ) );
		}
		return imports;
	}

}
