/**
 * [BoxLang]
 *
 * Copyright [2023] [Ortus Solutions, Corp]
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS"
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package ortus.boxlang.compiler.ast.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ortus.boxlang.compiler.ast.Position;
import ortus.boxlang.compiler.ast.expression.BoxFunctionInvocation;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.statement.component.BoxComponent;
import ortus.boxlang.runtime.BoxRuntime;
import ortus.boxlang.runtime.components.Component;
import ortus.boxlang.runtime.components.ComponentDescriptor;
import ortus.boxlang.runtime.context.IBoxContext;
import ortus.boxlang.runtime.scopes.Key;
import ortus.boxlang.runtime.services.ComponentService;
import ortus.boxlang.runtime.services.FunctionService;
import ortus.boxlang.runtime.types.IStruct;

/**
 * Audit missing functionality
 */
public class FeatureAuditVisitor extends VoidBoxVisitor {

	private static Map<String, String>			BIFMap					= new HashMap<String, String>();
	private static Map<String, String>			componentMap			= new HashMap<String, String>();
	private BoxRuntime							runtime;
	private FunctionService						functionService;
	private ComponentService					componentService;
	private List<FeatureUsed>					featuresUsed			= new ArrayList<FeatureUsed>();
	private Map<String, AggregateFeatureUsed>	aggregateFeaturesUsed	= new HashMap<>();

	static {
		BIFMap.put( "abs", "" );
		BIFMap.put( "acos", "" );
		BIFMap.put( "addsoaprequestheader", "" );
		BIFMap.put( "addsoapresponseheader", "" );
		BIFMap.put( "ajaxlink", "" );
		BIFMap.put( "ajaxonload", "" );
		BIFMap.put( "applicationstarttime", "" );
		BIFMap.put( "applicationstop", "" );
		BIFMap.put( "argon2checkhash", "" );
		BIFMap.put( "array", "" );
		BIFMap.put( "arrayappend", "" );
		BIFMap.put( "arrayavg", "" );
		BIFMap.put( "arraychunk", "" );
		BIFMap.put( "arrayclear", "" );
		BIFMap.put( "arraycontains", "" );
		BIFMap.put( "arraycontainsnocase", "" );
		BIFMap.put( "arraydelete", "" );
		BIFMap.put( "arraydeleteat", "" );
		BIFMap.put( "arraydeletenocase", "" );
		BIFMap.put( "arrayeach", "" );
		BIFMap.put( "arrayevery", "" );
		BIFMap.put( "arrayfilter", "" );
		BIFMap.put( "arrayfind", "" );
		BIFMap.put( "arrayfindall", "" );
		BIFMap.put( "arrayfindallnocase", "" );
		BIFMap.put( "arrayfindnocase", "" );
		BIFMap.put( "arrayfirst", "" );
		BIFMap.put( "arrayflatten", "" );
		BIFMap.put( "arrayflatmap", "" );
		BIFMap.put( "arraygetmetadata", "" );
		BIFMap.put( "arraygroupby", "" );
		BIFMap.put( "arrayindexexists", "" );
		BIFMap.put( "arrayinsertat", "" );
		BIFMap.put( "arrayisdefined", "" );
		BIFMap.put( "arrayisempty", "" );
		BIFMap.put( "arraylast", "" );
		BIFMap.put( "arraylen", "" );
		BIFMap.put( "arraymap", "" );
		BIFMap.put( "arraymax", "" );
		BIFMap.put( "arraymedian", "" );
		BIFMap.put( "arraymerge", "" );
		BIFMap.put( "arraymid", "" );
		BIFMap.put( "arraymin", "" );
		BIFMap.put( "arraynew", "" );
		BIFMap.put( "arraypop", "" );
		BIFMap.put( "arrayprepend", "" );
		BIFMap.put( "arraypush", "" );
		BIFMap.put( "arrayrange", "" );
		BIFMap.put( "arrayreduce", "" );
		BIFMap.put( "arrayreduceright", "" );
		BIFMap.put( "arrayresize", "" );
		BIFMap.put( "arrayreverse", "" );
		BIFMap.put( "arrayset", "" );
		BIFMap.put( "arraysetmetadata", "" );
		BIFMap.put( "arrayshift", "" );
		BIFMap.put( "arrayslice", "" );
		BIFMap.put( "arraysome", "" );
		BIFMap.put( "arraysort", "" );
		BIFMap.put( "arraysplice", "" );
		BIFMap.put( "arraysum", "" );
		BIFMap.put( "arrayswap", "" );
		BIFMap.put( "arraytolist", "" );
		BIFMap.put( "arraytostruct", "" );
		BIFMap.put( "arrayunique", "" );
		BIFMap.put( "arrayunshift", "" );
		BIFMap.put( "arrayzip", "" );
		BIFMap.put( "asc | ascii", "" );
		BIFMap.put( "asin", "" );
		BIFMap.put( "atn", "" );
		BIFMap.put( "beat", "" );
		BIFMap.put( "binarydecode", "" );
		BIFMap.put( "binaryencode", "" );
		BIFMap.put( "bitand", "" );
		BIFMap.put( "bitmaskclear", "" );
		BIFMap.put( "bitmaskread", "" );
		BIFMap.put( "bitmaskset", "" );
		BIFMap.put( "bitnot", "" );
		BIFMap.put( "bitor", "" );
		BIFMap.put( "bitshln", "" );
		BIFMap.put( "bitshrn", "" );
		BIFMap.put( "bitxor", "" );
		BIFMap.put( "booleanformat", "" );
		BIFMap.put( "bundleinfo", "" );
		BIFMap.put( "cacheclear", "" );
		BIFMap.put( "cachecount", "" );
		BIFMap.put( "cachedelete", "" );
		BIFMap.put( "cacheget", "" );
		BIFMap.put( "cachegetall", "" );
		BIFMap.put( "cachegetallids", "" );
		BIFMap.put( "cachegetdefaultcachename", "" );
		BIFMap.put( "cachegetengineproperties", "" );
		BIFMap.put( "cachegetmetadata", "" );
		BIFMap.put( "cachegetproperties", "" );
		BIFMap.put( "cachegetsession", "" );
		BIFMap.put( "cacheidexists", "" );
		BIFMap.put( "cachekeyexists", "" );
		BIFMap.put( "cacheput", "" );
		BIFMap.put( "cacheregionexists", "" );
		BIFMap.put( "cacheregionnew", "" );
		BIFMap.put( "cacheregionremove", "" );
		BIFMap.put( "cacheremove", "" );
		BIFMap.put( "cacheremoveall", "" );
		BIFMap.put( "cachesetproperties", "" );
		BIFMap.put( "callstackdump", "" );
		BIFMap.put( "callstackget", "" );
		BIFMap.put( "canonicalize", "" );
		BIFMap.put( "ceiling", "" );
		BIFMap.put( "usion_decrypt", "" );
		BIFMap.put( "usion_encrypt", "" );
		BIFMap.put( "charsetdecode", "" );
		BIFMap.put( "charsetencode", "" );
		BIFMap.put( "chr | char", "" );
		BIFMap.put( "cjustify", "" );
		BIFMap.put( "cleartimezone", "" );
		BIFMap.put( "collectioneach", "" );
		BIFMap.put( "collectionevery", "" );
		BIFMap.put( "collectionfilter", "" );
		BIFMap.put( "collectionmap", "" );
		BIFMap.put( "collectionreduce", "" );
		BIFMap.put( "collectionsome", "" );
		BIFMap.put( "compare", "" );
		BIFMap.put( "comparenocase", "" );
		BIFMap.put( "componentcacheclear", "" );
		BIFMap.put( "componentcachelist", "" );
		BIFMap.put( "componentinfo", "" );
		BIFMap.put( "compress", "" );
		BIFMap.put( "contractpath", "" );
		BIFMap.put( "cos", "" );
		BIFMap.put( "createdate", "" );
		BIFMap.put( "createdatetime", "" );
		BIFMap.put( "createdynamicproxy", "" );
		BIFMap.put( "createguid", "" );
		BIFMap.put( "createobject", "" );
		BIFMap.put( "createodbcdate", "" );
		BIFMap.put( "createodbcdatetime", "" );
		BIFMap.put( "createodbctime", "" );
		BIFMap.put( "createtime", "" );
		BIFMap.put( "createtimespan", "" );
		BIFMap.put( "createuniqueid", "" );
		BIFMap.put( "createuuid", "" );
		BIFMap.put( "csrfgeneratetoken", "" );
		BIFMap.put( "csrfverifytoken", "" );
		BIFMap.put( "ctcacheclear", "" );
		BIFMap.put( "ctcachelist", "" );
		BIFMap.put( "datasourceflushmetacache", "" );
		BIFMap.put( "dateadd", "" );
		BIFMap.put( "datecompare", "" );
		BIFMap.put( "dateconvert", "" );
		BIFMap.put( "datediff", "" );
		BIFMap.put( "dateformat", "" );
		BIFMap.put( "datepart", "" );
		BIFMap.put( "datetimeformat", "" );
		BIFMap.put( "day", "" );
		BIFMap.put( "dayofweek", "" );
		BIFMap.put( "dayofweekasstring", "" );
		BIFMap.put( "dayofweekshortasstring", "" );
		BIFMap.put( "dayofyear", "" );
		BIFMap.put( "daysinmonth", "" );
		BIFMap.put( "daysinyear", "" );
		BIFMap.put( "de", "" );
		BIFMap.put( "decimalformat", "" );
		BIFMap.put( "decodeforhtml", "" );
		BIFMap.put( "decodefromurl", "" );
		BIFMap.put( "decrementvalue", "" );
		BIFMap.put( "decrypt", "" );
		BIFMap.put( "decryptbinary", "" );
		BIFMap.put( "deleteclientvariable", "" );
		BIFMap.put( "deserialize", "" );
		BIFMap.put( "deserializejson", "" );
		BIFMap.put( "deserializexml", "" );
		BIFMap.put( "directorycopy", "" );
		BIFMap.put( "directorycreate", "" );
		BIFMap.put( "directorydelete", "" );
		BIFMap.put( "directoryexists", "" );
		BIFMap.put( "directorylist", "" );
		BIFMap.put( "directoryrename", "" );
		BIFMap.put( "dollarformat", "" );
		BIFMap.put( "dotnettocftype", "" );
		BIFMap.put( "duplicate", "" );
		BIFMap.put( "each", "" );
		BIFMap.put( "echo", "" );
		BIFMap.put( "empty", "" );
		BIFMap.put( "encodefor", "" );
		BIFMap.put( "encodeforcss", "" );
		BIFMap.put( "encodefordn", "" );
		BIFMap.put( "encodeforhtml", "" );
		BIFMap.put( "encodeforhtmlattribute", "" );
		BIFMap.put( "encodeforjavascript", "" );
		BIFMap.put( "encodeforldap", "" );
		BIFMap.put( "encodeforsql", "" );
		BIFMap.put( "encodeforurl", "" );
		BIFMap.put( "encodeforxml", "" );
		BIFMap.put( "encodeforxmlattribute", "" );
		BIFMap.put( "encodeforxpath", "" );
		BIFMap.put( "encrypt", "" );
		BIFMap.put( "encryptbinary", "" );
		BIFMap.put( "entitydelete", "" );
		BIFMap.put( "entityload", "" );
		BIFMap.put( "entityloadbyexample", "" );
		BIFMap.put( "entityloadbypk", "" );
		BIFMap.put( "entitymerge", "" );
		BIFMap.put( "entitynamearray", "" );
		BIFMap.put( "entitynamelist", "" );
		BIFMap.put( "entitynew", "" );
		BIFMap.put( "entityreload", "" );
		BIFMap.put( "entitysave", "" );
		BIFMap.put( "entitytoquery", "" );
		BIFMap.put( "esapidecode", "" );
		BIFMap.put( "esapiencode", "" );
		BIFMap.put( "evaluate", "" );
		BIFMap.put( "exp", "" );
		BIFMap.put( "expandpath", "" );
		BIFMap.put( "extensionexists", "" );
		BIFMap.put( "extensionlist", "" );
		BIFMap.put( "extract", "" );
		BIFMap.put( "fileappend", "" );
		BIFMap.put( "fileclose", "" );
		BIFMap.put( "filecopy", "" );
		BIFMap.put( "filedelete", "" );
		BIFMap.put( "fileexists", "" );
		BIFMap.put( "filegetmimetype", "" );
		BIFMap.put( "fileinfo", "" );
		BIFMap.put( "fileiseof", "" );
		BIFMap.put( "filemove", "" );
		BIFMap.put( "fileopen", "" );
		BIFMap.put( "fileread", "" );
		BIFMap.put( "filereadbinary", "" );
		BIFMap.put( "filereadline", "" );
		BIFMap.put( "fileseek", "" );
		BIFMap.put( "filesetaccessmode", "" );
		BIFMap.put( "filesetattribute", "" );
		BIFMap.put( "filesetlastmodified", "" );
		BIFMap.put( "fileskipbytes", "" );
		BIFMap.put( "fileupload", "" );
		BIFMap.put( "fileuploadall", "" );
		BIFMap.put( "filewrite", "" );
		BIFMap.put( "filewriteline", "" );
		BIFMap.put( "find", "" );
		BIFMap.put( "findnocase", "" );
		BIFMap.put( "findoneof", "" );
		BIFMap.put( "firstdayofmonth", "" );
		BIFMap.put( "fix", "" );
		BIFMap.put( "floor", "" );
		BIFMap.put( "formatbasen", "" );
		BIFMap.put( "generateargon2hash", "" );
		BIFMap.put( "generatebcrypthash", "" );
		BIFMap.put( "generatepbkdfkey", "" );
		BIFMap.put( "generatescrypthash", "" );
		BIFMap.put( "generatesecretkey", "" );
		BIFMap.put( "getapplicationmetadata", "" );
		BIFMap.put( "getapplicationsettings", "" );
		BIFMap.put( "getauthuser", "" );
		BIFMap.put( "getbasetagdata", "" );
		BIFMap.put( "getbasetaglist", "" );
		BIFMap.put( "getbasetemplatepath", "" );
		BIFMap.put( "getbuiltinfunction", "" );
		BIFMap.put( "getcanonicalpath", "" );
		BIFMap.put( "getclasspath", "" );
		BIFMap.put( "getclientvariableslist", "" );
		BIFMap.put( "getcomponentmetadata", "" );
		BIFMap.put( "getcontextroot", "" );
		BIFMap.put( "getcpuusage", "" );
		BIFMap.put( "getcurrentcontext", "" );
		BIFMap.put( "getcurrenttemplatepath", "" );
		BIFMap.put( "getdirectoryfrompath", "" );
		BIFMap.put( "getencoding", "" );
		BIFMap.put( "getexception", "" );
		BIFMap.put( "getfilefrompath", "" );
		BIFMap.put( "getfileinfo", "" );
		BIFMap.put( "getfreespace", "" );
		BIFMap.put( "getfunctioncalledname", "" );
		BIFMap.put( "getfunctiondata", "" );
		BIFMap.put( "getfunctionkeywords", "" );
		BIFMap.put( "getfunctionlist", "" );
		BIFMap.put( "getgatewayhelper", "" );
		BIFMap.put( "gethttprequestdata", "" );
		BIFMap.put( "gethttptimestring", "" );
		BIFMap.put( "getk2serverdoccount", "" );
		BIFMap.put( "getk2serverdoccountlimit", "" );
		BIFMap.put( "getlocale", "" );
		BIFMap.put( "getlocalecountry", "" );
		BIFMap.put( "getlocaledisplayname", "" );
		BIFMap.put( "getlocaleinfo", "" );
		BIFMap.put( "getlocalelanguage", "" );
		BIFMap.put( "getlocalhostip", "" );
		BIFMap.put( "getluceeid", "" );
		BIFMap.put( "getmemoryusage", "" );
		BIFMap.put( "getmetadata", "" );
		BIFMap.put( "getmetricdata", "" );
		BIFMap.put( "getnumericdate", "" );
		BIFMap.put( "getpagecontext", "" );
		BIFMap.put( "getprinterinfo", "" );
		BIFMap.put( "getprinterlist", "" );
		BIFMap.put( "getprofilesections", "" );
		BIFMap.put( "getprofilestring", "" );
		BIFMap.put( "getreadableimageformats", "" );
		BIFMap.put( "getsafehtml", "" );
		BIFMap.put( "getsoaprequest", "" );
		BIFMap.put( "getsoaprequestheader", "" );
		BIFMap.put( "getsoapresponse", "" );
		BIFMap.put( "getsoapresponseheader", "" );
		BIFMap.put( "getsystemfreememory", "" );
		BIFMap.put( "getsystemtotalmemory", "" );
		BIFMap.put( "gettagdata", "" );
		BIFMap.put( "gettaglist", "" );
		BIFMap.put( "gettempdirectory", "" );
		BIFMap.put( "gettempfile", "" );
		BIFMap.put( "gettemplatepath", "" );
		BIFMap.put( "gettickcount", "" );
		BIFMap.put( "gettimezone", "" );
		BIFMap.put( "gettimezoneinfo", "" );
		BIFMap.put( "gettoken", "" );
		BIFMap.put( "gettotalspace", "" );
		BIFMap.put( "getuserroles", "" );
		BIFMap.put( "getvariable", "" );
		BIFMap.put( "getvfsmetadata", "" );
		BIFMap.put( "getwriteableimageformats", "" );
		BIFMap.put( "hash", "" );
		BIFMap.put( "hash40", "" );
		BIFMap.put( "hmac", "" );
		BIFMap.put( "hour", "" );
		BIFMap.put( "htmlcodeformat", "" );
		BIFMap.put( "htmleditformat", "" );
		BIFMap.put( "htmlparse", "" );
		BIFMap.put( "iif", "" );
		BIFMap.put( "imageaddborder", "" );
		BIFMap.put( "imageblur", "" );
		BIFMap.put( "imagecaptcha", "" );
		BIFMap.put( "imageclearrect", "" );
		BIFMap.put( "imagecopy", "" );
		BIFMap.put( "imagecreatecaptcha", "" );
		BIFMap.put( "imagecrop", "" );
		BIFMap.put( "imagedrawarc", "" );
		BIFMap.put( "imagedrawbeveledrect", "" );
		BIFMap.put( "imagedrawcubiccurve", "" );
		BIFMap.put( "imagedrawimage", "" );
		BIFMap.put( "imagedrawline", "" );
		BIFMap.put( "imagedrawlines", "" );
		BIFMap.put( "imagedrawoval", "" );
		BIFMap.put( "imagedrawpoint", "" );
		BIFMap.put( "imagedrawquadraticcurve", "" );
		BIFMap.put( "imagedrawrect", "" );
		BIFMap.put( "imagedrawroundrect", "" );
		BIFMap.put( "imagedrawtext", "" );
		BIFMap.put( "imagefilter", "" );
		BIFMap.put( "imagefiltercolormap", "" );
		BIFMap.put( "imagefiltercurves", "" );
		BIFMap.put( "imagefilterkernel", "" );
		BIFMap.put( "imagefilterwarpgrid", "" );
		BIFMap.put( "imageflip", "" );
		BIFMap.put( "imagefonts", "" );
		BIFMap.put( "imageformats", "" );
		BIFMap.put( "imagegetblob", "" );
		BIFMap.put( "imagegetbufferedimage", "" );
		BIFMap.put( "imagegetexifmetadata", "" );
		BIFMap.put( "imagegetexiftag", "" );
		BIFMap.put( "imagegetheight", "" );
		BIFMap.put( "imagegetiptcmetadata", "" );
		BIFMap.put( "imagegetiptctag", "" );
		BIFMap.put( "imagegetmetadata", "" );
		BIFMap.put( "imagegetwidth", "" );
		BIFMap.put( "imagegrayscale", "" );
		BIFMap.put( "imageinfo", "" );
		BIFMap.put( "imagemakecolortransparent", "" );
		BIFMap.put( "imagemaketranslucent", "" );
		BIFMap.put( "imagenegative", "" );
		BIFMap.put( "imagenew", "" );
		BIFMap.put( "imageoverlay", "" );
		BIFMap.put( "imagepaste", "" );
		BIFMap.put( "imageread", "" );
		BIFMap.put( "imagereadbase64", "" );
		BIFMap.put( "imageresize", "" );
		BIFMap.put( "imagerotate", "" );
		BIFMap.put( "imagerotatedrawingaxis", "" );
		BIFMap.put( "imagescaletofit", "" );
		BIFMap.put( "imagesetantialiasing", "" );
		BIFMap.put( "imagesetbackgroundcolor", "" );
		BIFMap.put( "imagesetdrawingalpha", "" );
		BIFMap.put( "imagesetdrawingcolor", "" );
		BIFMap.put( "imagesetdrawingstroke", "" );
		BIFMap.put( "imagesetdrawingtransparency", "" );
		BIFMap.put( "imagesharpen", "" );
		BIFMap.put( "imageshear", "" );
		BIFMap.put( "imagesheardrawingaxis", "" );
		BIFMap.put( "imagetranslate", "" );
		BIFMap.put( "imagetranslatedrawingaxis", "" );
		BIFMap.put( "imagewrite", "" );
		BIFMap.put( "imagewritebase64", "" );
		BIFMap.put( "imagexordrawingmode", "" );
		BIFMap.put( "incrementvalue", "" );
		BIFMap.put( "inputbasen", "" );
		BIFMap.put( "insert", "" );
		BIFMap.put( "int", "" );
		BIFMap.put( "invalidateoauthaccesstoken", "" );
		BIFMap.put( "invoke", "" );
		BIFMap.put( "isarray", "" );
		BIFMap.put( "isbinary", "" );
		BIFMap.put( "isboolean", "" );
		BIFMap.put( "isclosure", "" );
		BIFMap.put( "iscustomfunction", "" );
		BIFMap.put( "isdate", "" );
		BIFMap.put( "isdateobject", "" );
		BIFMap.put( "isdate", "" );
		BIFMap.put( "isddx", "" );
		BIFMap.put( "isdebugmode", "" );
		BIFMap.put( "isdefined", "" );
		BIFMap.put( "isempty", "" );
		BIFMap.put( "isfileobject", "" );
		BIFMap.put( "isimage", "" );
		BIFMap.put( "isimagefile", "" );
		BIFMap.put( "isinstanceof", "" );
		BIFMap.put( "isipinrange", "" );
		BIFMap.put( "isipv6", "" );
		BIFMap.put( "isjson", "" );
		BIFMap.put( "isk2serverabroker", "" );
		BIFMap.put( "isk2serverdoccountexceeded", "" );
		BIFMap.put( "isk2serveronline", "" );
		BIFMap.put( "isleapyear", "" );
		BIFMap.put( "islocalhost", "" );
		BIFMap.put( "isnotmap", "" );
		BIFMap.put( "isnull", "" );
		BIFMap.put( "isnumeric", "" );
		BIFMap.put( "isnumericdate", "" );
		BIFMap.put( "isobject", "" );
		BIFMap.put( "ispdfarchive", "" );
		BIFMap.put( "ispdffile", "" );
		BIFMap.put( "ispdfobject", "" );
		BIFMap.put( "isquery", "" );
		BIFMap.put( "issafehtml", "" );
		BIFMap.put( "issimplevalue", "" );
		BIFMap.put( "issoaprequest", "" );
		BIFMap.put( "isspreadsheetfile", "" );
		BIFMap.put( "isspreadsheetobject", "" );
		BIFMap.put( "isstruct", "" );
		BIFMap.put( "isuserinanyrole", "" );
		BIFMap.put( "isuserinrole", "" );
		BIFMap.put( "isuserloggedin", "" );
		BIFMap.put( "isvalid", "" );
		BIFMap.put( "isvalidoauthaccesstoken", "" );
		BIFMap.put( "isvideofile", "" );
		BIFMap.put( "iswddx", "" );
		BIFMap.put( "isxml", "" );
		BIFMap.put( "isxmlattribute", "" );
		BIFMap.put( "isxmldoc", "" );
		BIFMap.put( "isxmlelem", "" );
		BIFMap.put( "isxmlnode", "" );
		BIFMap.put( "isxmlroot", "" );
		BIFMap.put( "iszipfile", "" );
		BIFMap.put( "javacast", "" );
		BIFMap.put( "jsstringformat", "" );
		BIFMap.put( "lcase", "" );
		BIFMap.put( "left", "" );
		BIFMap.put( "len", "" );
		BIFMap.put( "listappend", "" );
		BIFMap.put( "listavg", "" );
		BIFMap.put( "listchangedelims", "" );
		BIFMap.put( "listcompact", "" );
		BIFMap.put( "listcontains", "" );
		BIFMap.put( "listcontainsnocase", "" );
		BIFMap.put( "listdeleteat", "" );
		BIFMap.put( "listeach", "" );
		BIFMap.put( "listevery", "" );
		BIFMap.put( "listfilter", "" );
		BIFMap.put( "listfind", "" );
		BIFMap.put( "listfindnocase", "" );
		BIFMap.put( "listfirst", "" );
		BIFMap.put( "listgetat", "" );
		BIFMap.put( "listindexexists", "" );
		BIFMap.put( "listinsertat", "" );
		BIFMap.put( "listitemtrim", "" );
		BIFMap.put( "listlast", "" );
		BIFMap.put( "listlen", "" );
		BIFMap.put( "listmap", "" );
		BIFMap.put( "listprepend", "" );
		BIFMap.put( "listqualify", "" );
		BIFMap.put( "listreduce", "" );
		BIFMap.put( "listreduceright", "" );
		BIFMap.put( "listremoveduplicates", "" );
		BIFMap.put( "listremoveemptyitems", "" );
		BIFMap.put( "listrest", "" );
		BIFMap.put( "listsetat", "" );
		BIFMap.put( "listsome", "" );
		BIFMap.put( "listsort", "" );
		BIFMap.put( "listtoarray", "" );
		BIFMap.put( "listcompact", "" );
		BIFMap.put( "listvaluecount", "" );
		BIFMap.put( "listvaluecountnocase", "" );
		BIFMap.put( "ljustify", "" );
		BIFMap.put( "location", "" );
		BIFMap.put( "log", "" );
		BIFMap.put( "log10", "" );
		BIFMap.put( "lscurrencyformat", "" );
		BIFMap.put( "lsdateformat", "" );
		BIFMap.put( "lsdatetimeformat", "" );
		BIFMap.put( "lsdayofweek", "" );
		BIFMap.put( "lseurocurrencyformat", "" );
		BIFMap.put( "lsiscurrency", "" );
		BIFMap.put( "lsisdate", "" );
		BIFMap.put( "lsisnumeric", "" );
		BIFMap.put( "lsnumberformat", "" );
		BIFMap.put( "lsparsecurrency", "" );
		BIFMap.put( "lsparsedatetime", "" );
		BIFMap.put( "lsparseeurocurrency", "" );
		BIFMap.put( "lsparsenumber", "" );
		BIFMap.put( "lstimeformat", "" );
		BIFMap.put( "lsweek", "" );
		BIFMap.put( "ltrim", "" );
		BIFMap.put( "manifestread", "" );
		BIFMap.put( "max", "" );
		BIFMap.put( "metaphone", "" );
		BIFMap.put( "mid", "" );
		BIFMap.put( "millisecond", "" );
		BIFMap.put( "min", "" );
		BIFMap.put( "minute", "" );
		BIFMap.put( "month", "" );
		BIFMap.put( "monthasstring", "" );
		BIFMap.put( "monthshortasstring", "" );
		BIFMap.put( "newline", "" );
		BIFMap.put( "now", "" );
		BIFMap.put( "nowserver", "" );
		BIFMap.put( "nullvalue", "" );
		BIFMap.put( "numberformat", "" );
		BIFMap.put( "objectequals", "" );
		BIFMap.put( "objectload", "" );
		BIFMap.put( "objectsave", "" );
		BIFMap.put( "ormclearsession", "" );
		BIFMap.put( "ormcloseallsessions", "" );
		BIFMap.put( "ormclosesession", "" );
		BIFMap.put( "ormevictcollection", "" );
		BIFMap.put( "ormevictentity", "" );
		BIFMap.put( "ormevictqueries", "" );
		BIFMap.put( "ormexecutequery", "" );
		BIFMap.put( "ormflush", "" );
		BIFMap.put( "ormflushall", "" );
		BIFMap.put( "ormgetsession", "" );
		BIFMap.put( "ormgetsessionfactory", "" );
		BIFMap.put( "ormindex", "" );
		BIFMap.put( "ormindexpurge", "" );
		BIFMap.put( "ormreload", "" );
		BIFMap.put( "ormsearch", "" );
		BIFMap.put( "ormsearchoffline", "" );
		BIFMap.put( "pagepoolclear", "" );
		BIFMap.put( "pagepoollist", "" );
		BIFMap.put( "paragraphformat", "" );
		BIFMap.put( "parameterexists", "" );
		BIFMap.put( "parsedatetime", "" );
		BIFMap.put( "parsenumber", "" );
		BIFMap.put( "pi", "" );
		BIFMap.put( "precisionevaluate", "" );
		BIFMap.put( "preservesinglequotes", "" );
		BIFMap.put( "quarter", "" );
		BIFMap.put( "query", "" );
		BIFMap.put( "queryaddcolumn", "" );
		BIFMap.put( "queryaddrow", "" );
		BIFMap.put( "queryappend", "" );
		BIFMap.put( "queryclear", "" );
		BIFMap.put( "querycolumnarray", "" );
		BIFMap.put( "querycolumncount", "" );
		BIFMap.put( "querycolumndata", "" );
		BIFMap.put( "querycolumnexists", "" );
		BIFMap.put( "querycolumnlist", "" );
		BIFMap.put( "queryconvertforgrid", "" );
		BIFMap.put( "querycurrentrow", "" );
		BIFMap.put( "querydeletecolumn", "" );
		BIFMap.put( "querydeleterow", "" );
		BIFMap.put( "queryeach", "" );
		BIFMap.put( "queryevery", "" );
		BIFMap.put( "queryexecute", "" );
		BIFMap.put( "queryfilter", "" );
		BIFMap.put( "querygetcell", "" );
		BIFMap.put( "querygetresult", "" );
		BIFMap.put( "querygetrow", "" );
		BIFMap.put( "queryinsertat", "" );
		BIFMap.put( "querykeyexists", "" );
		BIFMap.put( "querymap", "" );
		BIFMap.put( "querynew", "" );
		BIFMap.put( "queryprepend", "" );
		BIFMap.put( "queryrecordcount", "" );
		BIFMap.put( "queryreduce", "" );
		BIFMap.put( "queryreverse", "" );
		BIFMap.put( "queryrowdata", "" );
		BIFMap.put( "queryrowswap", "" );
		BIFMap.put( "querysetcell", "" );
		BIFMap.put( "querysetrow", "" );
		BIFMap.put( "queryslice", "" );
		BIFMap.put( "querysome", "" );
		BIFMap.put( "querysort", "" );
		BIFMap.put( "quotedvaluelist", "" );
		BIFMap.put( "rand", "" );
		BIFMap.put( "randomize", "" );
		BIFMap.put( "randrange", "" );
		BIFMap.put( "reescape", "" );
		BIFMap.put( "refind", "" );
		BIFMap.put( "refindnocase", "" );
		BIFMap.put( "releasecomobject", "" );
		BIFMap.put( "rematch", "" );
		BIFMap.put( "rematchnocase", "" );
		BIFMap.put( "removecachedquery", "" );
		BIFMap.put( "removechars", "" );
		BIFMap.put( "render", "" );
		BIFMap.put( "repeatstring", "" );
		BIFMap.put( "replace", "" );
		BIFMap.put( "replacelist", "" );
		BIFMap.put( "replacelistnocase", "" );
		BIFMap.put( "replacenocase", "" );
		BIFMap.put( "rereplace", "" );
		BIFMap.put( "rereplacenocase", "" );
		BIFMap.put( "restdeleteapplication", "" );
		BIFMap.put( "restinitapplication", "" );
		BIFMap.put( "restsetresponse", "" );
		BIFMap.put( "reverse", "" );
		BIFMap.put( "right", "" );
		BIFMap.put( "rjustify", "" );
		BIFMap.put( "round", "" );
		BIFMap.put( "rtrim", "" );
		BIFMap.put( "runasync", "" );
		BIFMap.put( "sanitizehtml", "" );
		BIFMap.put( "second", "" );
		BIFMap.put( "sendgatewaymessage", "" );
		BIFMap.put( "serialize", "" );
		BIFMap.put( "serializejson | jsonserialize", "" );
		BIFMap.put( "serializexml | xmlserialize", "" );
		BIFMap.put( "sessioninvalidate", "" );
		BIFMap.put( "sessionrotate", "" );
		BIFMap.put( "sessionstarttime", "" );
		BIFMap.put( "setencoding", "" );
		BIFMap.put( "setlocale", "" );
		BIFMap.put( "setprofilestring", "" );
		BIFMap.put( "settimezone", "" );
		BIFMap.put( "setvariable", "" );
		BIFMap.put( "sgn", "" );
		BIFMap.put( "sin", "" );
		BIFMap.put( "sizeof", "" );
		BIFMap.put( "sleep", "" );
		BIFMap.put( "soundex", "" );
		BIFMap.put( "spanexcluding", "" );
		BIFMap.put( "spanincluding", "" );
		BIFMap.put( "spreadsheetaddautofilter", "" );
		BIFMap.put( "spreadsheetaddcolumn", "" );
		BIFMap.put( "spreadsheetaddfreezepane", "" );
		BIFMap.put( "spreadsheetaddimage", "" );
		BIFMap.put( "spreadsheetaddinfo", "" );
		BIFMap.put( "spreadsheetaddpagebreaks", "" );
		BIFMap.put( "spreadsheetaddrow", "" );
		BIFMap.put( "spreadsheetaddrows", "" );
		BIFMap.put( "spreadsheetaddsplitpane", "" );
		BIFMap.put( "spreadsheetcreatesheet", "" );
		BIFMap.put( "spreadsheetdeletecolumn", "" );
		BIFMap.put( "spreadsheetdeletecolumns", "" );
		BIFMap.put( "spreadsheetdeleterow", "" );
		BIFMap.put( "spreadsheetdeleterows", "" );
		BIFMap.put( "spreadsheetformatcell", "" );
		BIFMap.put( "spreadsheetformatcellrange", "" );
		BIFMap.put( "spreadsheetformatcolumn", "" );
		BIFMap.put( "spreadsheetformatcolumns", "" );
		BIFMap.put( "spreadsheetformatrow", "" );
		BIFMap.put( "spreadsheetformatrows", "" );
		BIFMap.put( "spreadsheetgetcellcomment", "" );
		BIFMap.put( "spreadsheetgetcellformula", "" );
		BIFMap.put( "spreadsheetgetcellvalue", "" );
		BIFMap.put( "spreadsheetgetcolumncount", "" );
		BIFMap.put( "spreadsheetinfo", "" );
		BIFMap.put( "spreadsheetmergecells", "" );
		BIFMap.put( "spreadsheetnew", "" );
		BIFMap.put( "spreadsheetread", "" );
		BIFMap.put( "spreadsheetreadbinary", "" );
		BIFMap.put( "spreadsheetremovesheet", "" );
		BIFMap.put( "spreadsheetsetactivesheet", "" );
		BIFMap.put( "spreadsheetsetactivesheetnumber", "" );
		BIFMap.put( "spreadsheetsetcellcomment", "" );
		BIFMap.put( "spreadsheetsetcellformula", "" );
		BIFMap.put( "spreadsheetsetcellvalue", "" );
		BIFMap.put( "spreadsheetsetcolumnwidth", "" );
		BIFMap.put( "spreadsheetsetfooter", "" );
		BIFMap.put( "spreadsheetsetheader", "" );
		BIFMap.put( "spreadsheetsetrowheight", "" );
		BIFMap.put( "spreadsheetshiftcolumns", "" );
		BIFMap.put( "spreadsheetshiftrows", "" );
		BIFMap.put( "spreadsheetwrite", "" );
		BIFMap.put( "sqr", "" );
		BIFMap.put( "sslcertificateinstall", "" );
		BIFMap.put( "sslcertificatelist", "" );
		BIFMap.put( "storeaddacl", "" );
		BIFMap.put( "storegetacl", "" );
		BIFMap.put( "storegetmetadata", "" );
		BIFMap.put( "storesetacl", "" );
		BIFMap.put( "storesetmetadata", "" );
		BIFMap.put( "stringeach", "" );
		BIFMap.put( "stringevery", "" );
		BIFMap.put( "stringfilter", "" );
		BIFMap.put( "stringlen", "" );
		BIFMap.put( "stringmap", "" );
		BIFMap.put( "stringreduce", "" );
		BIFMap.put( "stringreduceright", "" );
		BIFMap.put( "stringsome", "" );
		BIFMap.put( "stringsort", "" );
		BIFMap.put( "stripcr", "" );
		BIFMap.put( "structappend", "" );
		BIFMap.put( "structclear", "" );
		BIFMap.put( "structcopy", "" );
		BIFMap.put( "structcount", "" );
		BIFMap.put( "structdelete", "" );
		BIFMap.put( "structeach", "" );
		BIFMap.put( "structequals", "" );
		BIFMap.put( "structevery", "" );
		BIFMap.put( "structfilter", "" );
		BIFMap.put( "structfind", "" );
		BIFMap.put( "structfindkey", "" );
		BIFMap.put( "structfindvalue", "" );
		BIFMap.put( "structget", "" );
		BIFMap.put( "structgetmetadata", "" );
		BIFMap.put( "structinsert", "" );
		BIFMap.put( "structiscasesensitive", "" );
		BIFMap.put( "structisempty", "" );
		BIFMap.put( "structisordered", "" );
		BIFMap.put( "structkeyexists", "" );
		BIFMap.put( "structkeyarray", "" );
		BIFMap.put( "structkeylist", "" );
		BIFMap.put( "structkeytranslate", "" );
		BIFMap.put( "structlistnew", "" );
		BIFMap.put( "structmap", "" );
		BIFMap.put( "structnew", "" );
		BIFMap.put( "structreduce", "" );
		BIFMap.put( "structsetmetadata", "" );
		BIFMap.put( "structsome", "" );
		BIFMap.put( "structsort", "" );
		BIFMap.put( "structtosorted", "" );
		BIFMap.put( "structupdate", "" );
		BIFMap.put( "structvaluearray", "" );
		BIFMap.put( "systemcacheclear", "" );
		BIFMap.put( "systemoutput", "" );
		BIFMap.put( "tan", "" );
		BIFMap.put( "threadjoin", "" );
		BIFMap.put( "threadterminate", "" );
		BIFMap.put( "throw", "" );
		BIFMap.put( "timeformat", "" );
		BIFMap.put( "tobase64", "" );
		BIFMap.put( "tobinary", "" );
		BIFMap.put( "tonumeric", "" );
		BIFMap.put( "toscript", "" );
		BIFMap.put( "tostring", "" );
		BIFMap.put( "trace", "" );
		BIFMap.put( "transactioncommit", "" );
		BIFMap.put( "transactionrollback", "" );
		BIFMap.put( "transactionsetsavepoint", "" );
		BIFMap.put( "trim", "" );
		BIFMap.put( "truefalseformat", "" );
		BIFMap.put( "ucase", "" );
		BIFMap.put( "ucfirst", "" );
		BIFMap.put( "unserializejava", "" );
		BIFMap.put( "urldecode", "" );
		BIFMap.put( "urlencode", "" );
		BIFMap.put( "urlencodedformat", "" );
		BIFMap.put( "urlsessionformat", "" );
		BIFMap.put( "val", "" );
		BIFMap.put( "valuearray", "" );
		BIFMap.put( "valuelist", "" );
		BIFMap.put( "verifybcrypthash", "" );
		BIFMap.put( "verifyclient", "" );
		BIFMap.put( "verifyscrypthash", "" );
		BIFMap.put( "webservicenew", "" );
		BIFMap.put( "week", "" );
		BIFMap.put( "wrap", "" );
		BIFMap.put( "writebody", "" );
		BIFMap.put( "writedump", "" );
		BIFMap.put( "writelog", "" );
		BIFMap.put( "writeoutput", "" );
		BIFMap.put( "wsgetallchannels", "" );
		BIFMap.put( "wsgetsubscribers", "" );
		BIFMap.put( "wspublish", "" );
		BIFMap.put( "wssendmessage", "" );
		BIFMap.put( "xmlchildpos", "" );
		BIFMap.put( "xmlelemnew", "" );
		BIFMap.put( "xmlformat", "" );
		BIFMap.put( "xmlgetnodetype", "" );
		BIFMap.put( "xmlnew", "" );
		BIFMap.put( "xmlparse", "" );
		BIFMap.put( "xmlsearch", "" );
		BIFMap.put( "xmltransform", "" );
		BIFMap.put( "xmlvalidate", "" );
		BIFMap.put( "year", "" );
		BIFMap.put( "yesnoformat", "" );

		// Commented components have custom transformers and parser rules and don't use the generic component Box AST node.
		// They are also all implemented. We can add custom checks in the vistor for their specific BoxNode AST nodes if we want to report on them as being
		// used.
		componentMap.put( "_socialplugin", "" );
		componentMap.put( "abort", "" );
		componentMap.put( "admin", "" );
		componentMap.put( "ajaximport", "" );
		componentMap.put( "ajaxproxy", "" );
		componentMap.put( "applet", "" );
		componentMap.put( "application", "" );
		componentMap.put( "argument", "" );
		componentMap.put( "associate", "" );
		// componentMap.put( "break", "" );
		componentMap.put( "cache", "" );
		componentMap.put( "calendar", "" );
		// componentMap.put( "case", "" );
		// componentMap.put( "catch", "" );
		componentMap.put( "chart", "" );
		componentMap.put( "chartdata", "" );
		componentMap.put( "chartseries", "" );
		componentMap.put( "client", "" );
		componentMap.put( "clientsettings", "" );
		componentMap.put( "col", "" );
		componentMap.put( "collection", "" );
		componentMap.put( "component", "" );
		componentMap.put( "content", "" );
		// componentMap.put( "continue", "" );
		componentMap.put( "cookie", "" );
		componentMap.put( "dbinfo", "" );
		// componentMap.put( "defaultcase", "" );
		componentMap.put( "directory", "" );
		componentMap.put( "div", "" );
		componentMap.put( "document", "" );
		componentMap.put( "documentitem", "" );
		componentMap.put( "documentsection", "" );
		componentMap.put( "dump", "" );
		// componentMap.put( "else", "" );
		// componentMap.put( "elseif", "" );
		componentMap.put( "error", "" );
		componentMap.put( "exchangecalendar", "" );
		componentMap.put( "exchangeconnection", "" );
		componentMap.put( "exchangecontact", "" );
		componentMap.put( "exchangefilter", "" );
		componentMap.put( "exchangemail", "" );
		componentMap.put( "exchangetask", "" );
		componentMap.put( "execute", "" );
		componentMap.put( "exit", "" );
		componentMap.put( "feed", "" );
		componentMap.put( "file", "" );
		componentMap.put( "fileupload", "" );
		// componentMap.put( "finally", "" );
		componentMap.put( "flush", "" );
		componentMap.put( "form", "" );
		componentMap.put( "formgroup", "" );
		componentMap.put( "formitem", "" );
		componentMap.put( "forward", "" );
		componentMap.put( "ftp", "" );
		componentMap.put( "function", "" );
		componentMap.put( "grid", "" );
		componentMap.put( "gridcolumn", "" );
		componentMap.put( "gridrow", "" );
		componentMap.put( "gridupdate", "" );
		componentMap.put( "header", "" );
		componentMap.put( "htmlbody", "" );
		componentMap.put( "htmlhead", "" );
		componentMap.put( "htmltopdf", "" );
		componentMap.put( "htmltopdfitem", "" );
		componentMap.put( "http", "" );
		componentMap.put( "httpparam", "" );
		componentMap.put( "if", "" );
		componentMap.put( "image", "" );
		componentMap.put( "imap", "" );
		componentMap.put( "imapfilter", "" );
		// componentMap.put( "import", "" );
		componentMap.put( "include", "" );
		componentMap.put( "index", "" );
		componentMap.put( "input", "" );
		componentMap.put( "insert", "" );
		// componentMap.put( "interface", "" );
		componentMap.put( "invoke", "" );
		componentMap.put( "invokeargument", "" );
		componentMap.put( "layout", "" );
		componentMap.put( "layoutarea", "" );
		componentMap.put( "ldap", "" );
		componentMap.put( "location", "" );
		componentMap.put( "lock", "" );
		componentMap.put( "log", "" );
		componentMap.put( "login", "" );
		componentMap.put( "loginuser", "" );
		componentMap.put( "logout", "" );
		componentMap.put( "loop", "" );
		componentMap.put( "mail", "" );
		componentMap.put( "mailparam", "" );
		componentMap.put( "mailpart", "" );
		componentMap.put( "map", "" );
		componentMap.put( "mapitem", "" );
		componentMap.put( "mediaplayer", "" );
		componentMap.put( "menu", "" );
		componentMap.put( "menuitem", "" );
		componentMap.put( "messagebox", "" );
		componentMap.put( "module", "" );
		componentMap.put( "ntauthenticate", "" );
		componentMap.put( "oauth", "" );
		componentMap.put( "object", "" );
		componentMap.put( "objectcache", "" );
		componentMap.put( "output", "" );
		componentMap.put( "pageencoding", "" );
		componentMap.put( "param", "" );
		componentMap.put( "pdf", "" );
		componentMap.put( "pdfform", "" );
		componentMap.put( "pdfformparam", "" );
		componentMap.put( "pdfparam", "" );
		componentMap.put( "pdfsubform", "" );
		componentMap.put( "pod", "" );
		componentMap.put( "pop", "" );
		componentMap.put( "presentation", "" );
		componentMap.put( "presentationslide", "" );
		componentMap.put( "presenter", "" );
		componentMap.put( "print", "" );
		componentMap.put( "processingdirective", "" );
		componentMap.put( "procparam", "" );
		componentMap.put( "procresult", "" );
		componentMap.put( "progressbar", "" );
		// componentMap.put( "property", "" );
		componentMap.put( "query", "" );
		componentMap.put( "queryparam", "" );
		componentMap.put( "registry", "" );
		componentMap.put( "report", "" );
		componentMap.put( "reportparam", "" );
		// componentMap.put( "rethrow", "" );
		componentMap.put( "retry", "" );
		// componentMap.put( "return", "" );
		componentMap.put( "savecontent", "" );
		componentMap.put( "schedule", "" );
		componentMap.put( "script", "" );
		componentMap.put( "search", "" );
		componentMap.put( "select", "" );
		componentMap.put( "servlet", "" );
		componentMap.put( "servletparam", "" );
		componentMap.put( "set", "" );
		componentMap.put( "setting", "" );
		componentMap.put( "sharepoint", "" );
		componentMap.put( "silent", "" );
		componentMap.put( "sleep", "" );
		componentMap.put( "slider", "" );
		componentMap.put( "spreadsheet", "" );
		componentMap.put( "sprydataset", "" );
		componentMap.put( "stopwatch", "" );
		componentMap.put( "storedproc", "" );
		// componentMap.put( "switch", "" );
		componentMap.put( "table", "" );
		componentMap.put( "textarea", "" );
		componentMap.put( "textinput", "" );
		componentMap.put( "thread", "" );
		// componentMap.put( "throw", "" );
		componentMap.put( "timer", "" );
		componentMap.put( "tooltip", "" );
		componentMap.put( "trace", "" );
		componentMap.put( "transaction", "" );
		componentMap.put( "tree", "" );
		componentMap.put( "treeitem", "" );
		// componentMap.put( "try", "" );
		componentMap.put( "update", "" );
		componentMap.put( "wddx", "" );
		componentMap.put( "websocket", "" );
		// componentMap.put( "while", "" );
		componentMap.put( "window", "" );
		componentMap.put( "xml", "" );
		componentMap.put( "zip", "" );
		componentMap.put( "zipparam", "" );
	}

	public FeatureAuditVisitor() {
		super();
		runtime				= BoxRuntime.getInstance();
		functionService		= runtime.getFunctionService();
		componentService	= runtime.getComponentService();
	}

	public void visit( BoxFunctionInvocation node ) {
		String name = node.getName().toLowerCase();
		if ( BIFMap.containsKey( name ) ) {
			String	module	= BIFMap.get( name );
			boolean	missing	= !functionService.hasGlobalFunction( name );
			featuresUsed.add(
			    new FeatureUsed( name, FeatureType.BIF, module, missing, node.getPosition() )
			);
			String aggregateKey = name + FeatureType.BIF;
			if ( aggregateFeaturesUsed.containsKey( aggregateKey ) ) {
				aggregateFeaturesUsed.put( aggregateKey, aggregateFeaturesUsed.get( aggregateKey ).increment() );
			} else {
				aggregateFeaturesUsed.put( aggregateKey, new AggregateFeatureUsed( name, FeatureType.BIF, module, missing, 1 ) );
			}
		}
		super.visit( node );
	}

	public void visit( BoxComponent node ) {
		String name = node.getName().toLowerCase();
		if ( componentMap.containsKey( name ) ) {
			String	module	= componentMap.get( name );
			boolean	missing	= !componentService.hasComponent( name );
			if ( !missing ) {
				// If the component service has the component, but it's the dummy component, then it's actually missing.
				if ( componentService.getComponent( name ).componentClass.equals( DummyComponent.class ) ) {
					missing = true;
				}
			}
			featuresUsed.add(
			    new FeatureUsed( name, FeatureType.COMPONENT, module, missing, node.getPosition() )
			);
			String aggregateKey = name + FeatureType.COMPONENT;
			if ( aggregateFeaturesUsed.containsKey( aggregateKey ) ) {
				aggregateFeaturesUsed.put( aggregateKey, aggregateFeaturesUsed.get( aggregateKey ).increment() );
			} else {
				aggregateFeaturesUsed.put( aggregateKey, new AggregateFeatureUsed( name, FeatureType.COMPONENT, module, missing, 1 ) );
			}
		}
		super.visit( node );
	}

	public void visit( BoxMethodInvocation node ) {
		// TODO: check for known member methods
		super.visit( node );
	}

	public static void setupRuntimeStubs() {
		var	runtime				= BoxRuntime.getInstance();
		var	componentService	= runtime.getComponentService();

		// The script parser won't parse components it doesn't know about, so we need to seed dummies or the parsing will fail.
		componentMap.forEach( ( name, module ) -> {
			if ( !componentService.hasComponent( name ) ) {
				Key nameKey = Key.of( name );
				componentService.registerComponent(
				    new ComponentDescriptor(
				        nameKey,
				        DummyComponent.class,
				        module,
				        "",
				        null,
				        true,
				        false
				    ),
				    nameKey,
				    true );
			}
		} );
	}

	public enum FeatureType {
		BIF,
		MEMBER_METHOD,
		COMPONENT
	}

	public List<FeatureUsed> getFeaturesUsed() {
		return featuresUsed;
	}

	public List<AggregateFeatureUsed> getAggregateFeaturesUsed() {
		return new ArrayList<>( aggregateFeaturesUsed.values() ).stream().sorted().collect( Collectors.toList() );
	}

	/*
	 * Represents a unique usage of a feature
	 */
	public record FeatureUsed( String name, FeatureType type, String module, boolean missing, Position position ) {

		public String toString() {
			return String.format( "%s%s (%s) - %s Source line: %s", ( missing ? "[MISSING] " : "" ), name, type, module, getLine() );
		}

		public static String csvHeader() {
			return "Name,Type,Module,Missing,Line";
		}

		public String toCSV() {
			return String.format( "%s,%s,%s,%s,%s", name, type, module, missing, getLine() );
		}

		private String getLine() {
			if ( position != null ) {
				return String.valueOf( position.getStart().getLine() );
			} else {
				return "";
			}
		}
	}

	/*
	 * Represents an aggregate usage of a feature
	 */
	public record AggregateFeatureUsed( String name, FeatureType type, String module, boolean missing, int count )
	    implements Comparable<AggregateFeatureUsed> {

		public String toString() {
			return String.format( "%s%s (%s) - %s Count Used: %s", ( missing ? "[MISSING] " : "" ), name, type, module, count );
		}

		// increment the count
		public AggregateFeatureUsed increment() {
			return new AggregateFeatureUsed( name, type, module, missing, count + 1 );
		}

		@Override
		public int compareTo( AggregateFeatureUsed other ) {
			int typeComparison = this.type.compareTo( other.type );
			if ( typeComparison != 0 ) {
				return typeComparison;
			} else {
				return Integer.compare( other.count, this.count ); // for descending order
			}
		}

		public static String csvHeader() {
			return "Name,Type,Module,Missing,Count";
		}

		public String toCSV() {
			return String.format( "%s,%s,%s,%s,%s", name, type, module, missing, count );
		}
	}

	/**
	 * See constructor. This is used to stub out missing components so the parser doesn't fail.
	 * 
	 */
	private class DummyComponent extends Component {

		@Override
		public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
			throw new UnsupportedOperationException( "Unimplemented method '_invoke'" );
		}
	}

}
