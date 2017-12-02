#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=f8cd1f18e2c8c91b07ec4322671cfb316281ba4f

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
