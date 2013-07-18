#!/bin/bash
# 
# OWL Reasoner Evaluation Workshop (ORE) 2013
# Last updated: 18-Jul-13
# 
# base="/Volumes/Data/ORE"
base="/shareddata/homes/samantha"
ontbase="$base/ontologies"
rbase="$base/reasoners"
# 
# Using the script:
#
# sh startResultVerification <Operation> <Output>
#
# 	<Operation>		One of: sat | classification | consistency
#	<Output>		Output file
# 	<Profiles>		Profiles to be tested, any of: dl | el | rl (space separated)
# 
declare -a reasoners=(basevisor chainsaw elephant elk-loading-counted elk-loading-not-counted fact hermit jcel jfact konclude more-hermit more-pellet snorocket treasoner trowl wsclassifier)		
args=("$@") 
ELEMENTS=${#args[@]}		
if [ $ELEMENTS -gt 1 ]; then
	ext=""
	if [ "$1" = "classification" ]; then
		ext="owl"
	else
		ext="csv"
	fi	
	for (( i=0;i<$ELEMENTS;i++)); do 
		if [ $i -gt 1 ]; then
			profile=${args[${i}]}
			for file in $ontbase/functional/$profile/*; do
				filename=`basename $file`
				echo "Ontology: $filename"
				echo "Ontology filepath: $file"
				echo "Operation: $1"
				echo "Output file: $2"
				cmd=""
				for i in ${reasoners[@]}
				do
 				  cmd="$cmd $rbase/$i/output/$filename/$1.$ext"
				done
				java -Xmx8G -jar ResultChecker.jar $1 $2 $cmd
				printf '%*s\n' "${COLUMNS:-$(tput cols)}" '' | tr ' ' -
			done
		fi
	done
else
	echo "! Insufficient or no parameters given"
	echo ""
	echo "Usage:	sh startResultVerification <Operation> <Output>"
	echo "	<Operation>		One of: sat | classification | consistency"
	echo "	<Output>		Output file"
	echo ""
fi