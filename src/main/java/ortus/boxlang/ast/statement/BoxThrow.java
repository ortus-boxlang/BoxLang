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
package ortus.boxlang.ast.statement;

import java.util.Map;

import ortus.boxlang.ast.BoxExpr;
import ortus.boxlang.ast.BoxStatement;
import ortus.boxlang.ast.Position;

/**
 * AST Node representing an throw statement
 */
public class BoxThrow extends BoxStatement {

	private final BoxExpr	expression;
	private final BoxExpr	type;
	private final BoxExpr	message;
	private final BoxExpr	detail;
	private final BoxExpr	errorcode;
	private final BoxExpr	extendedinfo;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxThrow( BoxExpr expression, Position position, String sourceText ) {
		this( expression, null, null, null, null, null, position, sourceText );
	}

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxThrow( BoxExpr expression, BoxExpr type, BoxExpr message, BoxExpr detail, BoxExpr errorcode, BoxExpr extendedInfo, Position position,
	    String sourceText ) {
		super( position, sourceText );
		this.expression = expression;
		if ( this.expression != null ) {
			this.expression.setParent( this );
		}
		this.type = type;
		if ( this.type != null ) {
			this.type.setParent( this );
		}
		this.message = message;
		if ( this.message != null ) {
			this.message.setParent( this );
		}
		this.detail = detail;
		if ( this.detail != null ) {
			this.detail.setParent( this );
		}
		this.errorcode = errorcode;
		if ( this.errorcode != null ) {
			this.errorcode.setParent( this );
		}
		this.extendedinfo = extendedInfo;
		if ( this.extendedinfo != null ) {
			this.extendedinfo.setParent( this );
		}
	}

	public BoxExpr getExpression() {
		return expression;
	}

	public BoxExpr getType() {
		return type;
	}

	public BoxExpr getMessage() {
		return message;
	}

	public BoxExpr getDetail() {
		return detail;
	}

	public BoxExpr getErrorCode() {
		return errorcode;
	}

	public BoxExpr getExtendedInfo() {
		return extendedinfo;
	}

	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		if ( expression != null ) {
			map.put( "expression", expression.toMap() );
		} else {
			map.put( "expression", null );
		}
		if ( type != null ) {
			map.put( "type", type.toMap() );
		} else {
			map.put( "type", null );
		}
		if ( message != null ) {
			map.put( "message", message.toMap() );
		} else {
			map.put( "message", null );
		}
		if ( detail != null ) {
			map.put( "detail", detail.toMap() );
		} else {
			map.put( "detail", null );
		}
		if ( errorcode != null ) {
			map.put( "errorcode", errorcode.toMap() );
		} else {
			map.put( "errorcode", null );
		}
		if ( extendedinfo != null ) {
			map.put( "extendedinfo", extendedinfo.toMap() );
		} else {
			map.put( "extendedinfo", null );
		}
		return map;
	}
}
