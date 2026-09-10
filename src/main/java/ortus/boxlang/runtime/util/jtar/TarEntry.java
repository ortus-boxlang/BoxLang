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
 *
 * Vendored and adapted from https://github.com/xiaoxindada/jtar.
 * Original JTar copyright 2012 Kamran Zafar; Apache License 2.0.
 */
package ortus.boxlang.runtime.util.jtar;

import java.io.File;
import java.util.Date;

/** TAR archive entry. */
public class TarEntry {

	protected File		file;
	protected TarHeader	header;

	/**
	 * Creates an entry from a file and archive name.
	 * 
	 * @param file      The source file
	 * @param entryName The archive entry name
	 */
	public TarEntry( File file, String entryName ) {
		this.file = file;
		this.extractTarHeader( entryName );
	}

	/**
	 * Creates an entry from a TAR header buffer.
	 * 
	 * @param headerBuffer The encoded header
	 */
	public TarEntry( byte[] headerBuffer ) {
		this.header = new TarHeader();
		this.parseTarHeader( headerBuffer );
	}

	/**
	 * Creates an entry from an existing header.
	 * 
	 * @param header The TAR header
	 */
	public TarEntry( TarHeader header ) {
		this.header = header;
	}

	/** @return The logical archive entry name. */
	public String getName() {
		return header.namePrefix.length() == 0 ? header.name.toString() : header.namePrefix + "/" + header.name;
	}

	/**
	 * Replaces the logical entry name after processing an extended TAR header.
	 *
	 * @param name The logical entry name
	 */
	public void setName( String name ) {
		header.namePrefix	= new StringBuilder();
		header.name			= new StringBuilder( name );
	}

	/**
	 * Returns the TAR type flag.
	 *
	 * @return The TAR type flag
	 */
	public byte getTypeFlag() {
		return header.typeflag;
	}

	/**
	 * Returns the underlying TAR header.
	 *
	 * @return The TAR header
	 */
	public TarHeader getHeader() {
		return header;
	}

	/** @return The source file, if present. */
	public File getFile() {
		return file;
	}

	/** @return The entry size in bytes. */
	public long getSize() {
		return header.size;
	}

	/** @param size The entry size in bytes. */
	public void setSize( long size ) {
		header.size = size;
	}

	/** @return Whether this entry is a directory. */
	public boolean isDirectory() {
		return header.typeflag == TarHeader.LF_DIR || header.name.toString().endsWith( "/" );
	}

	/** @return Whether this entry is a symbolic or hard link. */
	public boolean isLink() {
		return header.typeflag == TarHeader.LF_LINK || header.typeflag == TarHeader.LF_SYMLINK;
	}

	/** @param entryName The archive entry name. */
	public void extractTarHeader( String entryName ) {
		header = TarHeader.createHeader( entryName, file.length(), file.lastModified() / 1000, file.isDirectory(), PermissionUtils.permissions( file ) );
	}

	/** @param output The destination header buffer. */
	public void writeEntryHeader( byte[] output ) {
		int offset = 0;
		offset	= TarHeader.getStringBytes( header.name, output, offset, TarHeader.NAMELEN );
		offset	= Octal.getOctalBytes( header.mode, output, offset, TarHeader.MODELEN );
		offset	= Octal.getOctalBytes( header.userId, output, offset, TarHeader.UIDLEN );
		offset	= Octal.getOctalBytes( header.groupId, output, offset, TarHeader.GIDLEN );
		offset	= Octal.getOctalBytes( header.size, output, offset, TarHeader.SIZELEN );
		offset	= Octal.getOctalBytes( header.modTime, output, offset, TarHeader.MODTIMELEN );
		int checksumOffset = offset;
		for ( int i = 0; i < TarHeader.CHKSUMLEN; i++ )
			output[ offset++ ] = ' ';
		output[ offset++ ]	= header.typeflag;
		offset				= TarHeader.getStringBytes( header.linkName, output, offset, TarHeader.NAMELEN );
		offset				= TarHeader.getStringBytes( header.magic, output, offset, TarHeader.USTAR_MAGICLEN );
		offset				= TarHeader.getStringBytes( header.userName, output, offset, TarHeader.USTAR_USER_NAMELEN );
		offset				= TarHeader.getStringBytes( header.groupName, output, offset, TarHeader.USTAR_GROUP_NAMELEN );
		offset				= Octal.getOctalBytes( header.devMajor, output, offset, TarHeader.USTAR_DEVLEN );
		offset				= Octal.getOctalBytes( header.devMinor, output, offset, TarHeader.USTAR_DEVLEN );
		TarHeader.getStringBytes( header.namePrefix, output, offset, TarHeader.USTAR_FILENAME_PREFIX );
		long checksum = 0;
		for ( byte value : output )
			checksum += value & 0xFF;
		Octal.getCheckSumOctalBytes( checksum, output, checksumOffset, TarHeader.CHKSUMLEN );
	}

	/** @param input The encoded header buffer. */
	public void parseTarHeader( byte[] input ) {
		int offset = 0;
		header.name			= TarHeader.parseString( input, offset, TarHeader.NAMELEN );
		offset				+= TarHeader.NAMELEN;
		header.mode			= ( int ) Octal.parseOctal( input, offset, TarHeader.MODELEN );
		offset				+= TarHeader.MODELEN;
		header.userId		= ( int ) Octal.parseOctal( input, offset, TarHeader.UIDLEN );
		offset				+= TarHeader.UIDLEN;
		header.groupId		= ( int ) Octal.parseOctal( input, offset, TarHeader.GIDLEN );
		offset				+= TarHeader.GIDLEN;
		header.size			= Octal.parseOctal( input, offset, TarHeader.SIZELEN );
		offset				+= TarHeader.SIZELEN;
		header.modTime		= Octal.parseOctal( input, offset, TarHeader.MODTIMELEN );
		offset				+= TarHeader.MODTIMELEN;
		header.checkSum		= ( int ) Octal.parseOctal( input, offset, TarHeader.CHKSUMLEN );
		offset				+= TarHeader.CHKSUMLEN;
		header.typeflag		= input[ offset++ ];
		header.linkName		= TarHeader.parseString( input, offset, TarHeader.NAMELEN );
		offset				+= TarHeader.NAMELEN;
		offset				+= TarHeader.USTAR_MAGICLEN + TarHeader.USTAR_USER_NAMELEN + TarHeader.USTAR_GROUP_NAMELEN + TarHeader.USTAR_DEVLEN * 2;
		header.namePrefix	= TarHeader.parseString( input, offset, TarHeader.USTAR_FILENAME_PREFIX );
	}

	@Override
	public boolean equals( Object object ) {
		return object instanceof TarEntry && getName().equals( ( ( TarEntry ) object ).getName() );
	}

	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	/** @return The entry modification time. */
	public Date getModTime() {
		return new Date( header.modTime * 1000 );
	}
}
