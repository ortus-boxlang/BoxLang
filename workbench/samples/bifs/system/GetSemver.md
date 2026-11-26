### Parse a version string

The result is a Semver object whose methods can be called directly.

```java
version = getSemver( "1.2.3-alpha+20151212" )
writeOutput( version.getMajor() & "." & version.getMinor() & "." & version.getPatch() )
```

Result: 1.2.3

### Compare and bump versions

```java
version = getSemver( "1.2.3" )
writeOutput( version.isGreaterThan( "1.2.0" ) & " " & version.nextMinor().toString() )
```

Result: true 1.3.0
