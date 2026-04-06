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
import java.net.URI;
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
import ortus.boxlang.runtime.bifs.BoxMemberExpose;
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
import ortus.boxlang.runtime.util.IBoxBinaryRepresentable;

public class BoxFile implements IType, IReferenceable, IBoxBinaryRepresentable {

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */
	/**
	 * Enum representing the file open modes
	 */
	public enum Mode {

		READ( "read" ),
		READBINARY( "readbinary" ),
		WRITE( "write" ),
		APPEND( "append" ),
		NONE( "none" );

		private final String value;

		Mode( String value ) {
			this.value = value;
		}

		public String getValue() {
			return this.value;
		}

		@Override
		public String toString() {
			return this.value;
		}

		public static Mode fromString( String mode ) {
			for ( Mode m : values() ) {
				if ( m.value.equalsIgnoreCase( mode ) ) {
					return m;
				}
			}
			throw new BoxRuntimeException( "Invalid file mode: [" + mode + "]. Valid modes are: read, readbinary, write, append" );
		}

		public boolean isReadMode() {
			return this == READ || this == READBINARY;
		}

		public boolean isWriteMode() {
			return this == WRITE || this == APPEND;
		}
	}

	/**
	 * Default charset for file operations
	 */
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
	 * The file mode for operations
	 */
	public Mode								mode;
	/**
	 * The file name (e.g. "file.txt")
	 */
	public String							filename;
	/**
	 * The absolute file path
	 */
	public String							filepath;
	/**
	 * The last modified date of the file
	 */
	public DateTime							lastModified;
	/**
	 * The parent directory of the file
	 */
	public String							directory;
	/**
	 * The size of the file in bytes
	 */
	public Long								size;
	/**
	 * The file status
	 */
	public String							status;
	/**
	 * The charset used for reading or writing the file
	 */
	public String							charset;
	/**
	 * Whether the file is opened in seekable mode
	 */
	public Boolean							seekable;
	/**
	 * Whether this BoxFile was implicitly cast from a string/path, indicating it should be closed after use in BIFs
	 */
	public boolean							implicitlyCast	= false;

	/**
	 * Metadata object
	 */
	public transient BoxMeta<?>				$bx;

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
	 * Constructor - creates a BoxFile reference without opening the file.
	 *
	 * @param file The file path
	 */
	public BoxFile( String file ) {
		this( Path.of( file ) );
	}

	/**
	 * Constructor - creates a BoxFile reference from a Path without opening the file.
	 *
	 * @param path The file path
	 */
	public BoxFile( Path path ) {
		this.mode		= Mode.NONE;
		this.path		= path;
		this.charset	= CHARSET_UTF8;
		this.seekable	= false;
		this.filename	= this.path.getFileName().toString();
		this.filepath	= this.path.toAbsolutePath().toString();
		this.directory	= this.path.getParent().toAbsolutePath().toString();
		this.size		= null;
		this.status		= null;
	}

	/**
	 * Constructor
	 *
	 * @param file The file to open
	 * @param mode The mode with which to open the file
	 *
	 * @deprecated Use {@link #BoxFile(String, Mode)} instead
	 */
	@Deprecated
	public BoxFile( String file, String mode ) {
		this( file, Mode.fromString( mode ), CHARSET_UTF8, null );
	}

	/**
	 * Constructor
	 *
	 * @param file The file to open
	 * @param mode The mode with which to open the file
	 */
	public BoxFile( String file, Mode mode ) {
		this( file, mode, null, null );
	}

	/**
	 * Constructor
	 *
	 * @param file     The file to open
	 * @param mode     The mode with which to open the file
	 * @param charset  The charset to use when reading or writing the file
	 * @param seekable Whether the file should be opened in seekable mode
	 *
	 * @deprecated Use {@link #BoxFile(String, Mode, String, Boolean)} instead
	 */
	@Deprecated
	public BoxFile( String file, String mode, String charset, Boolean seekable ) {
		this( file, Mode.fromString( mode ), charset, seekable );
	}

	/**
	 * Constructor
	 *
	 * @param file     The file to open
	 * @param mode     The mode with which to open the file
	 * @param charset  The charset to use when reading or writing the file
	 * @param seekable Whether the file should be opened in seekable mode
	 */
	public BoxFile( String file, Mode mode, String charset, Boolean seekable ) {
		// Resolve the path
		if ( file.toLowerCase().indexOf( "http" ) == 0 ) {
			try {
				URL fileURL = URI.create( file ).toURL();
				this.path = Path.of( fileURL.toURI() );
			} catch ( URISyntaxException e ) {
				throw new BoxRuntimeException( "The url [" + file + "] could not be parsed.  The reason was:" + e.getMessage() + "(" + e.getCause() + ")" );
			} catch ( MalformedURLException e ) {
				throw new BoxRuntimeException( "The url [" + file + "] could not be parsed.  The reason was:" + e.getMessage() + "(" + e.getCause() + ")" );
			}
		} else {
			this.path = Path.of( file );
		}
		this.mode		= Mode.NONE;
		this.charset	= CHARSET_UTF8;
		this.seekable	= false;
		this.filename	= this.path.getFileName().toString();
		this.filepath	= this.path.toAbsolutePath().toString();
		this.directory	= this.path.getParent().toAbsolutePath().toString();
		this.size		= null;
		this.status		= null;
		openAs( mode, charset, seekable );
	}

	/**
	 * Opens the file using the specified mode with default charset and auto-detected seekable.
	 *
	 * @param mode The mode with which to open the file (read, readbinary, write, append)
	 */
	public BoxFile openAs( Mode mode ) {
		return openAs( mode, null, null );
	}

	/**
	 * Opens the file using the specified mode, setting up the appropriate reader, writer, or byte channel.
	 *
	 * @param mode     The mode with which to open the file (read, readbinary, write, append)
	 * @param charset  The charset to use when reading or writing the file (null keeps current charset)
	 * @param seekable Whether the file should be opened in seekable mode (null auto-detects based on mode)
	 */
	public BoxFile openAs( Mode mode, String charset, Boolean seekable ) {
		if ( mode == Mode.NONE ) {
			return this;
		}
		// If already open in the same mode, it's a no-op
		if ( this.mode == mode ) {
			return this;
		}
		// If already open in a compatible mode, it's a no-op
		if ( this.mode != Mode.NONE ) {
			if ( isCompatibleMode( this.mode, mode ) ) {
				return this;
			}
			throw new BoxRuntimeException(
			    "The file [" + this.path.toAbsolutePath().toString() + "] is already opened in [" + this.mode + "] mode. Close it before opening in [" + mode
			        + "] mode." );
		}
		this.mode = mode;
		if ( charset != null ) {
			this.charset = charset;
		}
		if ( seekable != null ) {
			this.seekable = seekable;
		} else {
			switch ( mode ) {
				case READ :
				case READBINARY :
					this.seekable = true;
					break;
				default :
					this.seekable = false;
			}
		}
		try {
			switch ( mode ) {
				case READ :
					if ( !Files.isReadable( this.path ) ) {
						throw new BoxRuntimeException( "The file [" + this.path.toAbsolutePath().toString() + "] does not exist or is not readable." );
					}
					this.size = Files.size( this.path );
					this.reader = Files.newBufferedReader( this.path, Charset.forName( this.charset ) );
					break;
				case READBINARY :
					if ( !Files.isReadable( this.path ) ) {
						throw new BoxRuntimeException( "The file [" + this.path.toAbsolutePath().toString() + "] does not exist or is not readable." );
					}
					if ( !FileSystemUtil.isBinaryFile( this.filepath ) ) {
						throw new BoxRuntimeException( "The file [" + this.path.toAbsolutePath().toString() + "] is not a binary file." );
					}
					this.size = Files.size( this.path );
					this.byteChannel = Files.newByteChannel( this.path, StandardOpenOption.READ );
					break;
				case WRITE :
					if ( this.seekable ) {
						this.byteChannel = Files.newByteChannel( this.path,
						    Files.exists( this.path ) ? StandardOpenOption.WRITE : StandardOpenOption.CREATE_NEW );
					} else {
						this.writer = Files.newBufferedWriter( this.path,
						    Files.exists( this.path ) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW );
					}
					break;
				case APPEND :
					this.writer = Files.newBufferedWriter( this.path, Files.exists( this.path ) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE_NEW );
					break;
				case NONE :
					break;
			}
		} catch ( UnsupportedCharsetException e ) {
			throw new BoxRuntimeException( "The charset [" + this.charset + "] is invalid" );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return this;
	}

	/**
	 * Determines whether two file modes are compatible.
	 * Read modes (READ, READBINARY) are compatible with each other.
	 * Write modes (WRITE, APPEND) are compatible with each other.
	 *
	 * @param current   The current file mode
	 * @param requested The requested file mode
	 *
	 * @return true if the modes are compatible
	 */
	private boolean isCompatibleMode( Mode current, Mode requested ) {
		return ( current.isReadMode() && requested.isReadMode() ) || ( current.isWriteMode() && requested.isWriteMode() );
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
	public BoxFile seek( Integer offset ) {
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

	public BoxFile setLastModifiedTime( DateTime time ) {
		try {
			Files.setLastModifiedTime( this.path, FileTime.from( time.toInstant() ) );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		return this;
	}

	/**
	 * Retrieves the last modified time of a file
	 *
	 * @return
	 */
	@BoxMemberExpose
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
	public BoxFile writeLine( String content ) {
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
	public BoxFile append( String content ) {
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
	@BoxMemberExpose
	public void close() {
		try {
			if ( this.reader != null ) {
				reader.close();
				this.reader = null;
			}
			if ( this.writer != null ) {
				writer.close();
				this.writer = null;
			}
			if ( this.byteChannel != null ) {
				this.byteChannel.close();
				this.byteChannel = null;
			}
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
		this.mode = Mode.NONE;
	}

	/**
	 * To string which returns full file path
	 * 
	 * @return The string representation of this file object
	 */
	@Override
	public String toString() {
		return this.filepath;
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
	@Override
	public String asString() {
		return this.toString();
	}

	/**
	 * Get the BoxLang type name for this type
	 * 
	 * @return The BoxLang type name
	 */
	@Override
	public String getBoxTypeName() {
		return "File";
	}

	@Override
	public BoxMeta<?> getBoxMeta() {
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

	/**
	 * Get the byte array representation of this file.
	 *
	 * @return The byte array representation of this file.
	 */
	@Override
	public byte[] toByteArray() {
		try {
			return Files.readAllBytes( this.path );
		} catch ( IOException e ) {
			throw new BoxIOException( e );
		}
	}

}
