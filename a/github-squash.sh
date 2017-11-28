#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=d5d4e9286f209b52378fa2cb34aa1fb2d784ad9d

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
