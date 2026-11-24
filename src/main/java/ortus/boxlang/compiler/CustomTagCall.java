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
package ortus.boxlang.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import ortus.boxlang.compiler.ast.BoxExpression;
import ortus.boxlang.compiler.ast.expression.BoxFQN;
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxStringInterpolation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructType;
import ortus.boxlang.compiler.ast.statement.BoxAnnotation;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;

/**
 * Turns a custom tag shortcut, &lt;cf_menu&gt; or &lt;app:menu&gt; of an imported tag library, into a call of the
 * component (module) component, shared by the ASM and Java transformers.
 *
 * The component component takes its own name, template and tagLib attributes, while a shortcut call passes every attribute
 * to the tag. So attributes the tag itself is given under one of those names travel in the attributeCollection instead,
 * which the component component hands to the tag as ordinary attributes.
 */
public final class CustomTagCall {

	/**
	 * Lower-cased names of the attributes the component component reserves for itself
	 */
	private static final Set<String> RESERVED_ATTRIBUTES = Set.of( "name", "template", "taglib" );

	private CustomTagCall() {
	}

	/**
	 * Is this component a custom tag shortcut call?
	 *
	 * @param component the component node
	 *
	 * @return true for &lt;cf_menu&gt; and &lt;app:menu&gt;
	 */
	public static boolean isShortcut( BoxComponent component ) {
		return component.isTaglibTag() || component.getName().startsWith( "_" );
	}

	/**
	 * The attributes for the component component call that runs the tag
	 *
	 * @param component the shortcut call
	 *
	 * @return a new list: the attributes as written, the reserved ones moved into the attributeCollection, plus name and, for a
	 *         tag library, tagLib
	 */
	public static List<BoxAnnotation> componentAttributes( BoxComponent component ) {
		List<BoxAnnotation>	attributes			= new ArrayList<>( component.getAttributes() );
		List<BoxAnnotation>	reserved			= new ArrayList<>();
		BoxAnnotation		attributeCollection	= null;
		for ( BoxAnnotation attribute : attributes ) {
			String key = attribute.getKey().getValue();
			if ( RESERVED_ATTRIBUTES.contains( key.toLowerCase() ) ) {
				reserved.add( attribute );
			} else if ( key.equalsIgnoreCase( "attributeCollection" ) ) {
				attributeCollection = attribute;
			}
		}

		if ( !reserved.isEmpty() ) {
			BoxStructLiteral literalCollection = structLiteralOf( attributeCollection );
			if ( literalCollection != null ) {
				// an attributeCollection written as a literal struct gets the reserved attributes, unless it has them already
				List<BoxExpression> values = new ArrayList<>( literalCollection.getValues() );
				for ( BoxAnnotation attribute : reserved ) {
					if ( !hasKey( values, attribute.getKey().getValue() ) ) {
						values.add( new BoxIdentifier( attribute.getKey().getValue(), null, null ) );
						values.add( attribute.getValue() );
					}
				}
				literalCollection.setValues( values );
			} else {
				List<BoxExpression> values = new ArrayList<>();
				for ( BoxAnnotation attribute : reserved ) {
					values.add( new BoxIdentifier( attribute.getKey().getValue(), null, null ) );
					values.add( attribute.getValue() );
				}
				attributes.add(
				    new BoxAnnotation(
				        new BoxFQN( "attributeCollection", null, null ),
				        new BoxStructLiteral( BoxStructType.Unordered, values, null, null ),
				        null,
				        null )
				);
			}
			attributes.removeAll( reserved );
		}

		String tagName = component.isTaglibTag() ? component.getTaglibTagName() : component.getName().substring( 1 );
		attributes.add( new BoxAnnotation( new BoxFQN( "name", null, null ), new BoxStringLiteral( tagName, null, null ), null, null ) );
		if ( component.isTaglibTag() ) {
			attributes.add(
			    new BoxAnnotation( new BoxFQN( "tagLib", null, null ), new BoxStringLiteral( component.getTaglibPath(), null, null ), null, null ) );
		}
		return attributes;
	}

	private static boolean hasKey( List<BoxExpression> structValues, String key ) {
		for ( int i = 0; i < structValues.size(); i += 2 ) {
			BoxExpression keyExpression = structValues.get( i );
			if ( ( keyExpression instanceof BoxIdentifier identifier && identifier.getName().equalsIgnoreCase( key ) )
			    || ( keyExpression instanceof BoxFQN fqn && fqn.getValue().equalsIgnoreCase( key ) )
			    || ( keyExpression instanceof BoxStringLiteral string && string.getValue().equalsIgnoreCase( key ) ) ) {
				return true;
			}
		}
		return false;
	}

	private static BoxStructLiteral structLiteralOf( BoxAnnotation attributeCollection ) {
		if ( attributeCollection == null ) {
			return null;
		}
		BoxExpression value = attributeCollection.getValue();
		if ( value instanceof BoxStructLiteral structLiteral ) {
			return structLiteral;
		}
		if ( value instanceof BoxStringInterpolation interpolation && interpolation.getValues().size() == 1
		    && interpolation.getValues().get( 0 ) instanceof BoxStructLiteral structLiteral ) {
			return structLiteral;
		}
		return null;
	}
}
