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
package ortus.boxlang.transpiler.transformer.indexer;

import java.io.File;

import com.github.javaparser.ast.Node;

/**
 * Cross-reference abstract class
 */
public abstract class BoxLangCrossReferencer {

	protected File		source;
	protected File		destination;
	protected boolean	enabled;

	/**
	 * Store a reference into the index
	 *
	 * @param javaNode Java Parser AST Node
	 * @param boxNode  BoxLang AST Node
	 *
	 * @return
	 */
	public abstract Node storeReference( Node javaNode, ortus.boxlang.ast.BoxNode boxNode );

	public BoxLangCrossReferencer() {
		this.enabled = true;
	}

	public BoxLangCrossReferencer( File source, File destination ) {
		this.source			= source;
		this.destination	= destination;
		this.enabled		= true;
	}

	public File getSource() {
		return source;
	}

	public void setSource( File source ) {
		this.source = source;
	}

	public File getDestination() {
		return destination;
	}

	public void setDestination( File destination ) {
		this.destination = destination;
	}
}
