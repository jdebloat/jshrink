#!/bin/bash

ROOT_DIR=$(pwd)

temp_file=$(mktemp /tmp/XXXX)

echo "project,app_sloc,test_sloc,compiled_test_size_bytes,dependency_size_compressed_bytes" >project_metrics.csv
ls sample-projects | while read project; do
	
	rm -rf sample-projects/${project}/libs 2>&1 >/dev/null
	mvn -f sample-projects/${project}/pom.xml install -Dmaven.repo.local=sample-projects/${project}/libs --quiet --batch-mode -DskipTests=true 2>&1 >/dev/null
	
	#Get the app SLOC
	echo 0 >${temp_file}
	find "sample-projects/${project}" -name "src" | while read src_file; do
		if [ -d "${src_file}/main" ]; then
			i=$(sloccount "${src_file}/main" 2>&1 | awk -F"=" '($1 ~ /Total Physical.*/){print $2}')
			if [[ "${i}" != "" ]]; then
				echo ${i//,/} >>${temp_file}
			fi
		fi
	done
	app_sloc=$(cat ${temp_file} | awk '{total+=$1}END{print total}')

	#Get the test SLOC
	echo 0 >${temp_file}
	find "sample-projects/${project}" -name "src" | while read test_file; do
		if [ -d "${test_file}/test" ]; then
			i=$(sloccount "${test_file}/test" 2>&1 | awk -F"=" '($1 ~ /Total Physical.*/){print $2}')
                	if [[ "${i}" != "" ]]; then
                        	echo ${i//,/} >>${temp_file}
                	fi
		fi
	done
	test_sloc=$(cat ${temp_file} | awk '{total+=$1}END{print total}')

	#Get the size of the compile test cases
	echo 0 >${temp_file}
	find "sample-projects/${project}" -name "target" | while read test_classes; do
		if [ -d "${test_classes}/test-classes" ];then
			echo $(du -s "${test_classes}") >>${temp_file}
		fi
	done
	compiled_test_size_bytes=$(cat ${temp_file} | awk '{total+=$1}END{print total}')

	#Get the size of the dependencies in their compressed form
	dependency_size_compressed_bytes=$(du -s "sample-projects/${project}/libs" | awk '{print $1}')

	echo ${project},${app_sloc},${test_sloc},${compiled_test_size_bytes},${dependency_size_compressed_bytes} >>project_metrics.csv
done
