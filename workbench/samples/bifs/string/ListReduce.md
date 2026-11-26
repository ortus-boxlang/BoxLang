### Sum the items of a list

```java
numbers = "1,2,3,4,5,6,7,8,9,10"
sum = listReduce( numbers, ( previousValue, value ) -> previousValue + value, 0 )
writeOutput( "The sum of the digits #numbers# is #sum#" )

```

Result: The sum of the digits 1,2,3,4,5,6,7,8,9,10 is 55

### Sum another list

```java
numbers = "1,3,5,7"
reducedVal = listReduce( numbers, ( previousValue, value ) -> previousValue + value, 0 )
writeOutput( "The sum of the digits #numbers# is #reducedVal#" )

```

Result: The sum of the digits 1,3,5,7 is 16

### Use a custom delimiter

```java
writeOutput( listReduce( "2|3|4", ( acc, value ) -> acc * value, 1, "|" ) )

```

Result: 24

### Member function

```java
writeOutput( "a,b,c".listReduce( ( acc, value ) -> acc & value, "" ) )

```

Result: abc
