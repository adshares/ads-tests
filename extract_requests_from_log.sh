#!/bin/bash

function show_help {
    echo "Extracts ads requests from debug test log."
    echo "  -i input file (debug test log with error)"
    echo "  -o output file (script to reproduce error)"
}

while getopts "h?i:o:" opt; do
    case "$opt" in
    h|\?)
        show_help
        exit 0
        ;;
    i)  in_file=$OPTARG
        ;;
    o)  out_file=$OPTARG
        ;;
    *)  show_help
        exit 0
    esac
done

# check, if input exist
if [ ! -f $in_file ]; then
    echo "Input file does not exists."
    show_help
    exit 1
fi

# check, if output exist
if [ -f $out_file ]; then
    echo "File $out_file will be overwritten."
    echo "Are You sure to overwrite? (y/n)"
    read answer
    if [ "$answer" != "${answer#[Yy]}" ] ;then
        rm $out_file
    else
        echo "File will not be overwritten."
        exit 0
    fi
fi

# line in which first error occured
error_line=$(grep -n -m 1 java.lang $in_file | cut -d : -f 1)

#check, if error found in log
number_regex='^[0-9]+$'
if ! [[ $error_line =~ $number_regex ]] ; then
   echo "Cannot find errors in log."
   exit 0
fi

# file content to first error
head -n $error_line $in_file > result1

# extract all requests
grep "FunctionCaller - request" result1 > result2

# cut unnecessary data, leave only date and call
sed -E 's/^(\S+).*request:\s+(.*)$/\1 \2/g' result2 > result3

time_last=0
while read -u 10 line; do
	# time of request
	mdate="$(cut -d ' ' -f 1 <<< $line)"
	read hour minute second millisecond <<< ${mdate//[:.]/ }

	# time converted to milliseconds
	time=$(( ((10#$hour*60+10#$minute)*60+10#$second)*1000+10#$millisecond ))

	# add delay, first line is skipped
	if [ $time_last -gt 0 ]; then
		time_diff=$(( ${time} - ${time_last} ))
		if [ $time_diff -gt 0 ]; then
            while [ ${#time_diff} -lt 3 ]; do
                time_diff="0${time_diff}"
            done
            echo "sleep" ${time_diff:0:-3}.${time_diff: -3} >> $out_file
		fi
	fi
	time_last=$time

	# add request
	mRequest="$(cut -d ' ' -f 2- <<< $line)"
	echo $mRequest >> $out_file
done 10<result3

rm result1 result2 result3
chmod u+x $out_file
echo "File $out_file created successfully."
