#!/usr/bin/env bash
if [ $# -eq 0 ]; then
  tag='latest'
else
  tag=$1
fi

cd "$(dirname "$0")/.."
docker build -t lipid-server:"$tag" -f deploy/Dockerfile .
