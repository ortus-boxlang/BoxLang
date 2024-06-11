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
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.NoSuchElementException;

import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.bifs.MemberDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.dynamic.IReferenceable;
import ortus.boxlang.runtime.dynamic.casters.LongCaster;
import ortus.boxlang.runtime.interop.DynamicInteropService;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.exceptions.BoxIOException;
import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;
import ortus.boxlang.runtime.types.meta.BoxMeta;
import ortus.boxlang.runtime.types.meta.GenericMeta;
import ortus.boxlang.runtime.util.FileSystemUtil;

public class File implements IType, IReferenceable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	private static final String				MODE_READ		= "read";
	private static final String				MODE_READBINARY	= "readbinary";
	private static final String				MODE_WRITE		= "write";
	private static final String				MODE_APPEND		= "append";
	private static final String				CHARSET_UTF8	= "utf-8";
	/**
	 * The the reader object when mode is read
	 */
	protected BufferedReader				reader			= null;
	/**
	 * The the writer object when mode is write
	 */
	protected BufferedWriter				writer			= null;
	/**
	 * The the writer object when mode is write and seekable is set to true
	 */
	protected SeekableByteChannel			byteChannel		= null;
	/**
	 * The file path object
	 */
	protected final Path					path;

	/**
	 * The OS line separator
	 */
	private static final String				lineSeparator	= System.getProperty( "line.separator" );

	/**
	 * fileOpen implementation public properties
	 */

	/**
	 * The file mode for operations
	 */
	public final String						mode;
	public String							filename;
	public String							filepath;
	public DateTime							lastModified;
	public String							directory;
	public Long								size;
	public String							status;
	public String							charset;
	public Boolean							seekable;

	/**
	 * Metadata object
	 */
	public BoxMeta							$bx;

	/**
	 * Function service
	 */
	private static final FunctionService	functionService	= BoxRuntime.getInstance().getFunctionService();

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
		this( file, mode, CHARSET_UTF8, mode.contains( "read" ) ? true : false );
	}

	/**
	 * Constructor
	 *
	 * @param file The file to open
	 * @param mode The mode with which to open the file
	 */
	public File( String file, String mode, String charset, Boolean seekable ) {
		this.mode = mode;
		// System.out.println( "HTTP Index:" + StringCaster.cast( file.toLowerCase().indexOf( "http" ) ) );
		if ( file.toLowerCase().indexOf( "http" ) == 0 ) {
			try {
				URL fileURL = new URL( file );
				this.path = Path.of( fileURL.toURI() );
			} catch ( URISyntaxException e ) {
				throw new BoxRuntimeException( "The url [" + file + "] could not be parsed.  The reason was:" + e.getMessage() + "(" + e.getCause() + ")" );
			} catch ( MalformedURLException e ) {
				throw new BoxRuntimeException( "The url [" + file + "] could not be parsed.  The reason was:" + e.getMessage() + "(" + e.getCause() + ")" );
			}
		} else {
			this.path = Path.of( file );
		}
		if ( charset != null ) {
			this.charset = charset;
		} else {
			this.charset = CHARSET_UTF8;
		}
		if ( seekable != null ) {
			this.seekable = seekable;
		} else {
			switch ( mode ) {
				case MODE_READ :
				case MODE_READBINARY :
					this.seekable = true;
					break;
				default :
					this.seekable = false;
			}
		}
		filename	= path.getFileName().toString();
		filepath	= path.toAbsolutePath().toString();
		directory	= path.getParent().toAbsolutePath().toString();
		try {
			switch ( mode ) {
				case MODE_READ :
					if ( !Files.isReadable( path ) ) {
						throw new BoxRuntimeException( "The file [" + path.toAbsolutePath().toString() + "] does not exist or is not readable." );
					}
					this.size = Files.size( path );
					this.reader = Files.newBufferedReader( path, Charset.forName( charset ) );
					break;
				case MODE_READBINARY :
					if ( !Files.isReadable( path ) ) {
						throw new BoxRuntimeException( "The file [" + path.toAbsolutePath().toString() + "] does not exist or is not readable." );
					}
					if ( !FileSystemUtil.isBinaryFile( file ) ) {
						throw new BoxRuntimeException( "The file [" + path.toAbsolutePath().toString() + "] is not a binary file." );
					}
					this.size = Files.size( path );
					this.byteChannel = Files.newByteChannel( path, StandardOpenOption.READ );
					break;
				case MODE_WRITE :
					if ( this.seekable ) {
						this.byteChannel = Files.newByteChannel( path, Files.exists( path ) ? StandardOpenOption.WRITE : StandardOpenOption.CREATE_NEW );
					} else {
						this.writer = Files.newBufferedWriter( path, Files.exists( path ) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW );
					}
					break;
				case MODE_APPEND :
					this.writer = Files.newBufferedWriter( path, Files.exists( path ) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW );
					break;
				default :
					throw new BoxRuntimeException( "The mode provided, [" + mode + "] for this file [" + filepath + "] is not valid" );

			}
		} catch ( UnsupportedCharsetException e ) {
			throw new BoxRuntimeException( "The charset [" + charset + "] is invalid" );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Determines whether the reader is at EOF
	 *
	 * @return
	 */
	public Boolean isEOF() {
		Boolean isEOF = false;
		if ( this.byteChannel != null ) {
			try {
				return this.byteChannel.position() == this.byteChannel.size();
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		}
		if ( this.reader != null ) {
			try {
				this.reader.mark( 2 );
				isEOF = this.reader.read() == -1l;
				this.reader.reset();
			} catch ( IOException e ) {
				isEOF = true;
			}
		} else {
			throw new BoxRuntimeException( "This file object is in write or append mode.  Unable to determine EOF." );
		}
		return isEOF;
	}

	/**
	 * Reads the next line in a file
	 *
	 * @return the result of the line read
	 */
	public String readLine() {
		if ( this.reader == null ) {
			throw new BoxRuntimeException( "This file object was not opened in read mode" );
		}
		try {
			return this.reader.readLine();
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/*
	 * Reads a specified number of bytes from a file
	 */
	public Object read( Integer len ) {
		try {
			if ( this.reader != null ) {
				CharBuffer buffer = CharBuffer.allocate( len );
				this.reader.read( buffer );
				return buffer.toString();
			} else if ( this.byteChannel != null ) {
				ByteBuffer buffer = ByteBuffer.allocate( len );
				this.byteChannel.read( ByteBuffer.allocate( len ) );
				return ( byte[] ) buffer.array();
			} else {
				throw new BoxRuntimeException( "This file object was not opened in read mode" );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 *
	 * @param offset The number of characters to offset from the current position in the reader or writer
	 *
	 * @return File this object
	 */
	public File seek( Integer offset ) {
		if ( !this.seekable ) {
			throw new BoxRuntimeException(
			    "This file instance was opened with the seekable argument set to false. This operation for file [" + filepath + "] is not allowed" );
		}
		if ( this.byteChannel != null ) {
			try {
				this.byteChannel.position( LongCaster.cast( offset ) );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			}
		} else {
			try {
				this.reader.skip( offset );
			} catch ( IOException e ) {
				throw new BoxIOException( e );
			} catch ( NullPointerException e ) {
				throw new BoxRuntimeException( "This file object was not opened in read mode" );
			}
		}
		return this;
	}

	public File setLastModifiedTime( DateTime time ) {
		try {
			Files.setLastModifiedTime( this.path, FileTime.from( time.toInstant() ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return this;
	}

	public DateTime getLastModifedTime() {
		try {
			return new DateTime( Files.getLastModifiedTime( this.path ).toInstant() );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Writes a line to a file. If the file is not empty it will insert a new line prior to the write
	 *
	 * @param content The content to be written
	 *
	 * @return This file object
	 */
	public File writeLine( String content ) {
		try {
			Boolean isNewFile = LongCaster.cast( Files.size( this.path ) ).equals( 0l );
			if ( this.byteChannel != null ) {
				if ( isNewFile ) {
					this.byteChannel.write( ByteBuffer.wrap( content.getBytes() ) );
				} else {
					this.byteChannel.write( ByteBuffer.wrap( ( lineSeparator + content ).getBytes() ) );
				}
			} else if ( this.writer != null ) {
				if ( !isNewFile ) {
					this.writer.newLine();
				}
				this.writer.append( content );
				// flush the writer so the file size changes
				this.writer.flush();
			} else {
				throw new BoxRuntimeException( "This file object is not writeable.  Operation disallowed." );
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return this;
	}

	/**
	 * Appends to a file from the current position
	 *
	 * @param content The content to be written
	 *
	 * @return
	 */
	public File append( String content ) {
		try {
			if ( this.byteChannel != null ) {
				this.byteChannel.write( ByteBuffer.wrap( content.getBytes() ) );

			} else {
				this.writer.append( content );
				// flush the writer so the file size changes
				this.writer.flush();
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return this;
	}

	/**
	 * Closes either the read or write stream
	 */
	public void close() {
		try {
			if ( this.reader != null ) {
				reader.close();
			}
			if ( this.writer != null ) {
				writer.close();
			}
			if ( this.byteChannel != null ) {
				this.byteChannel.close();
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

	/**
	 * Convenience method to return the protected path object
	 *
	 * @return
	 */
	public Path getPath() {
		return this.path;
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
		DynamicInteropService.setField( this, key.getName().toLowerCase(), value );
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
		try {
			return DynamicInteropService.getField( this, key.getName().toLowerCase() ).get();
		} catch ( NoSuchElementException e ) {
			throw new BoxRuntimeException(
			    "The property [" + key.getName() + "] does not exist or is not public in the class [" + this.getClass().getSimpleName() + "]." );
		}
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

		return DynamicInteropService.invoke( context, this, name.getName(), safe, positionalArguments );
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

		MemberDescriptor memberDescriptor = functionService.getMemberMethod( name, BoxLangType.FILE );
		if ( memberDescriptor != null ) {
			return memberDescriptor.invoke( context, this, namedArguments );
		}

		return DynamicInteropService.invoke( context, this, name.getName(), safe, namedArguments );
	}

}
