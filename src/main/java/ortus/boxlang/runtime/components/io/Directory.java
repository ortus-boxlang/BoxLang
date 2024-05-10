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

import java.util.Map;
import java.util.Set;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.BIFDescriptor;
import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.Struct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.validation.Validator;

@BoxComponent
public class Directory extends Component {

	/**
	 * Stores actions reference to a BIFDescriptor
	 */
	private static final Map<Key, BIFDescriptor> actionsMap = Map.of(
	    Key.list, BoxRuntime.getInstance().getFunctionService().getGlobalFunction( Key.directoryList ),
	    Key.copy, BoxRuntime.getInstance().getFunctionService().getGlobalFunction( Key.directoryCopy ),
	    Key.create, BoxRuntime.getInstance().getFunctionService().getGlobalFunction( Key.directoryCreate ),
	    Key.delete, BoxRuntime.getInstance().getFunctionService().getGlobalFunction( Key.directoryDelete ),
	    Key.rename, BoxRuntime.getInstance().getFunctionService().getGlobalFunction( Key.directoryMove )
	);

	/**
	 * Constructor
	 */
	public Directory() {
		super();
		declaredAttributes = new Attribute[] {
		    new Attribute( Key.action, "string", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "list", "create", "delete", "rename", "copy" )
		    ) ),
		    new Attribute( Key.directory, "string", Set.of( Validator.REQUIRED, Validator.NON_EMPTY ) ),
		    new Attribute( Key._NAME, "string" ),
		    new Attribute( Key.filter, "string", "*" ),
		    new Attribute( Key.mode, "string" ),
		    new Attribute( Key.sort, "string" ),
		    new Attribute( Key.newDirectory, "string" ),
		    new Attribute( Key.destination, "string" ),
		    new Attribute( Key.recurse, "boolean", false ),
		    new Attribute( Key.type, "string", "all", Set.of(
		        Validator.REQUIRED,
		        Validator.NON_EMPTY,
		        Validator.valueOneOf( "dir", "file", "all" )
		    ) ),
		    new Attribute( Key.listInfo, "string", "query", Set.of(
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
	 * @attribute.newDirectory The new directory name for the rename action.
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
		String	newDirectory	= attributes.getAsString( Key.newDirectory );
		String	destination		= attributes.getAsString( Key.destination );
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
			rename( context, directory, newDirectory, createPath );
		} else if ( action.equals( Key.copy ) ) {
			copy( context, directory, destination, recurse, filter, createPath );
		} else {
			throw new BoxRuntimeException( "Unimplemeted directory action: " + action.getName() );
		}

		return DEFAULT_RETURN;
	}

	private void rename( IBoxContext context, String directory, String newDirectory, Boolean createPath ) {
		actionsMap.get( Key.rename )
		    .invoke(
		        context,
		        Map.of(
		            Key.oldPath, directory,
		            Key.newPath, newDirectory,
		            Key.createPath, createPath
		        ),
		        false,
		        Key.directoryMove
		    );
	}

	private void delete( IBoxContext context, String directory, Boolean recurse ) {
		actionsMap.get( Key.delete )
		    .invoke(
		        context,
		        Map.of(
		            Key.path, directory,
		            Key.recursive, recurse
		        ),
		        false,
		        Key.directoryDelete
		    );
	}

	private void create( IBoxContext context, String directory, Boolean createPath, String mode ) {
		Map<Key, Object> argumentsMap = Struct.of(
		    Key.path, directory,
		    Key.createPath, createPath,
		    Key.ignoreExists, false
		);
		if ( mode != null ) {
			argumentsMap.put( Key.mode, mode );
		}
		actionsMap.get( Key.create ).invoke(
		    context,
		    argumentsMap,
		    false,
		    Key.directoryCreate
		);
	}

	private void copy( IBoxContext context, String directory, String newDirectory, Boolean recurse, String filter, Boolean createPath ) {
		IStruct argumentsMap = Struct.of(
		    Key.source, directory,
		    Key.destination, newDirectory,
		    Key.recurse, recurse,
		    Key.filter, filter,
		    Key.createPath, createPath
		);
		actionsMap.get( Key.copy ).invoke( context, argumentsMap, false, Key.directoryCopy );
	}

	private void list( IBoxContext context, String directory, String name, String filter, String sort, Boolean recurse, String type, String listInfo ) {
		ExpressionInterpreter.setVariable(
		    context,
		    name,
		    actionsMap.get( Key.list )
		        .invoke(
		            context,
		            Map.of(
		                Key.path, directory,
		                Key.recurse, recurse,
		                Key.listInfo, listInfo,
		                Key.filter, filter != null ? filter : "",
		                Key.sort, sort != null ? sort : "",
		                Key.type, type
		            ),
		            false,
		            Key.directoryList
		        )
		);
	}

}
