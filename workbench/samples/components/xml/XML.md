### Script Syntax

Creates a new CFML XML document object.

<a href="https://try.boxlang.io/?code=eJw1jk0LgkAYhM%2F5K17ek0FkXWtbCQJvBUFQx9UWeWk%2FZD9Mif57anUaeGaYmbLbdFpBKxyJUskd6v6qFcIrmT0dBXmKoYkhBWT5lJPOkzU7xPVyhZhzVokglK0589bUQPfRQuRMuEA%2B8L0PSvYLOFP1YNkPskAD5Uc51EFhjRFQUCvhZiNcGpZ9bZaNlYP8JxDm2%2BSdTL8OUTcpTGdH%2BgEinUIs" target="_blank">Run Example</a>

```java
bx:xml variable="myXml" {
	writeOutput( "<?xml version=""1.0""?><catalog><song id=""1""><artist>Astley, Rick</artist><title>Never Gonna Give You Up</title></song></catalog>" );
}
writeDump( myXml );

```

Result: 

