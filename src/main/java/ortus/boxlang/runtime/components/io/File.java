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

import java.util.HashMap;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.events.BoxEvent;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class File extends Component {

	/**
	 * The runtime instance
	 */
	protected BoxRuntime						runtime				= BoxRuntime.getInstance();
	private final Key							fileAppendKey		= Key.of( "fileAppend" );
	private final Key							fileCopyKey			= Key.of( "fileCopy" );
	private final Key							fileDeleteKey		= Key.of( "fileDelete" );
	private final Key							fileMoveKey			= Key.of( "fileMove" );
	private final Key							fileReadKey			= Key.of( "fileRead" );
	private final Key							fileReadBinaryKey	= Key.of( "fileReadBinary" );
	private final Key							fileUploadKey		= Key.of( "fileUpload" );
	private final Key							fileUploadAllKey	= Key.of( "fileUploadAll" );
	private final Key							fileWriteKey		= Key.of( "fileWrite" );

	private final HashMap<Key, BIFDescriptor>	actionsMap			= new HashMap<Key, BIFDescriptor>() {

																		{
																			put( Key.append,
																			    runtime.getFunctionService().getGlobalFunction( fileAppendKey ) );
																			put( Key.copy,
																			    runtime.getFunctionService().getGlobalFunction( fileCopyKey ) );
																			put( Key.delete,
																			    runtime.getFunctionService().getGlobalFunction( fileDeleteKey ) );
																			put( Key.move,
																			    runtime.getFunctionService().getGlobalFunction( fileMoveKey ) );
																			put( Key.read,
																			    runtime.getFunctionService().getGlobalFunction( fileReadKey ) );
																			put( Key.readBinary,
																			    runtime.getFunctionService().getGlobalFunction( fileReadBinaryKey ) );
																			put( Key.upload,
																			    runtime.getFunctionService().getGlobalFunction( fileUploadKey ) );
																			put( Key.uploadAll,
																			    runtime.getFunctionService().getGlobalFunction( fileUploadAllKey ) );
																			put( Key.write,
																			    runtime.getFunctionService().getGlobalFunction( fileWriteKey ) );

																		}
																	};

	/**
	 * Constructor
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
		    new Attribute( Key.charset, "string", "utf-8" ),
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
		Key		action		= Key.of( attributes.getAsString( Key.action ) );
		String	output		= attributes.getAsString( Key.output );
		String	variable	= attributes.getAsString( Key.variable );

		if ( action.equals( Key.write ) ) {
			attributes.put( Key.data, output );
			actionsMap.get( Key.write ).invoke( context, attributes, false, fileWriteKey );
		} else if ( action.equals( Key.append ) ) {
			attributes.put( Key.data, output );
			actionsMap.get( Key.append ).invoke( context, attributes, false, fileAppendKey );
		} else if ( action.equals( Key.copy ) ) {
			actionsMap.get( Key.copy ).invoke( context, attributes, false, fileCopyKey );
		} else if ( action.equals( Key.delete ) ) {
			actionsMap.get( Key.delete ).invoke( context, attributes, false, fileDeleteKey );
		} else if ( action.equals( Key.move ) || action.equals( Key.rename ) ) {
			actionsMap.get( Key.move ).invoke( context, attributes, false, fileMoveKey );
		} else if ( action.equals( Key.read ) ) {
			if ( variable == null ) {
				throw new BoxRuntimeException( "The [variable] attribute is required for file action [read]." );
			}
			attributes.put( Key.filepath, attributes.get( Key.file ) );
			ExpressionInterpreter.setVariable(
			    context,
			    attributes.getAsString( Key.variable ),
			    actionsMap.get( Key.read ).invoke( context, attributes, false, fileReadKey )
			);
		} else if ( action.equals( Key.readBinary ) ) {
			if ( variable == null ) {
				throw new BoxRuntimeException( "The [variable] attribute is required for file action [readBinary]." );
			}
			attributes.put( Key.filepath, attributes.get( Key.file ) );
			ExpressionInterpreter.setVariable(
			    context,
			    attributes.getAsString( Key.variable ),
			    actionsMap.get( Key.readBinary ).invoke( context, attributes, false, fileReadBinaryKey )
			);
		} else {
			// Announce an interception so that modules can contribute to object creation requests
			HashMap<Key, Object> interceptorArgs = new HashMap<Key, Object>() {

				{
					put( Key.response, null );
					put( Key.context, context );
					put( Key.arguments, attributes );
				}
			};
			interceptorService.announce( BoxEvent.ON_FILECOMPONENT_ACTION, new Struct( interceptorArgs ) );
			if ( interceptorArgs.get( Key.response ) != null ) {
				ExpressionInterpreter.setVariable(
				    context,
				    attributes.getAsString( Key.variable ),
				    interceptorArgs.get( Key.response )
				);
			} else {
				throw new BoxRuntimeException( "The file action [" + action.getName() + "] is not currently supported" );
			}
		}

		return DEFAULT_RETURN;
	}

}
