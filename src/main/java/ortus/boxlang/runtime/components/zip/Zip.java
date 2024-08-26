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
package ortus.boxlang.runtime.components.zip;

import java.util.Set;

import ortus.boxlang.runtime.components.Attribute;
import ortus.boxlang.runtime.components.BoxComponent;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.ExpressionInterpreter;
import ortus.boxlang.runtime.dynamic.casters.StringCaster;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.types.Array;
import ortus.boxlang.runtime.types.IStruct;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.util.ZipUtil;
import ortus.boxlang.runtime.validation.Validator;

/**
 * This component allows you to compress/uncompress and manipulate zip/gzip files
 */
@BoxComponent( allowsBody = true, alias = "gzip" )
public class Zip extends Component {

	/**
	 * Constructor
	 */
	public Zip() {
		super();
		declaredAttributes = new Attribute[] {
		    // The action to take: delete, list, read, readBinary, unzip, zip
		    // The default is: zip
		    new Attribute(
		        Key.action,
		        "string",
		        "zip", Set.of(
		            Validator.NON_EMPTY,
		            Validator.valueOneOf( "delete", "list", "read", "readBinary", "unzip", "zip" )
		        )
		    ),
		    // File path to the zip/gzip file to be manipulated
		    // Actions that require this attribute: delete, list, read, readBinary, unzip, zip
		    new Attribute( Key.file, "string", Set.of( Validator.NON_EMPTY ) ),
		    // Destination directory where the zip/gzip file will be extracted
		    // Actions that require this attribute: unzip
		    new Attribute( Key.destination, "string", Set.of( Validator.NON_EMPTY ) ),
		    // Filter: This can be a regular expression or a Closure/Lambda that will be used to filter the files
		    // Actions that require this attribute: delete, list, unzip, zip
		    new Attribute( Key.filter, "any", Set.of( Validator.NON_EMPTY ) ),
		    // Pathname(s) on which the action is performed.
		    // Actions that require this attribute: delete, list, read, readBinary, unzip
		    new Attribute( Key.entryPath, "any", Set.of( Validator.NON_EMPTY ) ),
		    // Charset: Defaults to the machine's default charset, pass it in as null
		    new Attribute( Key.charset, "string", Set.of( Validator.NON_EMPTY ) ),
		    // Name: The name of the variable to store the result in when doing a list
		    // The result is an array of structs
		    // If not provided, the default is bxzip
		    // Actions that require this attribute: list
		    new Attribute( Key.result, "string", "bxzip", Set.of( Validator.NON_EMPTY ) ),
		    // Overwrite: Whether to overwrite the destination file(s) if it already exists when zipping/unzipping
		    // Default is false.
		    // Actions that require this attribute: zip, unzip
		    new Attribute( Key.overwrite, "boolean", false ),
		    // Prefix: The prefix to add to the files when zipping files, this is the directory name to store files in
		    // Default is null
		    // Actions that require this attribute: zip
		    new Attribute( Key.prefix, "string", Set.of( Validator.NON_EMPTY ) ),
		    // Recurse: Whether to recurse into subdirectories when zipping/unzipping
		    // Default is true
		    // Actions that require this attribute: list, zip, unzip
		    new Attribute( Key.recurse, "boolean", true ),
		    // FlatList: If false, the list action will return an array of structs with all kinds of information about the entries.
		    // If true, it will return a flat list of strings with the path of the entries.
		    // Default is false
		    // Actions that require this attribute: list
		    new Attribute( Key.flatList, "boolean", false ),
		    // Source: The absolute path to the source directory to be zipped.
		    // Actions that require this attribute: zip
		    new Attribute( Key.source, "string", Set.of( Validator.NON_EMPTY ) ),
		    // Variable: The name of the variable to store the read content in
		    // Actions that require this attribute: read, readBinary
		    new Attribute( Key.variable, "string", Set.of( Validator.NON_EMPTY ) )
		};
	}

	/**
	 * The BoxLang Zip component is a powerful component that allows you to interact with zip/gzip files.
	 * You can compress/uncompress files, list the contents of a zip file, read the contents of a file inside a zip file (text or binary),
	 * and delete files from a zip file.
	 * <p>
	 * <h2>Actions</h2>
	 * The Zip component has the following actions:
	 * <ul>
	 * <li><strong>delete</strong>: Delete a file from a zip file</li>
	 * <li><strong>list</strong>: List the contents of a zip file</li>
	 * <li><strong>read</strong>: Read the contents of a file inside a zip file</li>
	 * <li><strong>readBinary</strong>: Read the contents of a binary file inside a zip file</li>
	 * <li><strong>unzip</strong>: Uncompress a zip file</li>
	 * <li><strong>zip</strong>: Compress files into a zip file</li>
	 * </ul>
	 * <p>
	 * <h2>Attributes</h2>
	 * The Zip component has the following attributes:
	 * <ul>
	 * <li><strong>action</strong>: The action to take: delete, list, read, readBinary, unzip, zip (default)</li>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be manipulated. Actions: {@code list, read, readBinary, unzip, zip}</li>
	 * <li><strong>destination</strong>: Absolute destination directory where the zip/gzip file will be to. If it doesn't exist it will be created for you. Actions: {@code unzip}</li>
	 * <li><strong>filter</strong>: This can be a regular expression ({@code *.txt}) or a BoxLang Closure/Lambda ({@code (path) => path.endsWith(".txt")}) that will be used to filter the files. Actions: {@code delete, list, unzip, zip}</li>
	 * <ul>
	 * <li>The closure/lambda should return {@code true} to extract/compress/zip/list the entry and {@code false} to skip it.</li>
	 * <li>The closure/lambda should take a single argument which is the entry path.</li>
	 * <li>The regular expression is done in a case-insensitive manner.</li>
	 * </ul>
	 * <li><strong>entryPath</strong>: Zip entry path or an array of entry paths on which the action is performed. Actions: {@code delete, list, read, readBinary, unzip}</li>
	 * <ul>
	 * <li>Can be a single path string.</li>
	 * <li>Can be an array of paths which applies to multiple entry paths. Does not apply to {@code read, readBinary}</li>
	 * <li>Can be used to ONLY include in a listing the entries that match the entry path. The full path must be used</li>
	 * <li>Can be used to ONLY delete the entries that match the entry path. The full path must be used</li>
	 * <li>If the action is @{code read or readBinary} and you use an entry path, then a {@code variable} attribute must also be used to store the contents.</li>
	 * </ul>
	 * <li><strong>charset</strong>: Valid Java Charset to use when reading the contents of a file inside a zip file. Default is the machine's default charset. Actions: {@code read, readBinary}</li>
	 * <li><strong>result</strong>: The name of the variable to store the result in when doing a {@code list} operation on. If not provided, we will use {@code bxzip}. The result is an array of structs. Actions: {@code list}</li>
	 * <ul>
	 * <li><strong>fullpath</strong>: The full path of the entry: e.g. "folder1/folder2/file.txt"</li>
	 * <li><strong>name</strong>: The file name of the entry: e.g. "file.txt"</li>
	 * <li><strong>directory</strong>: The directory containing the entry: e.g. "folder1/folder2"</li>
	 * <li><strong>size</strong>: The size of the entry in bytes</li>
	 * <li><strong>compressedSize</strong>: The compressed size of the entry in bytes</li>
	 * <li><strong>type</strong>: The type of the entry: file or directory</li>
	 * <li><strong>dateLastModified</strong>: The date the entry was last modified</li>
	 * <li><strong>crc</strong>: The CRC checksum of the entry</li>
	 * <li><strong>comment</strong>: The comment of the entry</li>
	 * <li><strong>isEncrypted</strong>: Whether the entry is encrypted</li>
	 * <li><strong>isCompressed</strong>: Whether the entry is compressed</li>
	 * <li><strong>isDirectory</strong>: Whether the entry is a directory</li>
	 * </ul>
	 * <li><strong>overwrite</strong>: Whether to overwrite the destination file(s) if it already exists when zipping/unzipping. Default is false. Actions: {@code zip, unzip}</li>
	 * <li><strong>prefix</strong>: The prefix to add to the files when zipping files, this is the directory name to store files in. Not used by default. Actions: {@code zip}</li>
	 * <li><strong>recurse</strong>: Whether to recurse into subdirectories when listing/zipping/unzipping. Default is true. Actions: {@code list, zip, unzip}</li>
	 * <li><strong>flatList</strong>: If false, the {@code list} action will return an array of structs with all kinds of information about the entries. If true, it will return a flat list of strings with the path of the entries. Default is false.
	 * Actions: {@code list}</li>
	 * <li><strong>source</strong>: The absolute path to the source directory to be zipped. Actions: {@code zip}</li>
	 * </ul>
	 *
	 * <h2>Delete Action</h2>
	 * <p>
	 * The delete action allows you to delete entries from a zip file. You can delete a single entry or multiple entries.
	 * <p>
	 * <h3>Attributes</h3>
	 * <ul>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be manipulated</li>
	 * <li><strong>entryPath</strong>: Zip entry path or an array of entry paths on which the action is performed. Can also be used with a <code>filter</code> attribute</li>
	 * <li><strong>filter</strong>: This can be a regular expression (*.txt) or a BoxLang Closure/Lambda ((path) => path.endsWith(".txt")) that will be used to filter the files</li>
	 * <li><strong>recurse</strong>: Whether to recurse into subdirectories when listing/zipping/unzipping. Default is true</li>
	 * </ul>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * // Using entries
	 * zip action="delete" file="/path/to/file.zip" entryPath="folder1/folder2/file.txt"
	 * // Using filters
	 * zip action="delete" file="/path/to/file.zip" filter="*.txt"
	 * </pre>
	 *
	 * <h2>List Action</h2>
	 * <p>
	 * The list action allows you to list the contents of a zip file. You can list all entries or filter the entries using a regular expression or a closure/lambda.
	 * The list action can return a flat list of strings with the path of the entries or an array of structs with all kinds of information about the entries.
	 * <p>
	 * <h3>Attributes</h3>
	 * <ul>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be manipulated</li>
	 * <li><strong>filter</strong>: This can be a regular expression (*.txt) or a BoxLang Closure/Lambda ((path) => path.endsWith(".txt")) that will be used to filter the files</li>
	 * <li><strong>recurse</strong>: Whether to recurse into subdirectories when listing. Default is true</li>
	 * <li><strong>flatList</strong>: If false, the list action will return an array of structs with all kinds of information about the entries. If true, it will return a flat list of strings with the path of the entries. Default is false</li>
	 * <li><strong>result</strong>: The name of the variable to store the result in when doing a list operation on. If not provided, we will use {@code bxzip}. The result is an array of structs</li>
	 * </ul>
	 * <p>
	 * <h3>Struct Entry</h3>
	 * <p>
	 * Each entry will be a struct with the following keys:
	 * </p>
	 * <ul>
	 * <li><strong>fullpath</strong>: The full path of the entry: e.g. "folder1/folder2/file.txt"</li>
	 * <li><strong>name</strong>: The file name of the entry: e.g. "file.txt"</li>
	 * <li><strong>directory</strong>: The directory containing the entry: e.g. "folder1/folder2"</li>
	 * <li><strong>size</strong>: The size of the entry in bytes</li>
	 * <li><strong>compressedSize</strong>: The compressed size of the entry in bytes</li>
	 * <li><strong>type</strong>: The type of the entry: file or directory</li>
	 * <li><strong>dateLastModified</strong>: The date the entry was last modified</li>
	 * <li><strong>crc</strong>: The CRC checksum of the entry</li>
	 * <li><strong>comment</strong>: The comment of the entry</li>
	 * <li><strong>isEncrypted</strong>: Whether the entry is encrypted</li>
	 * <li><strong>isCompressed</strong>: Whether the entry is compressed</li>
	 * <li><strong>isDirectory</strong>: Whether the entry is a directory</li>
	 * </ul>
	 *
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * // List all entries
	 * zip action="list" file="/path/to/file.zip"
	 * // List all entries that end with .txt
	 * zip action="list" file="/path/to/file.zip" filter="*.txt"
	 * // List all entries that end with .txt and store the result in a variable called myZip
	 * zip action="list" file="/path/to/file.zip" filter="*.txt" result="myZip"
	 * // List all entries that end with .txt and return a flat list of strings
	 * zip action="list" file="/path/to/file.zip" filter="*.txt" flatList="true"
	 * </pre>
	 *
	 * <h2>Read Action</h2>
	 * <p>
	 * The read action allows you to read the contents of a file inside a zip file. You can read the contents of a text file only. If you want to read the contents of a binary file, use the <code>readBinary</code> action.
	 * <p>
	 * <h3>Attributes</h3>
	 * <ul>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be read</li>
	 * <li><strong>entryPath</strong>: Zip entry path to read the content</li>
	 * <li><strong>charset</strong>: Valid Java Charset to use when reading the contents of a file inside a zip file. Default is the machine's default charset</li>
	 * <li><strong>variable</strong>: The name of the variable to store the read content in</li>
	 * </ul>
	 *
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * // Read the contents of a file inside a zip file to a variable called bxzip
	 * zip action="read" file="/path/to/file.zip" entryPath="folder1/folder2/file.txt"
	 * // Read the contents of a file inside a zip file and store the result in a variable called myContent
	 * zip action="read" file="/path/to/file.zip" entryPath="folder1/folder2/file.txt" variable="myContent"
	 * </pre>
	 *
	 * <h2>ReadBinary Action</h2>
	 * <p>
	 * The readBinary action allows you to read the contents of a binary file inside a zip file to a variable.
	 * <p>
	 * <h3>Attributes</h3>
	 * <ul>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be read</li>
	 * <li><strong>entryPath</strong>: Zip entry path to read the content</li>
	 * <li><strong>variable</strong>: The name of the variable to store the binary content in</li>
	 * </ul>
	 *
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * // Read the contents of a binary file inside a zip file to a variable called bxzip
	 * zip action="readBinary" file="/path/to/file.zip" entryPath="folder1/folder2/file.jpg"
	 * // Read the contents of a binary file inside a zip file and store the result in a variable called myContent
	 * zip action="readBinary" file="/path/to/file.zip" entryPath="folder1/folder2/file.jpg" variable="myContent"
	 * </pre>
	 *
	 * <h2>Unzip Action</h2>
	 * <p>
	 * The unzip action allows you to uncompress a zip file. You can uncompress the entire zip file or filter the entries using a regular expression or a closure/lambda.
	 * If the destination directory does not exist, it will be created for you.
	 * <p>
	 * <h3>Attributes</h3>
	 * <ul>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be unzipped</li>
	 * <li><strong>destination</strong>: Absolute destination directory where the zip/gzip file will be extracted</li>
	 * <li><strong>filter</strong>: This can be a regular expression (*.txt) or a BoxLang Closure/Lambda ((path) => path.endsWith(".txt")) that will be used to filter the files</li>
	 * <li><strong>entryPath</strong>: Zip entry path or an array of entry paths on which the action is performed. Can also be used with a <code>filter</code> attribute</li>
	 * <li><strong>overwrite</strong>: Whether to overwrite the destination file(s) if it already exists when unzipping. Default is false</li>
	 * <li><strong>recurse</strong>: Whether to recurse into subdirectories when unzipping. Default is true</li>
	 * </ul>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * // Uncompress the entire zip file
	 * zip action="unzip" file="/path/to/file.zip" destination="/path/to/destination"
	 * // Uncompress the entire zip file and overwrite the destination files
	 * zip action="unzip" file="/path/to/file.zip" destination="/path/to/destination" overwrite="true"
	 * // Uncompress the entire zip file and overwrite the destination files and recurse into subdirectories
	 * zip action="unzip" file="/path/to/file.zip" destination="/path/to/destination" overwrite="true" recurse="false"
	 * // Uncompress the entire zip file and overwrite the destination files and recurse into subdirectories and filter the entries
	 * zip action="unzip" file="/path/to/file.zip" destination="/path/to/destination" overwrite="true" recurse="true" filter="*.txt"
	 * // Uncompress using a lambda filter
	 * zip action="unzip" file="/path/to/file.zip" destination="/path/to/destination" filter="(path) -> path.endsWith('.txt')"
	 * </pre>
	 *
	 * <h2>Zip Action</h2>
	 * <p>
	 * The zip action allows you to compress files into a zip file. You can compress the entire source directory or filter the entries using a regular expression or a closure/lambda.
	 * <p>
	 * <h3>Attributes</h3>
	 * <ul>
	 * <li><strong>file</strong>: Absolute filepath to the zip/gzip file to be created</li>
	 * <li><strong>source</strong>: The absolute path to the source directory to be zipped</li>
	 * <li><strong>filter</strong>: This can be a regular expression (*.txt) or a BoxLang Closure/Lambda ((path) => path.endsWith(".txt")) that will be used to filter the files</li>
	 * <li><strong>overwrite</strong>: Whether to overwrite the destination file(s) if it already exists when zipping. Default is false</li>
	 * <li><strong>prefix</strong>: The prefix to add to the files when zipping files, this is the directory name to store files in. Not used by default</li>
	 * <li><strong>recurse</strong>: Whether to recurse into subdirectories when zipping. Default is true</li>
	 * </ul>
	 * <p>
	 * Example:
	 *
	 * <pre>
	 * // Compress the entire source directory
	 * zip action="zip" file="/path/to/file.zip" source="/path/to/source"
	 * // Compress the entire source directory and overwrite the destination files
	 * zip action="zip" file="/path/to/file.zip" source="/path/to/source" overwrite="true"
	 * // Compress the entire source directory and overwrite the destination files and recurse into subdirectories
	 * zip action="zip" file="/path/to/file.zip" source="/path/to/source" overwrite="true" recurse="false"
	 * // Compress the entire source directory and overwrite the destination files and recurse into subdirectories and filter the entries
	 * zip action="zip" file="/path/to/file.zip" source="/path/to/source" overwrite="true" recurse="true" filter="*.txt"
	 * // Compress using a lambda filter
	 * zip action="zip" file="/path/to/file.zip" source="/path/to/source" filter="(path) -> path.endsWith('.txt')"
	 * </pre>
	 *
	 *
	 * @param context        The context in which the Component is being invoked
	 * @param attributes     The attributes to the Component
	 * @param body           The body of the Component
	 * @param executionState The execution state of the Component
	 *
	 * @attribute.action The action to take: delete, list, read, readBinary, unzip, zip
	 *
	 * @attribute.file Absolute filepath to the zip/gzip file to be manipulated. Actions: list, read, readBinary, unzip, zip
	 *
	 * @attribute.destination Absolute destination directory where the zip/gzip file will be to. If it doesn't exist it will be created for you. Actions: unzip
	 *
	 * @attribute.filter This can be a regular expression (*.txt) or a BoxLang Closure/Lambda ((path) => path.endsWith(".txt")) that will be used to filter the files. Actions: delete, list, unzip, zip
	 *
	 * @attribute.entryPath Zip entry path or an array of entry paths on which the action is performed. Actions: delete, list, read, readBinary, unzip
	 *
	 * @attribute.charset Valid Java Charset to use when reading the contents of a file inside a zip file. Default is the machine's default charset. Actions: read, readBinary
	 *
	 * @attribute.name The name of the variable to store the result in when doing a list operation on. If not provided, we will use bxzip. The result is an array of structs. Actions: list
	 *
	 * @attribute.overwrite Whether to overwrite the destination file(s) if it already exists when zipping/unzipping. Default is false. Actions: zip, unzip
	 *
	 * @attribute.prefix The prefix to add to the files when zipping files, this is the directory name to store files in. Not used by default. Actions: zip
	 *
	 * @attribute.recurse Whether to recurse into subdirectories when listing/zipping/unzipping. Default is true. Actions: list, zip, unzip
	 *
	 * @attribute.flatList If false, the list action will return an array of structs with all kinds of information about the entries. If true, it will return a flat list of strings with the path of the entries. Default is false. Actions: list
	 *
	 * @attribute.source The absolute path to the source directory to be zipped. Actions: zip
	 *
	 * @attribute.variable The name of the variable to store the read content in
	 */
	public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
		// How many zip params we have embedded as children
		executionState.put( Key.zipParams, new Array() );
		// Process the body
		BodyResult bodyResult = processBody( context, body );
		// IF there was a return statement inside our body, we early exit now
		if ( bodyResult.isEarlyExit() ) {
			return bodyResult;
		}

		// Setup basic variables
		String action = StringCaster.cast( attributes.getOrDefault( Key.action, "zip" ) );

		// Process the action
		switch ( action ) {
			case "delete" :
				// Delete a file from a zip file
				delete( context, attributes, executionState );
				break;
			case "list" :
				// List the contents of a zip file
				list( context, attributes, executionState );
				break;
			case "read" :
				// Read the contents of a file inside a zip file
				read( context, attributes, executionState );
				break;
			case "readBinary" :
				// Read the contents of a binary file inside a zip file
				readBinary( context, attributes, executionState );
				break;
			case "unzip" :
				// Uncompress a zip file
				unzip( context, attributes, executionState );
				break;
			case "zip" :
				// Compress files into a zip file
				zip( context, attributes, executionState );
				break;
			default :
				throw new BoxRuntimeException(
				    action + " is not a valid action for the Zip component. Valid actions are: delete, list, read, readBinary, unzip, zip"
				);
		}

		// Return the result
		return DEFAULT_RETURN;
	}

	/**
	 * Delete entries from a zip file
	 *
	 * @param context
	 * @param attributes
	 * @param executionState
	 */
	private void delete( IBoxContext context, IStruct attributes, IStruct executionState ) {
		// Required attributes: file
		String filepath = StringCaster.cast( attributes.get( Key.file ) );
		if ( filepath == null || filepath.isEmpty() ) {
			throw new BoxRuntimeException( "The file attribute is required for the delete action" );
		}

		// Optional attributes: filter, entryPath
		Object	filter		= attributes.get( Key.filter );
		Array	entryPaths	= toArrayOfEntryPaths( attributes.get( Key.entryPath ) );

		// Kawabunga!
		ZipUtil.deleteEntries( filepath, filter, entryPaths, context );
	}

	/**
	 * List the contents of a zip file
	 *
	 * @param context
	 * @param attributes
	 * @param executionState
	 */
	private void list( IBoxContext context, IStruct attributes, IStruct executionState ) {
		// Required attributes: file
		String source = StringCaster.cast( attributes.get( Key.file ) );
		if ( source == null || source.isEmpty() ) {
			throw new BoxRuntimeException( "The file attribute is required for the list action" );
		}

		// Optional attributes: filter, name, recurse, flatList
		Object	filter			= attributes.get( Key.filter );
		String	variableName	= StringCaster.cast( attributes.getOrDefault( Key._NAME, "bxzip" ) );
		boolean	recurse			= Boolean.TRUE.equals( attributes.get( Key.recurse ) );
		boolean	flatList		= Boolean.TRUE.equals( attributes.get( Key.flatList ) );

		// Kawabunga!
		Array	results;
		if ( flatList ) {
			results = ZipUtil.listEntriesFlat( source, filter, recurse, context );
		} else {
			results = ZipUtil.listEntries( source, filter, recurse, context );
		}

		// Store the results using the variable name
		ExpressionInterpreter.setVariable( context, variableName, results );
	}

	/**
	 * Read the contents of a file inside a zip file
	 *
	 * @param context
	 * @param attributes
	 * @param executionState
	 */
	private void read( IBoxContext context, IStruct attributes, IStruct executionState ) {
		// Required attributes: file, entryPath
		String	source		= StringCaster.cast( attributes.get( Key.file ) );
		String	entryPath	= StringCaster.cast( attributes.get( Key.entryPath ) );
		if ( source == null || source.isEmpty() ) {
			throw new BoxRuntimeException( "The file attribute is required for the read action" );
		}
		if ( entryPath == null || entryPath.isEmpty() ) {
			throw new BoxRuntimeException( "The entryPath attribute is required for the read action" );
		}

		// Optional attributes: charset, variable
		Object	charset		= attributes.get( Key.charset );
		String	variable	= StringCaster.cast( attributes.getOrDefault( Key.variable, "bxzip" ) );

		// Kawabunga!
		String	content;
		if ( charset == null ) {
			content = ZipUtil.readEntry( source, entryPath );
		} else {
			content = ZipUtil.readEntry( source, entryPath, StringCaster.cast( charset ) );
		}

		// Store the results using the variable name
		ExpressionInterpreter.setVariable( context, variable, content );
	}

	/**
	 * Read as Binary the contents of a file inside a zip file
	 *
	 * @param context
	 * @param attributes
	 * @param executionState
	 */
	private void readBinary( IBoxContext context, IStruct attributes, IStruct executionState ) {
		// Required attributes: file, entryPath
		String	source		= StringCaster.cast( attributes.get( Key.file ) );
		String	entryPath	= StringCaster.cast( attributes.get( Key.entryPath ) );
		if ( source == null || source.isEmpty() ) {
			throw new BoxRuntimeException( "The file attribute is required for the readAsBinary action" );
		}
		if ( entryPath == null || entryPath.isEmpty() ) {
			throw new BoxRuntimeException( "The entryPath attribute is required for the readAsBinary action" );
		}

		// Optional attributes: variable
		String	variable	= StringCaster.cast( attributes.getOrDefault( Key.variable, "bxzip" ) );

		// Kawabunga!
		byte[]	content		= ZipUtil.readBinaryEntry( source, entryPath );

		// Store the results using the variable name
		ExpressionInterpreter.setVariable( context, variable, content );
	}

	/**
	 * Uncompress a zip file
	 *
	 * @param context
	 * @param attributes
	 * @param executionState
	 */
	private void unzip( IBoxContext context, IStruct attributes, IStruct executionState ) {
		// required attributes: file, destination
		String	source		= StringCaster.cast( attributes.get( Key.file ) );
		String	destination	= StringCaster.cast( attributes.get( Key.destination ) );

		if ( source == null || source.isEmpty() ) {
			throw new BoxRuntimeException( "The file attribute is required for the unzip action" );
		}
		if ( destination == null || destination.isEmpty() ) {
			throw new BoxRuntimeException( "The destination attribute is required for the unzip action" );
		}

		// Optional attributes: filter, entryPath, overwrite, recurse
		Object	filter		= attributes.get( Key.filter );
		Array	entryPaths	= toArrayOfEntryPaths( attributes.get( Key.entryPath ) );
		boolean	overwrite	= Boolean.TRUE.equals( attributes.get( Key.overwrite ) );
		boolean	recurse		= Boolean.TRUE.equals( attributes.get( Key.recurse ) );

		// Kawabunga!
		ZipUtil.extractZip( source, destination, overwrite, recurse, filter, entryPaths, context );
	}

	/**
	 * Compress files into a zip file
	 *
	 * @param context
	 * @param attributes
	 * @param executionState
	 */
	private void zip( IBoxContext context, IStruct attributes, IStruct executionState ) {
		// Required attributes: file, source
		String	destinationFile	= StringCaster.cast( attributes.get( Key.file ) );
		String	source			= StringCaster.cast( attributes.get( Key.source ) );

		if ( destinationFile == null || destinationFile.isEmpty() ) {
			throw new BoxRuntimeException( "The file attribute is required for the zip action" );
		}
		if ( source == null || source.isEmpty() ) {
			throw new BoxRuntimeException( "The source attribute is required for the zip action" );
		}

		// Optional attributes: filter, overwrite, prefix, recurse
		Object	filter		= attributes.get( Key.filter );
		boolean	overwrite	= Boolean.TRUE.equals( attributes.get( Key.overwrite ) );
		boolean	recurse		= Boolean.TRUE.equals( attributes.get( Key.recurse ) );
		Object	prefix		= attributes.get( Key.prefix );

		// Kawabunga!
		ZipUtil.compressZip(
		    source,
		    destinationFile,
		    true,
		    overwrite,
		    prefix != null ? StringCaster.cast( prefix ) : null,
		    filter,
		    recurse,
		    context
		);
	}

	/**
	 * Converts the entryPath attribute to an Array of entry paths
	 *
	 * @param entryPath The entry path attribute
	 *
	 * @return An Array of entry paths
	 */
	private Array toArrayOfEntryPaths( Object entryPath ) {
		Array paths = new Array();
		if ( entryPath instanceof String ) {
			paths.add( entryPath );
		} else if ( entryPath instanceof Array ) {
			return ( Array ) entryPath;
		}
		return paths;
	}

}
