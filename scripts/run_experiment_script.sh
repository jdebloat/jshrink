#!/bin/bash

PWD=`pwd`
WORK_LIST="${PWD}/work_list.dat"
PROJECT_DIR="${PWD}/sample-projects"
DEBLOAT_APP="${PWD}/reachability-analysis-1.0-jar-with-dependencies.jar"
ORIGINAL_SIZE_FILE="${PWD}/original_size.csv"
DEBLOAT_SIZE_FILE="${PWD}/debloat_size.csv"
METHOD_DATA_FILE="${PWD}/method_data.csv"
JAVA="/Library/Java/JavaVirtualMachines/jdk1.8.0_181.jdk/Contents/Home/bin/java"

TEST_SCRIPT="mvn surefire:test"

if [ -f ${ORIGINAL_SIZE_FILE} ]; then
	>&2 echo "The original size file ("${ORIGINAL_SIZE_DIR}") already exists"
	echo 1
fi

if [ -f ${DEBLOAT_SIZE_FILE} ]; then
	>&2 echo "The debloat size file ("${DEBLOAT_SIZE_FILE}") already exists"
	echo 1
fi

if [ -f ${METHOD_DATA_FILE} ]; then
	>&2 echo "The method removal data file ("${METHOD_DATA_FILE}") already exists"
	echo 1
fi

echo "project,is_lib,jar,size_in_kB" >${ORIGINAL_SIZE_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,is_lib,jar,size_in_kB" >${DEBLOAT_SIZE_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,lib_methods,app_methods,lib_methods_removed,app_methods_removed" >${METHOD_DATA_FILE}

echo "Setting up 'reachability-analysis' tool"
if [ ! -f "${DEBLOAT_APP}" ]; then
	mvn -f ../reachability-analysis/pom.xml clean compile assembly:single >/dev/null
	exit_status=$?
	if [[ ${exit_status} != 0 ]]; then
		echo "Cannot build reachability analysis tool"
		exit 1
	fi
	cp "../reachability-analysis/target/reachability-analysis-1.0-jar-with-dependencies.jar" .
fi

cat ${WORK_LIST} |  while read item; do
	item_dir="${PROJECT_DIR}/${item}"
	cd "${item_dir}"

	echo "Processing : "${item}

	mvn clean prepare-package dependency:copy-dependencies -DincludeScope=system -DoutputDirectory=target/lib  2>&1 >/dev/null 
	exit_status=$?
	if [[ ${exit_status} == 0 ]]; then
		mvn jar:jar dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib 2>&1 >/dev/null

		lib_path=""
		#Get the size of the library jars
		if [ -d "target/lib" ]; then
			for lib in $(ls target/lib); do
		
				#Unpack the jars
				mv "target/lib/${lib}" .
				mkdir "target/lib/${lib}"
				unzip "${lib}" -d "target/lib/${lib}" 2>&1 >/dev/null
				rm "${lib}"		

				echo ${item},1,${lib},$(du -sk "target/lib/${lib}" | cut -f1) >>${ORIGINAL_SIZE_FILE}
				if [[ ${lib_path} != "" ]]; then
					lib_path+=":"
				fi
				lib_path+="${item_dir}/target/lib/${lib}"
			done
		fi

		#Get the size of the application
		echo ${item},0,APP,$(du -sk "target/classes" | cut -f1) >>${ORIGINAL_SIZE_FILE}

		temp_file=$(mktemp /tmp/XXXX)
	
		app_classpath=""
		if [ -d "target/classes" ];then
			app_classpath="--app-classpath target/classes"
		fi

		test_classpath=""
		if [ -d "target/test-classes" ];then
			test_classpath="--test-classpath target/test-classes"
		fi

		lib_classpath=""
		if [[ "${lib_path}" != "" ]];then
			lib_classpath="--lib-classpath ${lib_path}"
		fi

		${JAVA} -jar ${DEBLOAT_APP} ${app_classpath} ${lib_classpath} ${test_classpath} --public-entry --prune-app 2>&1 >$temp_file 
		exit_status=$?
		if [[ ${exit_status} == 0 ]];then

			#Get the size of the library sizes
			ls target/lib | while read lib; do
				echo ${item},0,1,0,,1,1,${lib},$(du -sk "target/lib/${lib}" | cut -f1) >>${DEBLOAT_SIZE_FILE}
			done

			#Get the application size
			echo ${item},0,1,0,,1,0,APP,$(du -sk "target/classes" | cut -f1) >>${DEBLOAT_SIZE_FILE}

			#Get the number of methods/methods wiped for main class as an entry point
			lib_methods=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods"){print $2}')
			app_methods=$(cat ${temp_file} | awk -F, '($1=="number_app_methods"){print $2}')
			lib_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods_removed"){print $2}')
			app_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_app_methods_removed"){print $2}')
			echo ${item},0,1,0,,1,${lib_methods},${app_methods},${lib_methods_removed},${app_methods_removed} >>${METHOD_DATA_FILE}
		else
			echo "Could not properly process "${item}
			echo "Output the following: "
			cat ${temp_file}
		fi
	else
		echo "Cannot properly compile/run tests for "${item}
	fi
done
