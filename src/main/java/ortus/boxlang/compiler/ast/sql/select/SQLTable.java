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

import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.sql.SQLNode;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * Abstract Node class representing SQL table declaration
 */
public class SQLTable extends SQLNode {

	private String	schema;

	private String	name;

	private String	alias;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	protected SQLTable( String schema, String name, String alias, Position position, String sourceText ) {
		super( position, sourceText );
		setSchema( schema );
		setName( name );
		setAlias( alias );
	}

	/**
	 * Get the schema name
	 */
	public String getSchema() {
		return schema;
	}

	/**
	 * Set the schema name
	 */
	public void setSchema( String schema ) {
		this.schema = schema;
	}

	/**
	 * Get the table name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the table name
	 */
	public void setName( String name ) {
		this.name = name;
	}

	/**
	 * Get the table alias
	 */
	public String getAlias() {
		return alias;
	}

	/**
	 * Set the table alias
	 */
	public void setAlias( String alias ) {
		this.alias = alias;
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
}
