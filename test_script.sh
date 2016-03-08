#!/bin/bash

SERVER=zoo.cs.yale.edu
PORT=80
FILENAME=filelist.txt
TIME=60

for PARALLEL in 80 90 100 110 120 130 140 150 160 170 180 190 200
do
  COMMAND="java Common.SHTTPTestClient -server $SERVER -servname $SERVER -port $PORT -parallel $PARALLEL -files $FILENAME -T $TIME >> result.txt"
  echo $COMMAND
  $COMMAND
  # command1
  # command2
  # commandN
done