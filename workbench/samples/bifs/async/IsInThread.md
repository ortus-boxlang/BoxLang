### isinthread Example

Check if the code is running inside a bx:thread.

<a href="https://try.boxlang.io/?code=eJwrSi0sTS0u0fP0C%2FEIcnV0UbBVSEvMKU615iovyixJ9S8tKSgt0VDILPbMC8koSk1M0dBU0LTmSqqwKgFzFRKTSzLz82yVikrzlBTyEnNTbZUyoUqVFKq5OIswLUA2zJqrlqs4JzW1QEPB0MDAAGQ2isUY2oEKACLTOhQ%3D" target="_blank">Run Example</a>

```java
request.INTHREAD = false;
writeOutput( isInThread() );
bx:thread action="run" name="inThread" {
	request.INTHREAD = isInThread();
}
sleep( 1000 );
writeOutput( request.INTHREAD );

```

Result: falsetrue

### Additional Examples

