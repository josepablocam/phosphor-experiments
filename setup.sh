#!/usr/bin/env bash

function install_maven_phosphor() {
  mvn install:install-file\
    -Dfile=Phosphor-0.0.2-SNAPSHOT.jar\
    -DgroupId=edu.columbia.cs.psl.phosphor\
    -DartifactId=Phosphor -Dversion=0.0.2-SNAPSHOT\
    -Dpackaging=jar
 }
 
 # Our caliper has been modified to avoid timeouts for longer running tests
function install_maven_caliper() {
  mvn install:install-file\
    -Dfile=caliper-0.5-rc1.jar\
    -DgroupId=com.google.caliper\
    -DartifactId=caliper\
    -Dversion=0.5-rc1\
    -Dpackaging=jar
  } 
 
if [ -z $JAVA_HOME ]
  then
    echo "Need to set JAVA_HOME"
    exit 1
fi

# Make sure we have a version of java that supports what we need in phosphor
java_version=$(java -version 2>&1 | awk '/version/ {print $3}')
if [[ ! $java_version == *1.8* ]]
  then
    echo "Building phosphor needs 1.8 (and caliper builds fine with that too)"
    exit 1
fi

# build phosphor locally and install to local maven repo
(cd phosphor; mvn package; cd Phosphor/target; install_maven_phosphor)

# build caliper locally and install to local maven repo
(cd caliper/caliper; mvn package; cd target; install_maven_caliper)
