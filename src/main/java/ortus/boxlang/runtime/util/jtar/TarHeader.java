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
import java.nio.charset.StandardCharsets;

/** TAR header representation. */
public class TarHeader {

	public static final int		NAMELEN			= 100, MODELEN = 8, UIDLEN = 8, GIDLEN = 8, SIZELEN = 12, MODTIMELEN = 12, CHKSUMLEN = 8;
	public static final byte	LF_NORMAL		= '0', LF_LINK = '1', LF_SYMLINK = '2', LF_DIR = '5', LF_GNU_LONGNAME = 'L', LF_PAX_EXTENDED = 'x',
	    LF_PAX_GLOBAL = 'g';
	public static final String	USTAR_MAGIC		= "ustar  ";
	public static final int		USTAR_MAGICLEN	= 8, USTAR_USER_NAMELEN = 32, USTAR_GROUP_NAMELEN = 32, USTAR_DEVLEN = 8, USTAR_FILENAME_PREFIX = 155;
	public StringBuilder		name			= new StringBuilder(), linkName = new StringBuilder(), magic = new StringBuilder( USTAR_MAGIC );
	public StringBuilder		userName		= new StringBuilder(), groupName = new StringBuilder(), namePrefix = new StringBuilder();
	public int					mode, userId, groupId, checkSum, devMajor, devMinor;
	public long					size, modTime;
	public byte					typeflag;

	/**
	 * Parses a TAR string field.
	 * 
	 * @param header The encoded header
	 * @param offset The field offset
	 * @param length The field length
	 * 
	 * @return The decoded string
	 */
	public static StringBuilder parseString( byte[] header, int offset, int length ) {
		int end = offset;
		while ( end < offset + length && header[ end ] != 0 )
			end++;
		return new StringBuilder( new String( header, offset, end - offset, StandardCharsets.UTF_8 ) );
	}

	/**
	 * Writes a TAR string field.
	 * 
	 * @param value  The value to write
	 * @param buffer The destination buffer
	 * @param offset The field offset
	 * @param length The field length
	 * 
	 * @return The offset after the field
	 */
	public static int getStringBytes( StringBuilder value, byte[] buffer, int offset, int length ) {
		byte[]	bytes	= value.toString().getBytes( StandardCharsets.UTF_8 );
		int		count	= Math.min( bytes.length, length );
		System.arraycopy( bytes, 0, buffer, offset, count );
		for ( int i = count; i < length; i++ )
			buffer[ offset + i ] = 0;
		return offset + length;
	}

	/**
	 * Creates a TAR header for a file or directory.
	 * 
	 * @param entryName   The archive entry name
	 * @param size        The entry size
	 * @param modTime     The modification time in seconds
	 * @param directory   Whether the entry is a directory
	 * @param permissions The TAR permission mode
	 * 
	 * @return The initialized TAR header
	 */
	public static TarHeader createHeader( String entryName, long size, long modTime, boolean directory, int permissions ) {
		TarHeader	header	= new TarHeader();
		String		name	= TarUtils.trim( entryName.replace( File.separatorChar, '/' ), '/' );
		int			split	= name.length() > 100 ? name.lastIndexOf( '/' ) : -1;
		if ( split > 0 ) {
			header.namePrefix	= new StringBuilder( name.substring( 0, split ) );
			name				= name.substring( split + 1 );
		}
		header.name		= new StringBuilder( name );
		header.mode		= permissions;
		header.typeflag	= directory ? LF_DIR : LF_NORMAL;
		header.size		= directory ? 0 : size;
		header.modTime	= modTime;
		return header;
	}
}
