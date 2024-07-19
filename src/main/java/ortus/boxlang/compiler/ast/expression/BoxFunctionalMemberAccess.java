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
package ortus.boxlang.compiler.ast.expression;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * I represent functional access to an instance member method
 * myArray.map( .UCase )
 */
public class BoxFunctionalMemberAccess extends BoxExpression {

	private String				name;
	private List<BoxArgument>	arguments;

	/**
	 * Constructor
	 *
	 * @param name       name of the member method
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxFunctionalMemberAccess( String name, List<BoxArgument> arguments, Position position, String sourceText ) {
		super( position, sourceText );
		setName( name );
		setArguments( arguments );
	}

	public String getName() {
		return name;
	}

	public List<BoxArgument> getArguments() {
		return arguments;
	}

	public void setName( String name ) {
		this.name = name;
	}

	public void setArguments( List<BoxArgument> arguments ) {
		replaceChildren( this.arguments, arguments );
		this.arguments = arguments;
		if ( this.arguments != null ) {
			this.arguments.forEach( arg -> arg.setParent( this ) );
		}
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "name", name );
		if ( arguments != null ) {
			map.put( "arguments", arguments.stream().map( BoxExpression::toMap ).collect( Collectors.toList() ) );
		} else {
			map.put( "arguments", null );
		}
		return map;
	}

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}

}
