#!/bin/bash

PWD=`pwd`
WORK_LIST="${PWD}/work_list.dat"
PROJECT_DIR="${PWD}/sample-projects"
DEBLOAT_APP="${PWD}/jshrink-app-1.0-SNAPSHOT-jar-with-dependencies.jar"
SIZE_FILE="${PWD}/size_data.csv"
JAVA="/usr/lib/jvm/java-8-oracle/bin/java"
TAMIFLEX="${PWD}/poa-2.0.3.jar"

if [ ! -f "${JAVA}" ]; then
	>&2 echo "Could not find Java 1.8 at the specified path: "${JAVA}
	>&2 echo "Please update the classpath in this script."
	exit 1
fi

if [ -f ${SIZE_FILE} ]; then
	>&2 echo "The original size file ("${SIZE_FILE}") already exists"
	exit 1
fi

echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,app_size_before,libs_size_before,app_size_after,libs_size_after,app_num_methods_before,libs_num_methods_before,app_num_methods_after,libs_num_methods_after,tests_run_before,tests_errors_before,tests_failed_before,tests_skipped_before,tests_run_after,tests_errors_after,tests_failed_after,tests_skipped_after" >${SIZE_FILE}

if [ ! -f "${DEBLOAT_APP}" ]; then
	echo "Cannot find "${DEBLOAT_APP}
	exit 1
fi


cat ${WORK_LIST} |  while read item; do
	item_dir="${PROJECT_DIR}/${item}"
	cd "${item_dir}"

	echo "Processing : "${item}

	temp_file=$(mktemp /tmp/XXXX)

	#A 3 hour timeout
	timeout 10800 ${JAVA} -Xmx20g -jar ${DEBLOAT_APP} --tamiflex ${TAMIFLEX} --maven-project ${item_dir} -T --public-entry --main-entry --test-entry --prune-app --remove-methods --verbose 2>&1 >${temp_file} 
	exit_status=$?
	if [[ ${exit_status} == 0 ]]; then
		cat ${temp_file}
		echo ""
		app_size_before=$(cat ${temp_file} | awk -F, '($1=="app_size_before"){print $2}')
		lib_size_before=$(cat ${temp_file} | awk -F, '($1=="libs_size_before"){print $2}')
		app_size_after=$(cat ${temp_file} | awk -F, '($1=="app_size_after"){print $2}')
		lib_size_after=$(cat ${temp_file} | awk -F, '($1=="libs_size_after"){print $2}')
		test_run_before=$(cat ${temp_file} | awk -F, '($1=="tests_run_before"){print $2}')
		test_errors_before=$(cat ${temp_file} | awk -F, '($1=="tests_errors_before"){print $2}')
		test_failures_before=$(cat ${temp_file} | awk -F, '($1=="tests_failed_before"){print $2}')
		test_skipped_before=$(cat ${temp_file} | awk -F, '($1=="tests_skipped_before"){print $2}')
		test_run_after=$(cat ${temp_file} | awk -F, '($1=="tests_run_after"){print $2}')
		test_errors_after=$(cat ${temp_file} | awk -F, '($1=="tests_errors_after"){print $2}')
		test_failures_after=$(cat ${temp_file} | awk -F, '($1=="tests_failed_after"){print $2}')
		test_skipped_after=$(cat ${temp_file} | awk -F, '($1=="tests_skipped_after"){print $2}')
		app_num_methods_before=$(cat ${temp_file} | awk -F, '($1=="app_num_methods_before"){print $2}')
		lib_num_methods_before=$(cat ${temp_file} | awk -F, '($1=="libs_num_methods_before"){print $2}')
		app_num_methods_after=$(cat ${temp_file} | awk -F, '($1=="app_num_methods_after"){print $2}')
		lib_num_methods_after=$(cat ${temp_file} | awk -F, '($1=="libs_num_methods_after"){print $2}')
		echo ${item},1,1,1,,1,${app_size_before},${lib_size_before},${app_size_after},${lib_size_after},${app_num_methods_before},${lib_num_methods_before},${app_num_methods_after},${lib_num_methods_after},${test_run_before},${test_errors_before},${test_failures_before},${test_skipped_before},${test_run_after},${test_errors_after},${test_failures_after},${test_skipped_after} >>${SIZE_FILE}
	elif [[ ${exit_status} == 124 ]];then
		echo "TIMEOUT!"
	else
		echo "Could not properly process "${item}
		echo "Output the following: "
		cat ${temp_file}
		echo ""
	fi
	rm ${temp_file}
done
