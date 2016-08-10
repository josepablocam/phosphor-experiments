#!/usr/bin/env bash

## Run a caliper test, both instrumented or not

function help {
  echo "Usage: ./run_experiment.sh <jar> <class-name> [--results results-file] [--debug]"
  }

if [ $# -lt 2 ]
  then
    help
    exit 1
fi

# jar with class to run
prog_jar=$1
# class to run
name=$2

# Necessary Phosphor stuff
phosphor_dir="phosphor/Phosphor/target"
phosphor_jar=$(find $phosphor_dir -iname "Phosphor-[0-9]*SNAPSHOT.jar" | xargs -I {} readlink -f {})
instrumented_jre=$(readlink -f $phosphor_dir/jre-inst-obj/)

# Setup for caliper
export CLASSPATH=~/.m2/repository/com/google/caliper/caliper/0.5-rc1/caliper-0.5-rc1.jar:\
~/.m2/repository/com/google/guava/guava/14.0.1/guava-14.0.1.jar:\
~/.m2/repository/com/google/code/gson/gson/2.2.3/gson-2.2.3.jar:\
$phosphor_jar:\
$prog_jar

# consume optional args if we got any
if [ $# -gt 2 ]
  then
    shift; shift; # consume first 2 arg
    while [[ $# -ge 1 ]]
    do
      key="$1"

      case $key in
        --results)
        results="--saveResults $2"
        shift; shift # consume both flag and arg
        ;;
        --debug)
        debug="--debug"
        shift
        ;;
        --run)
        run=1
        shift
        ;;
        *)
            # unknown option
        ;;
    esac
  done
fi

 # set up necessary flags if instrumenting
echo "Setting up instrumented JRE"
export JAVA_HOME=$instrumented_jre
export JAVA_TOOL_OPTIONS="-Xbootclasspath/a:$phosphor_jar -javaagent:$phosphor_jar"

if [ -z $run ]
  then
    echo "Bencharking $name"
    java com.google.caliper.Runner $name $results $debug
else
    echo "Running $name"
    java $name
fi