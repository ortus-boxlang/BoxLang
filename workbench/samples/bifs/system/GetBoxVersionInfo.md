### Basic Example

```java
getBoxVersionInfo();
```

This returns a struct with version information:

```java
{
  "boxlangId"      : "46b61819f59ecd2b0f09b3874f3e41cb",
  "buildDate"      : "2026-04-08 10:29:55",
  "bytecodeVersion": "4",
  "version"        : "1.12.0+53",
  "codename"       : "Jericho"
}
```

<a href="https://try.boxlang.io?code=eJxLTy1xyq8ISy0qzszP88xLy9fQtOYCAFp0B3I%3D" target="_blank">Run Example</a>

### Parsing Version Info

If you need to conditionally execute code based on the BoxLang version, we suggest using `val()` to extract the major/minor version number from the `version` field for use in conditionals:

```java
majorMinor = val( getBoxVersionInfo().version );
if( majorMinor >= "1.13" ) {
    print( "This code is running on Box version 1.13 or later." );
} else {
    print( "This code is running on Box version earlier than 1.13." );
}
```

<a href="https://try.boxlang.io?code=eJydzD8LwjAUBPC9n%2BLIlCyF2lHq4ObgJu5BX9sn6Yu8xCKI3934D3dvOW643%2BRPUbcsUdFh9sFioLyO1z1p4igb6aN19fxecMuKe4vpd1p1ME3dtAYOtwolZ2XJFmY3csIhHgml9SLCMqAYBcfXK88FihJ8Jq3N07%2BDQqI%2FLPIamBR59C%2B4%2FXgPVUBH0Q%3D%3D" target="_blank">Run Example</a>