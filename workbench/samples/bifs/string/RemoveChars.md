### Remove a string



<a href="https://try.boxlang.io/?code=eJwrSs3NL0t1zkgsKtZQUPJIzcnJV3B28%2FVRCM8vyklR0lEw01EwVdC05gIAIHwL%2Bw%3D%3D" target="_blank">Run Example</a>

```java
removeChars( "Hello BL World", 6, 5 );

```

Result: Hello World

### Additional Examples

<a href="https://try.boxlang.io?code=eJxtjskKwjAYhO95ijGnFkprT4rFiwvUgw%2BRNr8aaJKSJl3e3iCiCB7mNMs3k1OeTkH3CRxpO9LxIdyQgNcKTZByWWWoxUgQMKolSLHkPEO5jtoiRVqhKHCee2o9Sdjg%2B%2BB3%2BLRZdK%2BkG3K4BdN6ZQ0bvMMe%2FIIu8nCwcyfMnVds%2Bn6JkfznT5lh88Kxv7z3CHsCnS9Beg%3D%3D" target="_blank">Run Example</a>

```java
writeDump( removeChars( "Hi buddy!, Have a nice day.", 10, 18 ) ); // Expected output: Hi buddy!
// Member function
str = "I love Boxlang";
writeDump( str.removeChars( 1, 7 ) );
 // Expected output: Boxlang

```


