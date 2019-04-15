#!/bin/bash

PWD=`pwd`
WORK_LIST="${PWD}/work_list.dat"
PROJECT_DIR="${PWD}/sample-projects"
DEBLOAT_APP="${PWD}/jshrink-app-1.0-SNAPSHOT-jar-with-dependencies.jar"
SIZE_FILE="${PWD}/size_data.csv"
JAVA="/usr/bin/java"
TAMIFLEX="${PWD}/poa-2.0.3.jar"
TIMEOUT=10800 #3 hours
OUTPUT_LOG_DIR="${PWD}/class_collapser_with_tamiflex_output_log"

if [ ! -f "${JAVA}" ]; then
	>&2 echo "Could not find Java 1.8 at the specified path: "${JAVA}
	>&2 echo "Please update the classpath in this script."
	exit 1
fi

if [ ! -f "${TAMIFLEX}" ]; then
	>&2 echo "Could not find TamiFlex at specified path: "${TAMIFLEX}
	exit 1
fi

if [ ! -f ${SIZE_FILE} ]; then
	echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,tamiflex,remove_methods,method_inliner,class_collapser,parameter_removal,app_size_before,libs_size_before,app_size_after,libs_size_after,app_num_methods_before,libs_num_methods_before,app_num_methods_after,libs_num_methods_after,tests_run_before,tests_errors_before,tests_failed_before,tests_skipped_before,tests_run_after,tests_errors_after,tests_failed_after,tests_skipped_after,time_elapsed" >${SIZE_FILE}
else
	2>&1 echo "WARNING: size file \""${SIZE_FILE}"\" already exists. Appending to this file"
fi

if [ ! -f "${DEBLOAT_APP}" ]; then
	echo "Cannot find "${DEBLOAT_APP}
	exit 1
fi

cat ${WORK_LIST} |  while read item; do
	item_dir="${PROJECT_DIR}/${item}"
	cd "${item_dir}"

	#OUTPUT_LOG_DIR
	ITEM_LOG_DIR="${OUTPUT_LOG_DIR}/${item}"

	echo "Processing : "${item}

	temp_file=$(mktemp /tmp/XXXX)

	#A 3 hour timeout
	timeout ${TIMEOUT} ${JAVA} -Xmx20g -jar ${DEBLOAT_APP} --tamiflex ${TAMIFLEX} --maven-project ${item_dir} -T --public-entry --main-entry --test-entry --prune-app --skip-method-removal --class-collapser --log-directory "${ITEM_LOG_DIR}" --verbose 2>&1 >${temp_file} 
	exit_status=$?
	if [[ ${exit_status} == 0 ]]; then
		cat ${temp_file}
		echo ""
		app_size_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="app_size_before"){print $2}')
		lib_size_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="libs_size_before"){print $2}')
		app_size_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="app_size_after"){print $2}')
		lib_size_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="libs_size_after"){print $2}')
		test_run_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_run_before"){print $2}')
		test_errors_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_errors_before"){print $2}')
		test_failures_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_failed_before"){print $2}')
		test_skipped_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_skipped_before"){print $2}')
		test_run_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_run_after"){print $2}')
		test_errors_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_errors_after"){print $2}')
		test_failures_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_failed_after"){print $2}')
		test_skipped_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="tests_skipped_after"){print $2}')
		app_num_methods_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="app_num_methods_before"){print $2}')
		lib_num_methods_before=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="libs_num_methods_before"){print $2}')
		app_num_methods_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="app_num_methods_after"){print $2}')
		lib_num_methods_after=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="libs_num_methods_after"){print $2}')
		time_elapsed=$(cat "${ITEM_LOG_DIR}/log.dat" | awk -F, '($1=="time_elapsed"){print $2}')		

		#The current settings
		using_public_entry="1"
		using_main_entry="1"
		using_test_entry="1"
		custom_entry=""
		is_app_prune="1"
		tamiflex="1"
		remove_methods="0"
		method_inliner="0"
		class_collapser="1"
		parameter_removal="0"

		echo ${item},${using_public_entry},${using_main_entry},${using_test_entry},${custom_entry},${is_app_prune},${tamiflex},${remove_methods},${method_inliner},${class_collapser},${parameter_removal},${app_size_before},${lib_size_before},${app_size_after},${lib_size_after},${app_num_methods_before},${lib_num_methods_before},${app_num_methods_after},${lib_num_methods_after},${test_run_before},${test_errors_before},${test_failures_before},${test_skipped_before},${test_run_after},${test_errors_after},${test_failures_after},${test_skipped_after},${time_elapsed} >>${SIZE_FILE}
	elif [[ ${exit_status} == 124 ]];then
		echo "TIMEOUT!"
		echo "Output the following: "                           
                cat ${temp_file}
		echo ""
		rm -rf ${ITEM_LOG_DIR}
	else
		echo "Could not properly process "${item}
		echo "Output the following: "
		cat ${temp_file}
		echo ""
		rm -rf ${ITEM_LOG_DIR}
	fi
	rm ${temp_file}
done