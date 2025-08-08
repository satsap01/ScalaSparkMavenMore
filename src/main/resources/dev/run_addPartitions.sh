#!/bin/bash

# Load Configurations
CURRENT_PATH="$( dirname "$(readlink -f -- "$0")" )"
PROJECT_NAME=${CURRENT_PATH##*/}
PROJECT_PROPERTIES_DIR=$(readlink -f ${CURRENT_PATH}/../../../AppConfigs/Transformation/${PROJECT_NAME})
. ${PROJECT_PROPERTIES_DIR}/config.properties
sed -e ':a' -e '/\\$/N; s/\\\n//; ta' "${PROJECT_PROPERTIES_DIR}/${LOCAL_INPUT_ADD_PARTITIONS_FILE}" | while IFS= read -r line; do
  # Skip empty lines or lines starting with #
  [[ -z "$line" || "$line" =~ ^# ]] && continue

  # Check if line contains '='
  if [[ "$line" == *"="* ]]; then
    key="${line%%=*}"          # part before first '='
    value="${line#*=}"         # part after first '='

    # Remove surrounding quotes from value
    value="${value%\"}"
    value="${value#\"}"

    # Export the key and value (with quotes to preserve spaces)
    export "$key=\"$value\""
  else
    echo "Skipping invalid line: $line" >&2
  fi
done

ODATE=${1}

echo "SCRIPT STARTS -------- `date`"
echo

if [ -z "$ODATE" ]
then
 echo "ODATE MUST BE POPULATED"
 exit 1
else
 echo "Running with date $ODATE"
fi
echo

set -x
# Run Spark application
spark-submit \
--verbose \
--master ${SPARK_MASTER} \
--deploy-mode cluster \
--name ${PROJECT_NAME} \
--num-executors ${SPARK_NUM_EXECUTORS} \
--executor-memory ${SPARK_EXECUTOR_MEMORY} \
--executor-cores ${SPARK_EXECUTOR_CORES} \
--driver-memory ${SPARK_DRIVER_MEMORY} \
--queue ${SPARK_QUEUE} \
--class ${SPARK_DRIVER_CLASS_ADD_PARTITIONS} \
--conf spark.sql.parquet.compression.codec=snappy \
--conf spark.memory.offHeap.enabled=true \
--conf spark.memory.offHeap.size=500M   \
--files ${LOCAL_APPCONFIG_DIR}/${LOCAL_INPUT_ADD_PARTITIONS_FILE} \
--conf spark.dynamicAllocation.enabled=false \
${LOCAL_JAR_FILE} \
${ODATE} \
${LOCAL_APPCONFIG_DIR}/${LOCAL_INPUT_ADD_PARTITIONS_FILE} \
${LOCAL_HOST_TYPE}

set +x

echo "SCRIPT FINISH -------- `date`"
########################################################
return
exit $?

echo "SCRIPT EXIT CODE = ${STATUS}"
