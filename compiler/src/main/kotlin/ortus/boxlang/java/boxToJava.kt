package ortus.boxlang.java

import com.github.javaparser.ast.ImportDeclaration
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.PackageDeclaration
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.SimpleName
import com.github.javaparser.ast.modules.ModuleDeclaration
import com.strumenta.kolasu.model.processNodesOfType
import ortus.boxlang.parser.CFScript
import ortus.boxlang.parser.Component

fun CFScript.toJava(): com.github.javaparser.ast.CompilationUnit {
	val packageDeclaration = PackageDeclaration()

	val imports = NodeList<ImportDeclaration>()

	val components = NodeList<TypeDeclaration<*>>()
	this.processNodesOfType(
		Component::class.java,
		{ component -> components.add(component.toJava()) }
	)

	val module = ModuleDeclaration()
	return com.github.javaparser.ast.CompilationUnit(null, imports, components, null)
}

fun Component.toJava(): ClassOrInterfaceDeclaration {
	val classDeclaration = ClassOrInterfaceDeclaration()
	if (!this.identifier.isNullOrBlank())
		classDeclaration.name = SimpleName(this.identifier)
	this.functions.forEach {
		classDeclaration.addMethod(it.name)
	}
	return classDeclaration
}

//fun Function.toJava(): MethodDeclaration {
//	val methodDeclaration = MethodDeclaration()
//	methodDeclaration.name = SimpleName(this.identifier)
//	return methodDeclaration
//}
