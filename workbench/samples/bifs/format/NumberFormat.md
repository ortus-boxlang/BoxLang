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



<a href="https://try.boxlang.io/?code=eJzLK81NSi1yyy%2FKTSzRUDDUMzLWUVCKj9czMFBS0AQAkN8IJA%3D%3D" target="_blank">Run Example</a>

```java
numberFormat( 1.23, "__.00" )

```

Result: 1.23

### Two decimal places, integer input



<a href="https://try.boxlang.io/?code=eJzLK81NSi1yyy%2FKTSzRUDA0MtZRUIqP1zMwUFLQBACI7wf2" target="_blank">Run Example</a>

```java
numberFormat( 123, "__.00" )

```

Result: 123.00

### Zero will pad zeros; nine doesn’t



<a href="https://try.boxlang.io/?code=eJzLK801VLBV8CvNTUotcssvyk0s0VAwNDLWUVAyAAElBU2uvNJcIxxqLEEApKa8KLMk1b%2B0pKAUKJcHMlRNQUlfCUiCdWsCAA6%2BHMA%3D" target="_blank">Run Example</a>

```java
num1 = NumberFormat( 123, "00000" )
num2 = NumberFormat( 123, "99999" )
writeOutput( num1 & "/" & num2 )

```

Result: 00123/ 123

### show positive/negative sign



<a href="https://try.boxlang.io/?code=eJzLK801VLBV8CvNTUotcssvyk0s0VAw1VFQ0lZS0OTKK801wpDVhUuXF2WWpPqXlhSUAoXzQCapKSjpKwFJsEZNAAjzGnQ%3D" target="_blank">Run Example</a>

```java
num1 = NumberFormat( 5, "+" )
num2 = NumberFormat( -5, "+" )
writeOutput( num1 & "/" & num2 )

```

Result: +5/-5

### Formats to a dollar format (US)

You could use dollarFormat() or lsCurrencyFormat() instead

<a href="https://try.boxlang.io/?code=eJzLK81NSi1yyy%2FKTSzRUDA0MjYxNTO3sNRRUIpX0bHUs7RUUtAEAM%2F%2BCXk%3D" target="_blank">Run Example</a>

```java
numberFormat( 123456789, "_$,9.99" )

```

Result: $123,456,789.00

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyFkMEOgjAMhu88RcPBQNjGhmhCPJJ4Ir7CMp0nMzAo7283A6LC1lPTfPn%2Btnow9wTawZyv%2FbHrjXomIFixLQnEUjLOY0ghPUCeu3Gkl%2FhlHKc4iLDloFoNFRj1uK0ZUMBtOYEHqmx5ISYKx7EZikvIULy0FYqXzA8JJvj4Cz5lZ7ABGsrP1rX0TdBJSODSGaM8NuK9pNztPwz6GlLP1zt984g2P1%2F%2FJ%2Brx4BfKLaB3" target="_blank">Run Example</a>

```java
dump( numberFormat( 1.234, "__.00" ) ); // 1.23
dump( numberFormat( 1234, "__.00" ) ); // 1234.00
// 0 and 9 mask
dump( numberFormat( 123, "00000" ) )
dump( numberFormat( 123, "99999" ) )
dump( numberFormat( 123.12, "99.99999" ) )
// _ mask
dump( numberFormat( 123, "_____" ) )
dump( numberFormat( 123, "_.___" ) )
dump( numberFormat( 11.10, "__.000" ) )
// + & - mask
dump( numberFormat( 123, "+" ) )
dump( numberFormat( -123, "-" ) )
// , comma
dump( numberFormat( 123, "," ) )
dump( numberFormat( 123456, "," ) )
// L,C mask
dump( NumberFormat( 1, "L999" ) )
dump( NumberFormat( 1, "C000" ) )

```



### Default format with no mask

Without a mask the number is rounded to a whole number and grouped.

```java
writeOutput( numberFormat( 1234567.891 ) )
```

Result: 1,234,568

### Group digits and keep two decimal places

```java
writeOutput( numberFormat( 1234567.891, ",.00" ) )
```

Result: 1,234,567.89

### Using the member function

```java
writeOutput( (1234.567).numberFormat( ",.00" ) )
```

Result: 1,234.57

### Locale aware formatting with lsNumberFormat

`lsNumberFormat()` (or the `locale` argument) uses the given locale's separators.

```java
writeOutput( lsNumberFormat( 1234567.891, ",.00", "de_DE" ) )
```

Result: 1.234.567,89

### Non numeric values throw an error

```java
try {
	numberFormat( "abc" )
} catch ( any e ) {
	writeOutput( e.message )
}
```

Result: Can't cast [abc] to a Number.
