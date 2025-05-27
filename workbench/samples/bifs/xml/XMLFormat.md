### Basic xmlFormat() usage

In this example we demonstrate passing the invalid characters < and & into the xmlFormat() function to make them XML safe.

<a href="https://try.boxlang.io/?code=eJwrL8osSfUvLSkoLdFQqMjNccsvyk0EMpVs8vJTUu3c8%2FNTFNQScwusFbxKM5MrbfTBwkoKmgqa1lwAp5cT%2FQ%3D%3D" target="_blank">Run Example</a>

```java
writeOutput( xmlFormat( "<node>Good &amp; Juicy</node>" ) );

```

Result: &lt;node&gt;Good &amp;amp; Juicy&lt;/node&gt;

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrLinKzEtXsFVQ8lTIycxOVShITcwrLVFIKi0pSS1SUFPISs3JqdRTsuZKKc0t0FCIyM1xyy%2FKTSzRUCiGaNVU0LTmUtDXV8BuQGJugTXUEC4AsI0jVw%3D%3D" target="_blank">Run Example</a>

```java
string = "I like peanut butter & jelly.";
dump( XmlFormat( string ) );
 // I like peanut butter &amp; jelly.

```


