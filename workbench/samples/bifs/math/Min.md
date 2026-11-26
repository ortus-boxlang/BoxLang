### Minimum of 4 and 9




```java
<bx:set myNum1 = 4 > 
<bx:set myNum2 = 9 > 
<bx:output>The minimum of #myNum1# and #myNum2# numbers is #min( myNum1, myNum2 )#.</bx:output>
```

Result: The minimum of 4 and 9 is 4.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLTc7I11DIzczTUDDUUTBV0FTQBAA4QAS4" target="_blank">Run Example</a>

```java
echo( min( 1, 5 ) )

```

### Smaller of two numbers

```java
writeOutput( min( 3, 8 ) )

```

Result: 3

### Member function with a negative decimal

```java
n = -2.5
writeOutput( n.min( 1 ) )

```

Result: -2.5
