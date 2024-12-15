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
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;
import ortus.boxlang.runtime.scopes.Key;

/**
 * Class representing SQL table as a variable name
 */
public class SQLTableVariable extends SQLTable {

	private String	schema;

	private Key		name;

	/**
	 * Constructor
	 *
	 * @param position   position of the statement in the source code
	 * @param sourceText source code of the statement
	 */
	public SQLTableVariable( String schema, String name, String alias, int index, Position position, String sourceText ) {
		super( alias, index, position, sourceText );
		setSchema( schema );
		setName( name );
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
	public Key getName() {
		return name;
	}

	/**
	 * Set the table name
	 */
	public void setName( String name ) {
		this.name = Key.of( name );
	}
	
	public boolean isCalled( Key name ) {
		return this.name.equals( name ) || ( alias != null && alias.equals( name ) );
	}

	public String getVariableName() {
		if ( schema != null ) {
			return schema + "." + name.getName();
		} else {
			return name.getName();
		}
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

		if ( schema != null ) {
			map.put( "schema", schema );
		} else {
			map.put( "schema", null );
		}
		map.put( "name", name.getName() );
		if ( alias != null ) {
			map.put( "alias", alias.getName() );
		} else {
			map.put( "alias", null );
		}
		return map;
	}

}
