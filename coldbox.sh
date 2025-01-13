#!/bin/sh
cd ~/Sites/projects/boxlang
git pull --rebase --autostash
gradle build -x test -x javadoc

cd ~/Sites/projects/boxlang-web-support
git pull --rebase --autostash
gradle build -x test -x javadoc

cd ~/Sites/projects/boxlang-servlet
git pull --rebase --autostash
gradle buildRuntime
