### Remove a string



<a href="https://try.boxlang.io/?code=eJwrSs3NL0t1zkgsKtZQUPJIzcnJV3B28%2FVRCM8vyklR0lEw01EwVdC05gIAIHwL%2Bw%3D%3D" target="_blank">Run Example</a>

```java
removeChars( "Hello BL World", 6, 5 );

```

Result: Hello World

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtjr0KwjAcxPc8xZmphdDaSbE4qVBBHyJN%2FmKgaUqatPbtDR0UweGm%2B%2Fjd7E2gc7RDBk%2FWTXR6Sj9m4I1BG7VeNgKNnAgSvVEELZeCC1TbpD1y5DXKEpfXQCqQhothiOGAT5sl9062JY9H7FUwrmdj8DiCX9ElHm5REfGazd8nKVD8vKkEdiuM%2FaWtE%2BwNiL0%2FwA%3D%3D" target="_blank">Run Example</a>

```java
writeDump( removeChars( "Hi buddy!, Have a nice day.", 10, 18 ) ); // Expected output: Hi buddy!
// Member function
str = "I love Boxlang";
writeDump( str.removeChars( 1, 7 ) );
 // Expected output: Boxlang

```


