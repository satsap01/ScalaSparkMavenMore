#!/bin/bash

# Load Configurations
CURRENT_PATH="$( dirname "$(readlink -f -- "$0")" )"
PROJECT_NAME=${CURRENT_PATH##*/}
PROJECT_PROPERTIES_DIR=$(readlink -f ${CURRENT_PATH}/../../../AppConfigs/Transformation/${PROJECT_NAME})
. ${PROJECT_PROPERTIES_DIR}/config.properties

# Preprocess file with sed and save to temp file
temp_file=$(mktemp)
sed -e ':a' -e '/\\$/N; s/\\\n//; ta' "${PROJECT_PROPERTIES_DIR}/${LOCAL_INPUT_READ_WRITE_FILES}" > "$temp_file"

# Now read the temp file line-by-line in current shell
while IFS= read -r line; do
  [[ -z "$line" || "$line" =~ ^# ]] && continue

  if [[ "$line" == *"="* ]]; then
    key="${line%%=*}"
    value="${line#*=}"
    value="${value%\"}"
    value="${value#\"}"
    export "$key=$value"
  fi
done < "$temp_file"

rm "$temp_file"


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
  # Show input file from HDFS
  hdfs dfs -cat "file://${cluster_table_Path}${filePath_input}"
  echo "File from VM local"
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
--class ${SPARK_DRIVER_CLASS_READ_WRITE_FILES} \
--conf spark.sql.parquet.compression.codec=snappy \
--conf spark.memory.offHeap.enabled=true \
--conf spark.memory.offHeap.size=500M   \
--files ${LOCAL_APPCONFIG_DIR}/${LOCAL_INPUT_READ_WRITE_FILES} \
${LOCAL_JAR_FILE} \
${ODATE} \
${LOCAL_APPCONFIG_DIR}/${LOCAL_INPUT_READ_WRITE_FILES} \
${LOCAL_HOST_TYPE}

#######################################################
# HDFS output to display
#######################################################

if [ "${LOCAL_HOST_TYPE}" == "vm" ]; then
    OUTPUT_DIR="${cluster_table_Path}${filePath_users}"

    # Get the full path of the file that starts with part-0000
    OUTPUT_FILE=$(ls "$OUTPUT_DIR" |  grep 'part-0000' | head -n 1)
    echo "Output filename is : $OUTPUT_FILE"

    # Check and display the contents
    if [ -n "$OUTPUT_FILE" ]; then
      cat "${OUTPUT_DIR}/${OUTPUT_FILE}"
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
