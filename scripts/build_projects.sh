#!/bin/bash

PWD=`pwd`
projects_dir=${PWD}/sample-projects
cat work_list.dat | while read line; do
	cd "${projects_dir}/${line}"
	mvn install -Dmaven.repo.local=libs --quiet --batch-mode -DskipTests=true
done
