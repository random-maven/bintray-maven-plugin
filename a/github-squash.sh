#!/bin/bash

#
# squash commits after a point
#

set -e -u

point=098225e6d813cf7493f4e77938f4bd41db292594

git reset --soft $point

git add -A

git commit -m "develop"

git push --force
