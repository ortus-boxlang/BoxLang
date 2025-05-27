### Simple example for arraySort function

Uses the arraySort() function to get the sorted array and which sorted by type numeric

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiNDTQ4eI0AhG6lpZA0sQMSJgacMVacyWClAXnF5VoKBTDdOkoKOWV5qYWZSYrAZkpqcXJSgqa1lzlRZklqf6lJQWlQMVewf5%2BwUAliTmZValIehU0QUoB2KMlUg%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	10,
	20,
	-99,
	46,
	50
];
arraySort( someArray, "numeric", "desc" );
writeOutput( JSONSerialize( someArray ) );

```

Result: [50,46,20,10,-99]

### Simple example with member function

CF11+ Lucee4.5+

<a href="https://try.boxlang.io/?code=eJwrzs9NdSwqSqxUsFWIVuDiVHL293FxCw329PdT0gFyk%2FNzUtJKizPz88DcxJT8pFQwy6c02dUVzApy9PTxV%2BKKteYqhhmmV5xfVKKhoFSSWlGipKOglJJanKykoGnNVV6UWZLqX1pSUAqU9gr29wtOLcpMzMmsStVQgOtW0AQpBQBQui12" target="_blank">Run Example</a>

```java
someArray = [ 
	"COLDFUSION",
	"coldfusion",
	"adobe",
	"LucEE",
	"RAILO"
];
someArray.sort( "text", "desc" );
writeOutput( JSONSerialize( someArray ) );

```

Result: ["coldfusion","adobe","RAILO","LucEE","COLDFUSION"]

### Simple example with callback function

Uses the callback function

<a href="https://try.boxlang.io/?code=eJxVjcEKgkAQhs%2B7T%2FHjSWEJMuiQFOwhgiA9eIwOS8xBUFfGlbDw3ds1hLzMDDPf%2FF9vG9LMZsQRd0jxkULk%2BnbGAZGj3lHT1XYkipQ%2F6Mu836WRFJNawwsYnlbwPsDykUkTNKVlF6NfrAoxdDuCturXUyQ4nuCTmdzALZ626QxT7JFNcCnPzAOSTE6hvLhyVAyuG3zytSzykrgydfWmP5GP9egX8cxCsg%3D%3D" target="_blank">Run Example</a>

```java
someArray = [ 
	{
		NAME : "testemployee",
		AGE : "32"
	},
	{
		NAME : "employeetest",
		AGE : "36"
	}
];
arraySort( someArray, ( Any e1, Any e2 ) => {
	return compare( e1.NAME, e2.NAME );
} );
writeOutput( JSONSerialize( someArray ) );

```

Result: [{"NAME":"employeetest","AGE":"36"},{"NAME":"testemployee","AGE":"32"}] 

### Script member syntax: sort array of structs by multiple keys

Takes an array of structs and sorts by multiple different keys, similar to the way a query allows.

<a href="https://try.boxlang.io/?code=eJyVkl9vgjAUxZ%2FhU9zxhIkj4raXGZagsMRlukXMXpY9dFhnM2hNWzRm%2Bt2l4r%2ByLpkvhJ5z7%2F3dU0Cco9XLNJG8SKWAAN7Btn5sy3IKgXl%2F4sA9%2BE11nhIu5BDlWEnOE5tRZ6dn6EyOGK7UCZ4jLnNMT16CMiwqF6WSLHaqb1ub5i9i20REFBuISU7k7E%2FmAPFvLAn9%2Bh%2F3xsANM5KawOoGBKOXxW0ZsbcGbJd9GqBdzpYXIs1J7wzI3gzxjJiyRmhBxOWXrNLaHx0baT%2BZJxiXLrgQ0hWojfzm8bUNDQgeoFyWTN3K9MLeuP8Ww1VQVRzODVVlcSwLTnXnWmvsqCXOxkXxazgaD%2BLheBgOYggOY2v6eq3jy7rWnnma9RwmtSlHpSo938%2F3HvujyvVSlpfXiN1919GARrmu2ldvPEyt951oVUqtSQ9Ub63FLQds1GPJicRRkc9d0L%2BaMreaqysC" target="_blank">Run Example</a>

```java
arrayOfStructs = [ 
	{
		"userId" : 1,
		"firstName" : "John",
		"lastName" : "Doe",
		"departmentName" : "Sales",
		"active" : 1
	},
	{
		"userId" : 2,
		"firstName" : "Jane",
		"lastName" : "Smith",
		"departmentName" : "Marketing",
		"active" : 1
	},
	{
		"userId" : 3,
		"firstName" : "Alice",
		"lastName" : "Johnson",
		"departmentName" : "Sales",
		"active" : 0
	},
	{
		"userId" : 4,
		"firstName" : "Bob",
		"lastName" : "Brown",
		"departmentName" : "Sales",
		"active" : 1
	},
	{
		"userId" : 5,
		"firstName" : "Charlie",
		"lastName" : "Davis",
		"departmentName" : "Marketing",
		"active" : 0
	}
];
arrayOfStructs.sort( ( Any user1, Any user2 ) => {
	if( user1.ACTIVE != user2.ACTIVE ) {
		return user2.ACTIVE - user1.ACTIVE;
	}
	if( user1.DEPARTMENTNAME == user2.DEPARTMENTNAME || user1.ACTIVE == 0 ) {
		if( user1.LASTNAME == user2.LASTNAME ) {
			return user1.FIRSTNAME.compare( user2.FIRSTNAME );
		}
		return user1.LASTNAME.compare( user2.LASTNAME );
	}
	return user1.DEPARTMENTNAME.compare( user2.DEPARTMENTNAME );
} );
writeDump( arrayOfStructs );

```

Result: Sorts by active employees, then by their last name and finally by their first name

### Additional Examples

<a href="https://try.boxlang.io/?code=eJy1kE1rAjEQhs%2FJrxhyWiEoWuihorDYVQSrh%2BCpeFizIxU2myUftEvxvzeJWJRS8OJp3kyeeedDaIW5MWUHE3gHSthss3qdb8Vys2Y8PKWuq4O3R92kZ1npPSa18rIozmo7C4ruxrSMRkIbl4G4%2BHJgDr8cC7FCKxn0xrTyqr1CYmowAIVqjwYOvpEu9KO%2F%2F32bLO%2FyEdf7UPJNCVnnbwW8xHLrULW17jCtQPJFyj%2BNGCUnfgtfwFh0Az9H%2BP9lM8ibDnDIz3EEPZhMITgbdN40ILVqS4NZQPqxFw9MEnH4093Hgc%2Bj%2BwBZa%2BsN%2FrnUQ4b4ATj9oJY%3D" target="_blank">Run Example</a>

```java
SomeArray = [ 
	"COLDFUSION",
	"coldfusion",
	"adobe",
	"LucEE",
	"LUCEE"
];
arraySort( SomeArray, "text", "desc" );
dump( SomeArray );
// member function
SomeArray.sort( "text", "desc" );
dump( SomeArray );
SomeArray = [
	{
		NAME : "testemployee",
		AGE : "32"
	},
	{
		NAME : "employeetest",
		AGE : "36"
	}
];
arraySort( SomeArray, ( Any e1, Any e2 ) => {
	return compare( e1.NAME, e2.NAME );
} );
dump( SomeArray );
// member function with closure
SomeArray.sort( ( Any e1, Any e2 ) => {
	return compare( e1.NAME, e2.NAME );
} );
dump( SomeArray );

```


