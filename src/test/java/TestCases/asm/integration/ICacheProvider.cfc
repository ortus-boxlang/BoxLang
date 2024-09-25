/**
 * Copyright Since 2005 ColdBox Framework by Luis Majano and Ortus Solutions, Corp
 * www.ortussolutions.com
 * ---
 *
 * The main interface for a CacheBox cache provider.  You need to implement all the methods in order for CacheBox to work correctly for the implementing cache provider.
 *
 * @author Luis Majano
 */
interface {

	/**
	 * Get the name of this cache
	 */
	function getName();

	/**
	 * Set the cache name
	 *
	 * @name The name to set
	 *
	 * @return ICacheProvider
	 */
	function setName( required name );

}
