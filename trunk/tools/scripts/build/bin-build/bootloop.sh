#!/bin/bash
. ${HOME}/bin-build/stbitten-env.sh
echo looping running bitten ${BUILDER_NAME}
while [[ 1 = 1 ]];
do
    runbitten.sh
    sleep 1100
done
