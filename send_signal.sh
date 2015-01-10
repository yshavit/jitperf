#!/bin/bash

echo "$1" > __jpm_message.txt
shift
for arg in "${@}"; do
  echo "Rando${arg}" >> __jpm_message.txt
done

TARGET_PID=$(jps -lm|grep JitPerfMain|sed 's/ .*//')
if [ -z "$TARGET_PID" ]; then
  echo "JitPerfMain not running"
  exit 1
fi

for CLASS_ID in $(sed 1d __jpm_message.txt | sort | uniq); do
  if [ ! -e "Rando${arg}.class" ]; then
    echo "Compiling Rando${arg}"
    javac Rando${arg}.java
  fi
done

kill -s SIGUSR2 $TARGET_PID
