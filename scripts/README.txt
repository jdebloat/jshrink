--- Setup the VM ---

To setup the experiment up, please ensure Vagrant and VirtualBox is 
installed

For Linux:

"sudo apt install vagrant virtualbox"

Setup and ssh into the VM using:

"vagrant up && vagrant ssh"

--- Download the sample projects ---

To download the sample projects (used in the experiments), run:

"./download.sh"

This will download all the projects, from GitHub, stated in 
"sample-maven-projects.csv" into a directory called "sample-projects".

--- Run experiments ---

Copy the desired experiment run from "experiment_scripts" to the root
directory, and execute.

The script will run JShrink on all projects stated in the "work_list.dat"
file, and output to "size_data.csv".

Once the experiment script has finished running, please run 
"reset_work_list.sh" to clean the projects in "sample-projects" (the 
projects must be "cleaned" before any other experiments are run again!).
