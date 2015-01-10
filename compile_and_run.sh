#!/bin/bash
ls *.class &> /dev/null
if [ "$?" -eq 0 ]; then
  rm *.class
fi
set -e

javac -XDignore.symbol.file Randomizer.java AbstractRandomizer.java RandoOne.java JitPerfMain.java
java -XX:+PrintCompilation JitPerfMain
