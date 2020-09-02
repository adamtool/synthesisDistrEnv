#!/bin/bash
# @author Manuel Gieseking
IFS=',' read -r -a dep_folders <<< "$1"
IFS=',' read -r -a dep_repos <<< "$2"
IFS=',' read -r -a dep_rev <<< "$3"
PREFIX="dependencies"

if [ ! -d "$PREFIX" ]; then 
	echo "Creating the folder '$PREFIX'."
	mkdir $PREFIX
fi
cd $PREFIX

count=0
for dep in "${dep_folders[@]}"	# all dependencies
	do	
		echo "%%%%%%%%%%%%%%%% DEPENDENCY: $dep"
		if [ -d "$dep" ]; then 
            cd $dep
			echo "Start pulling the git repository ${dep_repos[$count]}"
            if [[ ${dep_rev[$count]} != 'HEAD' ]]; then
                git checkout master # if you don't want to checkout the HEAD revision, first go back  to master
            fi
			git pull                            # then pull,
            if [[ ${dep_rev[$count]} != 'HEAD' ]]; then
                git checkout ${dep_rev[$count]} # and then checkout the specific revision
            fi
            cd ..
		else 
			# The dependency is missing checkout the corresponding repo
			echo "The dependency '$PREFIX/$dep' does not exist."			
			echo "Start cloning the git repository ${dep_repos[$count]}"
			git clone ${dep_repos[$count]}
            if [[ ${dep_rev[$count]} != 'HEAD' ]]; then
                git checkout ${dep_rev[$count]}
            fi
		fi
	    count=$(($count+1));		
done

cd ..
