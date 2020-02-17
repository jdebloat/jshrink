#!/bin/bash

function run_script {
    curdir=$1
    experiment_script_dir=$2
    script=$3
    reset_script=$4
    worklist=$5

    echo "Running the experiment script ${script}"
    cp "${experiment_script_dir}/${script}" "${curdir}/${script}"
    ${curdir}/${script}
    rm "${curdir}/${script}"
    echo "Resetting the target projects"
    ${reset_script} ${worklist}
}

PWD="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
DOWNLOAD_SCRIPT="${PWD}/download.sh"
WORKLIST="${PWD}/work_list.dat"
EXPERIMENT_SCRIPTS_DIR="${PWD}/experiment_scripts"
RESET_SCRIPT="${PWD}/reset_work_list.sh"
JSHRINK_DIR="${PWD}/jshrink"
JSHRINK_JAR="${JSHRINK_DIR}/jshrink-app/target/jshrink-app-1.0-SNAPSHOT-jar-with-dependencies.jar"
JSHRINK_3_2_DEST="${PWD}/jshrink-app-1.0-SNAPSHOT-jar-with-dependencies_soot3_2.jar"
JSHRINK_3_3_DEST="${PWD}/jshrink-app-1.0-SNAPSHOT-jar-with-dependencies.jar"

echo "Building and copying the JShrink jars.."
mvn --file "${JSHRINK_DIR}/pom.xml" clean compile -pl jshrink-app -am

sleep 2
cp "${JSHRINK_JAR}" "${JSHRINK_3_3_DEST}"
mvn --file "${JSHRINK_DIR}/pom_soot-3.2.xml" clean compile -pl jshrink-app -am
sleep 2
cp "${JSHRINK_JAR}" "${JSHRINK_3_2_DEST}"

echo "Downloading the projects..."
${DOWNLOAD_SCRIPT} ${WORKLIST}

run_script "${PWD}" "${EXPERIMENT_SCRIPT_DIR}" "run_experiment_script_all_transformations_with_tamiflex_and_jmtrace_test_only.sh" "${RESET_SCRIPT}" "${WORKLIST}"
run_script "${PWD}" "${EXPERIMENT_SCRIPT_DIR}" "run_experiment_script_all_transformations_with_tamiflex_and_jmtrace_main_only.sh" "${RESET_SCRIPT}" "${WORKLIST}"
run_script "${PWD}" "${EXPERIMENT_SCRIPT_DIR}" "run_experiment_script_all_transformations_with_tamiflex_and_jmtrace_public_only.sh" "${RESET_SCRIPT}" "${WORKLIST}"
