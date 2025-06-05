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
import ortus.boxlang.compiler.ast.expression.BoxIdentifier;
import ortus.boxlang.compiler.ast.expression.BoxMethodInvocation;
import ortus.boxlang.compiler.ast.expression.BoxStringLiteral;
import ortus.boxlang.compiler.ast.expression.BoxStructLiteral;
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
		BIFMap.put( "abs", "core" );
		BIFMap.put( "acos", "core" );
		BIFMap.put( "addsoaprequestheader", "" );
		BIFMap.put( "addsoapresponseheader", "" );
		BIFMap.put( "ajaxlink", "" );
		BIFMap.put( "ajaxonload", "" );
		BIFMap.put( "applicationstarttime", "core" );
		BIFMap.put( "applicationstop", "core" );
		BIFMap.put( "argon2checkhash", "bx-argon" );
		BIFMap.put( "array", "" );
		BIFMap.put( "arrayappend", "core" );
		BIFMap.put( "arrayavg", "core" );
		BIFMap.put( "arraychunk", "" );
		BIFMap.put( "arrayclear", "core" );
		BIFMap.put( "arraycontains", "core" );
		BIFMap.put( "arraycontainsnocase", "core" );
		BIFMap.put( "arraydelete", "core" );
		BIFMap.put( "arraydeleteat", "core" );
		BIFMap.put( "arraydeletenocase", "core" );
		BIFMap.put( "arrayeach", "core" );
		BIFMap.put( "arrayevery", "core" );
		BIFMap.put( "arrayfilter", "core" );
		BIFMap.put( "arrayfind", "core" );
		BIFMap.put( "arrayfindall", "core" );
		BIFMap.put( "arrayfindallnocase", "core" );
		BIFMap.put( "arrayfindnocase", "core" );
		BIFMap.put( "arrayfirst", "core" );
		BIFMap.put( "arrayflatten", "" );
		BIFMap.put( "arrayflatmap", "" );
		BIFMap.put( "arraygetmetadata", "core" );
		BIFMap.put( "arraygroupby", "" );
		BIFMap.put( "arrayindexexists", "core" );
		BIFMap.put( "arrayinsertat", "core" );
		BIFMap.put( "arrayisdefined", "core" );
		BIFMap.put( "arrayisempty", "core" );
		BIFMap.put( "arraylast", "core" );
		BIFMap.put( "arraylen", "core" );
		BIFMap.put( "arraymap", "core" );
		BIFMap.put( "arraymax", "core" );
		BIFMap.put( "arraymedian", "core" );
		BIFMap.put( "arraymerge", "core" );
		BIFMap.put( "arraymid", "core" );
		BIFMap.put( "arraymin", "core" );
		BIFMap.put( "arraynew", "core" );
		BIFMap.put( "arraypop", "core" );
		BIFMap.put( "arrayprepend", "core" );
		BIFMap.put( "arraypush", "core" );
		BIFMap.put( "arrayrange", "core" );
		BIFMap.put( "arrayreduce", "core" );
		BIFMap.put( "arrayreduceright", "core" );
		BIFMap.put( "arrayresize", "core" );
		BIFMap.put( "arrayreverse", "core" );
		BIFMap.put( "arrayset", "core" );
		BIFMap.put( "arraysetmetadata", "" );
		BIFMap.put( "arrayshift", "core" );
		BIFMap.put( "arrayslice", "core" );
		BIFMap.put( "arraysome", "core" );
		BIFMap.put( "arraysort", "core" );
		BIFMap.put( "arraysplice", "core" );
		BIFMap.put( "arraysum", "core" );
		BIFMap.put( "arrayswap", "core" );
		BIFMap.put( "arraytolist", "core" );
		BIFMap.put( "arraytostruct", "core" );
		BIFMap.put( "arrayunique", "" );
		BIFMap.put( "arrayunshift", "core" );
		BIFMap.put( "arrayzip", "" );
		BIFMap.put( "asc | ascii", "core" );
		BIFMap.put( "asin", "core" );
		BIFMap.put( "atn", "core" );
		BIFMap.put( "beat", "" );
		BIFMap.put( "binarydecode", "core" );
		BIFMap.put( "binaryencode", "core" );
		BIFMap.put( "bitand", "core" );
		BIFMap.put( "bitmaskclear", "core" );
		BIFMap.put( "bitmaskread", "core" );
		BIFMap.put( "bitmaskset", "core" );
		BIFMap.put( "bitnot", "core" );
		BIFMap.put( "bitor", "core" );
		BIFMap.put( "bitshln", "core" );
		BIFMap.put( "bitshrn", "core" );
		BIFMap.put( "bitxor", "core" );
		BIFMap.put( "booleanformat", "core" );
		BIFMap.put( "bundleinfo", "" );
		BIFMap.put( "cacheclear", "bx-compat" );
		BIFMap.put( "cachecount", "bx-compat" );
		BIFMap.put( "cachedelete", "bx-compat" );
		BIFMap.put( "cacheget", "bx-compat" );
		BIFMap.put( "cachegetall", "bx-compat" );
		BIFMap.put( "cachegetallids", "bx-compat" );
		BIFMap.put( "cachegetdefaultcachename", "bx-compat" );
		BIFMap.put( "cachegetengineproperties", "bx-compat" );
		BIFMap.put( "cachegetmetadata", "bx-compat" );
		BIFMap.put( "cachegetproperties", "bx-compat" );
		BIFMap.put( "cachegetsession", "bx-compat" );
		BIFMap.put( "cacheidexists", "bx-compat" );
		BIFMap.put( "cachekeyexists", "bx-compat" );
		BIFMap.put( "cacheput", "bx-compat" );
		BIFMap.put( "cacheregionexists", "bx-compat" );
		BIFMap.put( "cacheregionnew", "bx-compat" );
		BIFMap.put( "cacheregionremove", "bx-compat" );
		BIFMap.put( "cacheremove", "bx-compat" );
		BIFMap.put( "cacheremoveall", "bx-compat" );
		BIFMap.put( "cachesetproperties", "bx-compat" );
		BIFMap.put( "callstackdump", "" );
		BIFMap.put( "callstackget", "core" );
		BIFMap.put( "canonicalize", "bx-esapi" );
		BIFMap.put( "ceiling", "core" );
		BIFMap.put( "cfusion_decrypt", "" );
		BIFMap.put( "cfusion_encrypt", "" );
		BIFMap.put( "charsetdecode", "core" );
		BIFMap.put( "charsetencode", "core" );
		BIFMap.put( "chr | char", "core" );
		BIFMap.put( "cjustify", "core" );
		BIFMap.put( "cleartimezone", "core" );
		BIFMap.put( "collectioneach", "" );
		BIFMap.put( "collectionevery", "" );
		BIFMap.put( "collectionfilter", "" );
		BIFMap.put( "collectionmap", "" );
		BIFMap.put( "collectionreduce", "" );
		BIFMap.put( "collectionsome", "" );
		BIFMap.put( "compare", "core" );
		BIFMap.put( "comparenocase", "core" );
		BIFMap.put( "componentcacheclear", "" );
		BIFMap.put( "componentcachelist", "" );
		BIFMap.put( "componentinfo", "" );
		BIFMap.put( "compress", "core" );
		BIFMap.put( "contractpath", "core" );
		BIFMap.put( "cos", "core" );
		BIFMap.put( "createdate", "core" );
		BIFMap.put( "createdatetime", "core" );
		BIFMap.put( "createdynamicproxy", "core" );
		BIFMap.put( "createguid", "core" );
		BIFMap.put( "createobject", "core" );
		BIFMap.put( "createodbcdate", "core" );
		BIFMap.put( "createodbcdatetime", "core" );
		BIFMap.put( "createodbctime", "core" );
		BIFMap.put( "createtime", "core" );
		BIFMap.put( "createtimespan", "core" );
		BIFMap.put( "createuniqueid", "" );
		BIFMap.put( "createuuid", "core" );
		BIFMap.put( "csrfgeneratetoken", "" );
		BIFMap.put( "csrfverifytoken", "" );
		BIFMap.put( "ctcacheclear", "" );
		BIFMap.put( "ctcachelist", "" );
		BIFMap.put( "datasourceflushmetacache", "" );
		BIFMap.put( "dateadd", "core" );
		BIFMap.put( "datecompare", "core" );
		BIFMap.put( "dateconvert", "core" );
		BIFMap.put( "datediff", "core" );
		BIFMap.put( "dateformat", "core" );
		BIFMap.put( "datepart", "core" );
		BIFMap.put( "datetimeformat", "core" );
		BIFMap.put( "day", "core" );
		BIFMap.put( "dayofweek", "core" );
		BIFMap.put( "dayofweekasstring", "core" );
		BIFMap.put( "dayofweekshortasstring", "core" );
		BIFMap.put( "dayofyear", "core" );
		BIFMap.put( "daysinmonth", "core" );
		BIFMap.put( "daysinyear", "core" );
		BIFMap.put( "de", "core" );
		BIFMap.put( "decimalformat", "core" );
		BIFMap.put( "decodeforhtml", "bx-esapi" );
		BIFMap.put( "decodefromurl", "bx-esapi" );
		BIFMap.put( "decrementvalue", "core" );
		BIFMap.put( "decrypt", "core" );
		BIFMap.put( "decryptbinary", "core" );
		BIFMap.put( "deleteclientvariable", "bx-compat" );
		BIFMap.put( "deserialize", "bx-compat" );
		BIFMap.put( "deserializejson", "core" );
		BIFMap.put( "deserializexml", "bx-compat" );
		BIFMap.put( "directorycopy", "core" );
		BIFMap.put( "directorycreate", "core" );
		BIFMap.put( "directorydelete", "core" );
		BIFMap.put( "directoryexists", "core" );
		BIFMap.put( "directorylist", "core" );
		BIFMap.put( "directoryrename", "core" );
		BIFMap.put( "dollarformat", "bx-compat" );
		BIFMap.put( "dotnettocftype", "" );
		BIFMap.put( "duplicate", "core" );
		BIFMap.put( "each", "" );
		BIFMap.put( "echo", "core" );
		BIFMap.put( "empty", "" );
		BIFMap.put( "encodefor", "bx-esapi" );
		BIFMap.put( "encodeforcss", "bx-esapi" );
		BIFMap.put( "encodefordn", "bx-esapi" );
		BIFMap.put( "encodeforhtml", "bx-esapi" );
		BIFMap.put( "encodeforhtmlattribute", "bx-esapi" );
		BIFMap.put( "encodeforjavascript", "bx-esapi" );
		BIFMap.put( "encodeforldap", "bx-esapi" );
		BIFMap.put( "encodeforsql", "bx-esapi" );
		BIFMap.put( "encodeforurl", "bx-esapi" );
		BIFMap.put( "encodeforxml", "bx-esapi" );
		BIFMap.put( "encodeforxmlattribute", "bx-esapi" );
		BIFMap.put( "encodeforxpath", "bx-esapi" );
		BIFMap.put( "encrypt", "core" );
		BIFMap.put( "encryptbinary", "core" );
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
		BIFMap.put( "esapidecode", "bx-esapi" );
		BIFMap.put( "esapiencode", "bx-esapi" );
		BIFMap.put( "evaluate", "bx-unsafe-evaulate" );
		BIFMap.put( "exp", "core" );
		BIFMap.put( "expandpath", "core" );
		BIFMap.put( "extensionexists", "" );
		BIFMap.put( "extensionlist", "" );
		BIFMap.put( "extract", "core" );
		BIFMap.put( "fileappend", "core" );
		BIFMap.put( "fileclose", "core" );
		BIFMap.put( "filecopy", "core" );
		BIFMap.put( "filedelete", "core" );
		BIFMap.put( "fileexists", "core" );
		BIFMap.put( "filegetmimetype", "core" );
		BIFMap.put( "fileinfo", "core" );
		BIFMap.put( "fileiseof", "core" );
		BIFMap.put( "filemove", "core" );
		BIFMap.put( "fileopen", "core" );
		BIFMap.put( "fileread", "core" );
		BIFMap.put( "filereadbinary", "core" );
		BIFMap.put( "filereadline", "core" );
		BIFMap.put( "fileseek", "core" );
		BIFMap.put( "filesetaccessmode", "core" );
		BIFMap.put( "filesetattribute", "core" );
		BIFMap.put( "filesetlastmodified", "core" );
		BIFMap.put( "fileskipbytes", "core" );
		BIFMap.put( "fileupload", "bx-web-support" );
		BIFMap.put( "fileuploadall", "bx-web-support" );
		BIFMap.put( "filewrite", "core" );
		BIFMap.put( "filewriteline", "core" );
		BIFMap.put( "find", "core" );
		BIFMap.put( "findnocase", "core" );
		BIFMap.put( "findoneof", "core" );
		BIFMap.put( "firstdayofmonth", "core" );
		BIFMap.put( "fix", "core" );
		BIFMap.put( "floor", "core" );
		BIFMap.put( "formatbasen", "core" );
		BIFMap.put( "generateargon2hash", "bx-argon" );
		BIFMap.put( "generatebcrypthash", "bx-bcrypt" );
		BIFMap.put( "generatepbkdfkey", "core" );
		BIFMap.put( "generatescrypthash", "bx-scrypt" );
		BIFMap.put( "generatesecretkey", "core" );
		BIFMap.put( "getapplicationmetadata", "core" );
		BIFMap.put( "getapplicationsettings", "" );
		BIFMap.put( "getauthuser", "" );
		BIFMap.put( "getbasetagdata", "core" );
		BIFMap.put( "getbasetaglist", "core" );
		BIFMap.put( "getbasetemplatepath", "core" );
		BIFMap.put( "getbuiltinfunction", "" );
		BIFMap.put( "getcanonicalpath", "core" );
		BIFMap.put( "getclasspath", "" );
		BIFMap.put( "getclientvariableslist", "bx-compat" );
		BIFMap.put( "getcomponentmetadata", "core" );
		BIFMap.put( "getcontextroot", "core" );
		BIFMap.put( "getcpuusage", "core" );
		BIFMap.put( "getcurrentcontext", "" );
		BIFMap.put( "getcurrenttemplatepath", "core" );
		BIFMap.put( "getdirectoryfrompath", "core" );
		BIFMap.put( "getencoding", "" );
		BIFMap.put( "getexception", "" );
		BIFMap.put( "getfilefrompath", "core" );
		BIFMap.put( "getfileinfo", "core" );
		BIFMap.put( "getfreespace", "core" );
		BIFMap.put( "getfunctioncalledname", "core" );
		BIFMap.put( "getfunctiondata", "" );
		BIFMap.put( "getfunctionkeywords", "" );
		BIFMap.put( "getfunctionlist", "core" );
		BIFMap.put( "getgatewayhelper", "" );
		BIFMap.put( "gethttprequestdata", "bx-web-support" );
		BIFMap.put( "gethttptimestring", "" );
		BIFMap.put( "getk2serverdoccount", "" );
		BIFMap.put( "getk2serverdoccountlimit", "" );
		BIFMap.put( "getlocale", "core" );
		BIFMap.put( "getlocalecountry", "" );
		BIFMap.put( "getlocaledisplayname", "core" );
		BIFMap.put( "getlocaleinfo", "core" );
		BIFMap.put( "getlocalelanguage", "" );
		BIFMap.put( "getlocalhostip", "" );
		BIFMap.put( "getluceeid", "" );
		BIFMap.put( "getmemoryusage", "bx-oshi" );
		BIFMap.put( "getmetadata", "core" );
		BIFMap.put( "getmetricdata", "" );
		BIFMap.put( "getnumericdate", "core" );
		BIFMap.put( "getpagecontext", "bx-web-support" );
		BIFMap.put( "getprinterinfo", "" );
		BIFMap.put( "getprinterlist", "" );
		BIFMap.put( "getprofilesections", "" );
		BIFMap.put( "getprofilestring", "" );
		BIFMap.put( "getreadableimageformats", "bx-image" );
		BIFMap.put( "getsafehtml", "bx-esapi" );
		BIFMap.put( "getsoaprequest", "" );
		BIFMap.put( "getsoaprequestheader", "" );
		BIFMap.put( "getsoapresponse", "" );
		BIFMap.put( "getsoapresponseheader", "" );
		BIFMap.put( "getsystemfreememory", "core" );
		BIFMap.put( "getsystemtotalmemory", "core" );
		BIFMap.put( "gettagdata", "" );
		BIFMap.put( "gettaglist", "" );
		BIFMap.put( "gettempdirectory", "core" );
		BIFMap.put( "gettempfile", "core" );
		BIFMap.put( "gettemplatepath", "" );
		BIFMap.put( "gettickcount", "core" );
		BIFMap.put( "gettimezone", "core" );
		BIFMap.put( "gettimezoneinfo", "core" );
		BIFMap.put( "gettoken", "core" );
		BIFMap.put( "gettotalspace", "core" );
		BIFMap.put( "getuserroles", "" );
		BIFMap.put( "getvariable", "bx-compat" );
		BIFMap.put( "getvfsmetadata", "" );
		BIFMap.put( "getwriteableimageformats", "" );
		BIFMap.put( "hash", "core" );
		BIFMap.put( "hash40", "" );
		BIFMap.put( "hmac", "core" );
		BIFMap.put( "hour", "core" );
		BIFMap.put( "htmlcodeformat", "" );
		BIFMap.put( "htmleditformat", "bx-compat" );
		BIFMap.put( "htmlparse", "" );
		BIFMap.put( "iif", "core" );
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
		BIFMap.put( "incrementvalue", "core" );
		BIFMap.put( "inputbasen", "core" );
		BIFMap.put( "insert", "core" );
		BIFMap.put( "int", "core" );
		BIFMap.put( "invalidateoauthaccesstoken", "" );
		BIFMap.put( "invoke", "core" );
		BIFMap.put( "isarray", "core" );
		BIFMap.put( "isbinary", "core" );
		BIFMap.put( "isboolean", "core" );
		BIFMap.put( "isclosure", "core" );
		BIFMap.put( "iscustomfunction", "core" );
		BIFMap.put( "isdate", "core" );
		BIFMap.put( "isdateobject", "core" );
		BIFMap.put( "isddx", "" );
		BIFMap.put( "isdebugmode", "core" );
		BIFMap.put( "isdefined", "core" );
		BIFMap.put( "isempty", "core" );
		BIFMap.put( "isfileobject", "core" );
		BIFMap.put( "isimage", "bx-image" );
		BIFMap.put( "isimagefile", "bx-image" );
		BIFMap.put( "isinstanceof", "core" );
		BIFMap.put( "isipinrange", "" );
		BIFMap.put( "isipv6", "core" );
		BIFMap.put( "isjson", "core" );
		BIFMap.put( "isk2serverabroker", "" );
		BIFMap.put( "isk2serverdoccountexceeded", "" );
		BIFMap.put( "isk2serveronline", "" );
		BIFMap.put( "isleapyear", "core" );
		BIFMap.put( "islocalhost", "core" );
		BIFMap.put( "isnotmap", "" );
		BIFMap.put( "isnull", "core" );
		BIFMap.put( "isnumeric", "core" );
		BIFMap.put( "isnumericdate", "core" );
		BIFMap.put( "isobject", "core" );
		BIFMap.put( "ispdfarchive", "" );
		BIFMap.put( "ispdffile", "" );
		BIFMap.put( "ispdfobject", "" );
		BIFMap.put( "isquery", "core" );
		BIFMap.put( "issafehtml", "bx-esapi" );
		BIFMap.put( "issimplevalue", "core" );
		BIFMap.put( "issoaprequest", "" );
		BIFMap.put( "isspreadsheetfile", "" );
		BIFMap.put( "isspreadsheetobject", "" );
		BIFMap.put( "isstruct", "core" );
		BIFMap.put( "isuserinanyrole", "" );
		BIFMap.put( "isuserinrole", "" );
		BIFMap.put( "isuserloggedin", "" );
		BIFMap.put( "isvalid", "core" );
		BIFMap.put( "isvalidoauthaccesstoken", "" );
		BIFMap.put( "isvideofile", "" );
		BIFMap.put( "iswddx", "bx-wddx" );
		BIFMap.put( "isxml", "core" );
		BIFMap.put( "isxmlattribute", "core" );
		BIFMap.put( "isxmldoc", "core" );
		BIFMap.put( "isxmlelem", "core" );
		BIFMap.put( "isxmlnode", "core" );
		BIFMap.put( "isxmlroot", "core" );
		BIFMap.put( "iszipfile", "core" );
		BIFMap.put( "javacast", "core" );
		BIFMap.put( "jsstringformat", "core" );
		BIFMap.put( "lcase", "core" );
		BIFMap.put( "left", "core" );
		BIFMap.put( "len", "core" );
		BIFMap.put( "listappend", "core" );
		BIFMap.put( "listavg", "core" );
		BIFMap.put( "listchangedelims", "core" );
		BIFMap.put( "listcompact", "core" );
		BIFMap.put( "listcontains", "core" );
		BIFMap.put( "listcontainsnocase", "core" );
		BIFMap.put( "listdeleteat", "core" );
		BIFMap.put( "listeach", "core" );
		BIFMap.put( "listevery", "core" );
		BIFMap.put( "listfilter", "core" );
		BIFMap.put( "listfind", "core" );
		BIFMap.put( "listfindnocase", "core" );
		BIFMap.put( "listfirst", "core" );
		BIFMap.put( "listgetat", "core" );
		BIFMap.put( "listindexexists", "core" );
		BIFMap.put( "listinsertat", "core" );
		BIFMap.put( "listitemtrim", "core" );
		BIFMap.put( "listlast", "core" );
		BIFMap.put( "listlen", "core" );
		BIFMap.put( "listmap", "core" );
		BIFMap.put( "listprepend", "core" );
		BIFMap.put( "listqualify", "core" );
		BIFMap.put( "listreduce", "core" );
		BIFMap.put( "listreduceright", "core" );
		BIFMap.put( "listremoveduplicates", "core" );
		BIFMap.put( "listremoveemptyitems", "" );
		BIFMap.put( "listrest", "core" );
		BIFMap.put( "listsetat", "core" );
		BIFMap.put( "listsome", "core" );
		BIFMap.put( "listsort", "core" );
		BIFMap.put( "listtoarray", "core" );
		BIFMap.put( "listcompact", "core" );
		BIFMap.put( "listvaluecount", "core" );
		BIFMap.put( "listvaluecountnocase", "core" );
		BIFMap.put( "ljustify", "core" );
		BIFMap.put( "location", "bx-web-support" );
		BIFMap.put( "log", "core" );
		BIFMap.put( "log10", "core" );
		BIFMap.put( "lscurrencyformat", "core" );
		BIFMap.put( "lsdateformat", "core" );
		BIFMap.put( "lsdatetimeformat", "core" );
		BIFMap.put( "lsdayofweek", "core" );
		BIFMap.put( "lseurocurrencyformat", "" );
		BIFMap.put( "lsiscurrency", "core" );
		BIFMap.put( "lsisdate", "core" );
		BIFMap.put( "lsisnumeric", "core" );
		BIFMap.put( "lsnumberformat", "core" );
		BIFMap.put( "lsparsecurrency", "core" );
		BIFMap.put( "lsparsedatetime", "core" );
		BIFMap.put( "lsparseeurocurrency", "" );
		BIFMap.put( "lsparsenumber", "core" );
		BIFMap.put( "lstimeformat", "core" );
		BIFMap.put( "lsweek", "core" );
		BIFMap.put( "ltrim", "core" );
		BIFMap.put( "manifestread", "" );
		BIFMap.put( "max", "core" );
		BIFMap.put( "metaphone", "" );
		BIFMap.put( "mid", "core" );
		BIFMap.put( "millisecond", "core" );
		BIFMap.put( "min", "core" );
		BIFMap.put( "minute", "core" );
		BIFMap.put( "month", "core" );
		BIFMap.put( "monthasstring", "core" );
		BIFMap.put( "monthshortasstring", "core" );
		BIFMap.put( "newline", "" );
		BIFMap.put( "now", "core" );
		BIFMap.put( "nowserver", "" );
		BIFMap.put( "nullvalue", "core" );
		BIFMap.put( "numberformat", "core" );
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
		BIFMap.put( "pagepoolclear", "core" );
		BIFMap.put( "pagepoollist", "" );
		BIFMap.put( "paragraphformat", "core" );
		BIFMap.put( "parameterexists", "" );
		BIFMap.put( "parsedatetime", "core" );
		BIFMap.put( "parsenumber", "core" );
		BIFMap.put( "pi", "core" );
		BIFMap.put( "precisionevaluate", "core" );
		BIFMap.put( "preservesinglequotes", "core" );
		BIFMap.put( "quarter", "core" );
		BIFMap.put( "query", "core" );
		BIFMap.put( "queryaddcolumn", "core" );
		BIFMap.put( "queryaddrow", "core" );
		BIFMap.put( "queryappend", "core" );
		BIFMap.put( "queryclear", "core" );
		BIFMap.put( "querycolumnarray", "core" );
		BIFMap.put( "querycolumncount", "core" );
		BIFMap.put( "querycolumndata", "core" );
		BIFMap.put( "querycolumnexists", "core" );
		BIFMap.put( "querycolumnlist", "core" );
		BIFMap.put( "queryconvertforgrid", "" );
		BIFMap.put( "querycurrentrow", "core" );
		BIFMap.put( "querydeletecolumn", "core" );
		BIFMap.put( "querydeleterow", "core" );
		BIFMap.put( "queryeach", "core" );
		BIFMap.put( "queryevery", "core" );
		BIFMap.put( "queryexecute", "core" );
		BIFMap.put( "queryfilter", "core" );
		BIFMap.put( "querygetcell", "core" );
		BIFMap.put( "querygetresult", "core" );
		BIFMap.put( "querygetrow", "core" );
		BIFMap.put( "queryinsertat", "core" );
		BIFMap.put( "querykeyexists", "core" );
		BIFMap.put( "querymap", "core" );
		BIFMap.put( "querynew", "core" );
		BIFMap.put( "queryprepend", "core" );
		BIFMap.put( "queryrecordcount", "core" );
		BIFMap.put( "queryreduce", "core" );
		BIFMap.put( "queryreverse", "core" );
		BIFMap.put( "queryrowdata", "core" );
		BIFMap.put( "queryrowswap", "core" );
		BIFMap.put( "querysetcell", "core" );
		BIFMap.put( "querysetrow", "core" );
		BIFMap.put( "queryslice", "core" );
		BIFMap.put( "querysome", "core" );
		BIFMap.put( "querysort", "core" );
		BIFMap.put( "quotedvaluelist", "core" );
		BIFMap.put( "rand", "core" );
		BIFMap.put( "randomize", "core" );
		BIFMap.put( "randrange", "core" );
		BIFMap.put( "reescape", "core" );
		BIFMap.put( "refind", "core" );
		BIFMap.put( "refindnocase", "core" );
		BIFMap.put( "releasecomobject", "" );
		BIFMap.put( "rematch", "core" );
		BIFMap.put( "rematchnocase", "core" );
		BIFMap.put( "removecachedquery", "" );
		BIFMap.put( "removechars", "core" );
		BIFMap.put( "render", "" );
		BIFMap.put( "repeatstring", "core" );
		BIFMap.put( "replace", "core" );
		BIFMap.put( "replacelist", "core" );
		BIFMap.put( "replacelistnocase", "core" );
		BIFMap.put( "replacenocase", "core" );
		BIFMap.put( "rereplace", "core" );
		BIFMap.put( "rereplacenocase", "core" );
		BIFMap.put( "restdeleteapplication", "" );
		BIFMap.put( "restinitapplication", "" );
		BIFMap.put( "restsetresponse", "" );
		BIFMap.put( "reverse", "core" );
		BIFMap.put( "right", "core" );
		BIFMap.put( "rjustify", "core" );
		BIFMap.put( "round", "core" );
		BIFMap.put( "rtrim", "core" );
		BIFMap.put( "runasync", "core" );
		BIFMap.put( "sanitizehtml", "bx-esapi" );
		BIFMap.put( "second", "core" );
		BIFMap.put( "sendgatewaymessage", "" );
		BIFMap.put( "serialize", "" );
		BIFMap.put( "serializejson | jsonserialize", "core" );
		BIFMap.put( "serializexml | xmlserialize", "" );
		BIFMap.put( "sessioninvalidate", "core" );
		BIFMap.put( "sessionrotate", "core" );
		BIFMap.put( "sessionstarttime", "core" );
		BIFMap.put( "setencoding", "" );
		BIFMap.put( "setlocale", "core" );
		BIFMap.put( "setprofilestring", "" );
		BIFMap.put( "settimezone", "core" );
		BIFMap.put( "setvariable", "bx-compat" );
		BIFMap.put( "sgn", "core" );
		BIFMap.put( "sin", "core" );
		BIFMap.put( "sizeof", "" );
		BIFMap.put( "sleep", "core" );
		BIFMap.put( "soundex", "" );
		BIFMap.put( "spanexcluding", "core" );
		BIFMap.put( "spanincluding", "core" );
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
		BIFMap.put( "sqr", "core" );
		BIFMap.put( "sslcertificateinstall", "" );
		BIFMap.put( "sslcertificatelist", "" );
		BIFMap.put( "storeaddacl", "" );
		BIFMap.put( "storegetacl", "" );
		BIFMap.put( "storegetmetadata", "" );
		BIFMap.put( "storesetacl", "" );
		BIFMap.put( "storesetmetadata", "" );
		BIFMap.put( "stringeach", "core" );
		BIFMap.put( "stringevery", "core" );
		BIFMap.put( "stringfilter", "core" );
		BIFMap.put( "stringlen", "core" );
		BIFMap.put( "stringmap", "core" );
		BIFMap.put( "stringreduce", "core" );
		BIFMap.put( "stringreduceright", "core" );
		BIFMap.put( "stringsome", "core" );
		BIFMap.put( "stringsort", "core" );
		BIFMap.put( "stripcr", "core" );
		BIFMap.put( "structappend", "core" );
		BIFMap.put( "structclear", "core" );
		BIFMap.put( "structcopy", "core" );
		BIFMap.put( "structcount", "core" );
		BIFMap.put( "structdelete", "core" );
		BIFMap.put( "structeach", "core" );
		BIFMap.put( "structequals", "core" );
		BIFMap.put( "structevery", "core" );
		BIFMap.put( "structfilter", "core" );
		BIFMap.put( "structfind", "core" );
		BIFMap.put( "structfindkey", "core" );
		BIFMap.put( "structfindvalue", "core" );
		BIFMap.put( "structget", "core" );
		BIFMap.put( "structgetmetadata", "core" );
		BIFMap.put( "structinsert", "core" );
		BIFMap.put( "structiscasesensitive", "core" );
		BIFMap.put( "structisempty", "core" );
		BIFMap.put( "structisordered", "core" );
		BIFMap.put( "structkeyexists", "core" );
		BIFMap.put( "structkeyarray", "core" );
		BIFMap.put( "structkeylist", "core" );
		BIFMap.put( "structkeytranslate", "core" );
		BIFMap.put( "structlistnew", "" );
		BIFMap.put( "structmap", "core" );
		BIFMap.put( "structnew", "core" );
		BIFMap.put( "structreduce", "core" );
		BIFMap.put( "structsetmetadata", "" );
		BIFMap.put( "structsome", "core" );
		BIFMap.put( "structsort", "core" );
		BIFMap.put( "structtosorted", "core" );
		BIFMap.put( "structupdate", "core" );
		BIFMap.put( "structvaluearray", "core" );
		BIFMap.put( "systemcacheclear", "core" );
		BIFMap.put( "systemoutput", "bx-compat" );
		BIFMap.put( "tan", "core" );
		BIFMap.put( "threadjoin", "core" );
		BIFMap.put( "threadterminate", "core" );
		BIFMap.put( "throw", "core" );
		BIFMap.put( "timeformat", "core" );
		BIFMap.put( "tobase64", "core" );
		BIFMap.put( "tobinary", "core" );
		BIFMap.put( "tonumeric", "core" );
		BIFMap.put( "toscript", "core" );
		BIFMap.put( "tostring", "core" );
		BIFMap.put( "trace", "core" );
		BIFMap.put( "transactioncommit", "core" );
		BIFMap.put( "transactionrollback", "core" );
		BIFMap.put( "transactionsetsavepoint", "core" );
		BIFMap.put( "trim", "core" );
		BIFMap.put( "truefalseformat", "core" );
		BIFMap.put( "ucase", "core" );
		BIFMap.put( "ucfirst", "core" );
		BIFMap.put( "unserializejava", "" );
		BIFMap.put( "urldecode", "core" );
		BIFMap.put( "urlencode", "core" );
		BIFMap.put( "urlencodedformat", "core" );
		BIFMap.put( "urlsessionformat", "" );
		BIFMap.put( "val", "core" );
		BIFMap.put( "valuearray", "core" );
		BIFMap.put( "valuelist", "core" );
		BIFMap.put( "verifybcrypthash", "bx-bcrypt" );
		BIFMap.put( "verifyclient", "" );
		BIFMap.put( "verifyscrypthash", "bx-scrypt" );
		BIFMap.put( "webservicenew", "" );
		BIFMap.put( "week", "core" );
		BIFMap.put( "wrap", "core" );
		BIFMap.put( "writebody", "" );
		BIFMap.put( "writedump", "core" );
		BIFMap.put( "writelog", "core" );
		BIFMap.put( "writeoutput", "core" );
		BIFMap.put( "wsgetallchannels", "bx-websocket" );
		BIFMap.put( "wsgetsubscribers", "bx-websocket" );
		BIFMap.put( "wspublish", "bx-websocket" );
		BIFMap.put( "wssendmessage", "bx-websocket" );
		BIFMap.put( "xmlchildpos", "core" );
		BIFMap.put( "xmlelemnew", "core" );
		BIFMap.put( "xmlformat", "core" );
		BIFMap.put( "xmlgetnodetype", "core" );
		BIFMap.put( "xmlnew", "core" );
		BIFMap.put( "xmlparse", "core" );
		BIFMap.put( "xmlsearch", "core" );
		BIFMap.put( "xmltransform", "core" );
		BIFMap.put( "xmlvalidate", "core" );
		BIFMap.put( "year", "core" );
		BIFMap.put( "yesnoformat", "core" );

		// Commented components have custom transformers and parser rules and don't use
		// the generic component Box AST node.
		// They are also all implemented. We can add custom checks in the vistor for
		// their specific BoxNode AST nodes if we want to report on them as being
		// used.
		componentMap.put( "_socialplugin", "" );
		componentMap.put( "abort", "core" );
		componentMap.put( "admin", "" );
		componentMap.put( "ajaximport", "" );
		componentMap.put( "ajaxproxy", "" );
		componentMap.put( "applet", "" );
		componentMap.put( "application", "core" );
		componentMap.put( "argument", "core" );
		componentMap.put( "associate", "core" );
		// componentMap.put( "break", "core" );
		componentMap.put( "cache", "core" );
		componentMap.put( "calendar", "" );
		// componentMap.put( "case", "core" );
		// componentMap.put( "catch", "core" );
		componentMap.put( "chart", "" );
		componentMap.put( "chartdata", "" );
		componentMap.put( "chartseries", "" );
		componentMap.put( "client", "" );
		componentMap.put( "clientsettings", "" );
		componentMap.put( "col", "" );
		componentMap.put( "collection", "" );
		componentMap.put( "component", "core" );
		componentMap.put( "content", "boxlang-web-support" );
		// componentMap.put( "continue", "core" );
		componentMap.put( "cookie", "boxlang-web-support" );
		componentMap.put( "dbinfo", "core" );
		// componentMap.put( "defaultcase", "core" );
		componentMap.put( "directory", "core" );
		componentMap.put( "div", "" );
		componentMap.put( "document", "bx-pdf" );
		componentMap.put( "documentitem", "bx-pdf" );
		componentMap.put( "documentsection", "bx-pdf" );
		componentMap.put( "dump", "core" );
		// componentMap.put( "else", "core" );
		// componentMap.put( "elseif", "core" );
		componentMap.put( "error", "" );
		componentMap.put( "exchangecalendar", "" );
		componentMap.put( "exchangeconnection", "" );
		componentMap.put( "exchangecontact", "" );
		componentMap.put( "exchangefilter", "" );
		componentMap.put( "exchangemail", "" );
		componentMap.put( "exchangetask", "" );
		componentMap.put( "execute", "core" );
		componentMap.put( "exit", "core" );
		componentMap.put( "feed", "" );
		componentMap.put( "file", "core" );
		componentMap.put( "fileupload", "" );
		// componentMap.put( "finally", "core" );
		componentMap.put( "flush", "core" );
		componentMap.put( "form", "" );
		componentMap.put( "formgroup", "" );
		componentMap.put( "formitem", "" );
		componentMap.put( "forward", "" );
		componentMap.put( "ftp", "" );
		componentMap.put( "function", "core" );
		componentMap.put( "grid", "" );
		componentMap.put( "gridcolumn", "" );
		componentMap.put( "gridrow", "" );
		componentMap.put( "gridupdate", "" );
		componentMap.put( "header", "boxlang-web-support" );
		componentMap.put( "htmlbody", "" );
		componentMap.put( "htmlhead", "boxlang-web-support" );
		componentMap.put( "htmltopdf", "" );
		componentMap.put( "htmltopdfitem", "" );
		componentMap.put( "http", "core" );
		componentMap.put( "httpparam", "core" );
		componentMap.put( "if", "core" );
		componentMap.put( "image", "bx-image" );
		componentMap.put( "imap", "" );
		componentMap.put( "imapfilter", "" );
		// componentMap.put( "import", "core" );
		componentMap.put( "include", "core" );
		componentMap.put( "index", "" );
		componentMap.put( "input", "" );
		componentMap.put( "insert", "core" );
		// componentMap.put( "interface", "core" );
		componentMap.put( "invoke", "core" );
		componentMap.put( "invokeargument", "core" );
		componentMap.put( "layout", "" );
		componentMap.put( "layoutarea", "" );
		componentMap.put( "ldap", "" );
		componentMap.put( "location", "boxlang-web-support" );
		componentMap.put( "lock", "core" );
		componentMap.put( "log", "core" );
		componentMap.put( "login", "" );
		componentMap.put( "loginuser", "" );
		componentMap.put( "logout", "" );
		componentMap.put( "loop", "core" );
		componentMap.put( "mail", "bx-mail" );
		componentMap.put( "mailparam", "bx-mail" );
		componentMap.put( "mailpart", "bx-mail" );
		componentMap.put( "map", "" );
		componentMap.put( "mapitem", "" );
		componentMap.put( "mediaplayer", "" );
		componentMap.put( "menu", "" );
		componentMap.put( "menuitem", "" );
		componentMap.put( "messagebox", "" );
		componentMap.put( "module", "core" );
		componentMap.put( "ntauthenticate", "" );
		componentMap.put( "oauth", "" );
		componentMap.put( "object", "core" );
		componentMap.put( "objectcache", "bx-compat" );
		componentMap.put( "output", "core" );
		componentMap.put( "pageencoding", "" );
		componentMap.put( "param", "core" );
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
		componentMap.put( "print", "core" );
		componentMap.put( "processingdirective", "core" );
		componentMap.put( "procparam", "core" );
		componentMap.put( "procresult", "core" );
		componentMap.put( "progressbar", "" );
		// componentMap.put( "property", "core" );
		componentMap.put( "query", "core" );
		componentMap.put( "queryparam", "core" );
		componentMap.put( "registry", "" );
		componentMap.put( "report", "" );
		componentMap.put( "reportparam", "" );
		// componentMap.put( "rethrow", "core" );
		componentMap.put( "retry", "" );
		// componentMap.put( "return", "core" );
		componentMap.put( "savecontent", "core" );
		componentMap.put( "schedule", "" );
		componentMap.put( "script", "core" );
		componentMap.put( "search", "" );
		componentMap.put( "select", "" );
		componentMap.put( "servlet", "" );
		componentMap.put( "servletparam", "" );
		componentMap.put( "set", "core" );
		componentMap.put( "setting", "core" );
		componentMap.put( "sharepoint", "" );
		componentMap.put( "silent", "core" );
		componentMap.put( "sleep", "core" );
		componentMap.put( "slider", "" );
		componentMap.put( "spreadsheet", "" );
		componentMap.put( "sprydataset", "" );
		componentMap.put( "stopwatch", "bx-compat" );
		componentMap.put( "storedproc", "core" );
		// componentMap.put( "switch", "core" );
		componentMap.put( "table", "" );
		componentMap.put( "textarea", "" );
		componentMap.put( "textinput", "" );
		componentMap.put( "thread", "core" );
		// componentMap.put( "throw", "core" );
		componentMap.put( "timer", "core" );
		componentMap.put( "tooltip", "" );
		componentMap.put( "trace", "core" );
		componentMap.put( "transaction", "core" );
		componentMap.put( "tree", "" );
		componentMap.put( "treeitem", "" );
		// componentMap.put( "try", "core" );
		componentMap.put( "update", "" );
		componentMap.put( "wddx", "bx-wddx" );
		componentMap.put( "websocket", "" );
		// componentMap.put( "while", "core");
		componentMap.put( "window", "" );
		componentMap.put( "xml", "core" );
		componentMap.put( "zip", "core" );
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
			if ( isQoQ( node ) ) {
				name	= "queryExecute (QoQ)";
				missing	= false;
			}
			featuresUsed.add(
			    new FeatureUsed( name, FeatureType.BIF, module, missing, node.getPosition() ) );
			String aggregateKey = name + FeatureType.BIF;
			if ( aggregateFeaturesUsed.containsKey( aggregateKey ) ) {
				aggregateFeaturesUsed.put( aggregateKey, aggregateFeaturesUsed.get( aggregateKey ).increment() );
			} else {
				aggregateFeaturesUsed.put( aggregateKey,
				    new AggregateFeatureUsed( name, FeatureType.BIF, module, missing, 1 ) );
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
				// If the component service has the component, but it's the dummy component,
				// then it's actually missing.
				if ( componentService.getComponent( name ).componentClass.equals( DummyComponent.class ) ) {
					missing = true;
				}
			}
			if ( isQoQ( node ) ) {
				name	= "query (QoQ)";
				// hard coded for now since we don't support
				missing	= false;
			}
			featuresUsed.add(
			    new FeatureUsed( name, FeatureType.COMPONENT, module, missing, node.getPosition() ) );
			String aggregateKey = name + FeatureType.COMPONENT;
			if ( aggregateFeaturesUsed.containsKey( aggregateKey ) ) {
				aggregateFeaturesUsed.put( aggregateKey, aggregateFeaturesUsed.get( aggregateKey ).increment() );
			} else {
				aggregateFeaturesUsed.put( aggregateKey,
				    new AggregateFeatureUsed( name, FeatureType.COMPONENT, module, missing, 1 ) );
			}
		}
		super.visit( node );
	}

	/**
	 * If a cfquery component has a dbType="query" attribute, it's a QoQ.
	 *
	 * @param node The BoxComponent node to check
	 *
	 * @return true if the component is a QoQ, false otherwise
	 */
	private boolean isQoQ( BoxComponent node ) {
		if ( node.getName().equalsIgnoreCase( "query" ) ) {
			return node.getAttributes().stream().filter(
			    ( anno ) -> anno.getKey().getValue().equalsIgnoreCase( "dbtype" )
			        && anno.getValue() instanceof BoxStringLiteral str
			        && str.getValue().equalsIgnoreCase( "query" ) )
			    .toList().size() > 0;
		}
		return false;
	}

	/**
	 * If a BoxFunctionInvocation call has a dbType="query" attribute, it's a QoQ.
	 *
	 * @param node The BoxFunctionInvocation node to check
	 *
	 * @return true if the BIF is a QoQ, false otherwise
	 */
	private boolean isQoQ( BoxFunctionInvocation node ) {
		if ( node.getName().equalsIgnoreCase( "queryexecute" ) && node.getArguments().size() > 0 ) {
			BoxStructLiteral options = null;
			if ( node.getArguments().get( 0 ).getName() == null ) {
				if ( node.getArguments().size() > 2 ) {
					// positional params. Look for the 3rd param
					if ( node.getArguments().get( 2 ).getValue() instanceof BoxStructLiteral opt ) {
						options = opt;
					}
				}
			} else {
				// named params. Look for an arg named "options"
				for ( var arg : node.getArguments() ) {
					if ( arg.getName() instanceof BoxStringLiteral str && str.getValue().equalsIgnoreCase( "options" )
					    && arg.getValue() instanceof BoxStructLiteral opt ) {
						options = opt;
						break;
					}
				}
			}
			// We found options, so let's look at them
			if ( options != null ) {
				// loop through values, if we find an ODD NUMBERED value that is "dbtype", then
				// see if there is a next value and if that next value is "query"
				for ( int i = 0; i < options.getValues().size(); i += 2 ) {
					var key = options.getValues().get( i );
					if ( ( key instanceof BoxStringLiteral str && str.getValue().equalsIgnoreCase( "dbtype" ) )
					    || ( key instanceof BoxIdentifier id && id.getName().equalsIgnoreCase( "dbtype" ) ) ) {
						if ( i + 1 < options.getValues().size() ) {
							if ( options.getValues().get( i + 1 ) instanceof BoxStringLiteral value
							    && value.getValue().equalsIgnoreCase( "query" ) ) {
								return true;
							}
						}
					}
				}
			}

		}
		return false;
	}

	public void visit( BoxMethodInvocation node ) {
		// TODO: check for known member methods
		super.visit( node );
	}

	public static void setupRuntimeStubs() {
		var	runtime				= BoxRuntime.getInstance();
		var	componentService	= runtime.getComponentService();

		// The script parser won't parse components it doesn't know about, so we need to
		// seed dummies or the parsing will fail.
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
				        false ),
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
			return String.format( "%s%s (%s) - %s Source line: %s", ( missing ? "[MISSING] " : "" ), name, type, module,
			    getLine() );
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
			return String.format( "%s%s (%s) - %s Count Used: %s", ( missing ? "[MISSING] " : "" ), name, type, module,
			    count );
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
	 * See constructor. This is used to stub out missing components so the parser
	 * doesn't fail.
	 *
	 */
	private class DummyComponent extends Component {

		@Override
		public BodyResult _invoke( IBoxContext context, IStruct attributes, ComponentBody body, IStruct executionState ) {
			throw new UnsupportedOperationException( "Unimplemented method '_invoke'" );
		}
	}

}
