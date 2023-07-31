package ortus.boxlang.parser

private fun findAttribute(name: String, htmlElementContext: CFMLParser.HtmlElementContext): String {
	val attribute = htmlElementContext.htmlAttribute()
		.filter { it.htmlAttributeName().text.equals(name, true) }
		.firstOrNull()
	attribute?.apply {
		return this.htmlAttributeValue()
			.text.replace("\"", "")
	}
	return ""
}

private fun getAttributeValue(attribute: String) =
	attribute.substring(1, attribute.length - 1)

fun CFMLParser.HtmlDocumentContext.toAst(): CFScript {
	val statements = mutableListOf<Statement>()
	this.htmlElements().forEach {
		when {
			it is CFMLParser.HtmlElementsContext -> {
				if (it.htmlElement().htmlTagName(0).text.equals("cfcomponent", true)) {
					val name = findAttribute("name", it.htmlElement())
					// TODO: assume one is present
					statements += it.htmlElement().htmlTagName(0).toAst(name, this.htmlElements())
				}

			}
			// TODO: cfmlElements
		}
	}
	this.cfmlElement().forEach {
		when {
			it.cfcomponent() != null -> {
				statements += it.cfcomponent().toAst()
			}
		}
	}
	return CFScript(statements)
}

fun CFMLParser.CfcomponentContext.toAst() = Component(
	identifier = this.htmlAttribute()
		.firstOrNull { it.htmlAttributeName().text.lowercase() == "name" }
		?.htmlAttributeValue()?.text?.let { getAttributeValue(it) } ?: "",
	functions = this.cfmlElement()
		.filter { it.cffunction() != null }
		.map { it.cffunction().toAst() }
)

fun CFMLParser.CffunctionContext.toAst() = Function(
	identifier = this.htmlAttribute()
		.firstOrNull { it.htmlAttributeName().text.lowercase() == "name" }
		?.htmlAttributeValue()?.text?.let { getAttributeValue(it) } ?: "",
	returnType = this.htmlAttribute()
		.firstOrNull { it.htmlAttributeName().text.lowercase() == "returntype" }
		?.htmlAttributeValue()?.text?.let { getAttributeValue(it) } ?: ""
)

private fun CFMLParser.HtmlTagNameContext.toAst(identifier: String, htmlElements: MutableList<CFMLParser.HtmlElementsContext>): Statement {
	val functions = mutableListOf<Function>()
	htmlElements.forEach {
		when {
			it is CFMLParser.HtmlElementsContext -> {
				if (it.htmlElement().htmlTagName(0).text.equals("cffunction", true)) {
					val name = findAttribute("name", it.htmlElement())
					val returnType = findAttribute("returntype", it.htmlElement())
					functions += Function(name, returnType, emptyList())
				}

			}
		}
	}
	return Component(
		identifier,
		functions
	)
}

