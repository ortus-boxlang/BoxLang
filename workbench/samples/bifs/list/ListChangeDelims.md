### Simple Example

Changes the delimiters in the list

<a href="https://try.boxlang.io/?code=eJzLySwucc5IzEtPdUnNycwt1lBQSsvP10lKLNLJyS9KzdXJLCguzVXSUVCqUVLQtOYCAKkAD9E%3D" target="_blank">Run Example</a>

```java
listChangeDelims( "foo,bar,lorem,ipsum", "|" );

```

Result: foo|bar|lorem|ipsum

### Example with Custom Delimiter

Changes the delimiters in the list using a custom delimiter

<a href="https://try.boxlang.io/?code=eJzLySwucc5IzEtPdUnNycwt1lBQSsvP10lKLKrJyS9KzdXJLCguza3JTa0pzs9NLcnIzEtX0lFQqgMRNUoKmtZcADvdFsc%3D" target="_blank">Run Example</a>

```java
listChangeDelims( "foo,bar|lorem,ipsum|me|something", "~", "|" );

```

Result: foo,bar~lorem,ipsum~me~something

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxlzssKwjAUhOF9n2LIqiXB4GVXBMEuKwo%2BQaxHG0jSkpzY19dSceN64P9Ga1ytHx3hkUPHdgjFFC3TOfOYuURrEx97E57UkLM%2BlRAXZwKrZyQKKpkXKTKRe6EgDgIVqrrQGifyN4q%2FKCbLPbqcePC4z6WPEYvEsU2MPcRabuRW7kS98E32Y4llXrn%2FD3rm5Jd7A5D0P3A%3D" target="_blank">Run Example</a>

```java
// Simple function
writeOutput( ListChangeDelims( "Plant,green,save,earth", "@" ) );
// Member function with custom delimiter
strLst = "1+2+3+4";
writeDump( strLst.listChangeDelims( "/", "+" ) );

```


