#!/usr/bin/env bash

# Commands
# Running Apache web server(httpd) -> sudo docker run -dit --name app1 -p 8080:80 httpd_final


# Modify these parameters before running
DIR=/var/www/html/
RootDir=/var/www/
ContainerHistoryNamePrefix=history_

# Main Loop
while :
do
    allContainerStats=$(docker stats --no-stream )
    allfile=allContainer
    echo "$allContainerStats" >> $RootDir$allfile
    echo "---------------------------------------- $date" >> $RootDir$allfile

    # loop through all containers
	for container in 'c916' '9005'
    do
            containerStats=$(echo "$allContainerStats" | grep $container)
            # Cpu
            cpu=$(echo $containerStats | awk '{print $3}')
            # Memory
            memory=$(echo $containerStats | awk '{print $4}')
            # Saving extracted data to files (current data file with ">" command & history available file with ">>" command)
            # Creating current log file
            echo  "Cpu $cpu" > $DIR$container
            echo "Memory $memory" >> $DIR$container
    done
done