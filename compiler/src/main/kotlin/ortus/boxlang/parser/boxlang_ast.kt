package ortus.boxlang.parser

import com.strumenta.kolasu.model.Node

data class CFScript(
    val body : List<Statement>
) : Node()

open class Statement(
    val dummy : String = ""
) : Node()

data class Function(
    val identifier: String,
    val returnType: String,
    val parameters: List<String> = emptyList(),
    val body: List<Statement> = emptyList()
): Node()

data class Component(
    val identifier: String,
    val functions: List<Function>
) : Statement()
