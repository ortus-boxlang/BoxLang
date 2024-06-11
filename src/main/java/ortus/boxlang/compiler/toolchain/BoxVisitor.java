package ortus.boxlang.compiler.toolchain;

import ortus.boxlang.compiler.ast.*;
import ortus.boxlang.compiler.ast.expression.*;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxDocumentationAnnotation;
import ortus.boxlang.compiler.ast.statement.BoxImport;
import ortus.boxlang.parser.antlr.BoxScriptGrammar;
import ortus.boxlang.parser.antlr.BoxScriptGrammarBaseVisitor;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.services.ComponentService;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is responsible for creating the AST from the ANTLR generated
 * parse tree.
 * <p>
 * A parser tree is a great jumping off point for creating an AST, but it is
 * not the AST itself as its
 * structure is dictated by the grammar and not the structure of the language
 * itself.
 * <p>
 * We create a standardized AST here, from whence we can then perform further
 * analysis and transformations and
 * eventually code generation, should that be the end goal.
 * <p>
 * Note that by the time this visitor is called, it should have been
 * thoroughly checked
 * as this visitor makes no checks on parameters (and it should not), and
 * raises no Issues.
 */
public class BoxVisitor extends BoxScriptGrammarBaseVisitor<BoxNode> {

	private boolean          inOutputBlock    = false;
	public  ComponentService componentService = BoxRuntime.getInstance()
														  .getComponentService();

	public void setInOutputBlock(boolean inOutputBlock) {
		this.inOutputBlock = inOutputBlock;
	}

	public boolean getInOutputBlock() {
		return inOutputBlock;
	}


	private final Tools                tools             = new Tools();
	private final BoxExpressionVisitor expressionVisitor = new BoxExpressionVisitor();

	/**
	 * Visit the class or interface context to generate the AST node for the
	 * top level node
	 *
	 * @param ctx the parse tree
	 * @return the AST node representing the class or interface
	 */
	@Override
	public BoxNode visitClassOrInterface(BoxScriptGrammar.ClassOrInterfaceContext ctx) {
		// NOTE: ANTLR renames rules that match Java keywords so interface_()
		// is used instead of interface
		return (ctx.boxClass() != null ? ctx.boxClass() : ctx.interface_()).accept(this);
	}

	/**
	 * Visit the boxClass context to generate the AST node for the class
	 *
	 * @param ctx the parse tree
	 * @return the AST node representing the class
	 */
	@Override
	public BoxNode visitBoxClass(BoxScriptGrammar.BoxClassContext ctx) {

		return null;
	}

	/**
	 * Visit the interface_ context to generate the AST node for the interface
	 *
	 * @param ctx the parse tree
	 * @return the AST node representing the interface
	 */
	@Override
	public BoxInterface visitInterface(BoxScriptGrammar.InterfaceContext ctx) {

		// Again, the BOX AST should really just accept expressions or
		// statements,
		// but for now we will cast and explore AST inheritance later (it may
		// be too late).

		List<BoxStatement>               body            = new ArrayList<>();
		List<BoxAnnotation>              preAnnotations  = new ArrayList<>();
		List<BoxAnnotation>              postAnnotations = new ArrayList<>();
		List<BoxDocumentationAnnotation> documentation   = new ArrayList<>();
		List<BoxImport>                  imports         = new ArrayList<>();

		ctx.importStatement()
		   .forEach(stmt -> {
			   imports.add((BoxImport) stmt.accept(this));
		   });

		ctx.preannotation()
		   .forEach(stmt -> {
			   preAnnotations.add((BoxAnnotation) stmt.accept(this));
		   });

		ctx.postannotation()
		   .forEach(annotation -> {
			   postAnnotations.add((BoxAnnotation) annotation.accept(this));
		   });

		ctx.abstractFunction()
		   .forEach(stmt -> {
			   body.add((BoxStatement) stmt.accept(this));
		   });

		// TODO: staticInitializer
		ctx.function()
		   .forEach(stmt -> {
			   body.add((BoxStatement) stmt.accept(this));
		   });

		return new BoxInterface(imports, body, preAnnotations, postAnnotations, documentation, tools.getPosition(ctx),
                                tools.getSourceText(ctx));

	}

	/**
	 * Visit the importStatement context to generate the AST node for the
	 * import statement
	 *
	 * @param ctx the parse tree
	 * @return the AST node representing the import statement
	 */
	@Override
	public BoxNode visitImportStatement(BoxScriptGrammar.ImportStatementContext ctx) {
		BoxExpression expr;
		BoxExpression alias  = null;
		String        prefix = "";
		Position      spos   = tools.getPosition(ctx.importFQN());

		if (ctx.PREFIX() != null) {
			prefix = ctx.PREFIX()
						.getText();
			spos = tools.getPosition(ctx.PREFIX());
		}
		expr = new BoxFQN(prefix + ctx.importFQN(), spos, prefix + tools.getSourceText(ctx.importFQN()));


		if (ctx.identifier() != null) {
			alias = ctx.identifier()
					   .accept(expressionVisitor);
		}

		// Note: The BOX AST should really just accept expressions and statements, but for
		// now we are just using BoxIdentifier as I don't want to change the AST unless there is a  good reason.
		// Whenever we find ourselves casting we should examine either the class inheritance model or the design.
		// BoxImport should not need to know that this is specifically a BoxIdentifier.
		return new BoxImport(expr, (BoxIdentifier) alias, tools.getPosition(ctx), tools.getSourceText(ctx));
	}

	/**
	 * Visit the expressions that are actually statements, and treat them as so
	 */
	@Override
	public BoxAssignment visitExprAssign(BoxScriptGrammar.ExprAssignContext ctx) {
		BoxExpression target = ctx.expression(0)
								  .accept(expressionVisitor);
		BoxExpression value = ctx.expression(1)
								 .accept(expressionVisitor);
		BoxAssignmentOperator operator = null;
		switch (ctx.op.getType()) {
			case BoxScriptGrammar.EQUALSIGN -> operator = BoxAssignmentOperator.Equal;
			case BoxScriptGrammar.PLUSEQUAL -> operator = BoxAssignmentOperator.PlusEqual;
			case BoxScriptGrammar.MINUSEQUAL -> operator = BoxAssignmentOperator.MinusEqual;
			case BoxScriptGrammar.STAREQUAL -> operator = BoxAssignmentOperator.StarEqual;
			case BoxScriptGrammar.SLASHEQUAL -> operator = BoxAssignmentOperator.SlashEqual;
			case BoxScriptGrammar.MODEQUAL -> operator = BoxAssignmentOperator.ModEqual;
			case BoxScriptGrammar.CONCATEQUAL -> operator = BoxAssignmentOperator.ConcatEqual;
		}
		// Note that modifiers are not seen in the expression version of assign

		return new BoxAssignment(target, operator, value, null, tools.getPosition(ctx), tools.getSourceText(ctx));
	}

	/**
	 * Visit variable declarations with or without assignments
	 */
	@Override
	public BoxNode visitVarDecl(BoxScriptGrammar.VarDeclContext ctx) {
		// The variable declaration here comes from the expression context. Note that
		// we expect the parse tree to have been verified by this point, so we can
		// safely assume we are seeing a valid assignment or declaration.

		var expr = (BoxAssignment) ctx.expression()
									  .accept(this);

		var modifiers = new ArrayList<BoxAssignmentModifier>();
		// Note that if more than one modifier is allowed, this will automatically
		// use it.
		ctx.varModifier()
		   .forEach(modifier -> {
			   modifiers.add(buildAssignmentModifier(modifier));
		   });

		expr.setModifiers(modifiers);
		return expr;
	}

	public BoxAssignmentModifier buildAssignmentModifier(BoxScriptGrammar.VarModifierContext ctx) {
		BoxAssignmentModifier modifier = null;
		// No error checks, we expect the parse tree to have been verified by this point
		// As we expect the modifiers to be expanded, we use a switch here
		switch (ctx.op.getType()) {
			case BoxScriptGrammar.VAR -> modifier = BoxAssignmentModifier.VAR;
		}
		return modifier;
	}

}
