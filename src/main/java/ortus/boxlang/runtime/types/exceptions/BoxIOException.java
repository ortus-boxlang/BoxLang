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
package ortus.boxlang.runtime.types.exceptions;

import java.io.IOException;
import java.nio.file.FileSystemException;

import ortus.boxlang.runtime.scopes.Key;

/**
 * Base exception for all custom exceptions thrown by the user
 */
public class BoxIOException extends BoxRuntimeException {

	public static final Key		ErrorCodeKey			= Key.of( "errorCode" );

	private static final String	ACCESS_DENIED			= "AccessDeniedException";
	private static final String	ATOMIC_MOVE_DENIED		= "AtomicMoveNotSupportedException";
	private static final String	FILE_ALREADY_EXISTS		= "FileAlreadyExistsException";
	private static final String	FILE_NOT_FOUND			= "FileNotFoundException";
	private static final String	DIRECTORY_NOT_EMPTY		= "DirectoryNotEmptyException";
	private static final String	CLOSED_CHANNEL			= "ClosedChannelException";
	private static final String	INTERRUPTED				= "InterruptedIOException";
	private static final String	INTERRUPTED_BY_TIMEOUT	= "InterruptedByTimeoutException";
	private static final String	NO_SUCH_FILE			= "NoSuchFileException";
	private static final String	NOT_DIRECTORY			= "NotDirectoryException";
	private static final String	EOF						= "EOFException";
	private static final String	FILE_LOCK_INTERRUPTED	= "FileLockInterruptionException";
	/**
	 * Applies to type = "custom". String error code.
	 */
	public String				errorCode				= "";

	/**
	 * Constructor
	 *
	 * @param message   The message
	 * @param errorCode The errorCode
	 */
	public BoxIOException( IOException exception ) {
		super( parseExceptionMessage( exception ), exception );

	}

	private static String parseExceptionMessage( IOException exception ) {
		String				exceptionType	= exception.getClass().getSimpleName();
		String				suspectedFile	= exception.getMessage();
		FileSystemException	fileException	= null;
		switch ( exceptionType ) {
			case ACCESS_DENIED :
				fileException = ( FileSystemException ) exception;
				suspectedFile = fileException.getFile();
				return "Access was denied to the file [" + suspectedFile + "].";
			case ATOMIC_MOVE_DENIED :
				return "Atomic move operation on [" + suspectedFile + "] failed. The cause was:" + exception.getCause();
			case EOF :
				return "An end of file or end of stream has been reached unexpectedly on file [" + suspectedFile + "]";
			case FILE_LOCK_INTERRUPTED :
				return "A file lock exception occurred on file [" + suspectedFile + "] while another thread was waiting to acquire a file lock";
			case FILE_ALREADY_EXISTS :
				fileException = ( FileSystemException ) exception;
				suspectedFile = fileException.getFile();
				return "The file [" + suspectedFile + "] already exists.";
			case FILE_NOT_FOUND :
				return "The file [" + suspectedFile + "] could not be found or does not exist.";
			case NO_SUCH_FILE :
				fileException = ( FileSystemException ) exception;
				suspectedFile = fileException.getFile();
				return "The file [" + suspectedFile + "] could not be found or does not exist.";
			case NOT_DIRECTORY :
				fileException = ( FileSystemException ) exception;
				suspectedFile = fileException.getFile();
				return "The file [" + suspectedFile + "] is not a directory.";
			case DIRECTORY_NOT_EMPTY :
				return "The directory [" + suspectedFile + "] is not empty.";
			case CLOSED_CHANNEL :
				return "Could not perform the requested operation on the file [" + suspectedFile + "]. The channel is closed.";
			case INTERRUPTED :
				return "Operation failed on the file [" + suspectedFile + "]. The request was interrupted.";
			case INTERRUPTED_BY_TIMEOUT :
				return "Operation failed on file [" + suspectedFile + "]. A timeout occurred.";
			default :
				return "An unknown exception occurred" + exception.getMessage() + ": " + exception.getCause();
		}

	}

}
