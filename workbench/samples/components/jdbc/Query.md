### Simple Query Usage

A simple query needs nothing more than a SQL statement:

```html
<bx:query name="totalUserCount">
    UPDATE users SET modifiedTime=GETDATE() WHERE id=1
</bx:query>
```

Though, for SELECT queries, a variable name is necessary to acquire the results object:

```html
<bx:query name="totalUserCount">
    SELECT COUNT(*) FROM users
</bx:query>
```

By default, the results will be in [Query](https://boxlang.ortusbooks.com/boxlang-language/reference/types/query) format. [Array](https://boxlang.ortusbooks.com/boxlang-language/reference/types/array) and [Struct](https://boxlang.ortusbooks.com/boxlang-language/reference/types/struct) results are also supported:

```html
<bx:query name="users" returnType="array">
    SELECT name, email FROM users
</bx:query>
```

For struct results, use `columnKey` to define the column name which will form the struct key in the resulting struct object:

```html
<bx:query name="user" returnType="struct" columnKey="name">
    SELECT name, email FROM users
    WHERE id=1
</bx:query>
<bx:output>
    #variables.user[ "Michael" ].firstname#
    #variables.user[ "Michael" ].email#
</bx:output>
```

### Caching Query Results

Queries can be cached by specifying `cache=true` and a cache timespan:

```html
<bx:query
    name="totalUserCount"
    cache="true"
    cacheTimeout="#createTimespan( 0, 0, 0, 2 )#"
>
    SELECT * FROM users
</bx:query>
```

You can customize the cache usage by creating a custom cache configuration [in your `boxlang.json`](https://boxlang.ortusbooks.com/getting-started/configuration#boxlang.json):

```json
// boxlang.json
{	
    "caches": {
		// JDBC query store
		"bxQuery": {
			"provider": "BoxCacheProvider",
			"properties": {
				"evictCount": 1,
				"evictionPolicy": "LRU",
				"freeMemoryPercentageThreshold": 0,
				"maxObjects": 500,
				// 30 minutes if not used
				"defaultLastAccessTimeout": 1800,
				// 60 minutes default
				"defaultTimeout": 3600,
				"objectStore": "ConcurrentSoftReferenceStore",
				"reapFrequency": 120,
				"resetTimeoutOnAccess": false,
				"useLastAccessTimeouts": true
			}
		}
	}
}
```

Then reference the cache name in the query `cacheProvider` attribute:

```html
<bx:query
    name="totalUserCount"
    cache="true"
    cacheTimeout="#createTimespan( 0, 0, 0, 2 )#"
    cacheProvider="bxQuery"
>
    SELECT * FROM users
</bx:query>
```
### Empty query

Create an empty query object


```java
<bx:script>
	myQuery = query();
</bx:script>

```


### Query with some data

Create query object with some initial data


```java
<bx:script>
	myQuery = query( foo=[
		1,
		2,
		3
	], bar=[
		"a",
		"b",
		"c"
	] );
</bx:script>

```


### Additional Examples


```java
myquery = query( columnName1=[ 
	1,
	2,
	3
], columnName2=[
	4,
	5,
	6
] );
dump( myquery );
column = "size";
values = [
	"small",
	"medium",
	"large"
];
myquery = query( "#column#"=values, column=values );
dump( myquery );
myquery = query( columnName=[] );
dump( var=myquery, label="empty query" );
myquery = query();
dump( var=myquery, label="no-argument query" );

```


