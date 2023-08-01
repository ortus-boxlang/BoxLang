package ortus.boxlang.parser

import java.io.File

open class BaseTest {
	protected val testboxDirectory = requireNotNull(System.getProperty("testboxdir"))
	protected val contentboxDirectory = requireNotNull(System.getProperty("contentboxdir"))
	protected val coldboxDirectory = requireNotNull(System.getProperty("coldboxdir"))

	protected fun scanForFiles(pathname: String, extensions: Set<String>, exclude: List<String> = emptyList()): List<File> {
		val fileList = mutableListOf<File>()
		val directory = File(pathname)
		require(directory.exists()) { "Directory not existing: ${directory.canonicalFile.absolutePath}" }
		require(directory.isDirectory)
		require(directory.canRead())

		directory.walk().forEach { file ->
			when {
				file.isFile && file.extension in extensions -> {
					if (exclude.isNotEmpty()) {
						if (!exclude.contains(file.name)) {
							fileList.add(file)
						}
					} else {
						fileList.add(file)
					}
				}
			}
		}
		return fileList
	}
}