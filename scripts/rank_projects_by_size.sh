#!/bin/bash

PWD=`pwd`
projects_dir="${PWD}/sample-projects"

cat work_list.dat | while read line; do
	echo ${line},$(du -s "${projects_dir}/${line}" | awk '{print $1}')
done | sort -n -t"," -k2 | awk -F, '{print $1}'
