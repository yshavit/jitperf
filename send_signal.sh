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

set -e
for CLASS_NAME in $(sed 1d __jpm_message.txt | sort | uniq); do
  if [ ! -e "${CLASS_NAME}.class" ]; then
    echo "Compiling ${CLASS_NAME}"
    javac ${CLASS_NAME}.java
  fi
done

kill -s SIGUSR2 $TARGET_PID
