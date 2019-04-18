#!/bin/bash

input_file=$1
current_project=""
project_status=""

cat ${input_file} | while read line; do
	if [[ ${line} == *"Processing :"* ]]; then
		if [[ ${current_project} != "" ]]; then
			echo ${current_project},${project_status}
		fi
		current_project=$(echo ${line} | cut -d" " -f3)
		project_status=""
	elif [[ ${line} == "TIMEOUT!" ]]; then
		project_status="TIMEOUT"
	elif [[ ${line} == *"Output logging info to"* ]]; then
		project_status="DONE"
	elif [[ ${line} == *"Cannot build tests for the target application"* ]]; then
		project_status="ISSUE_#36"
	elif [[ ${line} == *"Build failed!"* ]]; then
		project_status="BUILD_FAILURE"
	elif [[ ${line} == *"Class names not equal"* ]]; then
		project_status="ISSUE_#29"
	elif [[ ${line} == *"Failed to convert"* ]]; then
		project_status="ISSUE_#22"
	elif [[ ${line} == *"No method"* ]]; then
		project_status="ISSUE_#34"
	elif [[ ${line} == *"was found in an archive"* ]]; then
		project_status="ISSUE_#30"
	fi
done

if [[ ${current_project} != "" ]]; then                 
	echo ${current_project},${project_status}       
fi
