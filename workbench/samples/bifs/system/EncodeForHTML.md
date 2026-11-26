### Escape HTML characters

```java
writeOutput( encodeForHTML( '<b>"Tom" & Jerry</b>' ) )
```

Result: &lt;b&gt;&quot;Tom&quot; &amp; Jerry&lt;/b&gt;

### Use the htmlEditFormat alias

```java
writeOutput( htmlEditFormat( "5 > 3 & 2 < 4" ) )
```

Result: 5 &gt; 3 &amp; 2 &lt; 4
