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
package ortus.boxlang.runtime.config.segments;

import ortus.boxlang.runtime.config.util.PropertyHelper;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;

/**
 * This config segment is used to configure the default XML parsing security
 * and validation settings for the BoxLang runtime.
 *
 * All settings use descriptive, unabbreviated names. Backward compatibility
 * with legacy Lucee/CFML key names is handled in {@link #process(IStruct)}.
 */
public class XMLConfig implements IConfigSegment {

	/**
	 * Enable JAXP secure processing mode.
	 * When true, the XML parser applies security limits to prevent XML
	 * processing attacks.
	 * Defaults to {@code true}.
	 */
	public boolean	secureProcessing			= true;

	/**
	 * Disallow DOCTYPE declarations in the XML document.
	 * When true, parsing fails if the XML contains a DOCTYPE declaration.
	 * Defaults to {@code true}.
	 */
	public boolean	disallowDoctypeDeclaration	= true;

	/**
	 * Allow external general entities in the XML document.
	 * When false, external entities are disabled for security.
	 * Defaults to {@code false}.
	 * Old key: {@code externalGeneralEntities}
	 */
	public boolean	allowExternalEntities		= false;

	/**
	 * Enable lenient XML parsing.
	 * When true, the parser relaxes well-formedness and validation
	 * requirements — skipping external DTD loading when it is inaccessible.
	 * Defaults to {@code false}.
	 */
	public boolean	lenientProcessing			= false;

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Default empty constructor
	 */
	public XMLConfig() {
	}

	/**
	 * Processes the configuration struct. Each property supports both the new
	 * canonical key name and the legacy Lucee/CFML key name for backward
	 * compatibility.
	 *
	 * @param config the configuration struct
	 *
	 * @return the configuration
	 */
	@Override
	public IConfigSegment process( IStruct config ) {
		this.secureProcessing			= processBooleanCompat( config, Key.secureProcessing, Key.secure, this.secureProcessing );
		this.disallowDoctypeDeclaration	= processBooleanCompat( config, Key.disallowDoctypeDeclaration, Key.disallowDoctypeDecl,
		    this.disallowDoctypeDeclaration );
		this.allowExternalEntities		= processBooleanCompat( config, Key.allowExternalEntities, Key.externalGeneralEntities,
		    this.allowExternalEntities );
		this.lenientProcessing			= processBooleanCompat( config, Key.lenientProcessing, Key.lenient, this.lenientProcessing );

		return this;
	}

	/**
	 * @inheritDoc
	 */
	@Override
	public IStruct asStruct() {
		return Struct.ofNonConcurrent(
		    Key.secureProcessing, this.secureProcessing,
		    Key.disallowDoctypeDeclaration, this.disallowDoctypeDeclaration,
		    Key.allowExternalEntities, this.allowExternalEntities,
		    Key.lenientProcessing, this.lenientProcessing
		);
	}

	/**
	 * Utility method to read a boolean property with backward-compat fallback.
	 * Checks the canonical key first, then falls back to the legacy key.
	 *
	 * @param config       The config struct
	 * @param primaryKey   The canonical key to check first
	 * @param legacyKey    The legacy key to check as fallback
	 * @param defaultValue The default value if neither key is present
	 *
	 * @return The resolved boolean value
	 */
	private static boolean processBooleanCompat( IStruct config, Key primaryKey, Key legacyKey, boolean defaultValue ) {
		// Check canonical key first
		if ( config.containsKey( primaryKey ) ) {
			return PropertyHelper.processBoolean( config, primaryKey, defaultValue );
		}
		// Fall back to legacy key
		if ( config.containsKey( legacyKey ) ) {
			return PropertyHelper.processBoolean( config, legacyKey, defaultValue );
		}
		return defaultValue;
	}

	/**
	 * Normalize a raw settings struct by running it through an XMLConfig
	 * and returning the canonicalized result. This is for use when Application.bx
	 * settings may contain legacy key names.
	 *
	 * @param raw The raw settings struct (may contain legacy keys)
	 *
	 * @return A new struct with only canonical key names
	 */
	public static IStruct normalize( IStruct raw ) {
		XMLConfig config = new XMLConfig();
		config.process( raw );
		return config.asStruct();
	}

}
