### Sort a query using the querySort function on a column with date values in ascending order



<a href="https://try.boxlang.io/?code=eJytUU1LAzEQPW9%2BxaOnBKLb%2BnFRKqxND14EqzfxEM1QAt3smmZZi%2FS%2FO1kV1rN7ecw83sfAlCVeO79zsHjvKB4Efdi63dFDXrD8Ju%2Bpl5h5p98i2UTOMMw0MyHRlqJOvqZ9Yh%2BTz6L4FEVxZ3CFheZptVlXT2tjGJhybK2c4zjH4miD29iwJYmTxXyuwQClEZpeKihRHPUo7mzauPNp4y6mjbv8f5x4gboWZYl9ExN8EsMzH3mRGL9ZQ6IKB8Smr%2FTvdAuF5Q34nkipi2FoXzV1ayM3Zunp6Do9WMZMrj7%2B9LuubnN%2FH30iw8vf%2Fqz6Ar9hrUI%3D" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,createdDate", "integer,timestamp", [
	{
		ID : 1,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 2,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 3,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 4,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 5,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	}
] );
// sort it
querySort( exampleQuery, ( Any rowA, Any rowB ) => {
	return dateCompare( rowA.CREATEDDATE, rowB.CREATEDDATE );
} );
// dump it
writeDump( exampleQuery );

```

Result: 

### Sort a query using a sort member function on a column with date values in descending order



<a href="https://try.boxlang.io/?code=eJytUstqwzAQPFtfMeQkgRInfVxaUnCjHHopNPRWclCrJQhi2VVk3FDy7131Ac65vgy7wzwWpLLEa%2Bf3DhbvHcWjoA9bt3t6yguWP%2BQj9RIT7%2FRbJJvIGYaJZiYk2lHUydd0SOxj8kUUn6IoHgxusNA8rTbr6nltDANTjq2VcxznWBxtcBsbdiQxXcznGgxQGqHppYISxUkP4i7GjbscN%2B5q3Ljr%2F8eJLdStKEscmpjg09nbzjIpIVGFI2LTV%2FpvuofC8g58SaTUxYCpzM2rpm5t5LYsng0u09%2BmIQPFvaffctfVbS7vo09keJE4%2B2Os%2BgJlmauM" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,createdDate", "integer,timestamp", [
	{
		ID : 1,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 2,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 3,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 4,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	},
	{
		ID : 5,
		CREATEDDATE : dateAdd( "d", randRange( -100, 100 ), now() )
	}
] );
// sort it
exampleQuery.sort( ( Any rowA, Any rowB ) => {
	return -(dateCompare( rowA.CREATEDDATE, rowB.CREATEDDATE ));
} );
// dump it
writeDump( exampleQuery );

```

Result: 

### Sort a query using a sort member function on a column with string values in ascending order



<a href="https://try.boxlang.io/?code=eJxt0T1rwzAQBuDZ%2BhWHJwVETPqxpKSQxB28pF%2FZSgfZOlKRWHLPcp205L9XSlpwjLe7lwdeuEsSyBu9UyDhs0E6MNzLstrhc1hgdg5X2HKItRIK64J05bQ1sfCJcbhBEl%2BSig9JPnpj0Q%2BLoiyFKUyEn9KH1%2BVL9rTOHlc%2BipXdxCw6ig67GmCFdH12PcByTarvbgbcN%2BYk%2B%2FB2ANZGbjFA9g6jO5YkUFtyoN3FWcYh5MBhbg5Atp2L%2F2kBI5jdg%2B8gdA0ZKGxZSUJ%2BYuNOmzjxbhIKj3%2Btqimr0NqSdpj6hcPFX7z6BVXyeHk%3D" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,description", "integer,varchar", [
	{
		ID : 1,
		DESCRIPTION : "dog"
	},
	{
		ID : 2,
		DESCRIPTION : "cat"
	},
	{
		ID : 3,
		DESCRIPTION : "bird"
	},
	{
		ID : 4,
		DESCRIPTION : "zebra"
	},
	{
		ID : 5,
		DESCRIPTION : "snake"
	}
] );
// sort it
exampleQuery.sort( ( Any rowA, Any rowB ) => {
	return compare( rowA.DESCRIPTION, rowB.DESCRIPTION );
} );
// dump it
writeDump( exampleQuery );

```

Result: 

### Sort a query using the querySort function on a column with string values in descending order



<a href="https://try.boxlang.io/?code=eJxtkMtOhEAQRdf0V9ywgqSV%2BNpoxmQUF2zGx7gzLhq6MnYcGiwacTTz7zaoCRJ2996cykkqSZC3Zquh8NYS7wR9qLLe0n1fsPgZV9RFCI2WmpqCTe1MZUPpF%2BtoQyzfFRcviv30JIIvEQRZinMcSZ%2FSm%2FX1Q3b3mN2u%2FBTqahOKYC9H2PEMVig3xU5msNywnnKnM9wn5aym4NkM2Fj1Sj0onhFfiCRBU7GDcWL4w9qXCOMPSURY2h246pbyL10hxuIS3sTkWrY4iIqqrBVTNICHI6scDsYLYm%2Fe%2F%2Bp1W9a9vmPjKPXlv76nvgGugXsr" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,description", "integer,varchar", [
	{
		ID : 1,
		DESCRIPTION : "dog"
	},
	{
		ID : 2,
		DESCRIPTION : "cat"
	},
	{
		ID : 3,
		DESCRIPTION : "bird"
	},
	{
		ID : 4,
		DESCRIPTION : "zebra"
	},
	{
		ID : 5,
		DESCRIPTION : "snake"
	}
] );
// sort it
querySort( exampleQuery, ( Any rowA, Any rowB ) => {
	return -(compare( rowA.DESCRIPTION, rowB.DESCRIPTION ));
} );
// dump it
writeDump( exampleQuery );

```

Result: 

### Sort a query using the querySort function on a column with numeric values in ascending order



<a href="https://try.boxlang.io/?code=eJxt0EtPwkAQB%2FBz91NMemqTlYqPi1gIWA%2B94ANvxsO2O4GNZVunWyoavru7oAZrbzP%2F%2FJJ5RBFkjSokCHhrkLYM38W6KvDBNRAfwjm2AfhKcol1TqoyqtQ%2Bt4k2uETiG0H5SpCNnpn3yTwvTeAKzrmtktvFzWN6%2F5TezW3ky3LpM2%2FHj9hlD8uF6bKLHpYpkl037HEfmJHowrMeWGvxig6yFwhHLIqgLsmAMmz%2Fh4VtAjj%2BEIcApnoLVLZT%2FlPNIIR4DHYSoWlIw0YUwZ4M7OQQrn%2BD2SGYwMnQju%2BwOP7vTt2FI7b73k4268pt15IymNjm73ZOfQEvm4Fb" target="_blank">Run Example</a>

```java
// build a query
exampleQuery = queryNew( "id,description", "integer,varchar", [
	{
		ID : 3,
		DESCRIPTION : "dog"
	},
	{
		ID : 5,
		DESCRIPTION : "cat"
	},
	{
		ID : 4,
		DESCRIPTION : "bird"
	},
	{
		ID : 1,
		DESCRIPTION : "zebra"
	},
	{
		ID : 2,
		DESCRIPTION : "snake"
	}
] );
// sort it
querySort( exampleQuery, ( Any rowA, Any rowB ) => {
	return val( rowA.ID ) < val( rowB.ID ) ? -1 : val( rowA.ID ) == val( rowB.ID ) ? 0 : 1;
} );
// dump it
writeDump( exampleQuery );

```

Result: 

### Additional Examples


```java
people = QueryNew( "name,dob,age", "varchar,date,int", [ 
	[
		"Susi",
		CreateDate( 1970, 1, 1 ),
		70
	],
	[
		"Urs",
		CreateDate( 1995, 1, 1 ),
		40
	],
	[
		"Fred",
		CreateDate( 1960, 1, 1 ),
		50
	],
	[
		"Jim",
		CreateDate( 1988, 1, 1 ),
		30
	]
] );
Dump( var=people, label="people - original query" );
QuerySort( people, "name", "asc" );
dump( var=people, label="people - sorted by name" );
QuerySort( people, ( Any row1, Any row2 ) => {
	return compare( arguments.ROW1.AGE, arguments.ROW2.AGE );
} );
dump( var=people, label="people - sorted by age" );

```


