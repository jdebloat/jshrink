#!/bin/bash

PWD=`pwd`
WORK_LIST="${PWD}/work_list.dat"
PROJECT_DIR="${PWD}/sample-projects"
DEBLOAT_APP="${PWD}/reachability-analysis-1.0-jar-with-dependencies.jar"
SIZE_FILE="${PWD}/size_data.csv"
METHOD_DATA_FILE="${PWD}/method_data.csv"
TEST_DATA_FILE="${PWD}/test_data.csv"
JAVA="/usr/lib/jvm/java-8-oracle/bin/java"
TEST_PROCESSOR="${PWD}/test-output-processor-1.0-jar-with-dependencies.jar"

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

echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,debloated,path,is_lib,size_in_bytes" >${SIZE_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,debloated,lib_methods,app_methods" >${METHOD_DATA_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,debloated,num_tests_passed,num_tests_failed,num_tests_skipped" >${TEST_DATA_FILE}

if [ ! -f "${DEBLOAT_APP}" ]; then
	echo "Setting up 'reachability-analysis' tool"
	mvn -f ../reachability-analysis/pom.xml clean compile assembly:single 2>&1 >/dev/null
	exit_status=$?
	if [[ ${exit_status} != 0 ]]; then
		echo "Cannot build 'reachability analysis' tool"
		exit 1
	fi
	cp "../reachability-analysis/target/reachability-analysis-1.0-jar-with-dependencies.jar" .
fi

if [ ! -f "${TEST_PROCESSOR}" ]; then
	echo "Setting up 'test-output-processor' tool"
	mvn -f ../test-output-processor/pom.xml clean compile assembly:single 2>&1 >/dev/null
	exit_status=$?
	if [[ ${exit_status} != 0 ]]; then
		echo "Cannot build 'test-output-processor' tool"
		exit 1
	fi
	cp "../test-output-processor/target/test-output-processor-1.0-jar-with-dependencies.jar" .
fi

cat ${WORK_LIST} |  while read item; do
	item_dir="${PROJECT_DIR}/${item}"
	cd "${item_dir}"

	echo "Processing : "${item}

	rm -r "${item_dir}/libs" 2>&1 >/dev/null
	mvn -f "${item_dir}/pom.xml" clean install -Dmaven.repo.local="${item_dir}/libs" --batch-mode -fn 2>&1 >/dev/null
	exit_status=$?
	if [[ ${exit_status} == 0 ]]; then
		echo "Compiled successfully"
		temp_file=$(mktemp /tmp/XXXX)
		test_output=$(mktemp /tmp/XXXX)

		mvn -f "${item_dir}/pom.xml" test -Dmaven.repo.local="${item_dir}/libs" --batch-mode -fn 2>&1 >${test_output}

		before_tests=$(${JAVA} -jar ${TEST_PROCESSOR} ${test_output})

		#2.5 hour timeout
		timeout 9000 ${JAVA} -Xmx20g -jar ${DEBLOAT_APP} --maven-project ${item_dir} --public-entry --main-entry --test-entry --prune-app --remove-methods --verbose 2>&1 >${temp_file} 
		exit_status=$?
		if [[ ${exit_status} == 0 ]]; then

			mvn -f "${item_dir}/pom.xml" test -Dmaven.repo.local="${item_dir}/libs" --batch-mode -fn 2>&1 >${test_output}

			after_tests=$(${JAVA} -jar ${TEST_PROCESSOR} ${test_output})

			#Record the test information
			echo ${item},1,0,0,,1,0,${before_tests} >>${TEST_DATA_FILE}
			echo ${item},1,0,0,,1,1,${after_tests} >>${TEST_DATA_FILE}
				
			#Get the number of methods/methods wiped for main class as an entry point
			lib_methods=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods"){print $2}')
			app_methods=$(cat ${temp_file} | awk -F, '($1=="number_app_methods"){print $2}')
			lib_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods_removed"){print $2}')
			app_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_app_methods_removed"){print $2}')
			echo ${item},1,0,0,,1,0,${lib_methods},${app_methods} >>${METHOD_DATA_FILE}
			echo ${item},1,0,0,,1,1,$(echo "${lib_methods} - ${lib_methods_removed}" | bc),$(echo "${app_methods} - ${app_methods_removed}" | bc) >>${METHOD_DATA_FILE}

			#Get all the app sizes
			cat ${temp_file} | awk -F, '($1 ~ "app_size_decompressed_before_*")' | while read entry; do
				key=$(echo ${entry} | cut -d, -f1)
				value=$(echo ${entry} | cut -d, -f2)
				app_path=$(echo ${key} | cut -d_ -f5-)
				echo ${item},1,0,0,,1,0,${app_path},0,${value} >>${SIZE_FILE}
			done

			cat ${temp_file} | awk -F, '($1 ~ "app_size_decompressed_after_*")' | while read entry; do
				key=$(echo ${entry} | cut -d, -f1)
				value=$(echo ${entry} | cut -d, -f2)
				app_path=$(echo ${key} | cut -d_ -f5-)
				echo ${item},1,0,0,,1,1,${app_path},0,${value} >>${SIZE_FILE}
			done

			#Get all the lib sizes
			cat ${temp_file} | awk -F, '($1 ~ "lib_size_decompressed_before_*")' | while read entry; do
				key=$(echo ${entry} | cut -d, -f1)
				value=$(echo ${entry} | cut -d, -f2)
				lib_path=$(echo ${key} | cut -d_ -f5-)
				echo ${item},1,0,0,,1,0,${lib_path},1,${value} >>${SIZE_FILE}
			done

			cat ${temp_file} | awk -F, '($1 ~ "lib_size_decompressed_after_*")' | while read entry; do
				key=$(echo ${entry} | cut -d, -f1)
				value=$(echo ${entry} | cut -d, -f2)
				lib_path=$(echo ${key} | cut -d_ -f5-)
				echo ${item},1,0,0,,1,1,${lib_path},1,${value} >>${SIZE_FILE}
			done	
		else
			echo "Could not properly process "${item}
			echo "Output the following: "
			cat ${temp_file}
		fi
		rm ${temp_file}
		rm ${test_output}
	else
		echo "Failed to compile"
	fi
done
