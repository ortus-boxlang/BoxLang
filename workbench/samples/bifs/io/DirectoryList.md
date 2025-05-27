### An array of files in this directory



<a href="https://try.boxlang.io/?code=eJxLLCpKrPRP88lPTsxxy8xJLVawVUjJLEpNLskvqvTJLC7RUEitKEjMSwlILMnQUFDS01dS0NRRSEvMKU7VUVDKS8xNBQpYcwEAo5oXPg%3D%3D" target="_blank">Run Example</a>

```java
arrayOfLocalFiles = directoryList( expandPath( "./" ), false, "name" );

```

Result: [.DS_Store, .ortus, Application.cfc, MyDestinationDirectory, Page.cfc, assets, bifs, components, compressed_test.txt.gz, example.bxm, example.cfm, filepath, images, index.cfm, myNewFileName.txt, new, new_directory, server.json, setup_db.sql, some, test.txt, testcase.txt]

### A query of files in this directory sorted by date last modified



<a href="https://try.boxlang.io/?code=eJwrLE0tqvRPc8vMSS1WsFVIySxKTS7JL6r0ySwu0VBIrShIzEsJSCzJ0FBQ0tNXUtDUUUhLzClO1VFQKgTpVAIyQNglsSTVJ7G4xDc%2FJTMtMzVFwcU12Bmo3JoLAELdHpE%3D" target="_blank">Run Example</a>

```java
queryOfFiles = directoryList( expandPath( "./" ), false, "query", "", "DateLastModified DESC" );

```

Result: [
  {
  name : "testcase.txt",
  size : 0,
  type : "File",
  dateLastModified : {ts '2025-05-28 22:10:14'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "index.cfm",
  size : 10048,
  type : "File",
  dateLastModified : {ts '2025-05-26 22:11:54'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "bifs",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 22:10:29'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "filepath",
  size : 23,
  type : "File",
  dateLastModified : {ts '2025-05-26 22:10:14'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "components",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 22:10:12'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : ".DS_Store",
  size : 6148,
  type : "File",
  dateLastModified : {ts '2025-05-26 18:15:41'},
  attributes : "RWH",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "some",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 18:13:42'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "new_directory",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 18:13:41'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : ".ortus",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 18:13:41'},
  attributes : "RWXH",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "new",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "images",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "MyDestinationDirectory",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "compressed_test.txt.gz",
  size : 340,
  type : "File",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "test.txt",
  size : 614,
  type : "File",
  dateLastModified : {ts '2025-05-26 17:20:07'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "assets",
  size : 0,
  type : "Dir",
  dateLastModified : {ts '2025-05-26 17:03:57'},
  attributes : "RWX",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "myNewFileName.txt",
  size : 57,
  type : "File",
  dateLastModified : {ts '2025-05-26 16:38:57'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "example.bxm",
  size : 899,
  type : "File",
  dateLastModified : {ts '2025-05-26 11:05:52'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "example.cfm",
  size : 876,
  type : "File",
  dateLastModified : {ts '2025-05-26 11:01:38'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "Page.cfc",
  size : 1253,
  type : "File",
  dateLastModified : {ts '2025-05-24 11:23:02'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "server.json",
  size : 108,
  type : "File",
  dateLastModified : {ts '2025-05-24 10:35:52'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "setup_db.sql",
  size : 480,
  type : "File",
  dateLastModified : {ts '2025-05-24 04:42:10'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
},
  {
  name : "Application.cfc",
  size : 42,
  type : "File",
  dateLastModified : {ts '2025-05-24 04:42:10'},
  attributes : "RW",
  mode : "",
  directory : "/Users/scottsteinbeck/Downloads/BL-1468"
}
]

### An array of files in the temp directory

Including sub-directories and as an array containing full paths

<a href="https://try.boxlang.io/?code=eJxLLCpKrPRPC0nNLXDLzEktVrBVSMksSk0uyS%2Bq9MksLtFQUNLTV9JRKCkqTVXQtOYCAKoOD88%3D" target="_blank">Run Example</a>

```java
arrayOfTempFiles = directoryList( "./", true );

```

Result: [/Users/scottsteinbeck/Downloads/BL-1468/.DS_Store, /Users/scottsteinbeck/Downloads/BL-1468/.ortus, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/FileUpload.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/Forward.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/GetHTTPRequestData.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/GetHTTPTimeString.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/HtmlHead.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/Location.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/bifs/SetEncoding.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/components, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/components/Content.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/components/Cookie.md, /Users/scottsteinbeck/Downloads/BL-1468/.ortus/boxlang/web/components/Header.md, /Users/scottsteinbeck/Downloads/BL-1468/Application.cfc, /Users/scottsteinbeck/Downloads/BL-1468/MyDestinationDirectory, /Users/scottsteinbeck/Downloads/BL-1468/Page.cfc, /Users/scottsteinbeck/Downloads/BL-1468/assets, /Users/scottsteinbeck/Downloads/BL-1468/assets/img, /Users/scottsteinbeck/Downloads/BL-1468/assets/img/icons, /Users/scottsteinbeck/Downloads/BL-1468/bifs, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayAppend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayAvg.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayClear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayDelete.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayDeleteAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayEach.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayEvery.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayFilter.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayFind.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayFindAll.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayFirst.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayGetMetadata.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayIndexExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayInsertAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayLast.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayMap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayMax.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayMedian.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayMerge.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayMin.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayNew.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayPop.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayPrepend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayPush.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayRange.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayReduce.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayReduceRight.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayResize.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayReverse.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySet.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayShift.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySlice.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySome.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySort.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySplice.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySum.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArraySwap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayToList.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayToStruct.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/array/ArrayUnshift.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/async, /Users/scottsteinbeck/Downloads/BL-1468/bifs/async/IsInThread.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/async/RunAsync.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BinaryDecode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BinaryEncode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitAnd.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitMaskClear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitMaskRead.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitMaskSet.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitNot.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitOr.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitSh.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/binary/BitXor.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/cache, /Users/scottsteinbeck/Downloads/BL-1468/bifs/cache/Cache.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/conversion, /Users/scottsteinbeck/Downloads/BL-1468/bifs/conversion/ParseNumber.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/conversion/ToBase64.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/conversion/ToNumeric.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/conversion/ToScript.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/conversion/ToString.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsArray.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsBinary.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsBoolean.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsClosure.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsCustomFunction.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsDate.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsDateObject.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsDefined.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsEmpty.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsFileObject.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsIPv6.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsJSON.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsLeapYear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsLocalHost.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsNull.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsNumeric.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsObject.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsQuery.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsSimpleValue.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsStruct.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsValid.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsXML.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsXMLDoc.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsXMLElem.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsXMLNode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/decision/IsXMLRoot.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption/Decrypt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption/Encrypt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption/GeneratePBKDFKey.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption/GenerateSecretKey.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption/Hash.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/encryption/Hmac.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/format, /Users/scottsteinbeck/Downloads/BL-1468/bifs/format/BooleanFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/format/DecimalFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/format/NumberFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n/CurrencyFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n/GetLocale.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n/GetLocaleDisplayName.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n/GetLocaleInfo.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n/ParseCurrency.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/i18n/SetLocale.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/CreateTempFile.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/DirectoryCopy.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/DirectoryCreate.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/DirectoryDelete.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/DirectoryExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/DirectoryList.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/DirectoryMove.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/ExpandPath.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileAppend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileClose.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileCopy.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileDelete.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileGetMimeType.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileInfo.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileIsEOF.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileMove.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileOpen.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileRead.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileReadLine.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileSeek.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileSetAccessMode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileSetAttribute.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileSetLastModified.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileWrite.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/FileWriteLine.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/GetCanonicalPath.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/io/GetDirectoryFromPath.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/java, /Users/scottsteinbeck/Downloads/BL-1468/bifs/java/CreateDynamicProxy.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/jdbc, /Users/scottsteinbeck/Downloads/BL-1468/bifs/jdbc/QueryExecute.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/jdbc/TransactionCommit.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/jdbc/TransactionRollback.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/jdbc/TransactionSetSavepoint.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/GetToken.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListAppend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListAvg.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListChangeDelims.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListCompact.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListDeleteAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListEach.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListEvery.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListFilter.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListFind.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListGetAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListGetEndings.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListIndexExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListInsertAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListItemTrim.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListLen.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListMap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListPrepend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListQualify.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListReduceRight.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListRemoveDuplicates.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListRest.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListSetAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListSome.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListSort.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListToArray.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/list/ListValueCount.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Abs.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Acos.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Asin.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Atn.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Ceiling.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Cos.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/DecrementValue.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Exp.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Fix.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Floor.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/FormatBaseN.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/IncrementValue.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/InputBaseN.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Int.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Log.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Log10.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Max.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Min.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Pi.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/PrecisionEvaluate.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Rand.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/RandRange.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Randomize.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Round.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Sgn.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Sin.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Sqr.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/math/Tan.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryAddColumn.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryAddRow.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryAppend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryClear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryColumnArray.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryColumnCount.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryColumnData.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryColumnExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryColumnList.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryCurrentRow.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryDeleteColumn.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryDeleteRow.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryEach.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryEvery.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryFilter.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryGetCell.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryGetResult.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryInsertAt.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryKeyExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryMap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryNew.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryPrepend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryReduce.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryReverse.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryRowData.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QueryRowSwap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QuerySetCell.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QuerySetRow.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QuerySlice.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QuerySome.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/query/QuerySort.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Ascii.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/CharsetDecode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/CharsetEncode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Compare.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/CompareNoCase.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Find.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/FindOneOf.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Insert.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/JSStringFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Justify.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/LCase.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/LTrim.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Left.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ListReduce.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Mid.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/RTrim.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ReEscape.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ReFind.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ReMatch.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ReReplace.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/RemoveChars.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/RepeatString.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Replace.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ReplaceList.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/ReplaceNoCase.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Reverse.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Right.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/SpanExcluding.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/SpanIncluding.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringEach.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringEvery.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringFilter.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringMap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringReduce.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringReduceRight.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringSome.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StringSort.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/StripCR.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Trim.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/TrueFalseFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/UCFirst.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/UCase.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Val.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/Wrap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/string/YesNoFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructAppend.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructClear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructCopy.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructDelete.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructEach.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructEquals.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructEvery.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructFilter.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructFind.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructFindKey.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructFindValue.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructGet.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructInsert.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructIsCaseSensitive.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructIsOrdered.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructKeyArray.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructKeyExists.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructKeyList.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructMap.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructNew.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructReduce.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructSome.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructSort.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructToQueryString.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructToSorted.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructUpdate.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/struct/StructValueArray.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/ApplicationStop.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/CallStackGet.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/CreateGUID.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/CreateObject.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/CreateUUID.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/DE.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/Dump.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/Duplicate.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/EncodeForHTML.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetApplicationMetadata.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetBaseTagData.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetBaseTemplatePath.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetCurrentTemplatePath.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetFileFromPath.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetFunctionCalledName.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetFunctionList.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetTempDirectory.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/GetTickCount.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/IIF.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/Invoke.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/IsInstanceOf.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/JavaCast.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/PagePoolClear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/Sleep.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/SystemCacheClear.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/SystemOutput.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/Throw.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/URLDecode.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/URLEncodedFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/WriteLog.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/system/WriteOutput.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/ClearTimezone.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/CreateDateTime.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/CreateODBCDateTime.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/CreateTime.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/CreateTimeSpan.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/DateAdd.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/DateCompare.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/DateConvert.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/DateDiff.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/DatePart.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/DateTimeFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/GetTimezoneInfo.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/Now.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/ParseDateTime.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/SetTimezone.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/temporal/TimeUnits.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/type, /Users/scottsteinbeck/Downloads/BL-1468/bifs/type/GetMetaData.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/type/Len.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/type/NullValue.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLElemNew.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLFormat.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLGetNodeType.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLNew.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLParse.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLSearch.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/xml/XMLValidate.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/zip, /Users/scottsteinbeck/Downloads/BL-1468/bifs/zip/Compress.md, /Users/scottsteinbeck/Downloads/BL-1468/bifs/zip/Extract.md, /Users/scottsteinbeck/Downloads/BL-1468/components, /Users/scottsteinbeck/Downloads/BL-1468/components/async, /Users/scottsteinbeck/Downloads/BL-1468/components/async/Thread.md, /Users/scottsteinbeck/Downloads/BL-1468/components/debug, /Users/scottsteinbeck/Downloads/BL-1468/components/debug/Timer.md, /Users/scottsteinbeck/Downloads/BL-1468/components/io, /Users/scottsteinbeck/Downloads/BL-1468/components/io/Directory.md, /Users/scottsteinbeck/Downloads/BL-1468/components/io/File.md, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc/DBInfo.md, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc/ProcParam.md, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc/Query.md, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc/QueryParam.md, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc/StoredProc.md, /Users/scottsteinbeck/Downloads/BL-1468/components/jdbc/Transaction.md, /Users/scottsteinbeck/Downloads/BL-1468/components/net, /Users/scottsteinbeck/Downloads/BL-1468/components/net/HTTP.md, /Users/scottsteinbeck/Downloads/BL-1468/components/net/HTTPParam.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Abort.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Component.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Execute.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Exit.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Flush.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Include.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/InvokeArgument.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Lock.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Loop.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/ObjectComponent.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Output.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Param.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/SaveContent.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Setting.md, /Users/scottsteinbeck/Downloads/BL-1468/components/system/Silent.md, /Users/scottsteinbeck/Downloads/BL-1468/components/xml, /Users/scottsteinbeck/Downloads/BL-1468/components/xml/XML.md, /Users/scottsteinbeck/Downloads/BL-1468/components/zip, /Users/scottsteinbeck/Downloads/BL-1468/components/zip/Zip.md, /Users/scottsteinbeck/Downloads/BL-1468/compressed_test.txt.gz, /Users/scottsteinbeck/Downloads/BL-1468/example.bxm, /Users/scottsteinbeck/Downloads/BL-1468/example.cfm, /Users/scottsteinbeck/Downloads/BL-1468/filepath, /Users/scottsteinbeck/Downloads/BL-1468/images, /Users/scottsteinbeck/Downloads/BL-1468/images/uploads, /Users/scottsteinbeck/Downloads/BL-1468/index.cfm, /Users/scottsteinbeck/Downloads/BL-1468/myNewFileName.txt, /Users/scottsteinbeck/Downloads/BL-1468/new, /Users/scottsteinbeck/Downloads/BL-1468/new/location, /Users/scottsteinbeck/Downloads/BL-1468/new/location/for, /Users/scottsteinbeck/Downloads/BL-1468/new_directory, /Users/scottsteinbeck/Downloads/BL-1468/server.json, /Users/scottsteinbeck/Downloads/BL-1468/setup_db.sql, /Users/scottsteinbeck/Downloads/BL-1468/some, /Users/scottsteinbeck/Downloads/BL-1468/some/other, /Users/scottsteinbeck/Downloads/BL-1468/some/other/path, /Users/scottsteinbeck/Downloads/BL-1468/test.txt, /Users/scottsteinbeck/Downloads/BL-1468/testcase.txt]

### Filter files with closure

Lucee4.5+ Pass a closure instead of a string as `filter` param

<a href="https://try.boxlang.io/?code=eJwljU0KwjAQhdf2FENXFUIuIBWK2pU%2F4A3GZEojbVJmxmIR726Km%2Fd48D4%2BZMbl1rVhUGLyuUmgBh%2BYnCZezkG0gtKWBjochAyUEUfKs4ImLjCh9rCFeg%2BfYsOkL46wMm2I%2FpoOKJTpZpqG4FBDitZ1znB6JBWrbzVCPBPbp6RoOpyDy5ccxvaKzpGIuZ%2Ba4%2BVkR5%2Bdf9uu%2BK7xA0D6Pj4%3D" target="_blank">Run Example</a>

```java
arrayOfFilteredFiles = directoryList( ".", false, "name", ( Any path ) => {
	return ListFindNoCase( "Application.cfc,robots.txt,server.json,favicon.ico,.htaccess,README.md", path );
} );

```

Result: []

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLySxKTS7JL6r0ySwu0VBQ0i9LLNJPSSxJVNJRSEvMKU7VUVAqSCzJAHI1FBzzKhVAHAVNBVs7hWouzqLUktKiPIXEovTS3NS8kmK9AMcQD72MxOLg0rS0zAqgeXo5%2BelKCprWXLUgAgDHRiJT" target="_blank">Run Example</a>

```java
directoryList( "/var/data", false, "path", ( Any path ) => {
	return arguments.PATH.hasSuffix( ".log" );
} );

```

Result: []

