#!/usr/bin/env bash
if [ $# -eq 0 ]
  then
    tag='latest'
  else
    tag=$1
fi

cd ..
docker build -t lipid-server:$tag -f docker-build/Dockerfile .
cd docker-build
