#!/bin/bash

PWD=`pwd`
WORK_LIST="${PWD}/work_list.dat"
PROJECT_DIR="${PWD}/sample-projects"
DEBLOAT_APP="${PWD}/reachability-analysis-1.0-jar-with-dependencies.jar"
SIZE_FILE="${PWD}/original_size.csv"
METHOD_DATA_FILE="${PWD}/method_data.csv"
TEST_DATA_FILE="${PWD}/test_data.csv"
JAVA="/usr/lib/jvm/java-8-oracle/bin/java"
TEST_SCRIPT="${PWD}/test_script.sh"

if [ ! -f "${JAVA}" ]; then
	>&2 echo "Could not find Java 1.8 at the specified path: "${JAVA}
	>&2 echo "Please update the classpath in this script."
	exit 1
fi

if [ -f ${SIZE_FILE} ]; then
	>&2 echo "The original size file ("${SIZE_FILE}") already exists"
	exit 1
fi

if [ -f ${METHOD_DATA_FILE} ]; then
	>&2 echo "The method removal data file ("${METHOD_DATA_FILE}") already exists"
	exit 1
fi

if [ -f ${TEST_DATA_FILE} ]; then
	>&2 echo "The test data file ("${TEST_DATA_FILE}") already exists"
	exit 1
fi

echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,app_run,path,is_lib,size_in_bytes" >${SIZE_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,app_run,lib_methods,app_methods" >${METHOD_DATA_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,app_run,num_tests_run,num_tests_failed,num_test_errors" >${TEST_DATA_FILE}

echo "Setting up 'reachability-analysis' tool"
if [ ! -f "${DEBLOAT_APP}" ]; then
	mvn -f ../reachability-analysis/pom.xml clean compile assembly:single 2>&1 >/dev/null
	exit_status=$?
	if [[ ${exit_status} != 0 ]]; then
		echo "Cannot build 'reachability analysis' tool"
		exit 1
	fi
	cp "../reachability-analysis/target/reachability-analysis-1.0-jar-with-dependencies.jar" .
fi

cat ${WORK_LIST} |  while read item; do
	item_dir="${PROJECT_DIR}/${item}"
	cd "${item_dir}"

	echo "Processing : "${item}

	mvn -f "${item_dir}/pom.xml" install -Dmaven.repo.local="${item_dir}/libs" --batch-mode -fn
	exit_status=$?
	if [[ ${exit_status} == 0 ]]; then
		echo "Compiled successfully"
		temp_file=$(mkdir /tmp/XXXX)
		${JAVA} -Xms10240m -jar ${DEBLOAT_APP} --maven-project ${item_dir} --public-entry --prune-app --remove-methods 2>&1 >$temp_file 
		exit_status=$?
		if [[ ${exit_status} == 0 ]]; then

			#TODO: Need to process more data from ${temp_file} to get more data
			#app/lib size before and after debloat should be obtainable from ${temp_file}
			#Testing is going to prove more difficult

			#Get the number of methods/methods wiped for main class as an entry point
			lib_methods=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods"){print $2}')
			app_methods=$(cat ${temp_file} | awk -F, '($1=="number_app_methods"){print $2}')
			lib_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods_removed"){print $2}')
			app_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_app_methods_removed"){print $2}')
			echo ${item},0,1,0,,1,${lib_methods},${app_methods},${lib_methods_removed},${app_methods_removed},,, >>${METHOD_DATA_FILE}
		else
			echo "Could not properly process "${item}
			echo "Output the following: "
			cat ${temp_file}
		fi
	else
		echo "Failed to compile"
	fi
done
