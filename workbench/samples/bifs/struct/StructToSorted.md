### structToSorted  with an inline function sorting by key value

Use a function to sort the keys by numeric value descending

<a href="https://try.boxlang.io/?code=eJx9kE9rwkAQxc%2BZTzF4ihAPiVBog4JiQEFyaMT2KptRFs2u7B8lSL57dxPd0ktvvzdv3vAYLRuqjLLM4AwfCNGo3JTFCD%2FwPXGi2nx7fvO8W38WvTPt1Vex3fcyzbw2inQwNXEdcsLSjcLFWjIaUtDlAEcrmOFSoJbKLNvSNqQ42x8ulvSKNCNRc3GKcSFavPlpmvxyNvCZ2jRQhmN8QMSP8XMf58%2FlwYgUGasETtIcog5e0qkOfAeqwzsG2MmqH8eu4utXyf91cZzDXXEXss01xj9nnfUDVyhvuQ%3D%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	"NINE" : 9,
	"SIX" : 6,
	"THREE" : 3,
	"TWELVE" : 12,
	"tres" : 3,
	"seis" : 6,
	"nueve" : 9,
	"doce" : 12
};

function sortByNumericValuesDescending( Any value1, Any value2, Any key1, Any key2 ) {
	if( value1 > value2 ) {
		return -1;
	}
	return 1;
}
sortedStruct = StructToSorted( someStruct, sortByNumericValuesDescending );
writedump( sortedStruct );

```

Result: A new struct with the keys ordered by value descending

### structToSorted  member function with an inline function sorting by key value

Use a function to sort the keys by numeric value descending

<a href="https://try.boxlang.io/?code=eJx9kM2KwkAQhM%2Fpp2g8RVAhEQQ3KCgGFCQHI7rXZdLKoJmR%2BVGC5N3NJDre9lZfV3VTtJYl5UZZZnCGT4Sgl22ytIc%2FOB00kG9%2BnZ44vV%2Fv0tYZt3RMt4cWo9ixUaS9qYlrvycs3clfLCSjbgvqBOBkBTNcCtRSmWWV2ZIUZ4e%2FqyW9Is1IFFycQ1yICu9uGg2%2BOu70harIqxj7%2BISAn8J3HufvcGcEioxVAodRAkENH2yoBteBCv8O7X8z2su89cL%2Fe2I%2FgYfiTdCWty7r7zXWC0yjbRY%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	"NINE" : 9,
	"SIX" : 6,
	"THREE" : 3,
	"TWELVE" : 12,
	"tres" : 3,
	"seis" : 6,
	"nueve" : 9,
	"doce" : 12
};

function sortByNumericValuesDescending( Any value1, Any value2, Any key1, Any key2 ) {
	if( value1 > value2 ) {
		return -1;
	}
	return 1;
}
sortedStruct = someStruct.ToSorted( sortByNumericValuesDescending );
writedump( sortedStruct );

```

Result: A new struct with the keys ordered by value descending

### structToSorted  member function with an inline function sorting by key name

Use a function to sort the keys by name


```java
someStruct = { 
	"NINE" : 9,
	"SIX" : 6,
	"THREE" : 3,
	"TWELVE" : 12,
	"tres" : 3,
	"seis" : 6,
	"nueve" : 9,
	"doce" : 12
};

function sortByKeyName( Any value1, Any value2, Any key1, Any key2 ) {
	return compareNoCase( key1, key2 );
}
sortedStruct = someStruct.ToSorted( sortByKeyName );
writedump( sortedStruct );

```

Result: A new struct with the keys ordered by key name

### structToSorted  member function with sorttype argument

Use a function to sort the keys by name

<a href="https://try.boxlang.io/?code=eJwrzs9NDS4pKk0uUbBVqFbg4lTy8%2FRzVVKwUrDUAXKCPSNAbDMQO8QjyBUsYwzmhbv6hIG5hkYgfklRajFcsjg1sxiuL680tSwVbmJKfnIqRBdXrTVXcX5RSWoK3AHFcNfoheQHg%2BU0FJRKUitKlHQUlBKLk4FUWmJOcaqCpjVXeVEmUL40t0BDAcUYoBQAaYE6eQ%3D%3D" target="_blank">Run Example</a>

```java
someStruct = { 
	"NINE" : 9,
	"SIX" : 6,
	"THREE" : 3,
	"TWELVE" : 12,
	"tres" : 3,
	"seis" : 6,
	"nueve" : 9,
	"doce" : 12
};
sortedStruct = someStruct.ToSorted( "text", "asc", false );
writedump( sortedStruct );

```

Result: A new struct with the keys ordered by key name

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxtz8kKwjAQBuBz8hRDThU8uIIoPbice6k%2BQGynWugSp1O1iO9u1CrUSA7hn%2BTLTPImwIoxDpnqiMGHG0ihtDEZKpjDTQqhDKXRK437z3iqdcEpN8%2FKcCDF3VbVXhd2uWTokGkroiMSNa4YOWLWiljzn6Em7lAjC%2BR9IS%2BUMm7q3Hhw1uTnnb%2F2IdN7zHy1wqQkhKokTouDgt4H7j7wDbZlaK9g7MHvQ4rxykG51hUqm2KsIrvbQ4Tet80yYaROlwfpCG78" target="_blank">Run Example</a>

```java
myNestedStruct = { 
	"apple" : {
		"price" : 3,
		"quantity" : 10
	},
	"banana" : {
		"price" : 1,
		"quantity" : 5
	},
	"cherry" : {
		"price" : 2,
		"quantity" : 8
	},
	"date" : {
		"price" : 4,
		"quantity" : 12
	}
};
writeDump( var=myNestedStruct, label="Before sorting" );
writeDUmp( var=StructToSorted( myNestedStruct, "textNoCase", "desc", true ), label="After sorting" );

```


<a href="https://try.boxlang.io/?code=eJxdj0FvgkAUhM%2FyKyacILGQXfWEmGDszXih9g74MKQsmMduDTH9711CkOhtvsmbyTzVn4zKgzViuBs3ctTIq4HlzHLg1cxiYGG5yK3ykDQ9frPakFjOWo76h3rxVBI%2B4h0ezqIqPWR8NYoa3QXfyfH8KbB9tyTCEFVAAS7UFc7Cxpm04QYiskR1R5PxYZ2%2FyLlzpelg1M2zKzgeBy9RZznVsbunsmVC17KumqsLfwqcp0Cq2RT6q03tCV08TAX2U%2F9Zk5Sa%2BKXlH2GmY9A%3D" target="_blank">Run Example</a>

```java
myNumb.4 = "5";
myNumb.3 = "2";
myNumb.2 = "3";
myNumb.1 = "1";
cb = ( Any value1, Any value2, Any key1, Any key2 ) => {
	if( arguments.VALUE1 < arguments.VALUE2 // i.e. desc
	 ) return 1;
	 else return -1;
};
writeDump( var=myNumb, label="Before sorting" );
writeDUmp( var=StructToSorted( myNumb, cb ), label="After sorting" );

```


