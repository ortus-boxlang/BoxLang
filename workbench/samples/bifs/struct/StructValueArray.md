### Traditional function



<a href="https://try.boxlang.io/?code=eJwrLkksKS12zk9JLVawVahW4OL091awUjAyMNDh4nQOcnUMcXUB8w2BfD%2F%2FkHhffxdPN0%2BwoLGBCVDQydElPsg1MNQ1OAQoZgLWCFLo5h%2Fq5wIWMeGqteYqL8osSXUpzS3QUCguKSpNLglLzClNdSwqSqwEiSBcoamgac0FAJkwKWk%3D" target="_blank">Run Example</a>

```java
statusCodes = { 
	OK : 200,
	CREATED : 201,
	NOT_MODIFIED : 304,
	BAD_REQUEST : 400,
	NOT_FOUND : 404
};
writeDump( structValueArray( statusCodes ) );

```

Result: [200, 201, 304, 400, 404]

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxLzMvMTcwpVrBVqFbg4nT2D1ewUlDKzc9X0uHiDPB0B%2FHyM%2FOyQVxnxxCwZGp%2BOYjr5BnkAuInZ2QWFShx1VpzlRdllqSmlOYWaCgUlxSVJpeEJeaUpjoWFSVWaigkQi3SVNC05gIAdXoh7A%3D%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink",
	CAT : "meow",
	BIRD : "chirp"
};
writedump( structValueArray( animals ) );

```


