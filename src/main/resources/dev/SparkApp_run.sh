#!/bin/bash

# Load Configurations
CURRENT_PATH="$( dirname "$(readlink -f -- "$0")" )"
PROJECT_NAME=${CURRENT_PATH##*/}
PROJECT_PROPERTIES_DIR=$(readlink -f ${CURRENT_PATH}/../../../AppConfigs/Transformation/${PROJECT_NAME})
. ${PROJECT_PROPERTIES_DIR}/config.properties
. ${PROJECT_PROPERTIES_DIR}/${LOCAL_INPUT_TEMP_FILE_NAME}


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

if [ "${LOCAL_HOST_TYPE}" == "vm" ]; then
  # Remove HDFS output files
  hdfs dfs -rm -r -f -skipTrash /user/wow/o*

  sleep 5

  # Show input file from HDFS
  hdfs dfs -cat "/user/wow/input.txt"
  echo "File from HDFS"
else
  # Remove files from S3 output bucket
  aws s3 rm s3://sat-ailmt-output/ --recursive

  # Show input file from S3
  aws s3 cp s3://sat-ailmt-general/input.txt - | cat
  echo "File from S3"
fi


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
--class ${SPARK_DRIVER_CLASS_TEMP_TRF_SIF} \
--conf spark.sql.parquet.compression.codec=snappy \
--conf spark.memory.offHeap.enabled=true \
--conf spark.memory.offHeap.size=500M   \
--files ${LOCAL_INPUT_TEMP_FILE_PATH} \
${LOCAL_JAR_FILE} \
${ODATE} \
${LOCAL_INPUT_TEMP_FILE_PATH} \
${LOCAL_HOST_TYPE}

#######################################################
# HDFS output to display
#######################################################

if [ "${LOCAL_HOST_TYPE}" == "vm" ]; then
    OUTPUT_DIR="/user/wow/output"

    # Get the full path of the file that starts with part-0000
    OUTPUT_FILE=$(hdfs dfs -ls "$OUTPUT_DIR" | awk '{print $8}' | grep 'part-0000')
    echo "Output filename is : $OUTPUT_FILE"

    # Check and display the contents
    if [ -n "$OUTPUT_FILE" ]; then
      hdfs dfs -cat "$OUTPUT_FILE"
    else
      echo "No output file found in $OUTPUT_DIR"
    fi
else
    OUTPUT_DIR="s3://sat-ailmt-output/"

    # Get the full path of the first part-0000* file in the S3 output dir
    OUTPUT_FILE=$(aws s3 ls "$OUTPUT_DIR" | awk '{print $4}' | grep 'part-0000')

    if [ -n "$OUTPUT_FILE" ]; then
      echo "Output filename is: $OUTPUT_FILE"
      aws s3 cp "${OUTPUT_DIR}${OUTPUT_FILE}" - | cat
    else
      echo "No output file found in $OUTPUT_DIR"
    fi
fi



echo "SCRIPT FINISH -------- `date`"
########################################################
return
exit $?

echo "SCRIPT EXIT CODE = ${STATUS}"
