### Create an array of sequential numbers using the string syntax.



<a href="https://try.boxlang.io/?code=eJxLLCpSsFVILCpKrAxKzEtP1VBQMtLTM1VS0LTmKi%2FKLEn1Ly0pKC0BCodkZBZDFCpkJBYrKCmoQXg%2BqXkaIJaCJlBESSE1JzU3Na%2BkWA9sBACINxyk" target="_blank">Run Example</a>

```java
arr = arrayRange( "2..5" )
writeOutput( "This array has " & arrayLen( arr ) & " elements." )

```

Result: This array has 4 elements.

### Create an array of sequential numbers using the string syntax.



<a href="https://try.boxlang.io/?code=eJxLLCpSsFVILCpKrAxKzEtP1VAw1VEwNFDQtOYqL8osSfUvLSkoLdFQUArJyCyGqFPISCxWUFJQg%2FB8UvM0QCwFTaCIkkJqTmpual5JsZ4SyAgAdvQcfw%3D%3D" target="_blank">Run Example</a>

```java
arr = arrayRange( 5, 10 )
writeOutput( "This array has " & arrayLen( arr ) & " elements." )

```

Result: This array has 6 elements.

### Negative ranges

```java
writeOutput( arrayRange( "-2..2" ).toList() )
```

Result: -2,-1,0,1,2

### When to is less than from

The range always contains at least the from value.

```java
writeOutput( arrayRange( 5, 3 ).toList() )
```

Result: 5
