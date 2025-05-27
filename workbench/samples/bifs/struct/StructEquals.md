### Append options to config struct (without overwrite flag)



<a href="https://try.boxlang.io/?code=eJxLzs9Ly0w3VLBVqFbg4nRUsFIw0OHidALRXLXWXMlgaSOQNKqsIUi2vCizJNW%2FtKSgtERDobikqDS5xLWwNDGnWEMBotFQRwFmgqaCpjUXABwCHhk%3D" target="_blank">Run Example</a>

```java
config1 = { 
	A : 0,
	B : 0
};
config2 = {
	A : 0,
	B : 1
};
writeOutput( structEquals( config1, config2 ) );

```

Result: NO

