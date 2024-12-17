#!/bin/sh
cd ~/Sites/projects/boxlang
gradle build -x test -x javadoc

cd ~/Sites/projects/boxlang-web-support
gradle build -x test -x javadoc

cd ~/Sites/projects/boxlang-servlet
gradle buildRuntime
