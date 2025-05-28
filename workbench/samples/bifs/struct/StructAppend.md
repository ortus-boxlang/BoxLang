### Append options to config struct (without overwrite flag)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctdZc%2BQUlmfl5xSBpiKghUNYZRINki0uKSpNLHAsKUvNSNBSSwSbpKED16CikJeYUpypoWnOVF2WWpPqXlhSUlmgoeAX7%2BwWnFmUm5mRWpcJ0KWiC1AEAiAUo9A%3D%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
};
options = {
	B : 1,
	C : 1
};
structAppend( config, options, false );
writeOutput( JSONSerialize( config ) );

```

Result: {"A":0,"B":0,"C":1}

### Append options to config struct (same, but using member function)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctdZc%2BQUlmfl5xSBpiKghUNYZRINkk8F69RILClLzUjQUoIp1FNISc4pTFTStucqLMktS%2FUtLCkpLNBS8gv39glOLMhNzMqtSNRQgmhU0QeoAWzomUQ%3D%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
};
options = {
	B : 1,
	C : 1
};
config.append( options, false );
writeOutput( JSONSerialize( config ) );

```

Result: {"A":0,"B":0,"C":1}

### Append options to config struct (with overwrite flag)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctdZc%2BQUlmfl5xSBpiKghUNYZRINki0uKSpNLHAsKUvNSNBSSwSbpKMD0aFpzlRdllqT6l5YUlJZoKHgF%2B%2FsFpxZlJuZkVqXClCtogtQBAGzxJp0%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
};
options = {
	B : 1,
	C : 1
};
structAppend( config, options );
writeOutput( JSONSerialize( config ) );

```

Result: {"A":0,"B":1,"C":1}

### Creating a request context from form and url scopes

Demonstrates how to construct a Request Context (rc) that combines the values of the form and url scopes

<a href="https://try.boxlang.io/?code=eJwrSlawVaiuteYqLikqTS5xLChIzUvRUChK1lFIyy%2FKVdDEJlNalAOSKC%2FKLEn1Ly0pKC3RUPAK9vcLTi3KTMzJrEoFKVPQBKkBABt3H1k%3D" target="_blank">Run Example</a>

```java
rc = {};
structAppend( rc, form );
structAppend( rc, url );
writeOutput( JSONSerialize( rc ) );

```


### Polyfill for earlier versions

In older Boxlang version where this function is not supported yet, you can fall back to a native java method to achieve the same behavior except that it does not have the `overwriteFlag`.

<a href="https://try.boxlang.io/?code=eJxLzs9Ly0xXsFWoVuDidFSwUjDQ4eJ0AtFctdZc%2BQUlmfl5xSBpiKghUNYZRINkk8F69QpKSxxzcjQUYIo1rbnKizJLUv1LS4BSGgpewf5%2BwalFmYk5mVWpGgoQXQqaIHUAUXgj9A%3D%3D" target="_blank">Run Example</a>

```java
config = { 
	A : 0,
	B : 0
};
options = {
	B : 1,
	C : 1
};
config.putAll( options );
writeOutput( JSONSerialize( config ) );

```

Result: {"A":0,"B":0,"C":1}

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxlj80KwjAQhM%2FmKYacKhR6V3ooFcSTQgXPa7LQYpuUNLUH8d3tn1bxtrMz2XxDpqiobBDjAbFKjxdsICtrZShWp8N%2BULYwNymeWxFFyHLbQbXOsfGg6a3YtVUdoKQrl7FMf00Z4k4unhXW45XUMXkGwXA3B0U%2FJuM0oPQkyXkkYdu9%2F07qmo2GzxlL2NsPRuZdq%2FyUCt7b8Cu7Xiosbi8Ko8pWcwNF%2FrdMMmN3hc8HF6Q16%2F9OL2AAaSU%3D" target="_blank">Run Example</a>

```java
animals = { 
	COW : "moo",
	PIG : "oink"
};
// Show current animals
Dump( label="Current animals", var=animals );
// Create a new animal
newAnimal = {
	CAT : "meow"
};
// Append the newAnimal to animals
StructAppend( animals, newAnimal );
// Show animals, now includes cat
Dump( label="Animals with cat added", var=animals );

```


