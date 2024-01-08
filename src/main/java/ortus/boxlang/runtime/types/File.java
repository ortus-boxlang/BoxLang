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
package ortus.boxlang.runtime.types;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.IntegerCaster;
import ortus.boxlang.runtime.interop.DynamicJavaInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;

public class File implements IType, IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	private static final String		MODE_READ		= "read";
	private static final String		MODE_READBINARY	= "readbinary";
	private static final String		MODE_WRITE		= "write";
	private static final String		MODE_APPEND		= "append";
	/**
	 * The the reader object when mode is read
	 */
	protected final BufferedReader	reader;
	/**
	 * The the writer object when mode is read
	 */
	protected final BufferedWriter	writer;
	/**
	 * Write position used when fileSeek is invoked
	 */
	private Integer					offset;
	/**
	 * The file path object
	 */
	protected final Path			path;

	/**
	 * The OS line separator
	 */
	private static final String		lineSeparator	= System.getProperty( "line.separator" );

	/**
	 * fileOpen implementation public properties
	 */

	/**
	 * The file mode for operations
	 */
	public final String				mode;
	public String					filename;
	public String					filepath;
	public DateTime					lastModified;
	public String					directory;
	public Long						size;
	public String					status;

	/**
	 * Metadata object
	 */
	public BoxMeta					$bx;

	/**
	 * Function service
	 */
	private FunctionService			functionService;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param file The file to open
	 */
	public File( String file ) {
		this( file, "read" );
	}

	/**
	 * Constructor
	 *
	 * @param file The file to open
	 * @param mode The mode with which to open the file
	 */
	public File( String file, String mode ) {
		this.mode	= mode;
		this.path	= Path.of( file );
		filename	= path.getFileName().toString();
		filepath	= path.toAbsolutePath().toString();
		directory	= path.getParent().toAbsolutePath().toString();
		try {
			switch ( mode ) {
				case MODE_READ :
				case MODE_READBINARY :
					if ( !Files.isReadable( path ) ) {
						throw new BoxRuntimeException( "The file [" + path.toAbsolutePath().toString() + "] is not readable." );
					}
					this.size = Files.size( path );
					this.writer = null;
					this.reader = Files.newBufferedReader( path );
					break;
				case MODE_WRITE :
				case MODE_APPEND :
					if ( !Files.isWritable( path ) ) {
						throw new BoxRuntimeException( "The file [" + path.toAbsolutePath().toString() + "] is not writable." );
					}
					this.reader = null;
					this.writer = Files.newBufferedWriter( path );
					break;
				default :
					throw new BoxRuntimeException( "The mode provided, [" + mode + "] for this file [" + filepath + "] is not valid" );

			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	public Boolean isEOF() {
		Boolean isEOF = false;
		if ( this.reader != null ) {
			try {
				this.reader.mark( 2 );
				isEOF = this.reader.read() == -1l;
				this.reader.reset();
			} catch ( IOException e ) {
				isEOF = true;
			}
		}
		return isEOF;
	}

	public String readLine() {
		try {
			return this.reader.readLine();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	public File seek( Long offset ) {
		if ( this.writer != null ) {
			this.offset = IntegerCaster.cast( offset );
		} else {
			try {
				this.reader.skip( offset );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
		return this;
	}

	public File writeLine( String content ) {
		try {
			if ( offset != null ) {
				this.writer.write( lineSeparator + content, offset, lineSeparator.length() + content.length() );
			} else {
				this.writer.newLine();
				this.writer.append( content );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return this;
	}

	public void close() {
		try {
			if ( this.reader != null ) {
				reader.close();
			}
			if ( this.writer != null ) {
				writer.close();
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * --------------------------------------------------------------------------
	 * IType Interface Methods
	 * --------------------------------------------------------------------------
	 */
	/**
	 * Represent as string, or throw exception if not possible
	 *
	 * @return The string representation
	 */
	public String asString() {
		return this.toString();
	}

	public BoxMeta getBoxMeta() {
		if ( this.$bx == null ) {
			this.$bx = new GenericMeta( this );
		}
		return this.$bx;

	}

	/**
	 * --------------------------------------------------------------------------
	 * IReferenceable Interface Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Assign a value to a key
	 *
	 * @param key   The key to assign
	 * @param value The value to assign
	 */
	@Override
	public Object assign( IBoxContext context, Key key, Object value ) {
		DynamicJavaInteropService.setField( this, key.getName().toLowerCase(), value );
		return this;
	}

	/**
	 * Dereference this object by a key and return the value, or throw exception
	 *
	 * @param key  The key to dereference
	 * @param safe Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	@Override
	public Object dereference( IBoxContext context, Key key, Boolean safe ) {
		return DynamicJavaInteropService.getField( this, key.getName().toLowerCase() );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method) using positional arguments
	 *
	 * @param name                The key to dereference
	 * @param positionalArguments The positional arguments to pass to the invokable
	 * @param safe                Whether to throw an exception if the key is not found
	 *
	 * @return The requested object
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Object[] positionalArguments, Boolean safe ) {

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.FILE );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, positionalArguments );
		}

		return DynamicJavaInteropService.invoke( this, name.getName(), safe, positionalArguments );
	}

	/**
	 * Dereference this object by a key and invoke the result as an invokable (UDF, java method)
	 *
	 * @param name           The name of the key to dereference, which becomes the method name
	 * @param namedArguments The arguments to pass to the invokable
	 * @param safe           If true, return null if the method is not found, otherwise throw an exception
	 *
	 * @return The requested return value or null
	 */
	public Object dereferenceAndInvoke( IBoxContext context, Key name, Map<Key, Object> namedArguments, Boolean safe ) {

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.STRUCT );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicJavaInteropService.invoke( this, name.getName(), safe, namedArguments );
	}

}
