#!/bin/bash

get_file_size(){
	if [[ $OSTYPE == darwin* ]]; then
		echo $(stat -f%z $1)
	elif [[ $OSTYPE == linux-gnu ]]; then
		return $(stat --printf="%s" $1)
	else
		>&2 echo "Unsupported OS Type: "$OSTYPE
		exit 1
	fi
}


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

echo "project,is_lib,jar,size" >${ORIGINAL_SIZE_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,is_lib,jar,size" >${DEBLOAT_SIZE_FILE}
echo "project,using_public_entry,using_main_entry,using_test_entry,custom_entry,is_app_prune,lib_methods,app_methods,lib_methods_removed,app_methods_removed" >${METHOD_DATA_FILE}

if [ ! -f "${DEBLOAT_APP}" ]; then
	mvn -f ../reachability-analysis/pom.xml clean compile assembly:single >/dev/null
	cp "../reachability-analysis/target/reachability-analysis-1.0-jar-with-dependencies.jar" .
fi

cat ${WORK_LIST} |  while read item; do
	item_dir="${PROJECT_DIR}/${item}"
	cd "${item_dir}"

	echo "Processing :"${item}

	mvn clean prepare-package dependency:copy-dependencies -DincludeScope=system -DoutputDirectory=target/lib  2>&1 >/dev/null 
	mvn jar:jar dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/lib 2>&1 >/dev/null

	lib_path=""
	#Get the size of the library jars
	while read lib; do
		echo ${item},1,${lib},$(get_file_size "target/lib/${lib}") >>${ORIGINAL_SIZE_FILE}
		if [[ ${lib_path} != "" ]]; then
			lib_path+=":"
		fi
		lib_path+="${item_dir}/target/lib/${lib}"
	done <<< $(ls target/lib)

	#Get the size of the app jars (don't know why they'd be more than one, but might as well support the possibility)
	ls target/*.jar | while read app; do
		echo ${item},0,$(basename "${app}"),$(get_file_size "${app}") >>${ORIGINAL_SIZE_FILE}
	done

	temp_file=$(mktemp /tmp/XXXX)

	${JAVA} -jar ${DEBLOAT_APP} --app-classpath target/classes --lib-classpath ${lib_path} --test-classpath target/test-classes --main-entry --prune-app >$temp_file 


	#Rebuild the jar package from the modified *.class files
	mvn jar:jar 2>&1 >/dev/null

	#Get the size of the library jars for main class as an entry point

	ls target/lib | while read lib; do
		echo ${item},0,1,0,,1,1,${lib},$(get_file_size "target/lib/${lib}") >>${DEBLOAT_SIZE_FILE}
	done

	#Get the size of the app jars for main class as an entry point
	ls target/*.jar | while read app; do
		echo ${item},0,1,0,,1,0,$(basename "${app}"),$(get_file_size "${app}") >>${DEBLOAT_SIZE_FILE}
	done

	#Get the number of methods/methods wiped for main class as an entry point
	lib_methods=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods"){print $2}')
	app_methods=$(cat ${temp_file} | awk -F, '($1=="number_app_methods"){print $2}')
	lib_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_lib_methods_removed"){print $2}')
	app_methods_removed=$(cat ${temp_file} | awk -F, '($1=="number_app_methods_removed"){print $2}')
	echo ${item},0,1,0,,1,${lib_methods},${app_methods},${lib_methods_removed},${app_methods_removed} >>${METHOD_DATA_FILE}

done
