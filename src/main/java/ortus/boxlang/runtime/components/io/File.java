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
package ortus.boxlang.runtime.components.io;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class File extends Component {

	/**
	 * Constructor
	 *
	 * @param name The name of the component
	 */
	public File() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "append", "copy", "delete", "move", "read", "readbinary", "rename", "upload", "uploadall", "write" ),
		        Validator.valueRequires( "write", Key.file, Key.output )
		    ) ),
		    new Attribute( Key.file, "string" ),
		    new Attribute( Key.mode, "string" ),
		    new Attribute( Key.output, "string" ),
		    new Attribute( Key.addnewline, "boolean", false ),
		    new Attribute( Key.attributes, "string" ),
		    new Attribute( Key.charset, "string" ),
		    new Attribute( Key.source, "string" ),
		    new Attribute( Key.destination, "string" ),
		    new Attribute( Key.variable, "string" ),
		    new Attribute( Key.filefield, "string" ),
		    new Attribute( Key.nameconflict, "string", Set.of( Validator.valueOneOf( "error", "skip", "overwrite", "makeunique" ) ) ),
		    new Attribute( Key.accept, "string" ),
		    new Attribute( Key.result, "string" ),
		    new Attribute( Key.fixnewline, "boolean", false ),
		    new Attribute( Key.cachedwithin, "numeric" )
		};
	}

	/**
	 * Manages interactions with server files. Different combinations cause different attributes to be required.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action The action to take. One of: append, copy, delete, move, read, readbinary, rename, upload, uploadall, write
	 * 
	 * @attribute.file The file to act on
	 * 
	 * @attribute.mode The mode to open the file in
	 * 
	 * @attribute.output The output of the action
	 * 
	 * @attribute.addnewline Add a newline to the end of the file
	 * 
	 * @attribute.attributes Attributes to set on the file
	 * 
	 * @attribute.charset The character set to use
	 * 
	 * @attribute.source The source file
	 * 
	 * @attribute.destination The destination file
	 * 
	 * @attribute.variable The variable to store the result in
	 * 
	 * @attribute.filefield The file field to use
	 * 
	 * @attribute.nameconflict What to do if there is a name conflict
	 * 
	 * @attribute.accept The accept header
	 * 
	 * @attribute.result The result of the action
	 * 
	 * @attribute.fixnewline Fix newlines
	 * 
	 * @attribute.cachedwithin The time to cache the file within
	 * 
	 *
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		Key		action			= Key.of( attributes.getAsString( Key.action ) );
		String	file			= attributes.getAsString( Key.file );
		String	mode			= attributes.getAsString( Key.mode );
		String	output			= attributes.getAsString( Key.output );
		Boolean	addnewline		= attributes.getAsBoolean( Key.addnewline );
		String	fileAttributes	= attributes.getAsString( Key.attributes );
		String	charset			= attributes.getAsString( Key.charset );
		String	source			= attributes.getAsString( Key.source );
		String	destination		= attributes.getAsString( Key.destination );
		String	variable		= attributes.getAsString( Key.variable );
		String	filefield		= attributes.getAsString( Key.filefield );
		String	nameconflict	= attributes.getAsString( Key.nameconflict );
		String	accept			= attributes.getAsString( Key.accept );
		String	result			= attributes.getAsString( Key.result );
		Boolean	fixnewline		= attributes.getAsBoolean( Key.fixnewline );
		Double	cachedwithin	= attributes.getAsDouble( Key.cachedwithin );

		if ( action.equals( Key.write ) ) {
			write( context, file, output, addnewline, fileAttributes, charset, fixnewline, mode );
		} else {
			throw new BoxRuntimeException( "unimplemeted action: " + action );
		}

		return DEFAULT_RETURN;
	}

	private void write( IBoxContext context, String file, String output, Boolean addnewline, String fileAttributes, String charset, Boolean fixnewline,
	    String mode ) {
		charset	= charset == null ? "UTF-8" : charset;
		output	= addnewline ? output + "\n" : output;
		if ( fixnewline ) {
			output = output.replaceAll( "\r\n", java.io.File.separator );
		}
		// TODO: Apply attributes and mode
		FileSystemUtil.write( file, output, charset, true );
	}

}
