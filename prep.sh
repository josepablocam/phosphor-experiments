#!/usr/bin/env bash
# phosphor and caliper only seem to collaborate under java 1.7, everything else errors out
# which is a bit since phosphor needs be built with 1.8

function instrument_jre {
  java -Xmx6g -XX:MaxPermSize=512m -jar Phosphor-0.0.2-SNAPSHOT.jar -multiTaint -forceUnboxAcmpEq -withEnumsByValue $JAVA_HOME jre-inst-obj;
  chmod +x jre-inst-obj/bin/*;
  chmod +x jre-inst-obj/lib/*;
  chmod +x jre-inst-obj/jre/bin/*;
  chmod +x jre-inst-obj/jre/lib/*;
 }

java_version=$(java -version 2>&1 | awk '/version/ {print $3}')
if [[ ! $java_version == *1.7* ]]
  then
    echo "Phosphor and caliper only work joinly with java 1.7, switch versions"
    exit 1
fi


action=$1
phosphor_dir="phosphor/Phosphor/target"
inst_dir="target/pre_instr"

if [ -z $action ] || [ $action == "--jre" ]
  then
    echo "Creating obj tag instrumented JRE";
    # Taken from phosphor's instrumentJRE.sh
  (cd $phosphor_dir; instrument_jre)
fi

if [ -z $action ] || [ $action == "--package" ]
  then
    echo "Building project"
    mvn package

    echo "Instrumenting project jar"
    orig_jar=$(find target/ -depth 1 -iname "*SNAPSHOT.jar" | xargs -I {} readlink -f {})

    java -jar $phosphor_dir/Phosphor-0.0.2-SNAPSHOT.jar\
     -multiTaint -forceUnboxAcmpEq -withEnumsByValue $orig_jar $inst_dir
fi
