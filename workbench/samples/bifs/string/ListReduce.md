### Script Syntax



<a href="https://try.boxlang.io/?code=eJxVjc8KgkAYxM%2F5FMN6MfoO2v8whZ4giOieudWCrrK7nxHRu7dml24z85thNNeFNBYZREJTmtGcFrSkFa1pQ0ks0sBy7WmlrDvIki8ygh42hAg7%2FURrZKcatqdzxZK%2BUddLjJHleAUjIx0b%2Fd%2FDZCilwZsQY5wGD6Oc3LNr2UUQx7tE%2F9xc4bws1U05i%2FB3HUJ543m4LUwu%2BvkHSig%2B6w%3D%3D" target="_blank">Run Example</a>

```java
numbers = "1,2,3,4,5,6,7,8,9,10";
sum = listReduce( numbers, ( Any previousValue, Any value ) => {
	return previousValue + value;
}, 0 );
writeOutput( "The sum of the digits #numbers# is #sum#<br>" );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVjsEKwjAQRM%2FmK4b00mIOioiH0oJfIIh4V7tqoE1LslsR8d9NW0G8zT4eO%2BOkOZMPKKCXZmXWZqNz5amSC1XHUx15bQPvR5DCTbZBiq17ovPU21ZCFIXMiPohIkNR4qVmnli8%2B%2Fcwn6RcvQ0WyHL18JZpJ9wJp9CHOyFIg%2FYKjrGyN8sBybc6gY3Hb2Cihw8fC7FAew%3D%3D" target="_blank">Run Example</a>

```java
numbers = "1,3,5,7";
reducedVal = listReduce( numbers, ( Any previousValue, Any value ) => {
	return previousValue + value;
}, 0 );
writeOutput( "The sum of the digits #numbers# is #reducedVal#" );

```


