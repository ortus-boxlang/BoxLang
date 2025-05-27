### Basic applicationStop usage.

Halt the application.


```java
var applicationRequiresHalt = true;
if( applicationRequiresHalt ) applicationStop();
writeOutput( "Still Running" );

```

Result: // We don't expect anything to happen after the application has been stopped.

