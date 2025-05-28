### Using Custom Delimiter

Retrieve a pipe separated list of keys

<a href="https://try.boxlang.io/?code=eJwrLkksKS12zk9JLVawVahW4OL091awUjAyMNDh4nQOcnUMcXUB8w2BfD%2F%2FkHhffxdPN0%2BwoLGBCVDQydElPsg1MNQ1OAQoZgLWCFLo5h%2Fq5wIWMeGqteYqL8osSXUpzS3QUCguKSpNLvFOrfTJLC4BceFO0FFQUqgBYk0FTWsuADhoKX4%3D" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
};
writeDump( structKeyList( statusCodes, " | " ) );

```

Result: "OK | CREATED | NOT_MODIFIED | BAD_REQUEST | NOT_FOUND"

### Using Member Function

 Retrieve a comma separated list of keys using the member function

<a href="https://try.boxlang.io/?code=eJwrLkksKS12zk9JLVawVahW4OL091awUjAyMNDh4nQOcnUMcXUB8w2BfD%2F%2FkHhffxdPN0%2BwoLGBCVDQydElPsg1MNQ1OAQoZgLWCFLo5h%2Fq5wIWMeGqteYqL8osSXUpzS3QUChG2KmXnVrpk1lcoqGpoGnNBQD98yWb" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
};
writeDump( statusCodes.keyList() );

```

Result: "OK,CREATED,NOT_MODIFIED,BAD_REQUEST,NOT_FOUND"

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzMvMTcwpVrBVqFbg4nT2D1ewUlDKzc9X0uHiDPB0B%2FHyM%2FOyQVxnxxCwZGp%2BOYjr5BnkAuInZ2QWFShx1Vpz6esrBJcUlSaXeKdW%2BmQWl2gkQgzX5IIwQGJAi1DVKEAVKWhac6UmZ%2BTDBMCKgWIAVPcwOQ%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow",
	BIRD : "chirp"
};
// StructKeyList(animals)
animalList = StructKeyList( animals );
echo( animalList );

```


