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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.FileSystemUtil;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class Directory extends Component {

	/**
	 * Constructor
	 */
	public Directory() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "list", "create", "delete", "rename" )
		    ) ),
		    new Attribute( Key.directory, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key._NAME, "string" ),
		    new Attribute( Key.filter, "string" ),
		    new Attribute( Key.mode, "string" ),
		    new Attribute( Key.sort, "string" ),
		    new Attribute( Key.newdirectory, "string" ),
		    new Attribute( Key.recurse, "boolean", false ),
		    new Attribute( Key.type, "string", "all", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "dir", "file", "all" )
		    ) ),
		    new Attribute( Key.listInfo, "string", "all", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "name", "all" )
		    ) ),
		    new Attribute( Key.createPath, "boolean", true )
		};
	}

	/**
	 * Allows you to list, create, delete or rename a directory in the server file system.
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action The action to perform (list, create, delete, rename)
	 * 
	 * @attribute.directory The directory to perform the action on
	 * 
	 * @attribute.name Name for output record set.
	 * 
	 * @attribute.filter Filter applied to returned names. For example: *.bx You can use a pipe ("|") delimiter to specify multiple filters. For example:
	 *                   *.bxm|*.bx Filter pattern matches are case-sensitive on UNIX and Linux. Can also be a UDF/Closure which accepts the
	 *                   file/directory name and returns a Boolean value to indicate whether that item should be included in the result or not.
	 * 
	 * @attribute.mode Applies only to UNIX and Linux. Permissions. Octal values of Unix chmod command. Assigned to owner, group, and other, respectively.
	 *                 For example: 777
	 * 
	 * @attribute.sort Query column(s) by which to sort directory listing. Delimited list of columns from query output.
	 * 
	 * @attribute.newdirectory The new directory name for the rename action.
	 * 
	 * @attribute.recurse Recurse into subdirectories.
	 * 
	 * @attribute.type Type of directory listing to return (dir, file, all).
	 * 
	 * @attribute.listInfo Information to return in the listing (name, all).
	 * 
	 * @attribute.createPath Whether to create all paths necessary to create the directory path.
	 * 
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		Key		action			= Key.of( attributes.getAsString( Key.action ) );
		String	directory		= attributes.getAsString( Key.directory );
		String	name			= attributes.getAsString( Key._NAME );
		String	filter			= attributes.getAsString( Key.filter );
		String	mode			= attributes.getAsString( Key.mode );
		String	sort			= attributes.getAsString( Key.sort );
		String	newdirectory	= attributes.getAsString( Key.newdirectory );
		Boolean	recurse			= attributes.getAsBoolean( Key.recurse );
		String	type			= attributes.getAsString( Key.type );
		String	listInfo		= attributes.getAsString( Key.listInfo );
		Boolean	createPath		= attributes.getAsBoolean( Key.createPath );

		if ( action.equals( Key.list ) ) {
			list( context, directory, name, filter, sort, recurse, type, listInfo );
		} else if ( action.equals( Key.create ) ) {
			create( context, directory, createPath, mode );
		} else if ( action.equals( Key.delete ) ) {
			delete( context, directory, recurse );
		} else if ( action.equals( Key.rename ) ) {
			rename( context, directory, newdirectory, createPath, mode );
		} else {
			throw new BoxRuntimeException( "Unimplemeted action: " + action.getName() );
		}

		return DEFAULT_RETURN;
	}

	private void rename( IBoxContext context, String directory, String newdirectory, Boolean createPath, String mode ) {
		// TODO: Extract this logic in the directoryMove BIF to the FileSystem service and share this code
		Path	destinationPath	= Path.of( newdirectory );
		Path	sourcePath		= Path.of( directory );
		if ( !createPath && !Files.exists( destinationPath.getParent() ) ) {
			throw new BoxRuntimeException( "The directory [" + destinationPath.toAbsolutePath().toString()
			    + "] cannot be created because the parent directory [" + destinationPath.getParent().toAbsolutePath().toString()
			    + "] does not exist.  To prevent this error set the createPath argument to true." );
		} else if ( Files.exists( destinationPath ) ) {
			throw new BoxRuntimeException( "The target directory [" + destinationPath.toAbsolutePath().toString() + "] already exists" );
		} else {
			try {
				Files.createDirectories( destinationPath.getParent() );
				Files.move( sourcePath, destinationPath );
				if ( mode != null ) {
					FileSystemUtil.setPosixPermissions( newdirectory, mode );
				}
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
	}

	private void delete( IBoxContext context, String directory, Boolean recurse ) {
		FileSystemUtil.deleteDirectory( directory, recurse );
	}

	private void create( IBoxContext context, String directory, Boolean createPath, String mode ) {
		// TODO: Extract this logic in the directoryCreate BIF to the FileSystem service and share this code
		try {
			if ( createPath ) {
				Files.createDirectories( Path.of( directory ) );
			} else {
				Files.createDirectory( Path.of( directory ) );
			}
			if ( mode != null ) {
				FileSystemUtil.setPosixPermissions( directory, mode );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	private void list( IBoxContext context, String directory, String name, String filter, String sort, Boolean recurse, String type, String listInfo ) {
		// TODO: Extract this logic in the directoryList BIF to the FileSystem service and add optional filters
		// for type (dir, file, all), and listInfo (name, all)
		Object result = runtime.getFunctionService().getGlobalFunction( Key.directoryList ).invoke( context, Map.of(
		    Key.path, directory,
		    Key.recurse, recurse,
		    Key.listInfo, "query",
		    Key.filter, filter != null ? filter : "",
		    Key.sort, sort != null ? sort : "",
		    Key.type, type
		), false, Key.directoryList );
		ExpressionInterpreter.setVariable( context, name, result );
	}

}
