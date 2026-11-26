### Basic xmlFormat() usage

In this example we demonstrate passing the invalid characters < and & into the xmlFormat() function to make them XML safe.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQqMjNccsvyk0EMpVs8vJTUu3c8%2FNTFNQScwusFbxKM5MrbfTBwkoKmgqaAH%2BnE7g%3D" target="_blank">Run Example</a>

```java
writeOutput( xmlFormat( "<node>Good &amp; Juicy</node>" ) )

```

Result: &lt;node&gt;Good &amp;amp; Juicy&lt;/node&gt;

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLinKzEtXsFVQ8lTIycxOVShITcwrLVFIKi0pSS1SUFPISs3JqdRT4kopzS3QUIjIzXHLL8pNLNFQKIbo1FTQ5FLQ11fArj0xt8AaagQAUH4i1w%3D%3D" target="_blank">Run Example</a>

```java
string = "I like peanut butter & jelly."
dump( XmlFormat( string ) )
 // I like peanut butter &amp; jelly.

```


### Escape quotes and angle brackets

```java
writeOutput( xmlFormat( "a < b & c > d" ) )
```

Result: a &lt; b &amp; c &gt; d

### Use the string member function

```java
writeOutput( "<b>".xmlFormat() )
```

Result: &lt;b&gt;
