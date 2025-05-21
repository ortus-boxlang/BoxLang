# Maven Dependencies

If your project relies on Java third-party dependencies, you can use the included Maven `pom.xml` file in the BoxLang home.  You can add your dependencies there and then run the `mvn install` command to download them into the `lib/` folder.  The BoxLang runtime will automatically class load all the jars in that folder for you!  You can also use the `mvn clean` command to remove all the jars.

You can find Java dependencies here: <https://central.sonatype.com/>.  Just grab the Maven coordinates and add them to your `pom.xml` file.
