#!/usr/bin/env bash

./gradlew :cli:jar > /dev/null

if [ $? -ne 0 ]; then
    exit $?
fi

VERSION=$(cat gradle.properties | grep "^version" | awk -F "=" '{print $2}')

java -jar cli/output/cli-$VERSION.jar $@

exit $?