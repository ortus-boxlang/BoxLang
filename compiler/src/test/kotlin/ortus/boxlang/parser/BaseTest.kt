package ortus.boxlang.parser

open class BaseTest {
	protected val testboxDirectory = requireNotNull(System.getProperty("testboxdir"))
	protected val contentboxDirectory = requireNotNull(System.getProperty("contentboxdir"))
	protected val coldboxDirectory = requireNotNull(System.getProperty("coldboxdir"))
}