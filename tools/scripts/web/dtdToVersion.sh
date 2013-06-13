#!/bin/sh
grep 'ATTLIST version cldrVersion' | head -1 | cut -d' ' -f6 | tr -dc '[0-9].'
