#!/bin/bash

#
# produce documentaion site
#

base=$(git rev-parse --show-toplevel)

cd "$base"

mvn clean package -B
