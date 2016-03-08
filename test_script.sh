#!/bin/bash

SERVER=zoo.cs.yale.edu
PORT=80
FILENAME=filelist.txt
TIME=60

for PARALLEL in 1 2 3 4 5 10 15 20 30 40 50 60 70
do
  COMMAND="java Common.SHTTPTestClient -server $SERVER -servname $SERVER -port $PORT -parallel $PARALLEL -files $FILENAME -T $TIME >> result.txt"
  echo $COMMAND
  $COMMAND
  # command1
  # command2
  # commandN
done
