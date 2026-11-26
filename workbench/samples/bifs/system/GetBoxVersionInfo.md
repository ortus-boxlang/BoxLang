### Get the version information

Returns a struct describing the runtime build. The values depend on the installed BoxLang version.

```java
info = getBoxVersionInfo()
writeOutput( info.keyList() )
```

Result: a list of the version info keys, including version, buildDate, codename and boxlangId

### Parse the version info

Use val() to extract the major/minor number from the version field when you need to branch on the BoxLang version.

```java
majorMinor = val( getBoxVersionInfo().version )
if ( majorMinor >= 1.13 ) {
    print( "This code is running on BoxLang 1.13 or later." )
} else {
    print( "This code is running on a BoxLang version earlier than 1.13." )
}
```

Result: prints which side of version 1.13 the running BoxLang is on (depends on the installed version)
