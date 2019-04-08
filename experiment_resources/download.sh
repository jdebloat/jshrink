#!/bin/bash

PWD=`pwd`

# the list of GitHub repositories built by maven
project_list="${PWD}/sample-maven-projects.csv"

# the destination path to the downloaded projects
dest_dir="${PWD}/sample-projects"

# check whether the dest folder exists first
if [ ! -d "$dest_dir" ]; then
	mkdir $dest_dir
fi

printf "***************Start downloading the given GitHub projects**********************\n"

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

	if [ ! -d "${dest_dir}/${project}" ]; then
		mkdir "${dest_dir}/${project}"
		printf "Beginning to clone $line\n"
		`git clone "https://github.com/${line}.git" "${dest_dir}/${project}" > /dev/null 2>&1` 
		printf "Successfully cloned $line!\n\n"
	else
		printf "$line already cloned. Skipp it.\n"
	fi

	#Checkout to a particular date (this keeps experiments constant across time)
	cd ${dest_dir}/${project}
    	git checkout `git rev-list -n 1 --before="2018-10-15 12:00" master` >/dev/null 2>&1
	cd ${PWD}

done < $project_list

