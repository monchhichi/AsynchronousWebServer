#!/bin/bash

SERVER=zoo.cs.yale.edu
PORT=80
FILENAME=filelist.txt
TIME=60

for PARALLEL in 80 90 100 110 120 130
do
  COMMAND="java SHTTPTestClient -server $SERVER -servname $SERVER -port $PORT -parallel $PARALLEL -files $FILENAME -T $TIME"
  echo $COMMAND
  $COMMAND
  # command1
  # command2
  # commandN
done