#!/bin/bash

# copy this locally - only used for local preview.
if [ ! -f ./reports-v2.css ];
then
    wget -c 'https://www.unicode.org/reports/reports-v2.css'
fi
# copy this locally - only used for local preview.
if [ ! -f ./logo60s2.gif ];
then
    wget -c 'https://www.unicode.org/reports/logo60s2.gif'
fi

