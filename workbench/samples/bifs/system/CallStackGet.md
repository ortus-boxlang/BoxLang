### Tag Syntax

This example the factorial of a number is computed.


```java
<!--- callfact.cfm --->
<bx:try>
    <bx:include template="fact.cfm">

<bx:catch type="any">
    <bx:output>
        #bxcatch.MESSAGE#
        <br>#bxcatch.DETAIL#<br>
    </bx:output>
</bx:catch></bx:try>
```

Result: 

### Script Syntax

This example the factorial of a number is computed.


```java
<!--- fact.cfm --->
<bx:script>

	numeric function factorial( Any n ) {
		if( n == 1 ) {
			writeDump( callStackGet() );
			writeOutput( "<br>" );
			return 1;
		}
		 else {
			writeDump( callStackGet() );
			writeOutput( "<br>" );
			return n * factorial( n - 1 );
		}
	}
	factorial( 5 );
</bx:script>

```

Result: 

### Additional Examples


```java
dump( var=CallStackGet( type="json" ), label="json " );
dump( var=CallStackGet( type="json", offset=2 ), label="json with offset" );
dump( var=CallStackGet( type="json", maxframes=2 ), label="json with maxFrames" );
dump( var=CallStackGet( "string" ), label="string" );
dump( var=CallStackGet( "array" ), label="array" );
dump( var=CallStackGet( "html" ), label="html" );

```


