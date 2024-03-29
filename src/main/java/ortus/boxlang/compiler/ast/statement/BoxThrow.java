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
package ortus.boxlang.compiler.ast.statement;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxStatement;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * AST Node representing an throw statement
 */
public class BoxThrow extends BoxStatement {

	private BoxExpression	expression;
	private BoxExpression	type;
	private BoxExpression	message;
	private BoxExpression	detail;
	private BoxExpression	errorcode;
	private BoxExpression	extendedinfo;

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxThrow( BoxExpression expression, Position position, String sourceText ) {
		this( expression, null, null, null, null, null, position, sourceText );
	}

	/**
	 * Creates the AST node
	 *
	 * @param expression argument expression to assert
	 * @param position   position of the statement in the source code
	 * @param sourceText source code that originated the Node
	 */
	public BoxThrow( BoxExpression expression, BoxExpression type, BoxExpression message, BoxExpression detail, BoxExpression errorcode,
	    BoxExpression extendedInfo, Position position,
	    String sourceText ) {
		super( position, sourceText );
		setExpression( expression );
		setType( type );
		setMessage( message );
		setDetail( detail );
		setErrorCode( errorcode );
		setExtendedInfo( extendedInfo );
	}

	public BoxExpression getExpression() {
		return expression;
	}

	public BoxExpression getType() {
		return type;
	}

	public BoxExpression getMessage() {
		return message;
	}

	public BoxExpression getDetail() {
		return detail;
	}

	public BoxExpression getErrorCode() {
		return errorcode;
	}

	public BoxExpression getExtendedInfo() {
		return extendedinfo;
	}

	void setExpression( BoxExpression expression ) {
		replaceChildren( this.expression, expression );
		this.expression = expression;
		if ( this.expression != null ) {
			this.expression.setParent( this );
		}
	}

	void setType( BoxExpression type ) {
		replaceChildren( this.type, type );
		this.type = type;
		if ( this.type != null ) {
			this.type.setParent( this );
		}
	}

	void setMessage( BoxExpression message ) {
		replaceChildren( this.message, message );
		this.message = message;
		if ( this.message != null ) {
			this.message.setParent( this );
		}
	}

	void setDetail( BoxExpression detail ) {
		replaceChildren( this.detail, detail );
		this.detail = detail;
		if ( this.detail != null ) {
			this.detail.setParent( this );
		}
	}

	void setErrorCode( BoxExpression errorcode ) {
		replaceChildren( this.errorcode, errorcode );
		this.errorcode = errorcode;
		if ( this.errorcode != null ) {
			this.errorcode.setParent( this );
		}
	}

	void setExtendedInfo( BoxExpression extendedinfo ) {
		replaceChildren( this.extendedinfo, extendedinfo );
		this.extendedinfo = extendedinfo;
		if ( this.extendedinfo != null ) {
			this.extendedinfo.setParent( this );
		}
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

	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}
}
