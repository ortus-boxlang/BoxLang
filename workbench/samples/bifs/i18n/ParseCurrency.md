### lsParseCurrency Example

LSParseCurrency converts a locale-specific currency string to a number.

<a href="https://try.boxlang.io/?code=eJzLKQ5ILCpOdS4tKkrNS67UUFBSMTQy0DM1UFLQtOYCAKArCIo%3D" target="_blank">Run Example</a>

```java
lsParseCurrency( "$120.50" );

```

Result: 120.5

### Additional Examples


```java
<bx:output>
	#LSParseCurrency( 4.5 )#<br>
	#LSParseCurrency( "$4.50" )#<br>
	#LSParseCurrency( "£4.50", "English (UK)" )#
</bx:output>
```


