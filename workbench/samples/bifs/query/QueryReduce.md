### Reduce column to total

Sum one query column

<a href="https://try.boxlang.io/?code=eJxtj8FqwkAQhs%2FZp%2FjZU0KX0lZ6qSj4AE2g1FMpstGJLugmncw2BMm7d6NepDkNfPN%2F8zMVByctFvgJxH1OXQpdjczYUx28aAP9a3l7sGycF9oTR%2FQFlZxVklyjGm%2FQtmmO1Goz0psa8fOrSgbzL9w4T9PCbDLfCtuuJGY3YbxERX0jmyupxR431d1LH7QLW0pxpQYpVr6HNZdRIsNiidjGJIE9LB5QPq7ei3X%2BOVeDwdN4t2MnVARpgqS4K4nLP6JPXqo%3D" target="_blank">Run Example</a>

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
] );
total_fruits = queryReduce( fruits, ( Any a, Any b ) => {
	return a + b.AMOUNT;
}, 0 );
writeOutput( total_fruits );

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
] );
date = createDateTime( 2016, 3, 13, 17, 0, 0 );
totalAge = queryreduce( people, ( Any age = 0, Any row, Any rowNumber, Any recordset ) => {
	return age + DateDiff( "yyyy", recordset.DOB, date );
} );
writeDump( totalAge );

```


