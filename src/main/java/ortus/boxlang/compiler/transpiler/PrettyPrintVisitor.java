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
package ortus.boxlang.compiler.transpiler;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import com.github.javaparser.ast.ArrayCreationLevel;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.AnnotationMemberDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.CompactConstructorDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumConstantDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.InitializerDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.ReceiverParameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.comments.BlockComment;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.comments.LineComment;
import com.github.javaparser.ast.expr.ArrayAccessExpr;
import com.github.javaparser.ast.expr.ArrayCreationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.CastExpr;
import com.github.javaparser.ast.expr.CharLiteralExpr;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ConditionalExpr;
import com.github.javaparser.ast.expr.DoubleLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.InstanceOfExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LambdaExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.MemberValuePair;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.NullLiteralExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.PatternExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.SuperExpr;
import com.github.javaparser.ast.expr.SwitchExpr;
import com.github.javaparser.ast.expr.TextBlockLiteralExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.expr.VariableDeclarationExpr;
import com.github.javaparser.ast.modules.ModuleDeclaration;
import com.github.javaparser.ast.modules.ModuleExportsDirective;
import com.github.javaparser.ast.modules.ModuleOpensDirective;
import com.github.javaparser.ast.modules.ModuleProvidesDirective;
import com.github.javaparser.ast.modules.ModuleRequiresDirective;
import com.github.javaparser.ast.modules.ModuleUsesDirective;
import com.github.javaparser.ast.stmt.AssertStmt;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.BreakStmt;
import com.github.javaparser.ast.stmt.CatchClause;
import com.github.javaparser.ast.stmt.ContinueStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.EmptyStmt;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.LabeledStmt;
import com.github.javaparser.ast.stmt.LocalClassDeclarationStmt;
import com.github.javaparser.ast.stmt.LocalRecordDeclarationStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.stmt.SwitchEntry;
import com.github.javaparser.ast.stmt.SwitchStmt;
import com.github.javaparser.ast.stmt.SynchronizedStmt;
import com.github.javaparser.ast.stmt.ThrowStmt;
import com.github.javaparser.ast.stmt.TryStmt;
import com.github.javaparser.ast.stmt.UnparsableStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.stmt.YieldStmt;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.IntersectionType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.TypeParameter;
import com.github.javaparser.ast.type.UnionType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VarType;
import com.github.javaparser.ast.type.VoidType;
import com.github.javaparser.ast.type.WildcardType;
import com.github.javaparser.printer.DefaultPrettyPrinterVisitor;
import com.github.javaparser.printer.SourcePrinter;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;

public class PrettyPrintVisitor extends DefaultPrettyPrinterVisitor {

	private List<Object[]>			lineNumbers	= new ArrayList<>();

	/**
	 * Track the class we are inside of
	 */
	protected ArrayDeque<String>	insideClass	= new ArrayDeque<>();

	public PrettyPrintVisitor() {
		super( new DefaultPrinterConfiguration() );
	}

	public PrettyPrintVisitor( PrinterConfiguration configuration ) {
		super( configuration );
	}

	public PrettyPrintVisitor( PrinterConfiguration configuration, SourcePrinter printer ) {
		super( configuration, printer );
	}

	public List<Object[]> getLineNumbers() {
		return lineNumbers;
	}

	private void processNode( Runnable lambda, Node n ) {
		if ( n instanceof ClassOrInterfaceDeclaration ) {
			insideClass.push(
			    ( ( ClassOrInterfaceDeclaration ) n ).getFullyQualifiedName().orElseGet( () -> ( ( ClassOrInterfaceDeclaration ) n ).getNameAsString() ) );
		}
		int start = printer.getCursor().line;
		lambda.run();
		String currentClass = "unknown";
		if ( !insideClass.isEmpty() ) {
			currentClass = insideClass.peek();
		}
		lineNumbers.add( new Object[] { n, start, printer.getCursor().line, currentClass } );

		if ( n instanceof ClassOrInterfaceDeclaration ) {
			insideClass.pop();
		}
	}

	@Override
	public void visit( final CompilationUnit n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final PackageDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final NameExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final Name n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( SimpleName n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ClassOrInterfaceDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( RecordDeclaration n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final JavadocComment n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ClassOrInterfaceType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final TypeParameter n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final PrimitiveType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ArrayType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ArrayCreationLevel n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final IntersectionType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final UnionType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final WildcardType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final UnknownType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final FieldDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final VariableDeclarator n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ArrayInitializerExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final VoidType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final VarType n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( Modifier n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ArrayAccessExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ArrayCreationExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final AssignExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	/**
	 * work in progress for issue-545
	 */
	@Override
	public void visit( final BinaryExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final CastExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ClassExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ConditionalExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final EnclosedExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final FieldAccessExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final InstanceOfExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final PatternExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final CharLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final DoubleLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final IntegerLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final LongLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final StringLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final TextBlockLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final BooleanLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final NullLiteralExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ThisExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final SuperExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final MethodCallExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ObjectCreationExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final UnaryExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ConstructorDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final CompactConstructorDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final MethodDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final Parameter n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ReceiverParameter n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ExplicitConstructorInvocationStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final VariableDeclarationExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final LocalClassDeclarationStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final LocalRecordDeclarationStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final AssertStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final BlockStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final LabeledStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final EmptyStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ExpressionStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final SwitchStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( SwitchExpr n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final SwitchEntry n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final BreakStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final YieldStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ReturnStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final EnumDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final EnumConstantDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final InitializerDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final IfStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final WhileStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ContinueStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final DoStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ForEachStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ForStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final ThrowStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final SynchronizedStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final TryStmt n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final CatchClause n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final AnnotationDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final AnnotationMemberDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final MarkerAnnotationExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final SingleMemberAnnotationExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final NormalAnnotationExpr n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final MemberValuePair n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final LineComment n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( final BlockComment n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( LambdaExpr n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( MethodReferenceExpr n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( TypeExpr n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( NodeList n, Void arg ) {
		for ( Object node : n ) {
			processNode( () -> ( ( Node ) node ).accept( this, arg ), ( Node ) node );
		}
	}

	@Override
	public void visit( final ImportDeclaration n, final Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( ModuleDeclaration n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( ModuleRequiresDirective n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( ModuleExportsDirective n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( ModuleProvidesDirective n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( ModuleUsesDirective n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( ModuleOpensDirective n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

	@Override
	public void visit( UnparsableStmt n, Void arg ) {
		processNode( () -> super.visit( n, arg ), n );
	}

}