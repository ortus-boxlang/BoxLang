### Prepend to a List using the List member function



<a href="https://try.boxlang.io/?code=eJwrTs3MS0vNSfHJLC5RsFVQcs7JL05VCEnMyU4t0gnOLy1Q8EusylTiKkZVh8zVywESAUWpBal5KRoKSgGlaWmVCsEZmUUlSgqaXOVFmSWp%2FqUlBaUlGijaFDQBwS8r9w%3D%3D" target="_blank">Run Example</a>

```java
seinfeldList = "Close Talker,Soup Nazi"
seinfeldList = seinfeldList.listPrepend( "Puffy Shirt" )
writeOutput( seinfeldList )

```

Result: "Puffy Shirt,Close Talker,Soup Nazi"

### Prepend to a List using a dash delimiter



<a href="https://try.boxlang.io/?code=eJwrzs9N9cksLlGwVVAy1DXSNdY1UeIqRgjmAKmAotSC1LwUDQWYuI6CkoESkNBVUtDkKi%2FKLEn1Ly0pKC1BqFDQBADRvxvJ" target="_blank">Run Example</a>

```java
someList = "1-2-3-4"
someList = listPrepend( someList, "0", "-" )
writeOutput( someList )

```

Result: "0-1-2-3-4"

### Prepend to a List with Empty Fields On



<a href="https://try.boxlang.io/?code=eJwrzs9N9cksLlGwVVByS03S8U0s0nEsKFLiKkZI5ACpgKLUgtS8FA0FmLiOgpKOjldinhKIASRKikpTFTS5yosyS1L9S0sKSksQahU0AdM6IWw%3D" target="_blank">Run Example</a>

```java
someList = "Feb,Mar,Apr"
someList = listPrepend( someList, ",,Jan", ",", true )
writeOutput( someList )

```

Result: ",,Jan,Feb,Mar,Apr"

### Prepend to a List with Empty Fields Off



<a href="https://try.boxlang.io/?code=eJwrzs9N9cksLlGwVVByS03S8U0s0nEsKFLiKkZI5ACpgKLUgtS8FA0FmLiOgpKOjldino6OEogJJNISc4pTFTS5yosyS1L9S0sKSksQyhU0ASxPIg8%3D" target="_blank">Run Example</a>

```java
someList = "Feb,Mar,Apr"
someList = listPrepend( someList, ",,Jan,,", ",", false )
writeOutput( someList )

```

Result: "Jan,Feb,Mar,Apr"

### Additional Examples

<a href="https://try.boxlang.io/?code=eJxVzU0KwjAQhuF9TjHMKoXBnMCFoIVCBSEnaO0ggfyRTLTHt0VBXH2bj%2FcxBqwL2TNc1mlf9SpOODXJTTR4V%2BVWOHNcNKBt1dF4skRzWv0UH0QkXAUJcIiVi%2FCC0EGnjIErh5kL9C3exaWoqpRxq8ERkAYin578y%2BCHPbeQNXyfh3%2B8d2WT9vobVhg7jg%3D%3D" target="_blank">Run Example</a>

```java
// Simple Example
writeoutput( listPrepend( "Susi,LAS,,boxlang,,,test", "Inserted" ) )
// Member Function
strList = ",I,,love,boxlang,,"
writeDump( strList.listPrepend( "First" ) )

```


