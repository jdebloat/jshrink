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
		if [ -f "${project_dir}/${project}/dependency_warning.log" ]; then
			if ! grep -q 'BUILD SUCCESS' ${project_dir}/${project}/dependency_warning.log; then
				printf "Fails to analyze $line earlier. Redo it.\n"
				#mvn install -Dmaven.test.skip=true -f "${project_dir}/${project}/pom.xml" &> /dev/null
				#mvn dependency:resolve -f "${project_dir}/${project}/pom.xml" &> /dev/null
				mvn dependency:analyze -f "${project_dir}/${project}/pom.xml" &> ${project_dir}/${project}/dependency_warning.log
				#rm -r ~/.m2/repository
				printf "Finish resolving the dependency in $line\n\n"
			else 
				printf "$line has already been resolved.\n\n"
			fi
		else
			if grep -q 'BUILD SUCCESS' ${project_dir}/${project}/onr_build.log; then
				# only resolve dependencies of projects that build successfully and skip compiling and running test cases
				printf "Begin to resolve the dependency in $line\n"
				#mvn install -Dmaven.test.skip=true -f "${project_dir}/${project}/pom.xml" &> /dev/null
				#mvn dependency:resolve -f "${project_dir}/${project}/pom.xml" &> /dev/null
				mvn dependency:analyze -f "${project_dir}/${project}/pom.xml" &> ${project_dir}/${project}/dependency_warning.log
				#rm -r ~/.m2/repository
				printf "Finish resolving the dependency in $line\n\n"
			fi
		fi
	else
		printf "The project folder of $line does not exits.\n\n"
	fi
done < $project_list

