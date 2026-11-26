### Check if debug mode is enabled

```java
writeOutput( isBoolean( isDebugMode() ) )
```

Result: true

### Branch on debug mode

```java
if ( isDebugMode() ) {
	writeOutput( "Debugging is on" )
} else {
	writeOutput( "Debugging is off" )
}
```

Result: Debugging is off (unless the runtime was started in debug mode)
