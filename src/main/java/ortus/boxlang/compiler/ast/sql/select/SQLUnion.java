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
package ortus.boxlang.compiler.ast.sql.select;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Abstract Node class representing SQL UNION statement
 */
public class SQLUnion extends SQLNode {

	private SQLSelect		select;

	private SQLUnionType	type;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLUnion( SQLSelect select, SQLUnionType type, Position position, String sourceText ) {
		super( position, sourceText );
		setSelect( select );
		setType( type );
	}

	/**
	 * Get the SELECT statement
	 */
	public SQLSelect getSelect() {
		return select;
	}

	/**
	 * Set the SELECT statement
	 */
	public void setSelect( SQLSelect select ) {
		replaceChildren( this.select, select );
		this.select = select;
		select.setParent( this );
	}

	/**
	 * Get the type of the UNION
	 */
	public SQLUnionType getType() {
		return type;
	}

	/**
	 * Set the type of the UNION
	 */
	public void setType( SQLUnionType type ) {
		this.type = type;
	}

	@Override
	public void accept( VoidBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public BoxNode accept( ReplacingBoxVisitor v ) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException( "Unimplemented method 'accept'" );
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "select", select.toMap() );
		map.put( "type", enumToMap( type ) );

		return map;
	}

}
