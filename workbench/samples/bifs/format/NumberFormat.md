### Format to 2 decimal places 




```java
<!--- 1234.00 ---><bx:output>1.234 ('__.00') ->  <!--- 1.23 --->#numberFormat( 1.234, "__.00" )#<br/> 
1234 ('__.00') -> #numberFormat( 1234, "__.00" )#<br/> </bx:output>
```


### 0 and 9 mask 




```java
<bx:output>
123 ('00000') -> #numberFormat( 123, "00000" )#<br/>
123 ('99999') -> #numberFormat( 123, "99999" )#<br/>
123 ('99.99999') -> #numberFormat( 123.12, "99.99999" )#<br/>
</bx:output>
```


### _ mask




```java
<bx:output>
123 ('_____') -> #numberFormat( 123, "_____" )#<br/>
123 ('_.___') -> #numberFormat( 123, "_.___" )#<br/>
11.10 ('__.000') -> #numberFormat( 11.10, "__.000" )#<br/>
</bx:output>
```


###    + & - mask 




```java
<bx:output>
123 ('+') -> #numberFormat( 123, "+" )#<br/>
-123 ('-') -> #numberFormat( -123, "-" )#<br/>
</bx:output>
```


### , comma




```java
<bx:output>
123 (',') -> #numberFormat( 123, "," )#<br/>
123456 (',') -> #numberFormat( 123456, "," )#<br/>
</bx:output>
```


### L,C mask




```java
<bx:output>
1 ("L999") -> #NumberFormat( 1, "L999" )#<br/>
1 ("C000") -> #NumberFormat( 1, "C000" )#<br/>
</bx:output>
```


### Two decimal places, decimal input



<a href="https://try.boxlang.io/?code=eJzLK81NSi1yyy%2FKTSzRUDDUMzLWUVCKj9czMFBS0LTmAgChpwhp" target="_blank">Run Example</a>

```java
numberFormat( 1.23, "__.00" );

```

Result: 1.23

### Two decimal places, integer input



<a href="https://try.boxlang.io/?code=eJzLK81NSi1yyy%2FKTSzRUDA0MtZRUIqP1zMwUFLQtOYCAJlbCDs%3D" target="_blank">Run Example</a>

```java
numberFormat( 123, "__.00" );

```

Result: 123.00

### Zero will pad zeros; nine doesn’t



<a href="https://try.boxlang.io/?code=eJzLK801VLBV8CvNTUotcssvyk0s0VAwNDLWUVAyAAElBU1rrrzSXCMciixBAKyovCizJNW%2FtKSgFCiZBzJWTUFJXwlIgrUDVQAAfnwdew%3D%3D" target="_blank">Run Example</a>

```java
num1 = NumberFormat( 123, "00000" );
num2 = NumberFormat( 123, "99999" );
writeOutput( num1 & "/" & num2 );

```

Result: 00123/ 123

### show positive/negative sign



<a href="https://try.boxlang.io/?code=eJzLK801VLBV8CvNTUotcssvyk0s0VAw1VFQ0lZS0LTmyivNNcKQ1kXIlxdllqT6l5YUlALF80BmqSko6SsBSbBOoAoAb4AbLw%3D%3D" target="_blank">Run Example</a>

```java
num1 = NumberFormat( 5, "+" );
num2 = NumberFormat( -5, "+" );
writeOutput( num1 & "/" & num2 );

```

Result: +5/-5

### Formats to a dollar format (US)

You could use dollarFormat() or lsCurrencyFormat() instead

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yyy%2FKTSzRUDA0MjYxNTO3sNRRUIpX0bHUs7RUUtC05gIA43AJvg%3D%3D" target="_blank">Run Example</a>

```java
numberFormat( 123456789, "_$,9.99" );

```

Result: 1,2,3,4,5,6,7,8,9.00$

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFkMEOgjAMhu88RcPBQNjGhmhCPJJ4Ir7CMp0nMzAo728HMZuKW09L8%2FXb3%2BrJ3DPoJ3O%2BjsdhNOqZgWDVtiaQSsk4TyGH%2FABlObcTvcav49jFRoJPDqrX0IBRj9s%2FAwq4rUUQoBpbYYqJagaZz2IOGUsgbUUTSBahBBP8fRDuvi9gAzQWoQiI6YJQpyRwGYxRAR8Jr1Pv9h6Exo60fsTT5wCy3ff5f5HWrf0CltmjRQ%3D%3D" target="_blank">Run Example</a>

```java
dump( numberFormat( 1.234, "__.00" ) ); // 1.23
dump( numberFormat( 1234, "__.00" ) ); // 1234.00
// 0 and 9 mask
dump( numberFormat( 123, "00000" ) );
dump( numberFormat( 123, "99999" ) );
dump( numberFormat( 123.12, "99.99999" ) );
// _ mask
dump( numberFormat( 123, "_____" ) );
dump( numberFormat( 123, "_.___" ) );
dump( numberFormat( 11.10, "__.000" ) );
// + & - mask
dump( numberFormat( 123, "+" ) );
dump( numberFormat( -123, "-" ) );
// , comma
dump( numberFormat( 123, "," ) );
dump( numberFormat( 123456, "," ) );
// L,C mask
dump( NumberFormat( 1, "L999" ) );
dump( NumberFormat( 1, "C000" ) );

```


