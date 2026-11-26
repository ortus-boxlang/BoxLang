### Calculate average from query columns



<a href="https://try.boxlang.io/?code=eJxlkE1Lw0AQhs%2FJrxjSgwksS1IUrOAh2goVjdDmJj1skyFZyO7qfqSI%2BN8dW5FgbzPvzDMz7wjfD%2BjRwS28B7QfFR5SSGTLtFDI9uh8LRUmjDTtsUPLRmGbXljWYiOVGBLI4iNZtu3GECx%2BJzJ4jaPPOIrWS7iBnFFUlc8ripNSaxpJwt1qW9fro1hc8cs4%2BmITppgyj6bXZ8ycX%2F9j5lNmG87WLHhBRLyjqw9WenwJ%2Fi14slyezr5wIEa0okP4MQ%2Be3IN0MKuC2qN9MFYJah%2Bk8%2BXYpaen3ZshKL0UXkztJ3%2Ffg4x780RImkFGhZznOYkzcNgY3TpOyTdtrG4g" target="_blank">Run Example</a>

```java
athletes = queryNew( "id,name,bestTime", "integer,varchar,decimal" )
queryAddRow( athletes, [
	{
		ID : 0,
		NAME : "Anne",
		BESTTIME : 15.4
	},
	{
		ID : 1,
		NAME : "John",
		BESTTIME : 12.8
	},
	{
		ID : 2,
		NAME : "Sue",
		BESTTIME : 9.1
	}
] )
writeOutput( "Athlete's average best time is #NumberFormat( listAvg( queryColumnData( athletes, "bestTime" ).toList() ), "0.00" )# seconds." )

```

Result: Athlete's average best time is 12.43 seconds.

### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSXUpzS3QUPDJLC5xLEvXUFAy1DHSMVZS0FTQtFbQ11dwrShITS5JTVHILy0pKC2xUjDiAor6puYmpRYpuJXmJZdk5udx5QC1B5cUKdgqKJnomOqYKXGVI8wGSRaXFOnlQO0AGs2F1WhTALL2LTk%3D" target="_blank">Run Example</a>

```java
writeDump( ListAvg( "1,2,3" ) ); // Expected output: 2
// Member Function
listStr = "4,5,6"
writeDump( liststr.listAvg() )
 // Expected output: 5

```

### Average of a list

```java
writeOutput( listAvg( "10,20,30" ) )

```

Result: 20

### Member function with a decimal result

```java
writeOutput( "1,2,3,4".listAvg() )

```

Result: 2.5
