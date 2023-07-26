package ortus.boxlang.parser

import ortus.boxlang.parser.CFScript

// TODO: partially implemented
fun CFParser.ScriptContext.toAst() : CFScript {
    val statements = mutableListOf<Statement>()

    if (this.component() != null)
        statements += this.component().toAst()

    return CFScript(statements)
}

// TODO: partially implemented
fun CFParser.ComponentContext.toAst() = Component(
    identifier = this.identifier()?.text ?: "",
    functions = this.functionOrStatement()
        .filter { it.function() != null }
        .map { it.function().toAst() }
)

// TODO: partially implemented
fun CFParser.FunctionContext.toAst() = Function(
    identifier = this.functionSignature().identifier().text,
    returnType = this.functionSignature().returnType()?.text ?: ""
)