#!/usr/bin/env python

# This simple script combines json data outputs from the experiments
# related to benchmarking

import pandas as pd
import json
import sys


def _get_caliper_time(bench):
    """
    Collect the processed time associated with a benchmark from caliper json files.
    The bench is assumed to be the node at the path root -> 'run' -> 'measurements' in the
    json output file
    :param benchmark_json:
    :return: array of times collected for a particular microbenchmark in caliper
    """
    return [iter['processed'] for iter in  bench['v']['measurementSetMap']['TIME']['measurements']]


def parse_json(filepath):
    """
    Create a dataframe from caliper json files
    :param filepath:
    :return: dataframe of caliper results
    """
    print "==> Parsing " + filepath
    with open(filepath) as fhandle:
        data = json.load(fhandle)

    # environment information
    env_info_keys = ['jre.version', 'jre.availableProcessors', 'os.name', 'os.version']
    env_info = { key : [data['environment']['propertyMap'][key]] for key in env_info_keys }
    env_df = pd.DataFrame(env_info)

    class_name = data['run']['benchmarkName']
    # now actually get the experiment timing/memory info
    measurements = []
    for bench in data['run']['measurements']:
        # benchmark name
        test_name = bench['k']['variables']['benchmark']
        # TODO: ADD MEMORY USAGE HERE AS WELL
        df = pd.DataFrame({ 'class_name' : class_name, 'test_name': test_name, 'execution_time' : _get_caliper_time(bench)})
        measurements.append(df)
    measurements_df = pd.concat(measurements, axis = 0, ignore_index = True)
    return pd.concat([measurements_df, env_df], axis = 1).fillna(method = 'ffill')


def main(caliper_result_files, output_path):
    """
    Combine results in separate files
    :param caliper_result_files: paths for json results
    :param output_path: path for combined path
    :return: void
    """
    data = []
    for file_name in caliper_result_files:
        try:
            data.append(parse_json(file_name))
        except Exception:
            print "Failed to parse: " + file_name
            pass
    total_data = pd.concat(data, ignore_index = True)
    total_data.to_csv(output_path, mode = 'w', index = False)


def help_message():
    print "Usage: python combine_results.py <comma-sep-json-file-names> <output>"


# parse args
if __name__ == "__main__":
    if len(sys.argv) != 3:
        help_message()
        sys.exit()
    else:
        caliper_result_files = sys.argv[1].split(",")
        output_path = sys.argv[2]
        main(caliper_result_files, output_path)
