### Simple writeLog Code Example

Logs an order processed successfully message to `orders.log` which will be located in the BX logs directory, eg: `{bx.root}/logs/`


```java
writeLog( text="Order #order.getOrderID()# Processed Successfully", type="information", log="orders" );

```


### Additional Examples

<a href="https://try.boxlang.io/?code=eJwrL8osSfXJT9dQyMlPt1XyrQSylXQUSlIrSmzz8ss1NIHsyoJUW6XUoqL8IiUFTWsuAMW8EHs%3D" target="_blank">Run Example</a>

```java
writeLog( log="MyLog", text=now(), type="error" );

```


