#!/bin/sh
if [ "$#" == "0" ]; then
	echo "Usage: $0 task arg0 args1 ... argN"
	exit 1
fi
task=$1
shift
if [ "$#" == "0" ]; then
	./gradlew -q $task
else
	./gradlew -q $task
fi