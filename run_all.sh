# convenience script to run all experiments a certain number of times
# graph and save results
package="com.optimaltaint.experiments"
classes="no monotonic nonmonotonic"
versions="Naive Manual"

function full_name() {
  echo "$1.$2.$3"
}

function clean_workspace {
  if [ -d results/ ]
    then
      echo "Delete results/ (Y/N)?"
      read resp
      if [ $resp == "Y" ]
        then
          rm -rf results/
      fi
  fi
  mkdir -p results
}


clean_workspace
for class in $classes
do
  for version in $versions
  do
    program=$(full_name $package $class $version)
    for iter in {1..10}
    do
      echo "Running $program iteration:$iter"
     ./run_experiment.sh target/pre_instr/phosphor-experiments-1.0-SNAPSHOT.jar\
           $program\
         --results "results/$program.$iter.json"
    done
  done
done

# combine all of the result files into a csv
result_files=$(find results -type f  | awk -v ORS=, '{ print $1 }' | sed 's/,$//')
./combine_results.py $result_files results/combined.csv

# summarize quickly with R (just mean/std-dev etc) and save down figures
Rscript graph.R results/combined.csv results/