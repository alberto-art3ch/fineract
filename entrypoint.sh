#!/bin/sh

echo .
date
echo .
java --version

SPRING_PROFILES_ACTIVE=basicauth

JVM_OPTS="-Djava.awt.headless=true -Duser.country=US -Duser.language=en"

echo .
echo .

java -Dloader.path=/app/libs/ -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE $JVM_OPTS -jar /app/fineract-provider.jar

