### Set page encoding and suppress whitespace

Controls page-level processing directives for the enclosed content.

```java
<bx:processingDirective pageEncoding="UTF-8" suppressWhiteSpace="true">
    <h1>Compact Output</h1>
    <p>No extra whitespace.</p>
</bx:processingDirective>

```

### Suppress whitespace only

```java
<bx:processingDirective suppressWhiteSpace="true">
    #trim( output )#
</bx:processingDirective>

```
