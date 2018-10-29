#!/bin/bash

POM_FILE="$1"

#Run the tests (bonus: this creates the target/test-classes directory)
test_output=$(mktemp /tmp/XXXX)
mvn -f "${POM_FILE}" surefire:test >${test_output}

temp_file=$(mktemp /tmp/XXXX)
cat ${test_output} | awk '{if($1 == "[ERROR]" || $1 == "[INFO]" \
	|| $1 == "[WARNING]"){$1=""; print $0} else {print $0}}' >${temp_file}
mv "${temp_file}" "${test_output}"


test_string=$(sed -n "$(($(awk '/\s*Results/{ print NR; exit }'\
   	"${test_output}") +1))"',$p' "${test_output}" |grep "Tests run")

tests_run=$(echo "${test_string}" | xargs | cut -d, -f1 | cut -d" " -f3)
tests_failed=$(echo "${test_string}" | xargs | cut -d, -f2 | cut -d" " -f3)
tests_skipped=$(echo "${test_string}" | xargs | cut -d, -f3 | cut -d" " -f3)

#tests_run,tests_failed,tests_skipped
echo ${tests_run},${tests_failed},${tests_skipped}
