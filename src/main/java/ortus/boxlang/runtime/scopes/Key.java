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
package ortus.boxlang.runtime.scopes;

import java.io.Serializable;
import java.util.Arrays;

import ortus.boxlang.runtime.types.exceptions.BoxRuntimeException;

/**
 * Represents a case-insenstive key, while retaining the original case too.
 * Implements the Serializable interface in case duplication is requested within
 * a native HashMap or ArrayList
 */
public class Key implements Comparable<Key>, Serializable {

	// Static instances of common keys
	public static final Key		_0								= Key.of( 0 );
	public static final Key		_1								= Key.of( 1 );
	public static final Key		_2								= Key.of( 2 );
	public static final Key		_3								= Key.of( 3 );
	public static final Key		_4								= Key.of( 4 );
	public static final Key		_5								= Key.of( 5 );
	public static final Key		_6								= Key.of( 6 );
	public static final Key		_7								= Key.of( 7 );
	public static final Key		_8								= Key.of( 8 );
	public static final Key		_9								= Key.of( 9 );
	public static final Key		_10								= Key.of( 10 );
	public static final Key		_slash							= Key.of( "/" );

	// Global Dictionary
	public static final Key		__functionName					= Key.of( "__functionName" );
	public static final Key		__isMemberExecution				= Key.of( "__isMemberExecution" );
	public static final Key		_ANY							= Key.of( "any" );
	public static final Key		_ARRAY							= Key.of( "array" );
	public static final Key		_BOOLEAN						= Key.of( "boolean" );
	public static final Key		_CLASS							= Key.of( "class" );
	public static final Key		_DATE							= Key.of( "date" );
	public static final Key		_DATETIME						= Key.of( "datetime" );
	public static final Key		_DEFAULT						= Key.of( "default" );
	public static final Key		_EMPTY							= Key.of( "" );
	public static final Key		_EXTENDS						= Key.of( "extends" );
	public static final Key		_FILE							= Key.of( "file" );
	public static final Key		_HASHCODE						= Key.of( "hashcode" );
	public static final Key		_IMPLEMENTS						= Key.of( "implements" );
	public static final Key		_LIST							= Key.of( "list" );
	public static final Key		_name							= Key.of( "name" );
	public static final Key		_NAME							= Key.of( "name" );
	public static final Key		_NUMERIC						= Key.of( "numeric" );
	public static final Key		_PACKAGE						= Key.of( "package" );
	public static final Key		_QUERY							= Key.of( "query" );
	public static final Key		_STRING							= Key.of( "string" );
	public static final Key		_STRUCT							= Key.of( "struct" );
	public static final Key		_super							= Key.of( "super" );
	public static final Key		_UDF							= Key.of( "udf" );
	public static final Key		$bx								= Key.of( "$bx" );
	public static final Key		accept							= Key.of( "accept" );
	public static final Key		access							= Key.of( "access" );
	public static final Key		accessors						= Key.of( "accessors" );
	public static final Key		action							= Key.of( "action" );
	public static final Key		addnewline						= Key.of( "addnewline" );
	public static final Key		addToken						= Key.of( "addToken" );
	public static final Key		algorithm						= Key.of( "algorithm" );
	public static final Key		allow							= Key.of( "allow" );
	public static final Key		allowRealPath					= Key.of( "allowRealPath" );
	public static final Key		annotations						= Key.of( "annotations" );
	public static final Key		ANONYMOUSCLOSURE				= Key.of( "ANONYMOUSCLOSURE" );
	public static final Key		ANONYMOUSLAMBDA					= Key.of( "ANONYMOUSLAMBDA" );
	public static final Key		append							= Key.of( "append" );
	public static final Key		applicationName					= Key.of( "applicationName" );
	public static final Key		applicationService				= Key.of( "applicationService" );
	public static final Key		applicationSettings				= Key.of( "applicationSettings" );
	public static final Key		argumentCollection				= Key.of( "argumentCollection" );
	public static final Key		arguments						= Key.of( "arguments" );
	public static final Key		array							= Key.of( "array" );
	public static final Key		array1							= Key.of( "array1" );
	public static final Key		array2							= Key.of( "array2" );
	public static final Key		arrayFind						= Key.of( "arrayFind" );
	public static final Key		arrayFindAll					= Key.of( "arrayFindAll" );
	public static final Key		asOptional						= Key.of( "asOptional" );
	public static final Key		assocAttribs					= Key.of( "assocAttribs" );
	public static final Key		asyncService					= Key.of( "asyncService" );
	public static final Key		attribute						= Key.of( "attribute" );
	public static final Key		attributeCollection				= Key.of( "attributeCollection" );
	public static final Key		attributes						= Key.of( "attributes" );
	public static final Key		auth_password					= Key.of( "auth_password" );
	public static final Key		auth_type						= Key.of( "auth_type" );
	public static final Key		auth_user						= Key.of( "auth_user" );
	public static final Key		author							= Key.of( "author" );
	public static final Key		authType						= Key.of( "authType" );
	public static final Key		base64_or_object				= Key.of( "base64_or_object " );
	public static final Key		binary							= Key.of( "binary" );
	public static final Key		body							= Key.of( "body" );
	public static final Key		boxBif							= Key.of( "BoxBif" );
	public static final Key		boxCacheProvider				= Key.of( "BoxCacheProvider" );
	public static final Key		boxlang							= Key.of( "boxlang" );
	public static final Key		boxlangSessions					= Key.of( "boxlangSessions" );
	public static final Key		boxMember						= Key.of( "BoxMember" );
	public static final Key		boxRuntime						= Key.of( "boxRuntime" );
	public static final Key		buffersize						= Key.of( "buffersize" );
	public static final Key		bxDefaultDatasource				= Key.of( "bxDefaultDatasource" );
	public static final Key		bxRandomSeed					= Key.of( "bxRandomSeed" );
	public static final Key		cached							= Key.of( "cached" );
	public static final Key		cachedwithin					= Key.of( "cachedwithin" );
	public static final Key		cachedWithin					= Key.of( "cachedWithin" );
	public static final Key		cacheName						= Key.of( "cacheName" );
	public static final Key		caches							= Key.of( "caches" );
	public static final Key		cacheService					= Key.of( "cacheService" );
	public static final Key		callback						= Key.of( "callback" );
	public static final Key		caller							= Key.of( "caller" );
	public static final Key		canonicalize					= Key.of( "canonicalize" );
	public static final Key		caseSensitive					= Key.of( "caseSensitive" );
	public static final Key		cause							= Key.of( "cause" );
	public static final Key		cert_cookie						= Key.of( "cert_cookie" );
	public static final Key		cert_flags						= Key.of( "cert_flags" );
	public static final Key		cert_issuer						= Key.of( "cert_issuer" );
	public static final Key		cert_keysize					= Key.of( "cert_keysize" );
	public static final Key		cert_secretkeysize				= Key.of( "cert_secretkeysize" );
	public static final Key		cert_serialnumber				= Key.of( "cert_serialnumber" );
	public static final Key		cert_server_issuer				= Key.of( "cert_server_issuer" );
	public static final Key		cert_server_subject				= Key.of( "cert_server_subject" );
	public static final Key		cert_subject					= Key.of( "cert_subject" );
	public static final Key		cf_template_path				= Key.of( "cf_template_path" );
	public static final Key		cfid							= Key.of( "cfid" );
	public static final Key		cftoken							= Key.of( "cftoken" );
	public static final Key		cfvar							= Key.of( "cfvar" );
	public static final Key		charset							= Key.of( "charset" );
	public static final Key		charsetOrBufferSize				= Key.of( "charsetOrBufferSize" );
	public static final Key		childname						= Key.of( "childname" );
	public static final Key		classGenerationDirectory		= Key.of( "classGenerationDirectory" );
	public static final Key		className						= Key.of( "className" );
	public static final Key		clazz							= Key.of( "clazz" );
	public static final Key		clientCert						= Key.of( "clientCert" );
	public static final Key		clientCertPassword				= Key.of( "clientCertPassword" );
	public static final Key		closure							= Key.of( "closure" );
	public static final Key		codePrintHTML					= Key.of( "codePrintHTML" );
	public static final Key		codePrintPlain					= Key.of( "codePrintPlain" );
	public static final Key		coldfusion						= Key.of( "coldfusion" );
	public static final Key		collection						= Key.of( "collection" );
	public static final Key		column							= Key.of( "column" );
	public static final Key		column_name						= Key.of( "column_name" );
	public static final Key		columnKey						= Key.of( "columnKey" );
	public static final Key		columnList						= Key.of( "columnList" );
	public static final Key		columnName						= Key.of( "columnName" );
	public static final Key		columns							= Key.of( "columns" );
	public static final Key		columnType						= Key.of( "columnType" );
	public static final Key		columnTypeList					= Key.of( "columnTypeList" );
	public static final Key		compiler						= Key.of( "compiler" );
	public static final Key		component						= Key.of( "component" );
	public static final Key		componentService				= Key.of( "componentService" );
	public static final Key		compression						= Key.of( "compression" );
	public static final Key		condition						= Key.of( "condition" );
	public static final Key		configure						= Key.of( "configure" );
	public static final Key		content							= Key.of( "content" );
	public static final Key		content_length					= Key.of( "content_length" );
	public static final Key		content_type					= Key.of( "content_type" );
	public static final Key		context							= Key.of( "context" );
	public static final Key		context_path					= Key.of( "context_path" );
	public static final Key		contextual						= Key.of( "contextual" );
	public static final Key		conversionType					= Key.of( "conversionType" );
	public static final Key		cookies							= Key.of( "cookies" );
	public static final Key		copy							= Key.of( "copy" );
	public static final Key		count							= Key.of( "count" );
	public static final Key		country							= Key.of( "country" );
	public static final Key		create							= Key.of( "create" );
	public static final Key		createPath						= Key.of( "createPath" );
	public static final Key		currentRow						= Key.of( "currentRow" );
	public static final Key		customInterceptionPoints		= Key.of( "customInterceptionPoints" );
	public static final Key		customTagsDirectory				= Key.of( "customTagsDirectory" );
	public static final Key		data							= Key.of( "data" );
	public static final Key		datasource						= Key.of( "datasource" );
	public static final Key		datasources						= Key.of( "datasources" );
	public static final Key		datasourceService				= Key.of( "datasourceService" );
	public static final Key		datatype						= Key.of( "datatype" );
	public static final Key		date							= Key.of( "date" );
	public static final Key		date1							= Key.of( "date1" );
	public static final Key		date2							= Key.of( "date2" );
	public static final Key		dateFormat						= Key.of( "dateFormat" );
	public static final Key		dateLastModified				= Key.of( "dateLastModified" );
	public static final Key		datepart						= Key.of( "datepart" );
	public static final Key		day								= Key.of( "day" );
	public static final Key		days							= Key.of( "days" );
	public static final Key		debugInfo						= Key.of( "debugInfo" );
	public static final Key		debugMode						= Key.of( "debugMode" );
	public static final Key		deep							= Key.of( "deep" );
	public static final Key		defaultCache					= Key.of( "defaultCache" );
	public static final Key		defaultDatasource				= Key.of( "defaultDatasource" );
	public static final Key		defaultLastAccessTimeout		= Key.of( "defaultLastAccessTimeout" );
	public static final Key		defaultTimeout					= Key.of( "defaultTimeout" );
	public static final Key		defaultValue					= Key.of( "defaultValue" );
	public static final Key		delete							= Key.of( "delete" );
	public static final Key		deleteFile						= Key.of( "deleteFile" );
	public static final Key		delimiter						= Key.of( "delimiter" );
	public static final Key		delimiters						= Key.of( "delimiters" );
	public static final Key		dependsOn						= Key.of( "dependsOn" );
	public static final Key		depth							= Key.of( "depth" );
	public static final Key		description						= Key.of( "description" );
	public static final Key		descriptor						= Key.of( "descriptor" );
	public static final Key		destination						= Key.of( "destination" );
	public static final Key		detail							= Key.of( "detail" );
	public static final Key		dimensions						= Key.of( "dimensions" );
	public static final Key		directory						= Key.of( "directory" );
	public static final Key		directoryCopy					= Key.of( "directoryCopy" );
	public static final Key		directoryCreate					= Key.of( "directoryCreate" );
	public static final Key		directoryDelete					= Key.of( "directoryDelete" );
	public static final Key		directoryList					= Key.of( "directoryList" );
	public static final Key		directoryMove					= Key.of( "directoryMove" );
	public static final Key		disabled						= Key.of( "disabled" );
	public static final Key		display							= Key.of( "display" );
	public static final Key		doAll							= Key.of( "doAll" );
	public static final Key		documentation					= Key.of( "documentation" );
	public static final Key		dollarFormat					= Key.of( "dollarFormat" );
	public static final Key		doLowerIfAllUppercase			= Key.of( "doLowerIfAllUppercase" );
	public static final Key		domain							= Key.of( "domain" );
	public static final Key		dspLocale						= Key.of( "dspLocale" );
	public static final Key		dump							= Key.of( "dump" );
	public static final Key		duration						= Key.of( "duration" );
	public static final Key		elapsedTime						= Key.of( "elapsedTime" );
	public static final Key		elem							= Key.of( "elem" );
	public static final Key		elementCountForRemoval			= Key.of( "elementCountForRemoval" );
	public static final Key		elements						= Key.of( "elements" );
	public static final Key		EMPTY							= Key.of( "" );
	public static final Key		enabled							= Key.of( "enabled" );
	public static final Key		enableOutputOnly				= Key.of( "enableOutputOnly" );
	public static final Key		encoded							= Key.of( "encoded" );
	public static final Key		encoded_binary					= Key.of( "encoded_binary" );
	public static final Key		encodefor						= Key.of( "encodefor" );
	public static final Key		encodeUrl						= Key.of( "encodeUrl" );
	public static final Key		encoding						= Key.of( "encoding" );
	public static final Key		encodingBase64					= Key.of( "Base64" );
	public static final Key		encodingBase64Url				= Key.of( "Base64Url" );
	public static final Key		encodingHex						= Key.of( "Hex" );
	public static final Key		encodingUU						= Key.of( "UU" );
	public static final Key		end								= Key.of( "end" );
	public static final Key		endRow							= Key.of( "endRow" );
	public static final Key		enforceExplicitOutput			= Key.of( "enforceExplicitOutput" );
	public static final Key		environment						= Key.of( "environment" );
	public static final Key		error							= Key.of( "error" );
	public static final Key		errorcode						= Key.of( "errorcode" );
	public static final Key		errorDetail						= Key.of( "errorDetail" );
	public static final Key		errors							= Key.of( "errors" );
	public static final Key		escapeChars						= Key.of( "escapeChars" );
	public static final Key		evictCount						= Key.of( "evictCount" );
	public static final Key		evictionPolicy					= Key.of( "evictionPolicy" );
	public static final Key		execute							= Key.of( "execute" );
	public static final Key		executionMode					= Key.of( "executionMode" );
	public static final Key		executionState					= Key.of( "executionState" );
	public static final Key		executionTime					= Key.of( "executionTime" );
	public static final Key		expires							= Key.of( "expires" );
	public static final Key		expireURL						= Key.of( "expireURL" );
	public static final Key		explanation						= Key.of( "explanation" );
	public static final Key		expression						= Key.of( "expression" );
	public static final Key		expression1						= Key.of( "expression1" );
	public static final Key		expression2						= Key.of( "expression2" );
	public static final Key		expressions						= Key.of( "expressions" );
	public static final Key		extendedinfo					= Key.of( "extendedinfo" );
	public static final Key		fatalErrors						= Key.of( "fatalErrors" );
	public static final Key		file							= Key.of( "file" );
	public static final Key		fileContent						= Key.of( "fileContent" );
	public static final Key		filefield						= Key.of( "filefield" );
	public static final Key		filepath						= Key.of( "filepath" );
	public static final Key		filter							= Key.of( "filter" );
	public static final Key		find							= Key.of( "find" );
	public static final Key		findAll							= Key.of( "findAll" );
	public static final Key		findNoCase						= Key.of( "findNoCase" );
	public static final Key		firstRowAsHeaders				= Key.of( "firstRowAsHeaders" );
	public static final Key		fixnewline						= Key.of( "fixnewline" );
	public static final Key		format							= Key.of( "format" );
	public static final Key		freeMemoryPercentageThreshold	= Key.of( "freeMemoryPercentageThreshold" );
	public static final Key		from							= Key.of( "from" );
	public static final Key		fullname						= Key.of( "fullname" );
	public static final Key		function						= Key.of( "function" );
	public static final Key		functions						= Key.of( "functions" );
	public static final Key		functionService					= Key.of( "functionService" );
	public static final Key		gateway_interface				= Key.of( "gateway_interface" );
	public static final Key		generatedContent				= Key.of( "generatedContent" );
	public static final Key		generic							= Key.of( "generic" );
	public static final Key		getAsBinary						= Key.of( "getAsBinary" );
	public static final Key		getClass						= Key.of( "getClass" );
	public static final Key		getFileInfo						= Key.of( "getFileInfo" );
	public static final Key		group							= Key.of( "group" );
	public static final Key		groupCaseSensitive				= Key.of( "groupCaseSensitive" );
	public static final Key		hasEndTag						= Key.of( "hasEndTag" );
	public static final Key		hash40							= Key.of( "hash40" );
	public static final Key		header							= Key.of( "header" );
	public static final Key		headers							= Key.of( "headers" );
	public static final Key		hint							= Key.of( "hint" );
	public static final Key		hostname						= Key.of( "hostname" );
	public static final Key		hour							= Key.of( "hour" );
	public static final Key		hours							= Key.of( "hours" );
	public static final Key		HTTP							= Key.of( "http" );
	public static final Key		http_accept						= Key.of( "http_accept" );
	public static final Key		http_accept_encoding			= Key.of( "http_accept_encoding" );
	public static final Key		http_accept_language			= Key.of( "http_accept_language" );
	public static final Key		http_connection					= Key.of( "http_connection" );
	public static final Key		http_cookie						= Key.of( "http_cookie" );
	public static final Key		http_host						= Key.of( "http_host" );
	public static final Key		http_referer					= Key.of( "http_referer" );
	public static final Key		http_user_agent					= Key.of( "http_user_agent" );
	public static final Key		HTTP_Version					= Key.of( "http_version" );
	public static final Key		httpOnly						= Key.of( "httpOnly" );
	public static final Key		HTTPParams						= Key.of( "httpParams" );
	public static final Key		https							= Key.of( "https" );
	public static final Key		https_keysize					= Key.of( "https_keysize" );
	public static final Key		https_secretkeysize				= Key.of( "https_secretkeysize" );
	public static final Key		https_server_issuer				= Key.of( "https_server_issuer" );
	public static final Key		https_server_subject			= Key.of( "https_server_subject" );
	public static final Key		id								= Key.of( "id" );
	public static final Key		idleTime						= Key.of( "idleTime" );
	public static final Key		ignoreCase						= Key.of( "ignoreCase" );
	public static final Key		ignoreExists					= Key.of( "ignoreExists" );
	public static final Key		includeBody						= Key.of( "includeBody" );
	public static final Key		includeEmptyFields				= Key.of( "includeEmptyFields" );
	public static final Key		index							= Key.of( "index" );
	public static final Key		indicateNotExists				= Key.of( "indicateNotExists" );
	public static final Key		init							= Key.of( "init" );
	public static final Key		initialValue					= Key.of( "initialValue" );
	public static final Key		initMethod						= Key.of( "initMethod" );
	public static final Key		input							= Key.of( "input" );
	public static final Key		inserts							= Key.of( "inserts" );
	public static final Key		instance						= Key.of( "instance" );
	public static final Key		interceptionPoint				= Key.of( "interceptionPoint" );
	public static final Key		interceptor						= Key.of( "interceptor" );
	public static final Key		interceptors					= Key.of( "interceptors" );
	public static final Key		interceptorService				= Key.of( "interceptorService" );
	public static final Key		interfaces						= Key.of( "interfaces" );
	public static final Key		interrupted						= Key.of( "interrupted" );
	public static final Key		invoke							= Key.of( "invoke" );
	public static final Key		invokeArgs						= Key.of( "invokeArgs" );
	public static final Key		invokeImplicitAccessor			= Key.of( "invokeImplicitAccessor" );
	public static final Key		ip								= Key.of( "ip" );
	public static final Key		iso								= Key.of( "iso" );
	public static final Key		item							= Key.of( "item" );
	public static final Key		java							= Key.of( "java" );
	public static final Key		javaLibraryPaths				= Key.of( "javaLibraryPaths" );
	public static final Key		javascriptvar					= Key.of( "javascriptvar" );
	public static final Key		join							= Key.of( "join" );
	public static final Key		json							= Key.of( "json" );
	public static final Key		key								= Key.of( "key" );
	public static final Key		label							= Key.of( "label" );
	public static final Key		lambda							= Key.of( "lambda" );
	public static final Key		language						= Key.of( "language" );
	public static final Key		lastVisit						= Key.of( "lastVisit" );
	public static final Key		leaveIndex						= Key.of( "leaveIndex" );
	public static final Key		len								= Key.of( "len" );
	public static final Key		length							= Key.of( "length" );
	public static final Key		level							= Key.of( "level" );
	public static final Key		lexical							= Key.of( "lexical" );
	public static final Key		limit							= Key.of( "limit" );
	public static final Key		line							= Key.of( "line" );
	public static final Key		list							= Key.of( "list" );
	public static final Key		listInfo						= Key.of( "listInfo" );
	public static final Key		listToJSON						= Key.of( "listToJSON" );
	public static final Key		lJustify						= Key.of( "lJustify" );
	public static final Key		local_addr						= Key.of( "local_addr" );
	public static final Key		local_host						= Key.of( "local_host" );
	public static final Key		locale							= Key.of( "locale" );
	public static final Key		localeSensitive					= Key.of( "localeSensitive" );
	public static final Key		log								= Key.of( "log" );
	public static final Key		logger							= Key.of( "logger" );
	public static final Key		lucee							= Key.of( "lucee" );
	public static final Key		main							= Key.of( "main" );
	public static final Key		mapping							= Key.of( "mapping" );
	public static final Key		mappings						= Key.of( "mappings" );
	public static final Key		mask							= Key.of( "mask" );
	public static final Key		match							= Key.of( "match" );
	public static final Key		max								= Key.of( "max" );
	public static final Key		maxFrames						= Key.of( "maxFrames" );
	public static final Key		maxLength						= Key.of( "maxLength" );
	public static final Key		maxObjects						= Key.of( "maxObjects" );
	public static final Key		maxRows							= Key.of( "maxRows" );
	public static final Key		maxThreads						= Key.of( "maxThreads" );
	public static final Key		merge							= Key.of( "merge" );
	public static final Key		message							= Key.of( "message" );
	public static final Key		metadata						= Key.of( "metadata" );
	public static final Key		method							= Key.of( "method" );
	public static final Key		methodname						= Key.of( "methodname" );
	public static final Key		millisecond						= Key.of( "millisecond" );
	public static final Key		milliseconds					= Key.of( "milliseconds" );
	public static final Key		mimetype						= Key.of( "mimetype " );
	public static final Key		min								= Key.of( "min" );
	public static final Key		minute							= Key.of( "minute" );
	public static final Key		minutes							= Key.of( "minutes" );
	public static final Key		missingMethodArguments			= Key.of( "missingMethodArguments" );
	public static final Key		missingMethodName				= Key.of( "missingMethodName" );
	public static final Key		mode							= Key.of( "mode" );
	public static final Key		module							= Key.of( "module" );
	public static final Key		moduleMapping					= Key.of( "moduleMapping" );
	public static final Key		moduleName						= Key.of( "moduleName" );
	public static final Key		moduleRecord					= Key.of( "moduleRecord" );
	public static final Key		modules							= Key.of( "modules" );
	public static final Key		modulesDirectory				= Key.of( "modulesDirectory" );
	public static final Key		moduleService					= Key.of( "moduleService" );
	public static final Key		month							= Key.of( "month" );
	public static final Key		move							= Key.of( "move" );
	public static final Key		multiCharacterDelimiter			= Key.of( "multiCharacterDelimiter" );
	public static final Key		multipart						= Key.of( "multipart" );
	public static final Key		multipartType					= Key.of( "multipartType" );
	public static final Key		n								= Key.of( "n" );
	public static final Key		nameAsKey						= Key.of( "nameAsKey" );
	public static final Key		nameconflict					= Key.of( "nameconflict" );
	public static final Key		namespace						= Key.of( "namespace" );
	public static final Key		newDelimiter					= Key.of( "newDelimiter" );
	public static final Key		newDirectory					= Key.of( "newDirectory" );
	public static final Key		newPath							= Key.of( "newPath" );
	public static final Key		noInit							= Key.of( "noInit" );
	public static final Key		nulls							= Key.of( "null" );
	public static final Key		number							= Key.of( "number" );
	public static final Key		number1							= Key.of( "number1" );
	public static final Key		number2							= Key.of( "number2" );
	public static final Key		numIterations					= Key.of( "numIterations" );
	public static final Key		obj								= Key.of( "obj" );
	public static final Key		object							= Key.of( "object" );
	public static final Key		objectArgument					= Key.of( "objectArgument" );
	public static final Key		objectMappings					= Key.of( "objectMappings" );
	public static final Key		objectStore						= Key.of( "objectStore" );
	public static final Key		offset							= Key.of( "offset" );
	public static final Key		oldPath							= Key.of( "oldPath" );
	public static final Key		onAbort							= Key.of( "onAbort" );
	public static final Key		onApplicationEnd				= Key.of( "onApplicationEnd" );
	public static final Key		onApplicationRestart			= Key.of( "onApplicationRestart" );
	public static final Key		onApplicationStart				= Key.of( "onApplicationStart" );
	public static final Key		onError							= Key.of( "onError" );
	public static final Key		onLoad							= Key.of( "onLoad" );
	public static final Key		onMissingMethod					= Key.of( "onMissingMethod" );
	public static final Key		onMissingTemplate				= Key.of( "onMissingTemplate" );
	public static final Key		onParse							= Key.of( "onParse" );
	public static final Key		onRequest						= Key.of( "onRequest" );
	public static final Key		onRequestEnd					= Key.of( "onRequestEnd" );
	public static final Key		onRequestStart					= Key.of( "onRequestStart" );
	public static final Key		onSessionEnd					= Key.of( "onSessionEnd" );
	public static final Key		onSessionStart					= Key.of( "onSessionStart" );
	public static final Key		onUnload						= Key.of( "onUnload" );
	public static final Key		options							= Key.of( "options" );
	public static final Key		ordered							= Key.of( "ordered" );
	public static final Key		os								= Key.of( "os" );
	public static final Key		output							= Key.of( "output" );
	public static final Key		overwrite						= Key.of( "overwrite" );
	public static final Key		owner							= Key.of( "owner" );
	public static final Key		pageEncoding					= Key.of( "pageEncoding" );
	public static final Key		parallel						= Key.of( "parallel" );
	public static final Key		parameters						= Key.of( "parameters" );
	public static final Key		params							= Key.of( "params" );
	public static final Key		path							= Key.of( "path" );
	public static final Key		path_info						= Key.of( "path_info" );
	public static final Key		path_translated					= Key.of( "path_translated" );
	public static final Key		pattern							= Key.of( "pattern" );
	public static final Key		pid								= Key.of( "pid" );
	public static final Key		pos								= Key.of( "pos" );
	public static final Key		position						= Key.of( "position" );
	public static final Key		position1						= Key.of( "position1" );
	public static final Key		position2						= Key.of( "position2" );
	public static final Key		prefix							= Key.of( "prefix" );
	public static final Key		priority						= Key.of( "priority" );
	public static final Key		properties						= Key.of( "properties" );
	public static final Key		protocol						= Key.of( "protocol" );
	public static final Key		proxyPassword					= Key.of( "proxyPassword" );
	public static final Key		proxyPort						= Key.of( "proxyPort" );
	public static final Key		proxyServer						= Key.of( "proxyServer" );
	public static final Key		proxyUser						= Key.of( "proxyUser" );
	public static final Key		qualifier						= Key.of( "qualifier" );
	public static final Key		quarter							= Key.of( "quarter" );
	public static final Key		query							= Key.of( "query" );
	public static final Key		query_string					= Key.of( "query_string" );
	public static final Key		query1							= Key.of( "query1" );
	public static final Key		query2							= Key.of( "query2" );
	public static final Key		queryFormat						= Key.of( "queryFormat" );
	public static final Key		queryParams						= Key.of( "queryParams" );
	public static final Key		queryTimeout					= Key.of( "queryTimeout" );
	public static final Key		radix							= Key.of( "radix" );
	public static final Key		Raw_Trace						= Key.of( "Raw_Trace" );
	public static final Key		read							= Key.of( "read" );
	public static final Key		readBinary						= Key.of( "readBinary" );
	public static final Key		reapFrequency					= Key.of( "reapFrequency" );
	public static final Key		recordCount						= Key.of( "recordCount" );
	public static final Key		recurse							= Key.of( "recurse" );
	public static final Key		recursive						= Key.of( "recursive" );
	public static final Key		redirect						= Key.of( "redirect" );
	public static final Key		reg_expression					= Key.of( "reg_expression" );
	public static final Key		regex							= Key.of( "regex" );
	public static final Key		region							= Key.of( "region" );
	public static final Key		remote_addr						= Key.of( "remote_addr" );
	public static final Key		remote_host						= Key.of( "remote_host" );
	public static final Key		remote_user						= Key.of( "remote_user" );
	public static final Key		rename							= Key.of( "rename" );
	public static final Key		replacements					= Key.of( "replacements" );
	public static final Key		request_method					= Key.of( "request_method" );
	public static final Key		request_url						= Key.of( "request_url" );
	public static final Key		requestTimeout					= Key.of( "requestTimeout" );
	public static final Key		required						= Key.of( "required" );
	public static final Key		reset							= Key.of( "reset" );
	public static final Key		resetTimeoutOnAccess			= Key.of( "resetTimeoutOnAccess" );
	public static final Key		resolveUrl						= Key.of( "resolveUrl" );
	public static final Key		response						= Key.of( "response" );
	public static final Key		responseHeader					= Key.of( "responseHeader" );
	public static final Key		result							= Key.of( "result" );
	public static final Key		retainKeys						= Key.of( "retainKeys" );
	public static final Key		returnSubExpressions			= Key.of( "returnSubExpressions" );
	public static final Key		returnType						= Key.of( "returnType" );
	public static final Key		returnVariable					= Key.of( "returnVariable" );
	public static final Key		rJustify						= Key.of( "rJustify" );
	public static final Key		row								= Key.of( "row" );
	public static final Key		row_number						= Key.of( "row_number" );
	public static final Key		rowData							= Key.of( "rowData" );
	public static final Key		rowNumber						= Key.of( "rowNumber" );
	public static final Key		run								= Key.of( "run" );
	public static final Key		runtime							= Key.of( "runtime" );
	public static final Key		samesite						= Key.of( "samesite" );
	public static final Key		scale							= Key.of( "scale" );
	public static final Key		schedulerService				= Key.of( "schedulerService" );
	public static final Key		scope							= Key.of( "scope" );
	public static final Key		script_name						= Key.of( "script_name" );
	public static final Key		second							= Key.of( "second" );
	public static final Key		seconds							= Key.of( "seconds" );
	public static final Key		secure							= Key.of( "secure" );
	public static final Key		seed							= Key.of( "seed" );
	public static final Key		seekable						= Key.of( "seekable" );
	public static final Key		separator						= Key.of( "separator" );
	public static final Key		serializeQueryByColumns			= Key.of( "serializeQueryByColumns" );
	public static final Key		server							= Key.of( "server" );
	public static final Key		server_name						= Key.of( "server_name" );
	public static final Key		server_port						= Key.of( "server_port" );
	public static final Key		server_port_secure				= Key.of( "server_port_secure" );
	public static final Key		server_protocol					= Key.of( "server_protocol" );
	public static final Key		server_software					= Key.of( "server_software" );
	public static final Key		servlet							= Key.of( "servlet" );
	public static final Key		sessionCluster					= Key.of( "sessionCluster" );
	public static final Key		sessionId						= Key.of( "sessionId" );
	public static final Key		sessionManagement				= Key.of( "sessionManagement" );
	public static final Key		sessions						= Key.of( "sessions" );
	public static final Key		sessionStorage					= Key.of( "sessionStorage" );
	public static final Key		sessionTimeout					= Key.of( "sessionTimeout" );
	public static final Key		set								= Key.of( "set" );
	public static final Key		settings						= Key.of( "settings" );
	public static final Key		showDebugOutput					= Key.of( "showDebugOutput" );
	public static final Key		showerror						= Key.of( "showerror" );
	public static final Key		size							= Key.of( "size" );
	public static final Key		sleep							= Key.of( "sleep" );
	public static final Key		sort							= Key.of( "sort" );
	public static final Key		sortFunc						= Key.of( "sortFunc" );
	public static final Key		sortOrder						= Key.of( "sortOrder" );
	public static final Key		sortType						= Key.of( "sortType" );
	public static final Key		source							= Key.of( "source" );
	public static final Key		sql								= Key.of( "sql" );
	public static final Key		sqlParameters					= Key.of( "sqlParameters" );
	public static final Key		sqltype							= Key.of( "sqltype" );
	public static final Key		stackTrace						= Key.of( "stackTrace" );
	public static final Key		start							= Key.of( "start" );
	public static final Key		startRow						= Key.of( "startRow" );
	public static final Key		startTicks						= Key.of( "startTicks" );
	public static final Key		startTime						= Key.of( "startTime" );
	public static final Key		state							= Key.of( "state" );
	public static final Key		status							= Key.of( "status" );
	public static final Key		status_code						= Key.of( "status_code" );
	public static final Key		status_text						= Key.of( "status_text" );
	public static final Key		statusCode						= Key.of( "statusCode" );
	public static final Key		statusText						= Key.of( "statusText" );
	public static final Key		storedproc						= Key.of( "storedproc" );
	public static final Key		strict							= Key.of( "strict" );
	public static final Key		strictMapping					= Key.of( "strictMapping" );
	public static final Key		string							= Key.of( "string" );
	public static final Key		string_or_object				= Key.of( "string_or_object" );
	public static final Key		string1							= Key.of( "string1" );
	public static final Key		string2							= Key.of( "string2" );
	public static final Key		strip							= Key.of( "strip" );
	public static final Key		stripWhitespace					= Key.of( "stripWhitespace" );
	public static final Key		struct							= Key.of( "struct" );
	public static final Key		struct1							= Key.of( "struct1" );
	public static final Key		struct2							= Key.of( "struct2" );
	public static final Key		structure						= Key.of( "structure" );
	public static final Key		substring						= Key.of( "substring" );
	public static final Key		substring1						= Key.of( "substring1" );
	public static final Key		substringMatch					= Key.of( "substringMatch" );
	public static final Key		suffix							= Key.of( "suffix" );
	public static final Key		suppressWhiteSpace				= Key.of( "suppressWhiteSpace" );
	public static final Key		system							= Key.of( "system" );
	public static final Key		systemExecute					= Key.of( "systemExecute" );
	public static final Key		target							= Key.of( "target" );
	public static final Key		tagContext						= Key.of( "tagContext" );
	public static final Key		template						= Key.of( "template" );
	public static final Key		terminate						= Key.of( "terminate" );
	public static final Key		terminated						= Key.of( "terminated" );
	public static final Key		terminateOnTimeout				= Key.of( "terminateOnTimeout" );
	public static final Key		text							= Key.of( "text" );
	public static final Key		textQualifier					= Key.of( "textQualifier" );
	public static final Key		thisTag							= Key.of( "thisTag" );
	public static final Key		thread							= Key.of( "thread" );
	public static final Key		throwOnError					= Key.of( "throwOnError" );
	public static final Key		throwOnTimeout					= Key.of( "throwOnTimeout" );
	public static final Key		time							= Key.of( "time" );
	public static final Key		timeCreated						= Key.of( "timeCreated" );
	public static final Key		timeFormat						= Key.of( "timeFormat" );
	public static final Key		timeout							= Key.of( "timeout" );
	public static final Key		timespan						= Key.of( "timespan" );
	public static final Key		timezone						= Key.of( "timezone" );
	public static final Key		to								= Key.of( "to" );
	public static final Key		token							= Key.of( "token" );
	public static final Key		trim							= Key.of( "trim" );
	public static final Key		type							= Key.of( "type" );
	public static final Key		typename						= Key.of( "typename" );
	public static final Key		unit							= Key.of( "unit" );
	public static final Key		upload							= Key.of( "upload" );
	public static final Key		uploadAll						= Key.of( "uploadAll" );
	public static final Key		URL								= Key.of( "URL" );
	public static final Key		urlToken						= Key.of( "urlToken" );
	public static final Key		useCache						= Key.of( "useCache" );
	public static final Key		useCustomSerializer				= Key.of( "useCustomSerializer" );
	public static final Key		useLastAccessTimeouts			= Key.of( "useLastAccessTimeouts" );
	public static final Key		useQueryString					= Key.of( "useQueryString" );
	public static final Key		userAgent						= Key.of( "userAgent" );
	public static final Key		useRegex						= Key.of( "useRegex" );
	public static final Key		useSecureJSONPrefix				= Key.of( "useSecureJSONPrefix" );
	public static final Key		validator						= Key.of( "validator" );
	public static final Key		validators						= Key.of( "validators" );
	public static final Key		value							= Key.of( "value" );
	public static final Key		var								= Key.of( "var" );
	public static final Key		variable						= Key.of( "variable" );
	public static final Key		variables						= Key.of( "variables" );
	public static final Key		variant							= Key.of( "variant" );
	public static final Key		version							= Key.of( "version" );
	public static final Key		warning							= Key.of( "warning" );
	public static final Key		web_server_api					= Key.of( "web_server_api" );
	public static final Key		webURL							= Key.of( "webURL" );
	public static final Key		workstation						= Key.of( "workstation" );
	public static final Key		write							= Key.of( "write" );
	public static final Key		XML								= Key.of( "XML" );
	public static final Key		XMLAttributes					= Key.of( "XMLAttributes" );
	public static final Key		XMLCdata						= Key.of( "XMLCdata" );
	public static final Key		XMLChildren						= Key.of( "XMLChildren" );
	public static final Key		XMLComment						= Key.of( "XMLComment" );
	public static final Key		XMLDocType						= Key.of( "XMLDocType" );
	public static final Key		XMLName							= Key.of( "XMLName" );
	public static final Key		XMLNode							= Key.of( "XMLNode" );
	public static final Key		XMLNodes						= Key.of( "XMLNodes" );
	public static final Key		XMLNsPrefix						= Key.of( "XMLNsPrefix" );
	public static final Key		XMLNsURI						= Key.of( "XMLNsURI" );
	public static final Key		XMLParent						= Key.of( "XMLParent" );
	public static final Key		XMLRoot							= Key.of( "XMLRoot" );
	public static final Key		XMLText							= Key.of( "XMLText" );
	public static final Key		XMLType							= Key.of( "XMLType" );
	public static final Key		XMLValue						= Key.of( "XMLValue" );
	public static final Key		xpath							= Key.of( "xpath" );
	public static final Key		XSL								= Key.of( "XSL" );
	public static final Key		year							= Key.of( "year" );

	/**
	 * JDBC keys.
	 */
	public static final Key		driver							= Key.of( "driver" );
	public static final Key		host							= Key.of( "host" );
	public static final Key		port							= Key.of( "port" );
	public static final Key		username						= Key.of( "username" );
	public static final Key		password						= Key.of( "password" );
	public static final Key		dbname							= Key.of( "dbname" );
	public static final Key		table							= Key.of( "table" );
	public static final Key		isolation						= Key.of( "isolation" );
	public static final Key		procedure						= Key.of( "procedure" );
	public static final Key		procResult						= Key.of( "procResult" );
	public static final Key		resultSet						= Key.of( "resultSet" );
	public static final Key		savepoint						= Key.of( "savepoint" );
	public static final Key		nested							= Key.of( "nested" );
	public static final Key		onTheFly						= Key.of( "onTheFly" );
	// Datasource configuration keys
	public static final Key		connectionString				= Key.of( "connectionString" );
	public static final Key		minConnections					= Key.of( "minConnections" );
	public static final Key		maxConnections					= Key.of( "maxConnections" );
	public static final Key		database						= Key.of( "database" );
	public static final Key		dbtype							= Key.of( "dbtype" );
	public static final Key		blockfactor						= Key.of( "blockfactor" );
	public static final Key		fetchSize						= Key.of( "fetchsize" );
	public static final Key		cachedAfter						= Key.of( "cachedAfter" );
	public static final Key		debug							= Key.of( "debug" );
	public static final Key		ormoptions						= Key.of( "ormoptions" );
	public static final Key		cacheID							= Key.of( "cacheID" );
	public static final Key		cacheRegion						= Key.of( "cacheRegion" );
	public static final Key		clientInfo						= Key.of( "clientInfo" );
	public static final Key		fetchClientInfo					= Key.of( "fetchClientInfo" );
	public static final Key		lazy							= Key.of( "lazy" );
	public static final Key		psq								= Key.of( "psq" );

	// CFConfig-style datasource config keys
	public static final Key		dsn								= Key.of( "dsn" );
	public static final Key		custom							= Key.of( "custom" );

	// HikariCP configuration Key names. Includes all "Essential" and "Frquently Used" configuration keys, but no "Infrequently used" keys (for now.)
	// https://github.com/brettwooldridge/HikariCP?tab=readme-ov-file#gear-configuration-knobs-baby
	public static final Key		jdbcURL							= Key.of( "jdbcURL" );
	public static final Key		autoCommit						= Key.of( "autoCommit" );
	public static final Key		connectionTimeout				= Key.of( "connectionTimeout" );
	public static final Key		idleTimeout						= Key.of( "idleTimeout" );
	public static final Key		keepaliveTime					= Key.of( "keepaliveTime" );
	public static final Key		maxLifetime						= Key.of( "maxLifetime" );
	public static final Key		connectionTestQuery				= Key.of( "connectionTestQuery" );
	public static final Key		minimumIdle						= Key.of( "minimumIdle" );
	public static final Key		maximumPoolSize					= Key.of( "maximumPoolSize" );
	public static final Key		metricRegistry					= Key.of( "metricRegistry" );
	public static final Key		healthCheckRegistry				= Key.of( "healthCheckRegistry" );
	public static final Key		poolName						= Key.of( "poolName" );

	/**
	 * --------------------------------------------------------------------------
	 * Private Properties
	 * --------------------------------------------------------------------------
	 */

	/**
	 * The original key name
	 */
	protected String			name;

	/**
	 * The key name in upper case
	 */
	protected String			nameNoCase;

	/**
	 * The original value of the key, which could be a complex object
	 * if this key was being used to dereference a native Map.
	 */
	protected Object			originalValue;

	/**
	 * Keys are immutable, so we can cache the hash code
	 */
	protected int				hashCode;

	/**
	 * Serialization version
	 */
	private static final long	serialVersionUID				= 1L;

	/**
	 * --------------------------------------------------------------------------
	 * Constructors
	 * --------------------------------------------------------------------------
	 */

	/**
	 * Constructor
	 *
	 * @param name The target key to use, which is the original case.
	 */
	public Key( String name ) {
		this.name			= name;
		this.originalValue	= name;
		this.nameNoCase		= name.toUpperCase();
		this.hashCode		= this.nameNoCase.hashCode();
	}

	/**
	 * Constructor for a key that is not a string
	 *
	 * @param name The target key to use, which is the original case.
	 */
	public Key( String name, Object originalValue ) {
		this.name			= name;
		this.originalValue	= originalValue;
		this.nameNoCase		= name.toUpperCase();
		this.hashCode		= this.nameNoCase.hashCode();
	}

	/**
	 * --------------------------------------------------------------------------
	 * Methods
	 * --------------------------------------------------------------------------
	 */

	/**
	 * @return The key name in upper case.
	 */
	public String getNameNoCase() {
		return this.nameNoCase;
	}

	/**
	 * @return The original key case.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * @return The original value of the key, which could be a complex object
	 */
	public Object getOriginalValue() {
		return this.originalValue;
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Same key hash
	 *
	 * @param obj The object to compare against.
	 */
	@Override
	public boolean equals( Object obj ) {
		// Same object
		if ( this == obj ) {
			return true;
		}

		if ( obj != null && obj instanceof Key castedKey ) {
			// Same key name
			return hashCode() == castedKey.hashCode();
		}

		return false;
	}

	/**
	 * Verifies equality with the following rules:
	 * - Same object
	 * - Same key name (case-sensitive)
	 *
	 * @param obj The object to compare against.
	 *
	 * @return True if the objects are equal.
	 */
	public boolean equalsWithCase( Object obj ) {
		// Same object
		if ( this == obj )
			return true;
		// Null and class checks
		if ( obj == null || ! ( obj instanceof Key ) ) {
			return false;
		}
		// Same key name
		return getName().equals( ( ( Key ) obj ).getName() );
	}

	/**
	 * @return The hash code of the key name in upper case
	 */
	@Override
	public int hashCode() {
		return this.hashCode;
	}

	/**
	 * Static builder of a case-insensitive key using the incoming key name
	 *
	 * @param name The key name to use.
	 *
	 * @return A case-insensitive key class
	 */
	public static Key of( String name ) {
		int len = name.length();
		if ( len <= 3 ) {
			byte[] bytes = name.getBytes();
			// optimization for common cases where incoming string is actually an int up to
			// 3 digits
			if ( ( len == 1 && isDigit( bytes[ 0 ] ) )
			    || ( len == 2 && isDigit( bytes[ 0 ] ) && isDigit( bytes[ 1 ] ) )
			    || ( len == 3 && isDigit( bytes[ 0 ] ) && isDigit( bytes[ 1 ] ) && isDigit( bytes[ 2 ] ) ) ) {
				return new IntKey( Integer.parseInt( name ) );

			}
		}
		return new Key( name );
	}

	/**
	 * Verify if the key is empty or not
	 *
	 * @return True, if it's empty
	 */
	public boolean isEmpty() {
		return this.name.isEmpty();
	}

	/**
	 * A little helper to decide if a byte represents a digit 0-9
	 *
	 * @param b The byte to check
	 *
	 * @return True if the byte is a digit
	 */
	private static boolean isDigit( byte b ) {
		return b >= 48 && b <= 57;
	}

	/**
	 * Static builder of a case-insensitive key using the incoming key name
	 *
	 * @param obj Object value to use as the key
	 *
	 * @return A case-insensitive key class
	 */
	public static Key of( Object obj ) {
		if ( obj instanceof Double d ) {
			return Key.of( d );
		}
		if ( obj instanceof Integer i ) {
			return Key.of( i );
		}
		if ( obj instanceof Long l ) {
			return Key.of( l );
		}
		// TODO: TODO: also check this higher up so we can tell the user more about what
		// was null.
		if ( obj == null ) {
			throw new BoxRuntimeException( "Cannot create a key from a null object" );
		}
		return new Key( obj.toString(), obj );
	}

	/**
	 * Static builder of an Integer key
	 *
	 * @param obj Integer value to use as the key
	 *
	 * @return A case-insensitive key class
	 */
	public static IntKey of( Integer obj ) {
		return new IntKey( obj );
	}

	/**
	 * Static builder of an int key
	 *
	 * @param obj Int value to use as the key
	 *
	 * @return A case-insensitive key class
	 */
	public static IntKey of( int obj ) {
		return new IntKey( obj );
	}

	/**
	 * Static builder of a Double key
	 *
	 * @param obj Double value to use as the key
	 *
	 * @return An IntKey instance if the Double was an integer, otherwise a Key
	 *         instance.
	 */
	public static Key of( Double obj ) {
		return Key.of( obj.doubleValue() );
	}

	/**
	 * Static builder of an int key
	 *
	 * @param obj double value to use as the key
	 *
	 * @return An IntKey instance if the Double was an integer, otherwise a Key
	 *         instance.
	 */
	public static Key of( double obj ) {
		if ( obj == ( int ) obj ) {
			return new IntKey( ( int ) obj );
		} else {
			return new Key( String.valueOf( obj ), obj );
		}
	}

	/**
	 * Static builder of case-insensitive key trackers using an incoming array of
	 * key names
	 *
	 * @param names The key names to use. This can be on or more.
	 *
	 * @return An array of case-insensitive key classes
	 */
	public static Key[] of( String... names ) {
		return Arrays.stream( names ).map( Key::of ).toArray( Key[]::new );
	}

	/**
	 * The string representation of the key which includes
	 * the original case and the upper case version.
	 *
	 * @return The string representation of the key
	 */
	@Override
	public String toString() {
		// This is currently needed for JSON serialization via Jackson Jr.
		return getName();
	}

	/**
	 * Compare keys in a case-insensitive manner.
	 *
	 * @param otherKey The key to compare to.
	 *
	 * @return A negative integer, zero, or a positive integer if this key is less
	 *         than, equal to, or greater than the specified key.
	 */
	@Override
	public int compareTo( Key otherKey ) {
		return this.nameNoCase.compareTo( otherKey.nameNoCase );
	}

	/**
	 * Compare keys in a case-sensitive manner.
	 *
	 * @param otherKey The key to compare to.
	 *
	 * @return A negative integer, zero, or a positive integer if this key is less
	 *         than, equal to, or greater than the specified key.
	 */
	public int compareToWithCase( Key otherKey ) {
		return this.name.compareTo( otherKey.name );
	}

}
