#!/bin/bash

. run_createTables.sh '2025-01-01'
. run_addPartitions.sh '2025-01-01'
. run_readWriteFiles.sh '2025-01-01'

echo "SCRIPT FINISH -------- `date`"
########################################################
return
exit $?

echo "SCRIPT EXIT CODE = ${STATUS}"
