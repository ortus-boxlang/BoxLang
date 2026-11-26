### Reduce column to total

Sum one query column

<a href="https://try.boxlang.io/?code=eJxtj8uKwkAQRdfpr7j0KsFm8IEbIYIfMAqiKxHpaEUbnDZTqTaI%2BO%2B2j42YVcHhnnupkoOTGjn%2BA%2FFlSk0KXT6YsX%2Bn4EUb6LPl7cGycV5oTxzRCiq5qiR5RTVG0LaqjlRr86BvNeLeUCU38xWunKd2YdCar4VtUxCzazH6UVFrZEpOYo%2Bb8uOjOe3CllK8qEGKib%2FAmucpkCEfI44xSWAPiw6Kn8nvbDldqJtBN7Y27IRmQaogKT4msjvcal20" target="_blank">Run Example</a>

```java
fruits = queryNew( "fruit,amount", "varchar,integer", [ 
	{
		"fruit" : "apples",
		"amount" : 15
	},
	{
		"fruit" : "pineapples",
		"amount" : 3
	},
	{
		"fruit" : "strawberries",
		"amount" : 32
	}
] )
total_fruits = queryReduce( fruits, ( Any a, Any b ) => {
	return a + b.AMOUNT
}, 0 )
writeOutput( total_fruits )

```

Result: 50

### Additional Examples


```java
people = QueryNew( "name,dob,age", "varchar,date,int", [ 
	[
		"Susi",
		CreateDate( 1970, 1, 1 ),
		0
	],
	[
		"Urs",
		CreateDate( 1995, 1, 1 ),
		0
	],
	[
		"Fred",
		CreateDate( 1960, 1, 1 ),
		0
	],
	[
		"Jim",
		CreateDate( 1988, 1, 1 ),
		0
	]
] )
date = createDateTime( 2016, 3, 13, 17, 0, 0 )
totalAge = queryreduce( people, ( Any age = 0, Any row, Any rowNumber, Any recordset ) => {
	return age + DateDiff( "yyyy", recordset.DOB, date )
} )
writeDump( totalAge )

```

### Member function

```java
q = queryNew( "amount", "integer", [ [ 15 ], [ 3 ], [ 32 ] ] )
writeOutput( q.reduce( ( total, row ) -> total + row.amount, 0 ) )

```

Result: 50
