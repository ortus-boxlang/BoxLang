### Cache body content

Caches the output of the component body for the specified duration.

```java
<bx:cache action="cache" key="homepage" timespan="1/24">
    <h1>Welcome to our site</h1>
    <p>This content is cached for 1 hour.</p>
</bx:cache>

```

### Get a cached value

Retrieves a cached value and stores it in a variable.

```java
<bx:cache action="get" key="userCount" name="count">
<bx:dump var="#count#">

```

### Put a value into the cache

```java
<bx:cache action="put" key="config" value="#settings#">

```

### Put body content into the cache

```java
<bx:cache action="put" key="report">
    <table>
        <tr><td>Generated at: #now()#</td></tr>
    </table>
</bx:cache>

```

### Delete a cache entry

```java
<bx:cache action="delete" key="userCount">

```

### Flush all cache entries

```java
<bx:cache action="flush">

```

### Flush a specific cache key

```java
<bx:cache action="flush" key="homepage">

```

### Use a named cache region

```java
<bx:cache action="cache" key="productList" cacheName="products" timespan="1">
    #productService.getAll()#
</bx:cache>

```

### Cache with idle timeout

```java
<bx:cache action="cache" key="sessionData" timespan="1" idleTime="0.5">
    #expensiveOperation()#
</bx:cache>

```

### Disable caching temporarily

```java
<bx:cache action="cache" key="data" useCache="false">
    #alwaysFreshData()#
</bx:cache>

```
