#!/bin/bash

kotlinLineCount=$(find . -type f -name *.kt | xargs wc -l | tail -1 | awk '{print $1}')
htmlLineCount=$(find src/main/resources/ -type f -name *.html | xargs wc -l | tail -1 | awk '{print $1}')

echo "There are $kotlinLineCount lines of kotlin, and $htmlLineCount of html currently."
