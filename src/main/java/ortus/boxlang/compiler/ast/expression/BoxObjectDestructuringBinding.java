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
package ortus.boxlang.compiler.ast.expression;

import java.util.Map;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.BoxNode;
import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.visitor.ReplacingBoxVisitor;
import ortus.boxlang.compiler.ast.visitor.VoidBoxVisitor;

/**
 * A single object destructuring binding entry.
 */
public class BoxObjectDestructuringBinding extends BoxNode {

	private BoxExpression					key;
	private BoxExpression					target;
	private BoxObjectDestructuringPattern	pattern;
	private BoxExpression					defaultValue;
	private boolean							rest;

	/**
	 * BoxObjectDestructuringBinding.
	 */
	public BoxObjectDestructuringBinding( BoxExpression key, BoxExpression target, BoxObjectDestructuringPattern pattern, BoxExpression defaultValue,
	    boolean rest,
	    Position position, String sourceText ) {
		super( position, sourceText );
		setKey( key );
		setTarget( target );
		setPattern( pattern );
		setDefaultValue( defaultValue );
		setRest( rest );
	}

	/**
	 * @return source key expression for this binding.
	 */
	public BoxExpression getKey() {
		return key;
	}

	/**
	 * @return assignment target expression, or null for nested-only entries.
	 */
	public BoxExpression getTarget() {
		return target;
	}

	/**
	 * @return nested object pattern, or null for direct target bindings.
	 */
	public BoxObjectDestructuringPattern getPattern() {
		return pattern;
	}

	/**
	 * @return default value expression when the source key is missing/null.
	 */
	public BoxExpression getDefaultValue() {
		return defaultValue;
	}

	/**
	 * @return true when this binding is a rest capture.
	 */
	public boolean isRest() {
		return rest;
	}

	/**
	 * Set the binding source key expression.
	 *
	 * @param key source key expression
	 */
	public void setKey( BoxExpression key ) {
		replaceChildren( this.key, key );
		this.key = key;
		if ( this.key != null ) {
			this.key.setParent( this );
		}
	}

	/**
	 * Set the binding target expression.
	 *
	 * @param target target expression
	 */
	public void setTarget( BoxExpression target ) {
		replaceChildren( this.target, target );
		this.target = target;
		if ( this.target != null ) {
			this.target.setParent( this );
		}
	}

	/**
	 * Set the nested object pattern.
	 *
	 * @param pattern nested pattern
	 */
	public void setPattern( BoxObjectDestructuringPattern pattern ) {
		replaceChildren( this.pattern, pattern );
		this.pattern = pattern;
		if ( this.pattern != null ) {
			this.pattern.setParent( this );
		}
	}

	/**
	 * Set the binding default value expression.
	 *
	 * @param defaultValue default expression
	 */
	public void setDefaultValue( BoxExpression defaultValue ) {
		replaceChildren( this.defaultValue, defaultValue );
		this.defaultValue = defaultValue;
		if ( this.defaultValue != null ) {
			this.defaultValue.setParent( this );
		}
	}

	/**
	 * Set whether this binding is a rest capture.
	 *
	 * @param rest true for rest binding
	 */
	public void setRest( boolean rest ) {
		this.rest = rest;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<String, Object> toMap() {
		Map<String, Object> map = super.toMap();
		map.put( "rest", rest );
		map.put( "key", key != null ? key.toMap() : null );
		map.put( "target", target != null ? target.toMap() : null );
		map.put( "pattern", pattern != null ? pattern.toMap() : null );
		map.put( "defaultValue", defaultValue != null ? defaultValue.toMap() : null );
		return map;
	}

	/**
	 * Accept a non-mutating visitor.
	 *
	 * @param v visitor instance
	 */
	public void accept( VoidBoxVisitor v ) {
		v.visit( this );
	}

	/**
	 * Accept a replacing visitor.
	 *
	 * @param v visitor instance
	 *
	 * @return replacement node
	 */
	public BoxNode accept( ReplacingBoxVisitor v ) {
		return v.visit( this );
	}
}
