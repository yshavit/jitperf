#!/bin/bash

TARGET_PID=$(jps -lm|grep JitPerfMain|sed 's/ .*//')
kill -s SIGUSR2 $TARGET_PID
