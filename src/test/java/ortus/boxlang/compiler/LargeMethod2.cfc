/**
*********************************************************************************
* Copyright Since 2017 CommandBox by Ortus Solutions, Corp
* www.ortussolutions.com
********************************************************************************
* @author Brad Wood
*
* I represent the configuration of a CF engine.  I am agnostic and don't contain any particular
* behavior for a specific engine.  Not all the data I store applies to every engine though.
* I am capable of reading and writing to a standard JSON format, but if you want to read or write
* to/from a specific engine's format, you'll need to create one of my subclasses
*/
component accessors="true" {

	// ----------------------------------------------------------------------------------------
	// Depdendency Injections
	// ----------------------------------------------------------------------------------------

	property name='wirebox' inject='wirebox';
	property name='Util' inject='Util@cfconfig-services';
	property name='JSONPrettyPrint' inject='JSONPrettyPrint@JSONPrettyPrint';

	// ----------------------------------------------------------------------------------------
	// Properties for the internal workings
	// ----------------------------------------------------------------------------------------

	// The config file to read/write from/to
	// - For adobe, it's <installDir>/cfusion
	// - For Railo/Lucee, it's the server context or web context folder
	// - For BoxLang, it's the config folder inside the server home
	// - For generic JSON config, it's just the folder you want to read/write from
	property name='CFHomePath' type='string';

	// The name of the format this config provider can handle. "adobe", "lucee", or "railo"
	property name='format' type='string';
	// A semver range that covers the version of the formats that are covered.
	property name='version' type='string';

	// ----------------------------------------------------------------------------------------
	// CF Config properties that map to the CF engines
	// ----------------------------------------------------------------------------------------

	// One of the strings "never", "once", "always"
	property name='inspectTemplate' type='string' _isCFConfig=true;
	// Number of templates to cache
	property name='templateCacheSize' type='numeric' _isCFConfig=true;
	// Number of queries to keep in cache
	property name='queryCacheSize' type='numeric' _isCFConfig=true;
	// true/false When checked, at server level internal cache is used to store cached queries. By default, cached queries are stored in QUERY region supported by Ehcache.
	// Adobe-only
	property name='QueryInternalCacheEnabled' type='boolean' _isCFConfig=true;
	// True/false
	property name='componentCacheEnabled' type='boolean' _isCFConfig=true;
	// True/false
	property name='saveClassFiles' type='boolean' _isCFConfig=true;
	// True/false
	property name='UDFTypeChecking' type='boolean' _isCFConfig=true;
	// true/false
	property name='nullSupport' type='boolean' _isCFConfig=true;
	// true/false
	property name='dotNotationUpperCase' type='boolean' _isCFConfig=true;
	// true/false
	property name='handleUnquotedAttributeValueAsString' type='boolean' _isCFConfig=true;
	// true/false
	property name='suppressWhitespaceBeforecfargument' type='string' _isCFConfig=true;
	// One of the strings "standard", "small", "strict"
	property name='scopeCascading' type='string' _isCFConfig=true;
	// True/false
	property name='searchResultsets' type='boolean' _isCFConfig=true;

	property name='baseComponent' type='string' _isCFConfig=true;

	// EHcache (0), jcs (1), Redis (2), memcached (3)
	property name='serverCacheType' type='string' _isCFConfig=true;

	// Redis host
	property name='redisCacheStorageHost' type='string' _isCFConfig=true;
	// Redis port
	property name='redisCacheStoragePort' type='numeric' _isCFConfig=true;
	// Redis password
	property name='redisCacheStoragePassword' type='string' _isCFConfig=true;
	// true/false
	property name='redisCacheStorageIsSSL' type='boolean' _isCFConfig=true;
	
	

	// Charts can be cached either in memory or to disk. In memory caching is faster, but more memory intensive. (0 = memory cache, 1 = disk cache)
	property name='chartCacheType' type='numeric' _isCFConfig=true;
	// Time-to-Live of each chart in seconds
	property name='chartCacheTTL' type='numeric' _isCFConfig=true;
	// Maximum number of cached images
	property name='chartCacheSize' type='numeric' _isCFConfig=true;
	// Disk cache location.  When caching to disk, specifies the directory in which to store the generated charts.
	property name='chartCacheDiskLocation' type='string' _isCFConfig=true;

	// Ex: en_US
	property name='thisLocale' type='string' _isCFConfig=true;
	// Ex: 	America/Chicago
	property name='thisTimeZone' type='string' _isCFConfig=true;
	// Ex: 	pool.ntp.org
	property name='timeServer' type='string' _isCFConfig=true;
	// true/false
	property name='useTimeServer' type='boolean' _isCFConfig=true;

	// Ex: windows-1252 (Lucee: Default character used to read templates (*.cfm and *.cfc files))
	property name='templateCharset' type='string' _isCFConfig=true;
	// Ex: UTF-8 (Lucee: Default character set for output streams, form-, url-, and cgi scope variables and reading/writing the header)
	property name='webCharset' type='string' _isCFConfig=true;
	// Ex: windows-1252 (Default character set for reading from/writing to various resources)
	property name='resourceCharset' type='string' _isCFConfig=true;

	// One of the strings "cfml", "j2ee"
	property name='sessionType' type='string' _isCFConfig=true;
	// True/false
	property name='mergeURLAndForm' type='boolean' _isCFConfig=true;
	// True/false
	property name='applicationManagement' type='boolean' _isCFConfig=true;
	// True/false
	property name='sessionManagement' type='boolean' _isCFConfig=true;
	// True/false
	property name='clientManagement' type='boolean' _isCFConfig=true;
	// True/false
	property name='domainCookies' type='boolean' _isCFConfig=true;
	// True/false
	property name='clientCookies' type='boolean' _isCFConfig=true;

	// Cookie Timeout - Number of seconds
	property name='sessionCookieTimeout' type='numeric' _isCFConfig=true;
	// True/false
	property name='sessionCookieHTTPOnly' type='boolean' _isCFConfig=true;
	// True/false
	property name='sessionCookieSecure' type='boolean' _isCFConfig=true;
	// Disable updating ColdFusion internal cookies using ColdFusion tags/functions - True/false
	property name='sessionCookieDisableUpdate' type='boolean' _isCFConfig=true;
	// Cookie Samesite default value - Strict, Lax, None, or empty string
	property name='sessionCookieSamesite' type='string' _isCFConfig=true;


	// One of the strings "classic", "modern"
	property name='localScopeMode' type='string' _isCFConfig=true;
	// True/false
	property name='CGIReadOnly' type='string' _isCFConfig=true;
	// Timespan Ex: 0,5,30,0
	property name='sessionTimeout' type='string' _isCFConfig=true;
	// Timespan Ex: 0,5,30,0
	property name='applicationTimeout' type='string' _isCFConfig=true;
	// Timespan Ex: 0,5,30,0
	property name='sessionMaximumTimeout' type='string' _isCFConfig=true;
	// Timespan Ex: 0,5,30,0
	property name='applicationMaximumTimeout' type='string' _isCFConfig=true;

	// One of the strings "none", "mixed", "modern", "classic"
	property name='applicationListener' type='string' _isCFConfig=true;
	/* One of the strings
	* "curr2root" - Current dir to web root (Lucee and Adobe [option 2])
	* "curr" - Current dir only (Lucee only)
	* "root" - Only in web root (Lucee only)
	* "currorroot" -  Current dir or web root (Lucee and Adobe [option 3])
	* "curr2driveroot" - Current dir to drive root (Adobe only [option 1])
	*/
	property name='applicationMode' type='string' _isCFConfig=true;

	// Timespan Ex: 0,5,30,0
	property name='clientTimeout' type='string' _isCFConfig=true;
	// One of the strings "memory", "file", "cookie", <cache-name>, <datasource-name>
	property name='sessionStorage' type='string' _isCFConfig=true;
	// One of the strings "memory", "file", "cookie", <cache-name>, <datasource-name>, "Registry"
	property name='clientStorage' type='string' _isCFConfig=true;
	// Number of minutes between client storage purge.  Not to be less tham 30 minutes.
	property name='clientStoragePurgeInterval' type='numeric' _isCFConfig=true;
	// A struct of valid client storage locations including registry, cookie, and any configured datasources. Only used by Adobe.
	property name='clientStorageLocations' type='struct' _isCFConfig=true;
	// TODO: Add functions/commands to manage this manually.

	// One of the strings "memory", "redis".  Adobe use only.
	property name='sessionStorageLocation' type='string' _isCFConfig=true;
	// Passowrd for session storage.  Adobe use only.
	property name='sessionStoragePassword' type='string' _isCFConfig=true;
	// Host for session storage.  Adobe use only.
	property name='sessionStorageHost' type='string' _isCFConfig=true;
	// Timeout in ms for session storage.  Adobe use only.
	property name='sessionStorageTimeout' type='numeric' _isCFConfig=true;
	// Port for session storage  Adobe use only.
	property name='sessionStoragePort' type='numeric' _isCFConfig=true;


	// Timespan Ex: 0,5,30,0
	property name='requestTimeout' type='string' _isCFConfig=true;
	// True/false
	property name='requestTimeoutEnabled' type='boolean' _isCFConfig=true;

	// Blocked file extensions for CFFile uploads
	property name='blockedExtForFileUpload' type='string' _isCFConfig=true;
	// "none", "all" or a comma-delimited list with some combination of "cgi", "cookie", "form", "url".
	property name='scriptProtect' type='string' _isCFConfig=true;
	// True/false
	property name='perAppSettingsEnabled' type='boolean' _isCFConfig=true;
	// True/false
	property name='useUUIDForCFToken' type='boolean' _isCFConfig=true;
	// True/false
	property name='requestTimeoutInURL' type='boolean' _isCFConfig=true;
	// One of the strings "off", "simple", "smart"
	// for Lucee backwards compat, you can use "regular", "white-space", "white-space-pref" which map to the above options in the same order.
	// Adobe only has on and off so "simple" and "smart" both just map to the fetaure being on.
	property name='whitespaceManagement' type='string' _isCFConfig=true;
	// True/false
	property name='compression' type='boolean' _isCFConfig=true;
	// True/false
	property name='supressContentForCFCRemoting' type='boolean' _isCFConfig=true;
	// True/false
	property name='bufferTagBodyOutput' type='boolean' _isCFConfig=true;
	// Key is datasource name, value is struct of properties
	property name='datasources' type='struct' _isCFConfig=true;

	// Preserve single quotes (") in the SQL defined with the tag cfquery (Lucee only)
	property name='datasourcePreserveSingleQuotes' type='boolean' _isCFConfig=true;

	// Array of structs of properties.  Mail servers are uniquely identified by host
	property name='mailServers' type='array' _isCFConfig=true;
	/**
	 * Custom tags have no unique identifier.  In Adobe, there's a made up
	 * "virtual" key of /WEB-INF/customtags(somenumber), but it's never shown
	 * topside.  In Lucee, you *could* name a path, but you don't have to.
	 *
	 * We're going to store in an array, and later if we need to determine
	 * uniqueness, we'll manufacture a key to do so.
	 */
	// Array of tag paths ( value struct of properties )
	property name='customTagPaths' type='array' _isCFConfig=true;

	// Search for custom tags in subdirectories. (Lucee only)
	property name='customTagSearchSubdirectories' type='boolean' _isCFConfig=true;
	// Search in the caller directory for the custom tag. (Lucee only)
	property name='customTagSearchLocal' type='boolean' _isCFConfig=true;
	//  component path is cached and not resolved again.  (Lucee only)
	property name='customTagCachePaths' type='boolean' _isCFConfig=true;
	// These are the extensions used for Custom Tags, in the order they are searched.
	property name='customTagExtensions' type='string' _isCFConfig=true;

	// Component search paths (Lucee only). Key is the name
	property name='componentPaths' type='struct' _isCFConfig=true;

	// Encoding to use for mail. Ex: UTF-8
	property name='mailDefaultEncoding' type='string' _isCFConfig=true;
	// True/false enable mail spooling
	property name='mailSpoolEnable' type='boolean' _isCFConfig=true;
	// Number of seconds for interval
	property name='mailSpoolInterval' type='numeric' _isCFConfig=true;
	// Number of seconds to wait for mail server response
	property name='mailConnectionTimeout' type='numeric' _isCFConfig=true;
	// True/false to allow downloading attachments for undelivered emails
	property name='mailDownloadUndeliveredAttachments' type='boolean' _isCFConfig=true;
	// Sign messages with cert
	property name='mailSignMesssage' type='boolean' _isCFConfig=true;
	// Path to keystore
	property name='mailSignKeystore' type='string' _isCFConfig=true;
	// Password to the keystore
	property name='mailSignKeystorePassword' type='string' _isCFConfig=true;
	// Alias of the key with which the certificcate and private key is stored in keystore. The supported type is JKS (java key store) and pkcs12.
	property name='mailSignKeyAlias' type='string' _isCFConfig=true;
	// Password with which the private key is stored.
	property name='mailSignKeyPassword' type='string' _isCFConfig=true;
	// true/false Log all mail messages sent by ColdFusion.  Select this check box to save the To, From, and Subject fields of messages to a log file.
	property name='mailLogEnabled' type='boolean' _isCFConfig=true;
	// true/false Maintain connection to mail server.  Select this check box to keep the connection to a mail server open for reuse after delivering a message (recommended).
	property name='mailMaintainConnections' type='boolean' _isCFConfig=true;
	// Error Log Severity. Select the type of SMTP-related error messages to log.
	// One of the strings "debug", "information", "warning", "error"
	property name='mailLogSeverity' type='string' _isCFConfig=true;
	// Number of mail delivery threads
	property name='mailMaxThreads' type='numeric' _isCFConfig=true;


	// Key is virtual path, value is struct of properties
	property name='CFMappings' type='struct' _isCFConfig=true;
	// Key is log name, value is struct of properties
	property name='loggers' type='struct' _isCFConfig=true;
	// Enable HTTP status codes.  ColdFusion sets an error status code of 404 if the template is not found and an error status code of 500 for server errors.
	property name='errorStatusCode' type='boolean' _isCFConfig=true;
	// True/false
	property name='disableInternalCFJavaComponents' type='boolean' _isCFConfig=true;

	// True/false
	property name='secureJSON' type='boolean' _isCFConfig=true;
	// A string representing the JSON prefx like "//"
	property name='secureJSONPrefix' type='string' _isCFConfig=true;

	// Number of KB for buffer size (1024)
	property name='maxOutputBufferSize' type='numeric' _isCFConfig=true;

	// True/false
	property name='inMemoryFileSystemEnabled' type='boolean' _isCFConfig=true;
	// Number of MB for in memory file system
	property name='inMemoryFileSystemLimit' type='numeric' _isCFConfig=true;
	// Number of MB for in memory application file system
	property name='inMemoryFileSystemAppLimit' type='numeric' _isCFConfig=true;

	// True/false
	property name='watchConfigFilesForChangesEnabled' type='boolean' _isCFConfig=true;
	// Number of seconds
	property name='watchConfigFilesForChangesInterval' type='numeric' _isCFConfig=true;
	// List of file extensions. Ex: "xml,properties"
	property name='watchConfigFilesForChangesExtensions' type='string' _isCFConfig=true;

	// True/false
	property name='allowExtraAttributesInAttrColl' type='boolean' _isCFConfig=true;
	// True/false
	property name='disallowUnamedAppScope' type='boolean' _isCFConfig=true;
	// True/false
	property name='allowApplicationVarsInServletContext' type='boolean' _isCFConfig=true;
	// Number of minutes
	property name='CFaaSGeneratedFilesExpiryTime' type='numeric' _isCFConfig=true;
	// Absolute path to store index files for ORM search.
	property name='ORMSearchIndexDirectory' type='string' _isCFConfig=true;
	// default path (relative to the web root) to the directory containing the cfform.js file.
	property name='CFFormScriptDirectory' type='string' _isCFConfig=true;
	// Your Google maps API key
	property name='googleMapKey' type='string' _isCFConfig=true;

	// Your Lucee API key (Lucee doesn't use it, but ForgeBox will since it's passed to extension providers)
	property name='APIKey' type='string' _isCFConfig=true;

	// True/false
	property name='serverCFCEenabled' type='boolean' _isCFConfig=true;
	// Specify the absolute path to a CFC having onServerStart() method, like "c:\server.cfc". Or specify a dot delimited CFC path under webroot, like "a.b.server". By default, ColdFusion will look for server.cfc under webroot.
	property name='serverCFC' type='string' _isCFConfig=true;

	// file extensions as a comma separated list which gets compiled when used in the CFInclude tag * for all.
	property name='compileExtForCFInclude' type='string' _isCFConfig=true;

	/* Error Templates. One of the strings
	* "default" - Standard handling for engine. Blank for Adobe, "error.cfm" for Lucee/Railo. Not secure.
	* "secure" - Uses the engine's secure template.
	* "neo" - Mirrors appearance of default Adobe handler (Lucee/Railo)
	* Alternatively, you can provide the path to a custom template
	*/
	property name='generalErrorTemplate' type='string' _isCFConfig=true;
	property name='missingErrorTemplate' type='string' _isCFConfig=true;

	// Maximum number of parameters in a POST request sent to the server.
	property name='postParametersLimit' type='numeric' _isCFConfig=true;
	// Limits the amount of data in MB that can be posted to the server in a single request.
	property name='postSizeLimit' type='numeric' _isCFConfig=true;
	// Requests smaller than the specified limit in MB are not handled by the throttle.
	property name='throttleThreshold' type='numeric' _isCFConfig=true;
	// Limits total memory size in MB for the throttle
	property name='totalThrottleMemory' type='numeric' _isCFConfig=true;


	// Maximum number of simultaneous Template requests
	property name='maxTemplateRequests' type='numeric' _isCFConfig=true;
	// Maximum number of simultaneous Flash Remoting requests
	property name='maxFlashRemotingRequests' type='numeric' _isCFConfig=true;
	// Maximum number of simultaneous Web Service requests
	property name='maxWebServiceRequests' type='numeric' _isCFConfig=true;
	// Maximum number of simultaneous CFC function requests
	property name='maxCFCFunctionRequests' type='numeric' _isCFConfig=true;
	// Maximum number of simultaneous Report threads
	property name='maxReportRequests' type='numeric' _isCFConfig=true;
	// Maximum number of threads available for CFTHREAD
	property name='maxCFThreads' type='numeric' _isCFConfig=true;
	// Timeout requests waiting in queue after XX seconds
	property name='requestQueueTimeout' type='numeric' _isCFConfig=true;
	// Request Queue Timeout Page
	property name='requestQueueTimeoutPage' type='string' _isCFConfig=true;
	// Enable request queue on Lucee
	property name='requestQueueEnable' type='boolean' _isCFConfig=true;

	// Key is cache connection name, value is struct of properties
	property name='caches' type='struct' _isCFConfig=true;

	// Array of extension provider URLs (strings)
	property name='extensionProviders' type='array' _isCFConfig=true;

	// name of default Object cache connection
	property name='cacheDefaultObject' type='string' _isCFConfig=true;
	// name of default function cache connection
	property name='cacheDefaultFunction' type='string' _isCFConfig=true;
	// name of default Template cache connection
	property name='cacheDefaultTemplate' type='string' _isCFConfig=true;
	// name of default Query cache connection
	property name='cacheDefaultQuery' type='string' _isCFConfig=true;
	// name of default Resource cache connection
	property name='cacheDefaultResource' type='string' _isCFConfig=true;
	// name of default Include cache connection
	property name='cacheDefaultInclude' type='string' _isCFConfig=true;
	// name of default File cache connection
	property name='cacheDefaultFile' type='string' _isCFConfig=true;
	// name of default HTTP cache connection
	property name='cacheDefaultHTTP' type='string' _isCFConfig=true;
	// name of default WebService cache connection
	property name='cacheDefaultWebservice' type='string' _isCFConfig=true;

	// Line Debugger Settings - Allow Line Debugging
	property name='lineDebuggerEnabled' type='boolean' _isCFConfig=true;
	// Line Debugger Settings - Debugger Port
	property name='lineDebuggerPort' type='numeric' _isCFConfig=true;
	// Line Debugger Settings - Maximum Simultaneous Debugging Sessions:
	property name='lineDebuggerMaxSessions' type='numeric' _isCFConfig=true;

	// Enable robust error information (Adobe only)
	property name='robustExceptionEnabled' type='boolean' _isCFConfig=true;
	// Enable Ajax debugging window (Adobe only)
	property name='ajaxDebugWindowEnabled' type='boolean' _isCFConfig=true;
	// "Enable Request Debugging Output" in Adobe / "Enable debugging" in Lucee
	property name='debuggingEnabled' type='boolean' _isCFConfig=true;
	// Remote DOM Inspection Settings
	property name='weinreRemoteInspectionEnabled' type='boolean' _isCFConfig=true;
	// Report Execution Times
	property name='debuggingReportExecutionTimes' type='boolean' _isCFConfig=true;

	// Database Activity - Select this option to log the database activity for the SQL Query events and Stored Procedure events. - Lucee only
	property name='debuggingDBEnabled' type='boolean' _isCFConfig=true;
	// Exceptions - Select this option to log all exceptions raised for the request. - Lucee only
	property name='debuggingExceptionsEnabled' type='boolean' _isCFConfig=true;
	// Query Usage - Select this option to log the query usage information. - Lucee only
	property name='debuggingQueryUsageEnabled' type='boolean' _isCFConfig=true;
	// Tracing -Select this option to log trace event information. Tracing lets a developer track program flow and efficiency through the use of the CFTRACE tag.  - Lucee only
	property name='debuggingTracingEnabled' type='boolean' _isCFConfig=true;
	// Dump - Select this option to enable output produced with help of the tag cfdump and send to debugging. - Lucee only
	property name='debuggingDumpEnabled' type='boolean' _isCFConfig=true;
	// Timer - Select this option to show timer event information. Timers let a developer track the execution time of the code between the start and end tags of the CFTIMER tag. - Lucee only
	property name='debuggingTimerEnabled' type='boolean' _isCFConfig=true;
	// Implicit variable Access - Select this option to log all accesses to scopes, queries and threads that happens implicit (cascaded). - Lucee only
	property name='debuggingImplicitVariableAccessEnabled' type='boolean' _isCFConfig=true;

	// Maximum Logged Requests - Lucee only
	property name='debuggingMaxLoggedRequests' type='numeric' _isCFConfig=true;


	// Debugging Templates - Lucee only
	property name='debuggingTemplates' type='struct' _isCFConfig=true;

	// Debugging Highlight templates taking longer than the following ms
	property name='debuggingReportExecutionTimesMinimum' type='numeric' _isCFConfig=true;
	// Debugging Use the following output mode for long template request execution times
	property name='debuggingReportExecutionTimesTemplate' type='string' _isCFConfig=true;
	// Debugging Output Format (dockable.cfm, classic.cfm)
	property name='debuggingTemplate' type='string' _isCFConfig=true;
	// Debugging show General debug information
	property name='debuggingShowGeneral' type='boolean' _isCFConfig=true;
	// Debugging show Database Activity
	property name='debuggingShowDatabase' type='boolean' _isCFConfig=true;
	// Debugging show Exception Information
	property name='debuggingShowException' type='boolean' _isCFConfig=true;
	// Debugging show Tracing Information
	property name='debuggingShowTrace' type='boolean' _isCFConfig=true;
	// Debugging show Timer Information
	property name='debuggingShowTimer' type='boolean' _isCFConfig=true;
	// Debugging Flash Form Compile Errors and Messages
	property name='debuggingShowFlashFormCompileErrors' type='boolean' _isCFConfig=true;
	// Debugging Variables. Select this option to enable variable reporting.
	property name='debuggingShowVariables' type='boolean' _isCFConfig=true;
	// Debugging include application vars
	property name='debuggingShowVariableApplication' type='boolean' _isCFConfig=true;
	// Debugging include cgi vars
	property name='debuggingShowVariableCGI' type='boolean' _isCFConfig=true;
	// Debugging include client vars
	property name='debuggingShowVariableClient' type='boolean' _isCFConfig=true;
	// Debugging include cookie vars
	property name='debuggingShowVariableCookie' type='boolean' _isCFConfig=true;
	// Debugging include form vars
	property name='debuggingShowVariableForm' type='boolean' _isCFConfig=true;
	// Debugging include request vars
	property name='debuggingShowVariableRequest' type='boolean' _isCFConfig=true;
	// Debugging include server vars
	property name='debuggingShowVariableServer' type='boolean' _isCFConfig=true;
	// Debugging include session vars
	property name='debuggingShowVariableSession' type='boolean' _isCFConfig=true;
	// Debugging include URL vars
	property name='debuggingShowVariableURL' type='boolean' _isCFConfig=true;
	// Debugging IP Addresses
	property name='debuggingIPList' type='string' _isCFConfig=true;

	// Monitoring Service Port (Only used by Adobe CF)
	// The port for the monitoring service to bind to
	property name='monitoringServicePort' type='numeric' _isCFConfig=true;
	// The host for the monitoring service to bind to
	// See https://tracker.adobe.com/#/view/CF-4202562
	property name='monitoringServiceHost' type='string' _isCFConfig=true;

	// .NET Services (Only used by Adobe CF)
	// Java port for .NET services
	property name='dotNetPort' type='numeric' _isCFConfig=true;
	// .Net port of JNBridge for .NET services
	property name='dotNetClientPort' type='numeric' _isCFConfig=true;
	// Install path to the .NET services
	property name='dotNetInstallDir' type='string' _isCFConfig=true;
	// Protocol for the .NET services.  Possible options: TCP, ??
	property name='dotNetProtocol' type='string' _isCFConfig=true;

	// Log directory
	property name='logDirectory' type='string' _isCFConfig=true;
	// Maximum file size  (In KB)
	property name='logMaxFileSize' type='numeric' _isCFConfig=true;
	// Maximum number of archives
	property name='logMaxArchives' type='numeric' _isCFConfig=true;
	// Log slow pages taking longer than
	property name='logSlowRequestsEnabled' type='boolean' _isCFConfig=true;
	// Number of seconds threshold for logging slow pages
	property name='logSlowRequestsThreshold' type='numeric' _isCFConfig=true;
	// Log all CORBA calls
	property name='logCORBACalls' type='boolean' _isCFConfig=true;
	// Adobe && UNIX ONLY - Use operating system logging facilities
	property name='logSysLogEnabled' type='boolean' _isCFConfig=true;

	// Array of disabled log file names (Adobe CF only)
	property name='logFilesDisabled' type='array' _isCFConfig=true;

	// PDF Service Managers (Adobe CF only)
	property name='PDFServiceManagers' type='struct' _isCFConfig=true;

	// TODO:
	//property name='externalizeStrings' type='string' _isCFConfig=true;
	//property name='restMappings' type='array' _isCFConfig=true;
	//property name='componentBase' type='string' _isCFConfig=true;
	//property name='componentAutoImport' type='string' _isCFConfig=true;
	//property name='componentSearchLocal' type='boolean' _isCFConfig=true;
	//property name='componentImplicitNotation' type='boolean' _isCFConfig=true;
	//property name='cfxTags' type='string' _isCFConfig=true;


	// Developer Mode (Lucee only)
	property name='developerMode' type='boolean' _isCFConfig=true;

	// Enable logging for scheduled tasks
	property name='schedulerLoggingEnabled' type='boolean' _isCFConfig=true;
	property name='schedulerClusterDatasource' type='string' _isCFConfig=true;
	property name='schedulerLogFileExtensions' type='string' _isCFConfig=true;
	property name='scheduledTasks' type='struct' _isCFConfig=true;

	// Enable Event Gateway Services
	property name='eventGatewayEnabled' type='boolean' _isCFConfig=true;
	// Maximum number of events to queue
	property name='eventGatewayMaxQueueSize' type='numeric' _isCFConfig=true;
	// Event Gateway Processing Threads
	property name='eventGatewayThreadpoolSize' type='numeric' _isCFConfig=true;
	// Event Gateways > Gateway Instances
	property name='eventGatewayInstances' type='array' _isCFConfig=true;
	// Services > Event Gateway - Lucee specific
	// Lucee and Adobe event gateways are very different and cannot be transfered between engines.  As such, they are stored separately
	property name='eventGatewaysLucee' type='struct' _isCFConfig=true;
	// Event Gateways > Gateway Types
	property name='eventGatewayConfigurations' type='array' _isCFConfig=true;

	// Enable WebSocket Service
	property name='websocketEnabled' type='boolean' _isCFConfig=true;


	// Enable Flash remoting
	property name='FlashRemotingEnable' type='boolean' _isCFConfig=true;
	//  Enable Remote Adobe LiveCycle Data Management access
	property name='flexDataServicesEnable' type='boolean' _isCFConfig=true;
	// Enable RMI over SSL for Data Management
	property name='RMISSLEnable' type='boolean' _isCFConfig=true;
	// RMI SSL Keystore
	property name='RMISSLKeystore' type='string' _isCFConfig=true;
	// RMI SSL Keystore Password
	property name='RMISSLKeystorePassword' type='string' _isCFConfig=true;

	// Plain text admin password
	property name='adminPassword' type='string' _isCFConfig=true;
	// Plain text admin RDS password
	property name='adminRDSPassword' type='string' _isCFConfig=true;
	// True/false is RDS enabled?
	property name='adminRDSEnabled' type='boolean' _isCFConfig=true;
	// Plain text default password for new Lucee web context
	property name='adminPasswordDefault' type='string' _isCFConfig=true;
	// hashed salted password for Lucee
	property name='hspw' type='string' _isCFConfig=true;
	// hashed password for Lucee/Railo
	property name='pw' type='string' _isCFConfig=true;
	// Salt for admin password in Lucee
	property name='adminSalt' type='string' _isCFConfig=true;
	// hashed salted default password for new Lucee web context
	property name='defaultHspw' type='string' _isCFConfig=true;
	// hashed default password for new Lucee/Railo web context
	property name='defaultPw' type='string' _isCFConfig=true;


	// Password required for admin
	property name='adminLoginRequired' type='boolean' _isCFConfig=true;
	// Password required for RDS
	property name='adminRDSLoginRequired' type='boolean' _isCFConfig=true;
	// user ID required for admin login. False means just a password is required
	property name='adminUserIDRequired' type='boolean' _isCFConfig=true;
	// user ID required for RDS login. False means just a password is required
	property name='adminRDSUserIDRequired' type='boolean' _isCFConfig=true;
	// Default/root admin user ID
	property name='adminRootUserID' type='string' _isCFConfig=true;
	// Allow more than one user to be logged into the same userID at once in the admin
	property name='adminAllowConcurrentLogin' type='boolean' _isCFConfig=true;
	// Enable sandbox security
	property name='sandboxEnabled' type='boolean' _isCFConfig=true;

	// define the access for reading data from the admin. One of the strings open, closed, or protected
	property name='adminAccessWrite' type='string' _isCFConfig=true;
	// define the access for writing data from the admin. One of the strings open, closed, or protected
	property name='adminAccessRead' type='string' _isCFConfig=true;


	// List of allowed IPs for exposed services.  Formatted like 1.2.3.4,5.6.7.*
	property name='servicesAllowedIPList' type='string' _isCFConfig=true;
	// List of allowed IPs for admin access.  Formatted like 1.2.3.4,5.6.7.*
	property name='adminAllowedIPList' type='string' _isCFConfig=true;
	// Enable secure profile.  Note, fipping this flag doesn't actually change any of the security settings.  It really just tracks the fact that you've enabled it at some point.
	property name='secureProfileEnabled' type='boolean' _isCFConfig=true;

	// System output streams - Lucee only
	// values are strings indicating target stream (default,null,class:<class>,file:<file>)
	property name='systemOut' type='string' _isCFConfig=true;
	property name='systemErr' type='string' _isCFConfig=true;

	// TODO: adminUsers array (AuthorizedUsers)
	// TODO: sandboxes (contexts)

	// License key (only used for Adobe)
	property name='license' type='string' _isCFConfig=true;
	// Previous license key (required for an upgrade license key)
	property name='previousLicense' type='string' _isCFConfig=true;


	// TODO: Figure out what hashing algorithms each version of ACF use, and share the
	// same setting so the hashes passwords are as portable as possible

	// hashed admin password for Adobe CF11
	// TODO: Need to get 10, 11, 2016, and 2018 ironed out here.
	property name='ACF11Password' type='string' _isCFConfig=true;
	// hashed RDS password for Adobe CF11
	property name='ACF11RDSPassword' type='string' _isCFConfig=true;

	// Automatically Check for Updates. Select to automatically check for updates at every login.
	property name='updateCheckOnLoginEnable' type='boolean' _isCFConfig=true;
	// Check for updates every X days Enable
	property name='updateCheckOnScheduleEnable' type='boolean' _isCFConfig=true;
	// Number of days between updates
	property name='updateCheckOnScheduleDays' type='numeric' _isCFConfig=true;
	// If updates are available, send email notification to (comma-delimited list)
	property name='updateCheckOnScheduleToAddress' type='string' _isCFConfig=true;
	// If updates are available, send email notification from
	property name='updateCheckOnScheduleFromAddress' type='string' _isCFConfig=true;
	// Update site URL
	property name='updateSiteURL' type='string' _isCFConfig=true;
	// Update check proxy host
	property name='updateProxyHost' type='string' _isCFConfig=true;
	// Update check proxy port
	property name='updateProxyPort' type='numeric' _isCFConfig=true;
	// Update check proxy username
	property name='updateProxyUsername' type='string' _isCFConfig=true;
	// Update check proxy password
	property name='updateProxyPassword' type='string' _isCFConfig=true;

	// Adobe-only - Services > Cloud credentials
	property name='cloudCredentials' type='struct' _isCFConfig=true;
	// Adobe-only - Services > Cloud Configuration
	property name='cloudServices' type='struct' _isCFConfig=true;
	// Adobe-only - Security > IDP Configuration
	property name='SAMLIdentityProviders' type='struct' _isCFConfig=true;
	// Adobe-only - Security > SP Configuration
	property name='SAMLServiceProviders' type='struct' _isCFConfig=true;

	// BoxLang settings
	// Choose the compiler to use for the runtime. Valid values are: "java", "asm"
	property name='experimentalCompiler' type='string' _isCFConfig=true;
	// Enable experimental AST capture. If enabled, it will generate AST JSON data under the project's /grapher/data folder
	property name='experimentalASTCapture' type='boolean' _isCFConfig=true;
	// Extensions BoxLang will process as classes
	property name='validClassExtensions' type='array' _isCFConfig=true;
	// Where all generated classes will be placed
	property name='classGenerationDirectory' type='string' _isCFConfig=true;
	// By default BoxLang uses high-precision mathematics via BigDecimal operations
	property name='useHighPrecisionMath' type='boolean' _isCFConfig=true;
	// If true, you can call implicit accessors/mutators on object properties. By default it is enabled
	property name='invokeImplicitAccessor' type='boolean' _isCFConfig=true;
	// You can assign a global default datasource to be used in BoxLang
	property name='defaultDatasource' type='string' _isCFConfig=true;
	// The default return format for class invocations via web runtimes
	property name='defaultRemoteMethodReturnFormat' type='string' _isCFConfig=true;
	// A list of regex patterns that will match class paths, and if matched, execution will be disallowed
	property name='disallowedImports' type='array' _isCFConfig=true;
	// A list of BIF names that will be disallowed from execution
	property name='disallowedBifs' type='array' _isCFConfig=true;
	// A list of Component names that will be disallowed from execution
	property name='disallowedComponents' type='array' _isCFConfig=true;
	// An explicit whitelist of file extensions that are allowed to be uploaded - overrides any values in the disallowedWriteExtensions
	property name='allowedFileOperationExtensions' type='array' _isCFConfig=true;
	// The BoxLang module settings
	property name='modules' type='struct' _isCFConfig=true;
	/*
	* paths should be semi-colon seperated. 
	* To Allow a file: {path-of-file}; To Allow a directory & files in it: {path-to-directory}/*; 
	* To Allow a directory & sub-directories: {path-to-directory}/**; 
	* To Block a file: !{path-of-file}; 
	* To Block a directory & sub-directories: !{path-to-directory}/**; 
	* Precedence decreases from left to right. 
	* Suppose directory A has directory B & C inside it.
	* To Allow B & Block C: !A/C/*;A/**;
	*/
	// Paths where pre-compiled bytecode can be executed from. (Adobe only)
	property name='pathFilterBytecodeExecutionPaths' type='string' _isCFConfig=true;
	// Paths where scheduled tasks can write log files to. (Adobe only)
	property name='pathFilterSchedulerExecutionPaths' type='string' _isCFConfig=true;

	/**
	* Constructor
	*/
	function init() {
		// This will need to be set before you can read/write
		setCFHomePath( '' );
	}

	/**
	* Custom setter to clean up paths
	*/
	function setCFHomePath( required string CFHomePath ) {
		variables.CFHomePath = normalizeSlashes( arguments.CFHomePath );
		return this;
	}

	/**
	* Check if CF home dir exists.
	* Individual providers can override this if they want a more specific check.
	*/
	boolean function CFHomePathExists( string CFHomePath=getCFHomePath() ) {
		if( !len( arguments.CFHomePath ) ) {
			throw( message="No CF Home provided.", type="cfconfigException" );
		}
		if( directoryExists( arguments.CFHomePath ) ) {
			return true;
		} else if( fileExists( arguments.CFHomePath ) ) {
			return true;
		}
		return false;
	}

	////////////////////////////////////////
	// Custom Setters for complex types
	////////////////////////////////////////

	/**
	* Add a single PDF Service Manager to the config
	*
	* @name name of the PDF Service Manager to save or update
	* @hostname The host of the PDF service
	* @port The port of the PDF service
	* @isHTTPS True/false whether the remote service is using HTTPS
	* @weight A number to set the weight for this service
	* @isLocal True for local host
	* @isEnabled True for enabled
	*/
	function addPDFServiceManager(
		required string name,
		required string hostname,
		required string port,
		boolean isHTTPS=false,
		numeric weight=2,
		boolean isLocal,
		boolean isEnabled=true,
		string engine
	) {

		if( isNull( arguments.isLocal ) ) {
			if( arguments.hostname == '127.0.0.1' || arguments.hostname == 'localhost' ) {
				arguments.isLocal = true;
			} else {
				arguments.isLocal = false;
			}
		}

		var PDFServiceManager = {};
		PDFServiceManager[ 'hostname' ] = hostname;
		PDFServiceManager[ 'port' ] = port;
		PDFServiceManager[ 'isHTTPS' ] = isHTTPS;
		PDFServiceManager[ 'weight' ] = weight;
		PDFServiceManager[ 'isLocal' ] = isLocal;
		PDFServiceManager[ 'isEnabled' ] = isEnabled;
		PDFServiceManager[ 'engine' ] = engine ?: 'WebKit';

		var thisPDFServiceManagers = getPDFServiceManagers() ?: {};
		thisPDFServiceManagers[ arguments.name ] = PDFServiceManager;
		setPDFServiceManagers( thisPDFServiceManagers );
		return this;
	}

	/**
	* Add a single cache to the config
	*
	* @name name of the cache to save or update
	* @class Java class of implementing provider
	* @type The type of cache. This is a shortcut for providing the "class" parameter. Values "ram", and "ehcache".
	* @readOnly No idea what this does
	* @storage Is this cache used for session or client scope storage?
	* @custom A struct of settings that are meaningful to this cache provider.
	* @bundleName OSGI bundle name to load the class from
	* @bundleVersion OSGI bundle version to load the class from
	*/
	function addCache(
		required string name,
		string type,
		string class,
		boolean readOnly,
		boolean storage,
		struct custom,
		string bundleName,
		string bundleVersion
	) {
		var cacheConnection = {};
		if( !isNull( class ) ) { cacheConnection[ 'class' ] = class; }
		if( !isNull( type ) ) { cacheConnection[ 'type' ] = type; }
		if( !isNull( readOnly ) ) { cacheConnection[ 'readOnly' ] = readOnly; }
		if( !isNull( storage ) ) { cacheConnection[ 'storage' ] = storage; }
		if( !isNull( custom ) ) { cacheConnection[ 'custom' ] = custom; }
		if( !isNull( bundleName ) ) { cacheConnection[ 'bundleName' ] = bundleName; }
		if( !isNull( bundleVersion ) ) { cacheConnection[ 'bundleVersion' ] = bundleVersion; }

		var thisCaches = getCaches() ?: {};
		thisCaches[ arguments.name ] = cacheConnection;
		setCaches( thisCaches );
		return this;
	}

	/**
	* Add a single cloud cred to the config
	*
	* @name name of the cloud cred to save or update
	* @vendor name of the cloud cred to save or update (AZURE or AWS)
	* @connectionString In format Example: EndPoint=sb://(namespace).servicebus.windows.net/;SharedAccessKeyName=(key);SharedAccessKey=(key)
	* @region AWS Region
	* @accessKey AWS Access Key
	* @secretKey AWS Secret Key
	*/
	function addCloudCredential(
		required string name,
		required string vendor,
		string connectionString,
		string region,
		string accessKey,
		string secretKey
	) {
		var cloudCredential = {};
		if( !isNull( vendor ) ) { cloudCredential[ 'vendor' ] = vendor; }
		if( !isNull( connectionString ) ) { cloudCredential[ 'connectionString' ] = connectionString; }
		if( !isNull( region ) ) { cloudCredential[ 'region' ] = region; }
		if( !isNull( accessKey ) ) { cloudCredential[ 'accessKey' ] = accessKey; }
		if( !isNull( secretKey ) ) { cloudCredential[ 'secretKey' ] = secretKey; }

		var thisCloudCredentials = getCloudCredentials() ?: {};
		thisCloudCredentials[ arguments.name ] = cloudCredential;
		setCloudCredentials( thisCloudCredentials );
		return this;
	}

	/**
	* Add a single SAML Identity Provider to the config
	*
	* @name Name of SAML Identity Provider
	* @description Description of SAML Identity Provider
	* @encryptCertificate PEM encoded certificate for encryption
	* @encryptRequests Whether to encrypt requests
	* @entityId Entity ID
	* @logoutResponseURL Logout Response URL
	* @signCertificate PEM encoded certificate for signing
	* @signRequests Whether to sign requests
	* @SLOBinding SLO Binding (REDIRECT, or POST)
	* @SLOURL SLO URL
	* @SSOBinding SSO Binding (REDIRECT, or POST)
	* @SSOURL SSO URL
	* @metadataUrl Metadata URL
	*/
	function addSAMLIdentityProvider(
		required string name,
		string description,
		string encryptCertificate,
		boolean encryptRequests,
		string entityId,
		string logoutResponseURL,
		string signCertificate,
		boolean signRequests,
		string SLOBinding,
		string SLOURL,
		string SSOBinding,
		string SSOURL,
		string metadataURL
	) {
		var SAMLIdentityProvider = {};
		if( !isNull( description ) ) { SAMLIdentityProvider[ 'description' ] = description; }
		if( !isNull( encryptCertificate ) ) { SAMLIdentityProvider[ 'encryptCertificate' ] = encryptCertificate; }
		if( !isNull( encryptRequests ) ) { SAMLIdentityProvider[ 'encryptRequests' ] = encryptRequests; }
		if( !isNull( entityId ) ) { SAMLIdentityProvider[ 'entityId' ] = entityId; }
		if( !isNull( logoutResponseURL ) ) { SAMLIdentityProvider[ 'logoutResponseURL' ] = logoutResponseURL; }
		if( !isNull( signCertificate ) ) { SAMLIdentityProvider[ 'signCertificate' ] = signCertificate; }
		if( !isNull( signRequests ) ) { SAMLIdentityProvider[ 'signRequests' ] = signRequests; }
		if( !isNull( SLOBinding ) ) { SAMLIdentityProvider[ 'SLOBinding' ] = SLOBinding; }
		if( !isNull( SLOURL ) ) { SAMLIdentityProvider[ 'SLOURL' ] = SLOURL; }
		if( !isNull( SSOBinding ) ) { SAMLIdentityProvider[ 'SSOBinding' ] = SSOBinding; }
		if( !isNull( SSOURL ) ) { SAMLIdentityProvider[ 'SSOURL' ] = SSOURL; }
		if( !isNull( metadataURL ) ) { SAMLIdentityProvider[ 'metadataURL' ] = metadataURL; }

		var thisSAMLIdentityProviders = getSAMLIdentityProviders() ?: {};
		thisSAMLIdentityProviders[ arguments.name ] = SAMLIdentityProvider;
		setSAMLIdentityProviders( thisSAMLIdentityProviders );
		return this;
	}

	/**
	* Add a single SAML Service Provider to the config
	*
	* @name Name of SAML Service Provider
	* @ACSBinding Assertion Consumer Service binding (REDIRECT, or POST)
	* @ACSURL Assertion Consumer Service URL
	* @allowIdpInitiatedSSO Alow IdP Initiated SSO
	* @description SAML Service Provider Description
	* @entityId Entity ID
	* @logoutResponseSigned Logout Response Signed
	* @signKeystoreAliasSigning Keystore Alias
	* @signKeystorePassword Signing Keystore Password
	* @signKeystorePath Signing Keystore Path
	* @signMetadata Sign Metadata  (No corresponding form element in web UI)
	* @signRequests Sign Requests
	* @SLOBinding SLO Binding (REDIRECT, or POST)
	* @SLOURL SLO URL
	* @stateStore Request Store (empty string (default), "redis" or "cache")
	* @strict Strict (No corresponding form element in web UI)
	* @wantAssertionsSigned Want Assertions Signed
	*/
	function addSAMLServiceProvider(
		required string name,
		string ACSBinding,
		string ACSURL,
		boolean allowIdpInitiatedSSO,
		string description,
		string entityId,
		boolean logoutResponseSigned,
		string signKeystoreAlias,
		string signKeystorePassword,
		string signKeystorePath,
		boolean signMetadata,
		boolean signRequests,
		string SLOBinding,
		string SLOURL,
		string stateStore,
		boolean strict,
		boolean wantAssertionsSigned
	) {
		var SAMLServiceProvider = {};
		if( !isNull( ACSBinding ) ) { SAMLServiceProvider[ 'ACSBinding' ] = ACSBinding; }
		if( !isNull( ACSURL ) ) { SAMLServiceProvider[ 'ACSURL' ] = ACSURL; }
		if( !isNull( allowIdpInitiatedSso ) ) { SAMLServiceProvider[ 'allowIdpInitiatedSso' ] = allowIdpInitiatedSso; }
		if( !isNull( description ) ) { SAMLServiceProvider[ 'description' ] = description; }
		if( !isNull( entityId ) ) { SAMLServiceProvider[ 'entityId' ] = entityId; }
		if( !isNull( logoutResponseSigned ) ) { SAMLServiceProvider[ 'logoutResponseSigned' ] = logoutResponseSigned; }
		if( !isNull( signKeystoreAlias ) ) { SAMLServiceProvider[ 'signKeystoreAlias' ] = signKeystoreAlias; }
		if( !isNull( signKeystorePassword ) ) { SAMLServiceProvider[ 'signKeystorePassword' ] = signKeystorePassword; }
		if( !isNull( signKeystorePath ) ) { SAMLServiceProvider[ 'signKeystorePath' ] = signKeystorePath; }
		if( !isNull( signMetadata ) ) { SAMLServiceProvider[ 'signMetadata' ] = signMetadata; }
		if( !isNull( signRequests ) ) { SAMLServiceProvider[ 'signRequests' ] = signRequests; }
		if( !isNull( SLOBinding ) ) { SAMLServiceProvider[ 'SLOBinding' ] = SLOBinding; }
		if( !isNull( SLOURL ) ) { SAMLServiceProvider[ 'SLOURL' ] = SLOURL; }
		if( !isNull( stateStore ) ) { SAMLServiceProvider[ 'stateStore' ] = stateStore; }
		if( !isNull( strict ) ) { SAMLServiceProvider[ 'strict' ] = strict; }
		if( !isNull( wantAssertionsSigned ) ) { SAMLServiceProvider[ 'wantAssertionsSigned' ] = wantAssertionsSigned; }

		var thisSAMLServiceProviders = getSAMLServiceProviders() ?: {};
		thisSAMLServiceProviders[ arguments.name ] = SAMLServiceProvider;
		setSAMLServiceProviders( thisSAMLServiceProviders );
		return this;
	}

	/**
	* Add a single cloud service to the config
	*
	* @name Name of Cloud Service
	* @config struct representing all settings for this service
	*/
	function addCloudService(
		required string name,
		required struct config,
	) {
		var thisCloudServices = getCloudServices() ?: {};
		thisCloudServices[ arguments.name ] = config;
		setCloudServices( thisCloudServices );
		return this;
	}

	/**
	* Add a single debugging template to the config
	*
	* @label Custom name of this template
	* @type Type of debugging template. i.e. lucee-classic
	* @iprange A comma separated list of strings of ip definitions
	* @fullname CFC invocation path to the component that declares the fields for this template (defaulted for known types)
	* @path File system path to component that declares the fields for this template (defaulted for known types)
	* @id Id of Template
	* @custom A struct of settings that are meaningful to this debug template.
	*/
	function addDebuggingTemplate(
		required string label,
		required string type,
		string id,
		string fullname,
		string iprange,
		string path,
		struct custom
	) {
		var debuggingTemplate = {
			'label' : arguments.label,
			'type' : arguments.type
		};
		if( !isNull( id ) ) { debuggingTemplate[ 'id' ] = id; };
		if( !isNull( fullname ) ) { debuggingTemplate[ 'fullname' ] = fullname; };
		if( !isNull( iprange ) ) { debuggingTemplate[ 'iprange' ] = iprange; };
		if( !isNull( path ) ) { debuggingTemplate[ 'path' ] = path; };
		if( !isNull( custom ) ) { debuggingTemplate[ 'custom' ] = custom; };

		var thisDebuggingTemplates = getDebuggingTemplates() ?: {};
		thisDebuggingTemplates[ arguments.label ] = debuggingTemplate;
		setDebuggingTemplates( thisDebuggingTemplates );
		return this;
	}

	/**
	* Add a single datasource to the config
	*
	* @name Name of datasource
	* @allowSelect Allow select operations
	* @allowDelete Allow delete operations
	* @allowUpdate Allow update operations
	* @allowInsert Allow insert operations
	* @allowCreate Allow create operations
	* @allowGrant Allow grant operations
	* @allowRevoke Allow revoke operations
	* @allowDrop Allow drop operations
	* @allowAlter Allow alter operations
	* @allowStoredProcs Allow Stored proc calls
	* @blob Enable blob
	* @blobBuffer Number of bytes to retreive in binary fields
	* @class Java class of driver
	* @clob Enable clob
	* @clobBuffer Number of chars to retreive in long text fields
	* @maintainConnections Maintain connections accross client requests
	* @sendStringParametersAsUnicode Enable High ASCII characters and Unicode for data sources configured for non-Latin characters
	* @connectionLimit Max number of connections. -1 means unlimimted
	* @connectionTimeout Connection idle timeout in minutes
	* @liveTimeout Connection timeout in minutes
	* @connectionTimeoutInterval Number of seconds connections are checked to see if they've timed out
	* @alwaysSetTimeout If true, sets the timeout to the connection string
	* @maxPooledStatements Max pooled statements if maintain connections is on.
	* @queryTimeout Max time in seconds a query is allowed to run.  Set to 0 to disable
	* @disableConnections Suspend all client connections
	* @loginTimeout Number of seconds for login timeout
	* @custom Extra JDBC URL query string without leading &
	* @database name of database
	* @dbdriver Type of database driver
	*  - MSSQL -- SQL Server driver
	*  - MSSQL2 -- jTDS driver
	*  - PostgreSql
	*  - Oracle
	*  - Other -- Custom JDBC URL
	*  - MySQL
	* @dsn JDBC URL (jdbc:mysql://{host}:{port}/{database})
	* @host name of host
	* @metaCacheTimeout Not sure-- Lucee had this in the XML
	* @password Unencrypted password
	* @port Port to connect on
	* @storage True/False use this datasource as client/session storage (Lucee)
	* @username Username to connect with
	* @validate Enable validating this datasource connection every time it's used
	* @validationQuery Query to run when validating datasource connection
	* @logActivity Enable logging queries to a text file
	* @logActivityFile A file path ending with .txt to log to
	* @disableAutogeneratedKeyRetrieval Disable retrieval of autogenerated keys
	* @SID Used for Oracle datasources
	* @serviceName Used for Oracle datasources
	* @linkedServers Enable Oracle linked servers support
	* @clientHostname Client Information - Client hostname
	* @clientUsername Client Information - Client username
	* @clientApplicationName Client Information - Application name
	* @clientApplicationNamePrefix Client Information - Application name prefix
	* @description Description of this datasource.  Informational only.
	* @requestExclusive Exclusive connections for request
	* @bundleName OSGI bundle name to load the class from
	* @bundleVersion OSGI bundle version to load the class from
	* @timezone Timezone for this datasource, used for datetime value coercion
	*
	* logActivity notes
	* ;SpyAttributes=(log=(file)C:/foobar.txt; linelimit=80;logTName=yes;timestamp=yes)</
	*/
	function addDatasource(
			required string name,
			boolean allowSelect,
			boolean allowDelete,
			boolean allowUpdate,
			boolean allowInsert,
			boolean allowCreate,
			boolean allowGrant,
			boolean allowRevoke,
			boolean allowDrop,
			boolean allowAlter,
			boolean allowStoredProcs,
			boolean blob,
			numeric blobBuffer,
			string class,
			boolean clob,
			numeric clobBuffer,
			boolean maintainConnections,
			boolean sendStringParametersAsUnicode,
			numeric connectionLimit,
			numeric connectionTimeout,
			numeric liveTimeout,
			numeric connectionTimeoutInterval,
			boolean alwaysSetTimeout,
			numeric maxPooledStatements,
			numeric queryTimeout,
			numeric loginTimeout,
			boolean disableConnections,
			string custom,
			string database,
			string dbdriver,
			string dsn,
			string host,
			numeric metaCacheTimeout,
			string password, // Unencrypted
			string port,
			boolean storage,
			string username,
			boolean validate,
			string validationQuery,
			boolean logActivity,
			string logActivityFile,
			boolean disableAutogeneratedKeyRetrieval,
			string SID,
			string serviceName,
			boolean linkedServers,
			boolean clientHostname,
			boolean clientUsername,
			boolean clientApplicationName,
			string clientApplicationNamePrefix,
			string description,
			boolean requestExclusive,
			string bundleName,
			string bundleVersion,
			string timezone
		) {
		var ds = {};
		if( !isNull( database ) ) { ds[ 'database' ] = database; };

		if( !isNull( allowSelect ) ) { ds[ 'allowSelect' ] = allowSelect; };
		if( !isNull( allowDelete ) ) { ds[ 'allowDelete' ] = allowDelete; };
		if( !isNull( allowUpdate ) ) { ds[ 'allowUpdate' ] = allowUpdate; };
		if( !isNull( allowInsert ) ) { ds[ 'allowInsert' ] = allowInsert; };
		if( !isNull( allowCreate ) ) { ds[ 'allowCreate' ] = allowCreate; };
		if( !isNull( allowGrant ) ) { ds[ 'allowGrant' ] = allowGrant; };
		if( !isNull( allowRevoke ) ) { ds[ 'allowRevoke' ] = allowRevoke; };
		if( !isNull( allowDrop ) ) { ds[ 'allowDrop' ] = allowDrop; };
		if( !isNull( allowAlter ) ) { ds[ 'allowAlter' ] = allowAlter; };
		if( !isNull( allowStoredProcs ) ) { ds[ 'allowStoredProcs' ] = allowStoredProcs; };

		if( !isNull( blob ) ) { ds[ 'blob' ] = blob; };
		if( !isNull( class ) ) { ds[ 'class' ] = class; };
		if( !isNull( dbdriver ) ) { ds[ 'dbdriver' ] = dbdriver; };
		if( !isNull( clob ) ) { ds[ 'clob' ] = clob; };
		if( !isNull( connectionLimit ) ) { ds[ 'connectionLimit' ] = connectionLimit; };
		if( !isNull( connectionTimeout ) ) { ds[ 'connectionTimeout' ] = connectionTimeout; };
		if( !isNull( liveTimeout ) ) { ds[ 'liveTimeout' ] = liveTimeout; };
		if( !isNull( alwaysSetTimeout ) ) { ds[ 'alwaysSetTimeout' ] = alwaysSetTimeout; };
		if( !isNull( custom ) ) { ds[ 'custom' ] = custom; };
		if( !isNull( dsn ) ) { ds[ 'dsn' ] = dsn; };
		if( !isNull( password ) ) { ds[ 'password' ] = password; };
		if( !isNull( host ) ) { ds[ 'host' ] = host; };
		if( !isNull( metaCacheTimeout ) ) { ds[ 'metaCacheTimeout' ] = metaCacheTimeout; };
		if( !isNull( port ) ) { ds[ 'port' ] = port; };
		if( !isNull( storage ) ) { ds[ 'storage' ] = storage; };
		if( !isNull( username ) ) { ds[ 'username' ] = username; };
		if( !isNull( validate ) ) { ds[ 'validate' ] = validate; };
		if( !isNull( SID ) ) { ds[ 'SID' ] = SID; };
		if( !isNull( serviceName ) ) { ds[ 'serviceName' ] = serviceName; };
		if( !isNull( maintainConnections ) ) { ds[ 'maintainConnections' ] = maintainConnections; };
		if( !isNull( sendStringParametersAsUnicode ) ) { ds[ 'sendStringParametersAsUnicode' ] = sendStringParametersAsUnicode; };
		if( !isNull( maxPooledStatements ) ) { ds[ 'maxPooledStatements' ] = maxPooledStatements; };
		if( !isNull( connectionTimeoutInterval ) ) { ds[ 'connectionTimeoutInterval' ] = connectionTimeoutInterval; };
		if( !isNull( queryTimeout ) ) { ds[ 'queryTimeout' ] = queryTimeout; };
		if( !isNull( logActivity ) ) { ds[ 'logActivity' ] = logActivity; };
		if( !isNull( logActivityFile ) ) { ds[ 'logActivityFile' ] = logActivityFile; };
		if( !isNull( disableConnections ) ) { ds[ 'disableConnections' ] = disableConnections; };
		if( !isNull( loginTimeout ) ) { ds[ 'loginTimeout' ] = loginTimeout; };
		if( !isNull( clobBuffer ) ) { ds[ 'clobBuffer' ] = clobBuffer; };
		if( !isNull( blobBuffer ) ) { ds[ 'blobBuffer' ] = blobBuffer; };
		if( !isNull( disableAutogeneratedKeyRetrieval ) ) { ds[ 'disableAutogeneratedKeyRetrieval' ] = disableAutogeneratedKeyRetrieval; };
		if( !isNull( validationQuery ) ) { ds[ 'validationQuery' ] = validationQuery; };
		if( !isNull( linkedServers ) ) { ds[ 'linkedServers' ] = linkedServers; };
		if( !isNull( clientHostname ) ) { ds[ 'clientHostname' ] = clientHostname; };
		if( !isNull( clientUsername ) ) { ds[ 'clientUsername' ] = clientUsername; };
		if( !isNull( clientApplicationName ) ) { ds[ 'clientApplicationName' ] = clientApplicationName; };
		if( !isNull( clientApplicationNamePrefix ) ) { ds[ 'clientApplicationNamePrefix' ] = clientApplicationNamePrefix; };
		if( !isNull( description ) ) { ds[ 'description' ] = description; };
		if( !isNull( requestExclusive ) ) { ds[ 'requestExclusive' ] = requestExclusive; };
		if( !isNull( bundleName ) ) { ds[ 'bundleName' ] = bundleName; };
		if( !isNull( bundleVersion ) ) { ds[ 'bundleVersion' ] = bundleVersion; };
		if( !isNull( timezone ) ) { ds[ 'timezone' ] = timezone; };

		var thisDatasources = getDataSources() ?: {};
		thisDatasources[ arguments.name ] = ds;
		setDatasources( thisDatasources );
		return this;
	}

	/**
	* Add a single mail server to the config
	*
	* @idleTimout Idle timeout in seconds
	* @lifeTimeout Overall timeout in seconds
	* @password Plain text password for mail server
	* @port Port for mail server
	* @smtp Host address of mail server
	* @ssl True/False to use SSL for connection
	* @tls True/False to use TLS for connection
	* @username Username for mail server
	*/
	function addMailServer(
		numeric idleTimeout,
		numeric lifeTimeout,
		string password,
		numeric port,
		string smtp,
		boolean SSL,
		boolean TLS,
		string username
	) {

		var mailServer = {};
		if( !isNull( idleTimeout ) ) { mailServer[ 'idleTimeout' ] = idleTimeout; };
		if( !isNull( lifeTimeout ) ) { mailServer[ 'lifeTimeout' ] = lifeTimeout; };
		if( !isNull( password ) ) { mailServer[ 'password' ] = password; };
		if( !isNull( port ) ) { mailServer[ 'port' ] = port; };
		if( !isNull( smtp ) ) { mailServer[ 'smtp' ] = smtp; };
		if( !isNull( ssl ) ) { mailServer[ 'ssl' ] = ssl; };
		if( !isNull( tls ) ) { mailServer[ 'tls' ] = tls; };
		if( !isNull( username ) ) { mailServer[ 'username' ] = username; };

		var thisMailServers = getMailServers() ?: [];
		thisMailServers.append( mailServer );
		setMailServers( thisMailServers );
		return this;
	}

	/**
	* Add a single client storage location to the config.  Only used for Adobe engines
	*
	* @name Name of the storage.  "cookie", "registry" or a datasource name
	* @description The description of the storage
	* @DSN Name of DSN for JDBC storage locations
	* @disableGlobals Disable global client variable updates
	* @purgeEnable Purge data for clients that remain unvisited
	* @purgeTimeout Number of days before purging data
	* @type The string "cookie", "registry", or "JDBC"
	*/
	function addClientStorageLocation(
		required string name,
		string description,
		string DSN,
		boolean disableGlobals,
		boolean purgeEnable,
		numeric purgeTimeout,
		string type
	) {

		var clientStorageLocation = {};
		if( !isNull( name ) ) { clientStorageLocation[ 'name' ] = name; };
		if( !isNull( description ) ) { clientStorageLocation[ 'description' ] = description; };
		if( !isNull( DSN ) ) { clientStorageLocation[ 'DSN' ] = DSN; };
		if( !isNull( disableGlobals ) ) { clientStorageLocation[ 'disableGlobals' ] = disableGlobals; };
		if( !isNull( purgeEnable ) ) { clientStorageLocation[ 'purgeEnable' ] = purgeEnable; };
		if( !isNull( purgeTimeout ) ) { clientStorageLocation[ 'purgeTimeout' ] = purgeTimeout; };
		if( !isNull( type ) ) { clientStorageLocation[ 'type' ] = type; };

		var thisClientStorageLocations = getClientStorageLocations() ?: {};
		thisClientStorageLocations[ name ] = clientStorageLocation;
		setClientStorageLocations( thisClientStorageLocations );
		return this;
	}

	/**
	* Add a single CF mapping to the config
	*
	* @virtual The virtual path such as /foo
	* @physical The physical path that the mapping points to
	* @archive Path to the Lucee/Railo archive
	* @inspectTemplate String containing one of "never", "once", "always", "" (inherit)
	* @listenerMode
	* @listenerType
	* @primary Strings containing one of "physical", "archive"
	* @readOnly True/false
	* @toplevel Defines whether a mapping is a top level mapping. If yes it can be called over the URL. True/false
	*/
	function addCFMapping(
			required string virtual,
			string physical,
			string archive,
			string inspectTemplate,
			string listenerMode,
			string listenerType,
			string primary,
			boolean readOnly,
			boolean toplevel
		) {

		var mapping = {};
		if( !isNull( physical ) ) { mapping.physical = physical; };
		if( !isNull( archive ) ) { mapping.archive = archive; };
		if( !isNull( arguments.inspectTemplate ) ) { mapping.inspectTemplate = arguments.inspectTemplate; };
		if( !isNull( listenerMode ) ) { mapping.listenerMode = listenerMode; };
		if( !isNull( database ) ) { mapping.listenerType = listenerType; };
		if( !isNull( primary ) ) { mapping.primary = primary; };
		if( !isNull( readOnly ) ) { mapping.readOnly = readOnly; };
		if( !isNull( toplevel ) ) { mapping.toplevel = toplevel; };


		var thisCFMappings = getCFMappings() ?: {};
		thisCFMappings[ arguments.virtual ] = mapping;
		setCFMappings( thisCFMappings );
		return this;
	}

	/**
	* Add a single rest mapping to the config
	*/
	function addRestMapping() { throw 'addRestMapping() not implemented'; }

	/**
	* Add a single custom tag to the config
	*
	* @physical The physical path that the engine should search
	* @archive Path to the Lucee/Railo archive
	* @name Name of the Custom Tag Path
	* @inspectTemplate String containing one of "never", "once", "always", "" (inherit)
	* @primary Strings containing one of "physical", "archive"
	* @trusted true/false
	*/
	function addCustomTagPath(
			string physical,
			string archive,
			string name,
			string inspectTemplate,
			string primary,
			boolean trusted

	) {

		var customTagPath = {};

		if( !isNull( physical ) ) { customTagPath[ 'physical' ] = physical; };
		if( !isNull( archive ) ) { customTagPath[ 'archive' ] = archive; };
		if( !isNull( name ) ) { customTagPath[ 'name' ] = name; };
		if( !isNull( inspectTemplate ) ) { customTagPath[ 'inspectTemplate' ] = inspectTemplate; };
		if( !isNull( primary ) ) { customTagPath[ 'primary' ] = primary; };
		if( !isNull( trusted ) ) { customTagPath[ 'trusted' ] = trusted; };

		var thisCustomTagPaths = getCustomTagPaths() ?: [];
		thisCustomTagPaths.append( customTagPath );
		setCustomTagPaths( thisCustomTagPaths );
		return this;
	}


	/**
	* Add a single component Path to the config
	*
	* @name Name of the Component Path
	* @physical The physical path that the engine should search
	* @archive Path to the Lucee/Railo archive
	* @inspectTemplate String containing one of "never", "once", "always", "inherit"
	* @primary Strings containing one of "resource", "archive"
	*/
	function addComponentPath(
			required string name,
			string physical,
			string archive,
			string primary,
			string inspectTemplate
	) {


		if( !len( physical ?: '' ) && !len( archive ?: '' ) ) {
			error( "You must specify a physical or archive location. (or both)" );
		}

		var componentPath = {
			"name": arguments.name
		};

		if( !IsNull( arguments["primary"] ) ) {
			componentPath["primary"] =  arguments.primary;
		}

		if( !IsNull( arguments["inspectTemplate"] ) ) {
			componentPath["inspectTemplate"] =  arguments.inspectTemplate;
		}

		if( !IsNull(arguments["archive"]) ){
			componentPath["archive"] = arguments["archive"];
		}

		if( !IsNull(arguments["physical"]) ){
			componentPath["physical"] = arguments["physical"];
		}


		var thisComponentPaths = getComponentPaths() ?: {};
		thisComponentPaths[arguments.name] = componentPath;
		setComponentPaths( thisComponentPaths );
		return this;
	}

	/**
	* This is how we figure out our internal comparison key...
	*
	* @physical The physical path that the engine should search
	* @archive Path to the Lucee/Railo archive
	*/
	private function _getCustomTagPathKey(
			string physical,
			string archive

	) {
		return "physical:(" & ( physical ?: "" ) & ")_archive:(" & ( archive ?: "" ) & ")";
	}

	/**
	* Add a single logger to the config
	*
	* @name name for the logger
	* @appender resource or console
	* @appenderArguments args for the appender
	* @appenderClass A full class path to a Appender class
	* @layout one of 'classic', 'html', 'xml', or 'pattern'
	* @layoutArguments args for the layout
	* @layoutClass A full class path to a Layout class
	* @level log level
	*/
	function addLogger(
		required string name,
		string appender,
		string appenderClass,
		struct appenderArguments,
		string layout,
		struct layoutArguments,
		string layoutClass,
		string level
	) {

		var logger = {};
		if( !isNull( appender ) ) { logger[ 'appender' ] = appender; };
		if( !isNull( appenderArguments ) ) { logger[ 'appenderArguments' ] = appenderArguments; };
		if( !isNull( appenderClass ) ) { logger[ 'appenderClass' ] = appenderClass; };
		if( !isNull( layout ) ) { logger[ 'layout' ] = layout; };
		if( !isNull( layoutArguments ) ) { logger[ 'layoutArguments' ] = layoutArguments; };
		if( !isNull( layoutClass ) ) { logger[ 'layoutClass' ] = layoutClass; };
		if( !isNull( level ) ) { logger[ 'level' ] = level; };

		var thisLoggers = getLoggers() ?: {};
		thisLoggers[ arguments.name ] = logger;
		setLoggers( thisLoggers );
		return this;
	}

	/**
	* Add a single Scheduled Task to the config
	* @task The name of the task
	* @url The full URL to hit
	* @group The group for the task (Adobe only)
	* @chained Is this task chained? (Adobe only)
	* @clustered Is this task clustered? (Adobe only)
	* @crontime Schedule in Cron format (Adobe only)
	* @endDate Date when task will end as 1/1/2000
	* @endTime Time when task will end as 9:57:00 AM
	* @eventhandler Specify a dot-delimited CFC path under webroot, for example a.b.server (without the CFC extension). The CFC should implement CFIDE.scheduler.ITaskEventHandler. (Adobe only)
	* @exclude Comma-separated list of dates or date range for exclusion in the schedule period. (Adobe only)
	* @file Save output of task to this file
	* @httpPort The port for the main task URL
	* @httpProxyPort The port for the proxy server
	* @interval The type of schedule. Once, Weekly, Daily, Monthly, an integer containing the number of seconds between runs
	* @misfire What to do in case of a misfire.  Ignore, FireNow, invokeHander (Adobe only)
	* @oncomplete Comma-separated list of chained tasks to be run after the completion of the current task (task1:group1,task2:group2...) (Adobe only)
	* @onexception Specify what to do if a task results in error. Ignore, Pause, ReFire, InvokeHandler (Adobe only)
	* @overwrite Overwrite the log file? (Adobe only)
	* @password Basic auth password to use when hitting URL
	* @priority An integer that indicates the priority of the task. (Adobe only)
	* @proxyPassword Proxy server password
	* @proxyServer Name of the proxy server to use
	* @proxyUser Proxy server username
	* @saveOutputToFile Save output to a file?
	* @repeat -1 to repeat forever, otherwise integer.
	* @requestTimeOut Number of seconds to timeout the request.  Empty string for none.
	* @resolveurl When saving output of task to file, Resolve internal URLs so that links remain intact.
	* @retrycount The number of reattempts if the task results in an error. (Adobe only)
	* @startDate The date to start executing the task
	* @startTime The date to end excuting the task
	* @status The current status of the task.  Running, Paused
	* @username Basic auth username to use when hitting URL
	* @autoDelete (Lucee only)
	* @hidden Do not show in admin UI (Lucee only)
	* @unique  If set run the task only once at time. Every time a task is started, it will check if still a task from previous round is running, if so no new test is started. (Lucee only)
	*/
	function addScheduledTask(
		required string task,
		string url,
		string group,
		boolean chained,
		boolean clustered,
		string crontime,
		string endDate,
		string endTime,
		string eventhandler,
		string exclude,
		string file,
		string httpPort,
		string httpProxyPort,
		string interval,
		string misfire,
		string oncomplete,
		string onexception,
		string overwrite,
		string password,
		string priority,
		string proxyPassword,
		string proxyServer,
		string proxyUser,
		boolean saveOutputToFile,
		string repeat,
		string requestTimeOut,
		boolean resolveurl,
		string retryCount,
		string startDate,
		string startTime,
		string status,
		string username,
		string autoDelete,
		string hidden,
		string unique
		) {

		var scheduledTask = {};

		for( var arg in arguments ) {
			if( !isNull( arguments[ arg ] ) ) { scheduledTask[ arg ] = arguments[ arg ]; };
		}

		var thisScheduledTasks = getScheduledTasks() ?: {};
		var taskID = arguments.task;
		if( !isNull( arguments.group ) && len( arguments.group ) ){
			taskID = arguments.group & ':' & arguments.task;
		}
		thisScheduledTasks[ taskID ] = scheduledTask;
		setScheduledTasks( thisScheduledTasks );
		return this;
	}

	/**
	* Add a single Gateway (Lucee) to the config
	* @gatewayId An event gateway ID to identify the specific event gateway instance.
	* @CFCPath Component path (dot delimieted) to the gateway CFC
	* @ListenerCFCPath Component path (dot delimieted) to the listener CFC
	* @custom A struct of additional configuration for this gateway
	* @startupMode The startup mode of the gateway.  Values: manual, automatic, disabled
	*/
	function addGatewayLucee(required string gatewayID,
								required string CFCPath,
								string listenerCFCPath,
								struct custom={},
								string startupMode="automatic")
	{
		var gatewayInstance={};

		for (var arg in arguments) {
			if (!isNull(arguments[ arg ]) && arg != 'gatewayID' ) { gatewayInstance[ arg ]=arguments[ arg ]; };
		}

		var thisEventGatewayInstances=getEventGatewaysLucee() ?: {};
		thisEventGatewayInstances[ gatewayID ]=gatewayInstance;
		setEventGatewaysLucee(thisEventGatewayInstances);

		return this;
	}

	/**
	* Add a single Gateway instance to the config
	* @gatewayId An event gateway ID to identify the specific event gateway instance.
	* @type The event gateway type, which you select from the available event gateway types, such as SMS or Socket.
	* @cfcPaths The absolute path to the listener CFC or CFCs that handles incoming messages.
	* @mode The event gateway start-up status; one of the following: automatic, manual, disabled
	* @configurationPath A configuration file, if necessary for this event gateway type or instance.
	*/
	function addGatewayInstance(required string gatewayId,
								required string type,
								required array cfcPaths,
								string mode = "manual",
								string configurationPath = "")
	{
		var gatewayInstance={};

		for (var arg in arguments) {
			if (!isNull(arguments[ arg ])) { gatewayInstance[ arg ]=arguments[ arg ]; };
		}

		var thisEventGatewayInstances=getEventGatewayInstances() ?: [];
		var i=0;
		for( var thisEventGatewayInstance in thisEventGatewayInstances ) {
			i++;
			if( thisEventGatewayInstance.gatewayId == gatewayId ) {
				thisEventGatewayInstances.deleteAt( i );
				break;
			}
		}
		thisEventGatewayInstances.append(gatewayInstance);
		setEventGatewayInstances(thisEventGatewayInstances);

		return this;
	}

	/**
	* Add a single Gateway configuration to the config
	* @type The event gateway type, which you will use when adding a gatewayInstance.
	* @description Description
	* @class Java Class
	* @starttimeout Startup Timeout(in seconds)
	* @killontimeout Stop on Startup Timeout
	*/
	function addGatewayConfiguration(required string type,
									 required string description,
									 required string class,
									 numeric starttimeout = 30,
									 boolean killontimeout = true)
	{
		var gatewayConfiguration={};

		for (var arg in arguments) {
			if (!isNull(arguments[ arg ])) { gatewayConfiguration[ arg ]=arguments[ arg ]; };
		}

		var thisEventGatewayConfigurations=getEventGatewayConfigurations() ?: [];

		var i=0;
		for( var thisGatewayConfiguration in thisEventGatewayConfigurations ) {
			i++;
			if( thisGatewayConfiguration.type == type ) {
				thisEventGatewayConfigurations.deleteAt( i );
				break;
			}
		}

		thisEventGatewayConfigurations.append(gatewayConfiguration);
		setEventGatewayConfigurations(thisEventGatewayConfigurations);

		return this;
	}

	/**
	* Get a struct representation of the config settings
	*/
	function getMemento(){
		var memento = {};
		for( var propName in getConfigProperties() ) {
			var thisValue = this[ 'get' & propName ]();
			if( !isNull( thisValue ) ) {
				memento[ propName ] = thisValue;
			}
		}
		// Force keys to be alphabetizes for consistent serialization
		memento = convertStructToSorted( memento );
		
		// This could be an empty struct if nothing has been set.
		return memento;
	}

	/**
	* Get a formatted string JSON representation of the config settings
	*/
	function toString(){
		return getJSONPrettyPrint().formatJson( getMemento() );
	}

	/**
	* Set a struct representation of the config settings
	* @memento The config data to set
	*/
	function setMemento( required struct memento ){
		variables.append( memento, true );
		return this;
	}

	/**
	* Merge a memento into the current settings.  Simple settings will overwrite.
	* Complex settings like datasources will be merged via name.
	*
	* @memento The config data to set
	*/
	function mergeMemento( required struct memento, target=variables ){

		// For array configs, here is the name of the nested key in the struct to compare to determine uniqueness.
		// An empty string means the array is just a simple array of strings to compare directly, not a struct.
		var arrayMap = {
			'mailServers' : 'smtp',
			'customTagPaths' : 'physical',
			'customTagMappings' : 'physical',
			'extensionProviders' : '',
			'logFilesDisabled' : '',
			'javaLibraryPaths' : '',
			'customTagsDirectory' : '',
			'customComponentsDirectory' : '',
			'modulesDirectory' : '',
			'eventGatewayInstances' : 'gatewayId',
			'eventGatewayConfigurations' : 'type',
			'componentMappings' : 'virtual',
			'dumpWriters' : 'name',
			'resourceProviders' : 'scheme',
			'extensions' : 'id',
			'cacheClasses' : 'class',
			'validClassExtensions' : '',
			'validTemplateExtensions' : '', // break this out, or externalize the mapping information from it
			'disallowedFileOperationExtensions' : '',
			'scheduledTasks' : 'name',
			'cfcPaths' : ''
		};

		for( var prop in memento ) {
			var setting = memento[ prop ];
			if( isSimpleValue( setting ) ) {
				target[ prop ] = setting;
			} else if( isStruct( setting ) ) {
				target[ prop ] = target[ prop ] ?: {};
				structAppend( target[ prop ], setting, true );
			} else if( isArray( setting ) ) {
				target[ prop ] = target[ prop ] ?: [];
				if( !arrayMap.keyExists( prop ) ) {
					throw( message='Array config type [#prop#] not mapped for merging.  Please report this as a bug.', type='cfconfigException' );
				}
				var uniqueKey = arrayMap[ prop ];
				for( var item in setting ) {
					if( uniqueKey == '' ) {
						// Lucee returns the index, not a boolean
						var exists = target[ prop ].containsNoCase( item );
					} else {
						var exists = target[ prop ].find( (p)=>p.keyExists(uniqueKey) && p[ uniqueKey ] == item[ uniqueKey ] );
					}
					if( !exists ) {
						target[ prop ].append( item );
					} else if( isStruct( item ) ) {
						// Merge the item into the existing one
						mergeMemento( item, target[ prop ][ exists ] );
					}
				}
			}

		}
		return this;
	}

	/**
	* Return cached array of config property names
	*/
	function getConfigProperties(){
		variables.configProperties = variables.configProperties ?: generateConfigProperties();
		return variables.configProperties;
	}

	/**
	* Gnerate array of config property names
	*/
	private function generateConfigProperties(){
		variables.md = variables.md ?: getUtil().getInheritedMetaData( this );
		var configProperties = [];
		for( var prop in md.properties ) {
			if( prop._isCFConfig ?: false ) {
				configProperties.append( prop.name );
			}
		}
		return configProperties;
	}


	/*
	* Turns all slashes in a path to forward slashes except for \\ in a Windows UNC network share
	* Also changes double slashes to a single slash
	*/
	function normalizeSlashes( string path ) {
		if( path.left( 2 ) == '\\' ) {
			return '\\' & path.replace( '\', '/', 'all' ).right( -2 );
		} else {
			return path.replace( '\', '/', 'all' ).replace( '//', '/', 'all' );
		}
	}


	/*
	* Sort a structs keys alphabetically.  There is not native support for
	* sorted structs in Lucee at the time I'm writing this.
	*/
	function convertStructToSorted( required struct unsortedStruct ) {
		var sortedStruct = structNew('ordered');

		// Sort the struct keys and insert them in that order to ordered struct
		unsortedStruct.keyArray().sort( 'textnocase' ).each( function( i ) {
			// Recurse into nested structs
			if( isStruct( unsortedStruct[ i ] ) ) {
				sortedStruct[ i ] = convertStructToSorted( unsortedStruct[ i ] );
			} else {
				sortedStruct[ i ] = unsortedStruct[ i ];
			}
		 } );

		return sortedStruct;
	}

	/**
	* Escapes placeholders like ${foo} in all deep struct keys and array elements with \${foo}.
	* This will recursively follow all nested structs and arrays.
	*
	* @dataStructure A string, struct, or array to perform deep replacement on.
	*/
	function escapeDeepSystemSettings( required any dataStructure ) {
		// If it's a struct...
		if( isStruct( dataStructure ) ) {
			// Loop over and process each key
			for( var key in dataStructure ) {
				var expandedKey = escapeSystemSettings( key );
				if( isNull( dataStructure[ key ] ) ) {
					dataStructure[ expandedKey ] = nullValue();
				} else {
					dataStructure[ expandedKey ] = escapeDeepSystemSettings( dataStructure[ key ] );
				}
				if( expandedKey != key ) dataStructure.delete( key );
			}
			return dataStructure;
		// If it's an array...
		} else if( isArray( dataStructure ) ) {
			var i = 0;
			// Loop over and process each index
			for( var item in dataStructure ) {
				i++;
				if( !isNull( item ) ) {
					dataStructure[ i ] = escapeDeepSystemSettings( item );
				}
			}
			return dataStructure;
		// If it's a string...
		} else if ( isSimpleValue( dataStructure ) ) {
			// Just do the replacement
			return escapeSystemSettings( dataStructure );
		}
		// Other complex variables like XML or CFC instance would just get skipped for now.
		return dataStructure;
	}


	/**
	* Escapes placeholders like ${foo} in a string with \${foo}.
	*
	* @text The string to do the replacement on
	*/
	function escapeSystemSettings( required string text ) {
		// escape all system settings
		return reReplaceNoCase( text, '(\$\{.*?})', '\\1', 'all' );
	}

	any function readJSONC( string configFilePath ) {
		
		if( !fileExists( configFilePath ) ) {
			throw 'The config file #configFilePath# does not exist';
		}

		var configDataStr = fileRead(configFilePath);
		// Remove single-line comments
		configDataStr = reReplace(configDataStr, "(\s|\n)//.*?(\r\n|\n|\r)", "\1\2", "all");
		// Remove multi-line and Javadoc-style comments more accurately
		configDataStr = reReplace(configDataStr, "/\*.*?\*/", "", "all");
		
		try {
			var configData = deserializeJSON(configDataStr);
		} catch (any e) {
			SystemOutput( configDataStr, 1 );
			SystemOutput( 'The config file #configFilePath# is not valid JSON, or the comments weren''t removed properly.', 1 );
			rethrow;
		}
		return configData;
	}


}