### Tag Syntax

In the following example, the function call requests element number 2 from the string, using the delimiter '[:;".' 


```java
<bx:output>
<bx:set mystring = "four," & char( 32 ) & char( 9 ) & char( 10 ) & ",five, nine,zero:;" & char( 10 ) & "nine,ten:, eleven:;twelve:;thirteen," & char( 32 ) & char( 9 ) & char( 10 ) & ",four" >
getToken(mystring, 3) is : #getToken( mystring, 3 )#
</bx:output>
```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwryc9OzYsvSS0uUbBVcE8tCQHxNRSUKlNzcvLLdYpSU6x08osS89JTdZJySlOtrHVyEstS81JSi3QKMvOyrayVdBSMdBSUrJQUNK25yosyS1JdSnMLNBRKEAYDJQA3yCIW" target="_blank">Run Example</a>

```java
token_test = GetToken( "yellow,red:,orange,blue:;,lavender,pink:;", 2, ":" );
writeDump( token_test );

```


