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

import java.io.File;
import java.io.PrintStream;

import ortus.boxlang.compiler.parser.ParsingResult;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * This will load classes, but not compile them.
 */
public class NoOpBoxpiler extends Boxpiler {

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Private constructor
	 */
	public NoOpBoxpiler() {
		super();
	}

	@Override
	public Key getName() {
		return Key.noOp;
	}

	@Override
	public void printTranspiledCode( ParsingResult result, ClassInfo classInfo, PrintStream target ) {
		throw new BoxRuntimeException( "NoOpBoxpiler does not support printing transpiled code" );
	}

	@Override
	public void compileClassInfo( String classPoolName, String FQN ) {
		// logger.debug( "Java BoxPiler Compiling " + FQN );
		ClassInfo classInfo = getClassPool( classPoolName ).get( FQN );
		if ( classInfo == null ) {
			throw new BoxRuntimeException( "ClassInfo not found for " + FQN );
		}
		if ( classInfo.resolvedFilePath() != null ) {
			File sourceFile = classInfo.resolvedFilePath().absolutePath().toFile();
			// Check if the source file contains Java bytecode by reading the first few bytes
			if ( diskClassUtil.isJavaBytecode( sourceFile ) ) {
				classInfo.getClassLoader().defineClasses( FQN, sourceFile, classInfo );
				return;
			}
			throw new BoxRuntimeException( "NoOpBoxpiler does not support compiling source files." );
		} else if ( classInfo.source() != null ) {
			throw new BoxRuntimeException( "NoOpBoxpiler does not support compiling source files." );
		} else if ( classInfo.interfaceProxyDefinition() != null ) {
			throw new BoxRuntimeException( "NoOpBoxpiler does not support compiling source files." );
		} else {
			throw new BoxRuntimeException( "Unknown class info type: " + classInfo.toString() );
		}
	}

}
