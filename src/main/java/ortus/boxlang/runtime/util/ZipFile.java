package ortus.boxlang.runtime.util;

import java.nio.file.Path;

public class ZipFile {

	// Internal state
	private Path sourcePath;
	private Path targetPath;

	/**
	 * Defines the source file or directory to zip
	 *
	 * @param source The source path
	 * @return This ZipFile instance for chaining
	 */
	public ZipFile source( Path source ) {
		this.sourcePath = source;
		return this;
	}

	/**
	 * Defines the destination zip file
	 *
	 * @param target The zip file path
	 * @return This ZipFile instance for chaining
	 */
	public ZipFile to( Path target ) {
		this.targetPath = target;
		return this;
	}

	/**
	 * Executes the compression
	 */
	public void compress() {
		// Week 3: logic goes here
	}
}
