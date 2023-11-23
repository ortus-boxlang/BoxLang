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
package ortus.boxlang.ast.statement;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing a if statement
 */
public class BoxTry extends BoxStatement {

	private final List<BoxStatement>	tryBody;
	private final List<BoxTryCatch>		catches;
	private final List<BoxStatement>	finallyBody;

	/**
	 *
	 * @param tryBody
	 * @param catches
	 * @param finallyBody
	 * @param position    position of the statement in the source code
	 * @param sourceText  source code that originated the Node
	 */
	public BoxTry( List<BoxStatement> tryBody, List<BoxTryCatch> catches, List<BoxStatement> finallyBody, Position position, String sourceText ) {
		super( position, sourceText );
		this.tryBody		= Collections.unmodifiableList( tryBody );
		this.catches		= Collections.unmodifiableList( catches );
		this.finallyBody	= Collections.unmodifiableList( finallyBody );
		this.tryBody.forEach( arg -> arg.setParent( this ) );
		this.catches.forEach( arg -> arg.setParent( this ) );
		this.finallyBody.forEach( arg -> arg.setParent( this ) );
	}

	public List<BoxStatement> getTryBody() {
		return tryBody;
	}

	public List<BoxTryCatch> getCatches() {
		return catches;
	}

	public List<BoxStatement> getFinallyBody() {
		return finallyBody;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "tryBody", tryBody.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		map.put( "catches", catches.stream().map( BoxTryCatch::toMap ).collect( Collectors.toList() ) );
		map.put( "finallyBody", finallyBody.stream().map( BoxStatement::toMap ).collect( Collectors.toList() ) );
		return map;
	}
}
