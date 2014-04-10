#!/bin/sh
if [ "$#" == "0" ]; then
	echo "Usage: $0 task arg0 args1 ... argN"
	exit 1
fi
taskname=$1
shift
if [ "$#" == "0" ]; then
	echo ./gradlew -q $taskname
else
	echo ./gradlew -q $taskname -Pargs="$*"
fi