### Builds a simple query using queryNew and queryAddRow

Pass in row data directly to queryAddRow argument.

<a href="https://try.boxlang.io/?code=eJwljDEKwkAQRWv3FMNUBqaxFSyE1BbiBRb3qwtm1clsliDePYPp3of3fkEb6UCfCp1PaFvinMSyPcHiXAx3qExRr4%2BoTF34m8eUzi%2BXi9dC37DximlPO3Fca1%2Fco2GmhBuijXTROsTC4ecvTbOhr8N7%2FaBuAbdaKxU%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" )
queryAddRow( news, {
	"id" : 1,
	"title" : "Dewey defeats Truman"
} )
writeDump( news )

```


### Builds a simple query using queryNew and queryAddRow member syntax

Pass in row data directly to queryAddRow argument.

<a href="https://try.boxlang.io/?code=eJwljDEKwkAQRWv3FMNUCSyCrWCX2kK8wOB%2BdcGsOpnNEsS7Z8TufXjvF7SJDvSu0OWI1hHnFC3bAxydi%2BEGjbPo5S7K1IfiwVZSOj1d%2FoSN%2B0x72kXHf%2BeLBzQslHCF2ERnraMUDl%2Fvm2bDUMdXR78r6leaKSjh" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" )
news.addRow( {
	"id" : 1,
	"title" : "Dewey defeats Truman"
} )
writeDump( news )

```


### Builds a simple query using queryNew queryAddRow and querySetCell

Using Script with the queryAddRow querySetCell functions to populate the query.

<a href="https://try.boxlang.io/?code=eJydjTsOwjAQRHtOsXIFkhvoKRBpQwFcwMIDRDhr2Kyxcnvi8OkQEt1ofo%2BRO1rSLUH6DfKUTOOtNhpg7KBZcYLYu5PD2Ymh2WRsrrzfxqHMZf3ydtA1QniaZevLw9x8yz%2BQChk9eRzhtKO9pNbxH6TFb1INpuzChSJTHeNIydIoqtRe34wHN2FYsg%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" )
queryAddRow( news )
querySetCell( news, "id", "1" )
querySetCell( news, "title", "Dewey defeats Truman" )
queryAddRow( news )
querySetCell( news, "id", "2" )
querySetCell( news, "title", "Men walk on Moon" )
writeDump( news )

```


### Builds a simple query using queryNew queryAddRow and querySetCell with rows number

The example above could be simplified this way:

<a href="https://try.boxlang.io/?code=eJx9jTEOwjAQBHtecXJFJDdJT4FIGwrgAxZeIMI5w8XGyu8xhKQjzem02pllpJ429IyQYY%2B0JtVaHdrgoHT%2BOeAK0S8j55sRRcXq29xae%2FC5zJnWVE3xEWEH56Y8qz6SMp%2FyX2WeqpEwkMUFJvR0ktgZXgJHd6UW5md3A6Zk3J08U%2BM9%2F6AkbUAdu8dIUPEGPvpUVw%3D%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" )
queryAddRow( news, 2 )
querySetCell( news, "id", "1", 1 )
querySetCell( news, "title", "Dewey defeats Truman", 1 )
querySetCell( news, "id", "2", 2 )
querySetCell( news, "title", "Men walk on Moon", 2 )
writeDump( news )

```


### Builds a simple query using queryNew queryAddRow with multiple rows as an array

The example above could be simplified even more this way:

<a href="https://try.boxlang.io/?code=eJxdzTELwjAQBeA5%2BRWPTBZu0VFwELrWQdzEIZhTg21S08RQxP9ubCfd3sH77jnOAzZ4JA7jjvMCyhqKNrasqGQX%2BcqBnjqcbzooVHJqbo3Z%2B1J2RROOUrykEEUqrLGkb55flFPVnHmE4QvrOOAQUqedkuJNP2r1pxp2yLq9wzs03k9Cnsp%2BDjZynbp%2BXkf1ASqVODk%3D" target="_blank">Run Example</a>

```java
news = queryNew( "id,title", "integer,varchar" )
queryAddRow( news, [
	{
		"id" : 1,
		"title" : "Dewey defeats Truman"
	},
	{
		"id" : 2,
		"title" : "Men walk on Moon"
	}
] )
writeDump( news )

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJx1zTELwjAQBeC5%2BRWPTC0USqmgKA4FHRws6CoOwQQMNKleE0sR%2F7upInSo093Be%2FeZ%2FuAV9VjjPsxKdTG4lqkVRnEkTEip5LHp2l%2BilMMZw3yLKQokK2QZOl3XIOU8WRRsOntiUT5PWcQ%2Fb%2FHQAoJI9LCNE043lrPzgIbzqih0%2F6gT5OwP%2BWTRboMl8kVwq3K%2FDfuIbx35ixv5r%2BBLb24j6w10yl4R" target="_blank">Run Example</a>

```java
myQuery = queryNew( "id,name" )
addedRows = queryAddRow( myQuery, 3 ); // will return 3
queryAddRow( myQuery, [
	17,
	"added via array notation"
] )
anotherRow = queryAddRow( myQuery ); // will return 4
queryAddRow( myQuery, {
	ID : 18,
	NAME : "added via struct notation"
} )
dump( myQuery )

```

### Add a row from a struct

```java
q = queryNew( "id,name" )
row = queryAddRow( q, { id: 1, name: "Ann" } )
writeOutput( row )

```

Result: 1

### Add several empty rows

```java
q = queryNew( "id,name" )
writeOutput( queryAddRow( q, 3 ) & " " & queryRecordCount( q ) )

```

Result: 3 3

### Member function with an array of arrays

```java
q = queryNew( "id,name", "integer,varchar" )
q.addRow( [ [ 1, "Ann" ], [ 2, "Bob" ] ] )
writeOutput( queryRecordCount( q ) & ": " & arrayToList( q.columnData( "name" ) ) )

```

Result: 2: Ann,Bob
