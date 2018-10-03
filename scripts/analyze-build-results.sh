#!/bin/bash

# the list of GitHub repositories built by maven
project_list="/media/troy/Disk2/ONR/BigQuery/sample-maven-projects.csv"

# the root directory that contains the Java projects downloaded from GitHub
project_dir="/media/troy/Disk2/ONR/BigQuery/sample-projects"

# check whether the project directory exists first
if [ ! -d "$project_dir" ]; then
	printf "The root directory of GitHub projects does not exist.\n"
	exit 1
fi

while read line
do
	#echo "$line"
	if [[ $line != *"/"* ]]; then
		# incorrect repo name
		continue
	fi
	line=`echo $line | awk -F'\r' '{print $1}'`
	username=`echo $line | awk -F'/' '{print $1}'`
	reponame=`echo $line | awk -F'/' '{print $2}'`
	#echo "Name=${username}; Repo=${reponame}"
	project="${username}_${reponame}"

	if [ -d "${project_dir}/${project}" ]; then
		if [ -f "${project_dir}/${project}/onr_build.log" ]; then
			if grep -q 'BUILD SUCCESS' ${project_dir}/${project}/onr_build.log; then
				printf "$line builds successfully\n\n"
			else 
				if grep -q -i 'BUILD FAILURE\|ERROR\|EXCEPTION' ${project_dir}/${project}/onr_build.log; then
					printf "$line fails to build.\n\n"
				else
					printf "$line build status unclear.\n\n"
				fi
			fi 
		else 
			printf "$line is not built yet.\n\n"
		fi
	else
		printf "The project folder of $line does not exits.\n\n"
	fi
done < $project_list
