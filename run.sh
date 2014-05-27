#!/bin/bash
if [ "$#" == "0" ]; then
	echo "Usage: $0 arg0 args1 ... argN"
	exit 1
fi
taskname=$1
shift
if [ "$#" == "0" ]; then
	./gradlew run
else
	./gradlew run -Pargs="$*" 
fi