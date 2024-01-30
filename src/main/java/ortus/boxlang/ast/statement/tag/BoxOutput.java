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
package ortus.boxlang.ast.statement.tag;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxNode;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * Root node for a tag/templating (program) cfm/bxm
 */
public class BoxOutput extends BoxStatement {

	private final List<BoxStatement>	body;
	private final BoxExpr				query;
	private final BoxExpr				group;
	private final BoxExpr				groupCaseSensitive;
	private final BoxExpr				startRow;
	private final BoxExpr				maxRows;
	private final BoxExpr				encodeFor;

	/**
	 * Creates an AST for a program which is represented by a list of statements
	 *
	 * @param statements list of the statements nodes
	 * @param position   position within the source code
	 * @param sourceText source code
	 *
	 * @see Position
	 * @see BoxStatement
	 */
	public BoxOutput( List<BoxStatement> statements, BoxExpr query, BoxExpr group, BoxExpr groupCaseSensitive, BoxExpr startRow, BoxExpr maxRows,
	    BoxExpr encodeFor, Position position, String sourceText ) {
		super( position, sourceText );
		this.body = statements;
		this.body.forEach( statement -> statement.setParent( this ) );
		this.query = query;
		if ( query != null ) {
			this.query.setParent( this );
		}
		this.group = group;
		if ( group != null ) {
			this.group.setParent( this );
		}
		this.groupCaseSensitive = groupCaseSensitive;
		if ( groupCaseSensitive != null ) {
			this.groupCaseSensitive.setParent( this );
		}
		this.startRow = startRow;
		if ( startRow != null ) {
			this.startRow.setParent( this );
		}
		this.maxRows = maxRows;
		if ( maxRows != null ) {
			this.maxRows.setParent( this );
		}
		this.encodeFor = encodeFor;
		if ( encodeFor != null ) {
			this.encodeFor.setParent( this );
		}
	}

	public List<BoxStatement> getBody() {
		return body;
	}

	public BoxExpr getQuery() {
		return query;
	}

	public BoxExpr getGroup() {
		return group;
	}

	public BoxExpr getGroupCaseSensitive() {
		return groupCaseSensitive;
	}

	public BoxExpr getStartRow() {
		return startRow;
	}

	public BoxExpr getMaxRows() {
		return maxRows;
	}

	public BoxExpr getEncodeFor() {
		return encodeFor;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();

		map.put( "body", body.stream().map( BoxNode::toMap ).collect( Collectors.toList() ) );
		if ( query != null ) {
			map.put( "query", query.toMap() );
		}
		if ( group != null ) {
			map.put( "group", group.toMap() );
		}
		if ( groupCaseSensitive != null ) {
			map.put( "groupCaseSensitive", groupCaseSensitive.toMap() );
		}
		if ( startRow != null ) {
			map.put( "startRow", startRow.toMap() );
		}
		if ( maxRows != null ) {
			map.put( "maxRows", maxRows.toMap() );
		}
		if ( encodeFor != null ) {
			map.put( "encodeFor", encodeFor.toMap() );
		}

		return map;
	}

}
