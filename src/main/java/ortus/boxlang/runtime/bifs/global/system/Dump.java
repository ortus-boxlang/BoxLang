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
package ortus.boxlang.runtime.bifs.global.system;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ortus.boxlang.runtime.bifs.BIF;
import ortus.boxlang.runtime.bifs.BoxBIF;
import ortus.boxlang.runtime.bifs.BoxMember;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.casters.BooleanCaster;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.interop.DynamicObject;
import ortus.boxlang.runtime.scopes.ArgumentsScope;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Argument;
import ortus.boxlang.runtime.types.BoxLangType;
import ortus.boxlang.runtime.util.DumpUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxBIF
@BoxBIF( alias = "writeDump" )
@BoxMember( type = BoxLangType.ANY, name = "dump" )
@BoxMember( type = BoxLangType.ARRAY, name = "dump" )
@BoxMember( type = BoxLangType.BOOLEAN, name = "dump" )
@BoxMember( type = BoxLangType.DATE, name = "dump" )
@BoxMember( type = BoxLangType.DATETIME, name = "dump" )
@BoxMember( type = BoxLangType.FILE, name = "dump" )
@BoxMember( type = BoxLangType.LIST, name = "dump" )
@BoxMember( type = BoxLangType.NUMERIC, name = "dump" )
@BoxMember( type = BoxLangType.QUERY, name = "dump" )
@BoxMember( type = BoxLangType.STRUCT, name = "dump" )
@BoxMember( type = BoxLangType.STRING, name = "dump" )
@BoxMember( type = BoxLangType.UDF, name = "dump" )
@BoxMember( type = BoxLangType.CLOSURE, name = "dump" )
@BoxMember( type = BoxLangType.LAMBDA, name = "dump" )
@BoxMember( type = BoxLangType.XML, name = "dump" )
@BoxMember( type = BoxLangType.CUSTOM, name = "dump" )
public class Dump extends BIF {

	private static final ThreadLocal<Set<Integer>>		dumpedObjects		= ThreadLocal.withInitial( HashSet::new );

	private static final ConcurrentMap<String, String>	dumpTemplateCache	= new ConcurrentHashMap<>();

	private static final Logger							logger				= LoggerFactory.getLogger( Dump.class );

	// This is hard-coded for now. Needs to be dynamic for other dump formats like plain text, etc
	private static final String							TEMPLATES_BASE_PATH	= "/dump/html/";

	/**
	 * Constructor
	 */
	public Dump() {
		super();
		declaredArguments = new Argument[] {
		    // The target object to dump
		    new Argument( false, "any", Key.var ),
		    // A custom label to display above the dump (Only in HTML output)
		    new Argument( false, Argument.STRING, Key.label, "" ),
		    // The number of levels to display when dumping collections. Great to avoid dumping the entire world!
		    new Argument( false, Argument.NUMERIC, Key.top, 0 ),
		    // Whether to expand the dump. By default, the dump is expanded on the first level only
		    new Argument( false, Argument.BOOLEAN, Key.expand, true ),
		    // Whether to do a hard abort the request after dumping
		    new Argument( false, Argument.BOOLEAN, Key.abort, false ),
		    // The output location which can be "buffer", "console", or "{absolute file path}", default is "buffer" (backwards compat => browser)
		    new Argument( false, Argument.STRING, Key.output, Set.of( Validator.NON_EMPTY ) ),
		    // The output format which can be "html" or "text". The default is based on the output location
		    new Argument( false, Argument.STRING, Key.format, Set.of( Validator.valueOneOf( "html", "text" ), Validator.NON_EMPTY ) ),
		    // Show UDFs or not
		    new Argument( false, Argument.BOOLEAN, Key.showUDFs, true )
		};
	}

	/**
	 * Outputs the contents of a variable (simple or complex) of any type for debugging purposes to a specific output location.
	 * <p>
	 * The available {@code output} locations are:
	 * - <strong>buffer<strong>: The output is written to the buffer, which is the default location. If running on a web server, the output is written to the browser.
	 * - <strong>console</strong>: The output is printed to the System console.
	 * - <strong>{absolute_file_path}</strong> The output is written to a file with the specified filename path.
	 * <p>
	 * The output {@code format} can be either HTML or plain text.
	 * The default format is HTML if the output location is the buffer or a web server or a file, otherwise it is plain text for the console.
	 *
	 * @param context   The context in which the BIF is being invoked.
	 * @param arguments Argument scope for the BIF.
	 *
	 * @argument.var The variable to dump, can be any type
	 *
	 * @argument.label A custom label to display above the dump (Only in HTML output)
	 *
	 * @argument.top The number of levels to display when dumping collections. Great to avoid dumping the entire world! Default is inifinity. (Only in HTML output)
	 *
	 * @argument.expand Whether to expand the dump. Be default, we try to expand as much as possible. (Only in HTML output)
	 *
	 * @argument.abort Whether to do a hard abort the request after dumping. Default is false
	 *
	 * @argument.output The output format which can be "buffer", "console", or "{absolute file path}". The default is "buffer".
	 *
	 * @argument.format The format of the output to a <strong>filename</strong>. Can be "html" or "text". The default is according to the output location.
	 *
	 * @argument.showUDFs Show UDFs or not. Default is true. (Only in HTML output)
	 */
	public Object _invoke( IBoxContext context, ArgumentsScope arguments ) {
		// Abort as String and empty means true <bx:dump var="" abort>
		Object abort = arguments.get( Key.abort );
		if ( abort instanceof String castedAbort && castedAbort.isEmpty() ) {
			arguments.put( Key.abort, true );
		}

		// Dump the object
		DumpUtil.dump(
		    context,
		    DynamicObject.unWrap( arguments.get( Key.var ) ),
		    arguments.getAsString( Key.label ),
		    IntegerCaster.cast( arguments.get( Key.top ) ),
		    arguments.getAsBoolean( Key.expand ),
		    BooleanCaster.cast( arguments.get( Key.abort ) ),
		    arguments.getAsString( Key.output ),
		    arguments.getAsString( Key.format ),
		    arguments.getAsBoolean( Key.showUDFs )
		);

		return null;
	}

}
