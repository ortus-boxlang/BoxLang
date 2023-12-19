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
package ortus.boxlang.transpiler;

import java.util.List;

import com.github.javaparser.ast.CompilationUnit;

/**
 * Results of the Java transformation
 * Contains a Java AST for the entry point and a collection of
 * AST for each class such as UDF other callables
 */
public class TranspiledCode {

	private final CompilationUnit		entryPoint;
	private final List<CompilationUnit>	callables;

	/**
	 *
	 * @param script    the compilation unit containing the entry point
	 * @param callables list of
	 */
	public TranspiledCode( CompilationUnit script, List<CompilationUnit> callables ) {
		this.entryPoint	= script;
		this.callables	= callables;
	}

	public CompilationUnit getEntryPoint() {
		return entryPoint;
	}

	public List<CompilationUnit> getCallables() {
		return callables;
	}
}
