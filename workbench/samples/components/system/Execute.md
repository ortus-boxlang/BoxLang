### Script syntax

If you want to execute a script (.sh,.cmd,.bat), use bash (linux) or cmd.exe (windows) as the command and the script as argument for the shell interpreter.


```java
bx:execute name="bash" arguments="/opt/jq.sh #cmdArgs#" variable="standardOut" errorVariable="errorOut" timeout="10";

```


### Script syntax with terminateOnTimeout

Printing a PDF using lpr


```java
bx:execute name="lpr" arguments="-P 'My Print Job Name' 'C:/Users/devguy/Documents/server/mynewfile.pdf'" timeout="5" terminateOnTimeout="true";

```


