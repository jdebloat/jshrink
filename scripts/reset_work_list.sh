#!/bin/bash

PWD=`pwd`
work_list="${PWD}/work_list.dat"
project_dir="${PWD}/sample-projects"

cat ${work_list} | while read project; do
	cd "${project_dir}/${project}"
	mvn clean
	git checkout -f .
done
