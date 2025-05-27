###  The simple Querysome example

Here,we've example to check whether the 75 is exists or not in myquery mark column value.


```java
<bx:script>
	myQuery = queryNew( "id,name,mark", "integer,varchar,integer", [
		[
			1,
			"Rahu",
			75
		],
		[
			2,
			"Ravi",
			80
		]
	] );
	result = querySome( myQuery, ( Any details ) => {
		return details.MARK == 75;
	} );
	writeOutput( (result ? "Some" : "No") & " matches  Record found!" );
</bx:script>

```

Result: Some matches Record found!

### The Query Member Function example

Here,we've example to check whether the 85 is exists or not in myquery mark column value using query member function.


```java
<bx:script>
	myQuery = queryNew( "id,name,mark", "integer,varchar,integer", [
		[
			1,
			"Rahu",
			75
		],
		[
			2,
			"Ravi",
			80
		]
	] );
	result = myQuery.Some( ( Any details ) => {
		return details.MARK == 85;
	} );
	writeOutput( (result ? "Some" : "No") & " matches  Record found!" );
</bx:script>

```

Result: No matches Record found!

### Additional Examples

<a href="https://try.boxlang.io/?code=eJyNUE1Lw0AQPWd%2FxSOHsguDJAe1RVNQgwcPESmeSg9bs9WF5qPTpCGI%2F92JtRdJwWUXZmfem3nzalfVW4cEL63jPnOdRljawlFercm%2Bu5AQHiy%2FfVim3DaOfNlIbgkVLFUQhIt270OS6IGdlFN5GvHsOiLEcmGGWqSCFf0SXnk%2Fgp9dnsM%2FsstHCFdnBzz5YgQ%2Fnf7FqxXMjTrYrc9l%2Fd2w%2FqIqBFz%2FWELQuCt7cNXRKcjaYu34%2BN1xL70tDJI5PlXArmm5hNbDxNRvNmJkL0fMEuZF%2BnxPyKpOG2HMERlMJvgP9jZBHEVGpH4Nejv2QmmLWuMoXXLf3pR0hw%3D%3D" target="_blank">Run Example</a>

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
valid = querySome( people, ( Any row, Any rowNumber, Any qryData ) => {
	return ((DateDiff( "yyyy", row.DOB, Now() ) > 0) && (DateDiff( "yyyy", row.DOB, Now() ) <= 100));
} );
writeDump( valid );

```


