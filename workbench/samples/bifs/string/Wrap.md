### Script Syntax



<a href="https://try.boxlang.io/?code=eJwrTs0rSc1LTlWwVVAKycgsVgCiRIXiksS8lMSiFIViqLSekjVXeVFiQUFqSjBCB0hEA65GR8FcQROkLLMk1b%2B0pKC0REMBXQ9QHgDmUSdM" target="_blank">Run Example</a>

```java
sentence = "This is a standard sentence.";
wrappedSentence = wrap( sentence, 7 );
writeOutput( wrappedSentence );

```

Result: This is a standar d senten ce.

### Tag Syntax




```java
<bx:set sentence = "This is a standard sentence." >
<bx:set wrappedSentence = wrap( sentence, 7 ) >
<bx:output>#wrappedSentence#</bx:output>
```

Result: This is a standar d senten ce.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxFjksLwjAQhO%2F%2BiiEHacEXXn2AV8GrV0nbNVnYJiFZLf33tnrwNMyDj5EY3KNo5uBwgrlA2HldDzQLujHYnluUNnPSeSM2uJd1hGfMUE%2B43m%2BTWgUF2wiVb5ht4g4dvUli6iko4hOF%2ByQEjfATXEaUmDwX5dYqdRiogU1JZssxlI05LKj1sYI5pkxngyWGCVxB%2Fp9X2O9QT405bn%2Bj%2BrD4AN3XSNI%3D" target="_blank">Run Example</a>

```java
long_string = "A light-weight dynamic scripting language for the JVM that enables the rapid development of simple to highly sophisticated web applications.";
echo( "<pre>" & wrap( long_string, 20 ) & "</pre>" );

```


