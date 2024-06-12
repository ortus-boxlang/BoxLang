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
package ortus.boxlang.runtime.components.system;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.ArrayCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class Associate extends Component {

	/**
	 * --------------------------------------------------------------------------
	 * Constructor(s)
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 */
	public Associate() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.baseTag, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key.dataCollection, "string", "AssocAttribs", Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * Allows subtag data to be saved with a base tag. Applies only to custom tags.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attributes.baseTag The base tag to associate the data with
	 * 
	 * @attributes.dataCollection The collection of data to associate with the base tag
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		String	baseTag				= attributes.getAsString( Key.baseTag );
		Key		dataCollectionName	= Key.of( attributes.getAsString( Key.dataCollection ) );
		IStruct	childState			= context.findClosestComponent( Key.module );
		IStruct	childAttributes		= childState.getAsStruct( Key.attributes ).getAsStruct( Key.attributes );

		IStruct	parentState			= context.findClosestComponent( Key.module,
		    ( s ) -> s != childState && s.getAsKey( Key.customTagName ).equals( Key.of( baseTag ) ) );
		if ( parentState == null ) {
			throw new RuntimeException( "Associate component is not nested in the body of a custom tag named [" + baseTag + "]" );
		}

		IStruct	dataCollection		= parentState.getAsStruct( Key.dataCollection );
		Object	DataCollectionObj	= dataCollection.get( dataCollectionName );
		if ( DataCollectionObj == null ) {
			dataCollection.put( dataCollectionName, Array.of( childAttributes ) );
		} else {
			Array theData = ArrayCaster.cast( DataCollectionObj );
			theData.append( childAttributes );
			dataCollection.put( dataCollectionName, theData );
		}

		return DEFAULT_RETURN;
	}
}
