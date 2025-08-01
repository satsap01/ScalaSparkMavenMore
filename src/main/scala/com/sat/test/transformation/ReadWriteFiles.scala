package com.sat.test.transformation

import com.sat.test.common._
import com.sat.test.fields.Constants
import com.sat.test.fields.vars._
import org.apache.commons.io.FileUtils
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

import java.io.{File, FileInputStream}
import java.util.Properties

object ReadWriteFiles {
  def main(args: Array[String]): Unit = {
    val spark = getSparkSession(args)
    tablePath = getTablePath

    //##########################################
    //# get input from properties file
    //##########################################
    val inputFilePath = tablePath + getMyEnvPath("filePath_input")
    val outputFilePath = tablePath + getMyEnvPath("filePath_users")

    //##########################################
    //# Read input file
    //##########################################
    val dfFromCSV = spark.read.option("header", "true").schema("id INT, name STRING").csv(inputFilePath)
    dfFromCSV.show()

    //##########################################
    //# Filter data from input file
    //##########################################
    import spark.implicits._
    val filtered = dfFromCSV.filter("id > 3")
    val filteredStrings = filtered.map(row => s"${row.getInt(0)},${row.getString(1)}")
    filteredStrings.write.mode(SaveMode.Append).text(outputFilePath)

    //##########################################
    //# Read and display filtered data
    //##########################################
    spark.read.option("header", "true").schema("id INT, name STRING").csv(outputFilePath).show()

    spark.stop()
  }
  def getSparkSession(args: Array[String]): SparkSession = {
    properties = new Properties
    odate = args(0)
    input_properties = args(1)
    host_type = args(2)
    properties.load(new FileInputStream(input_properties))
    val spark = if (host_type == "local") SparkController.startLocalTest() else SparkController.start()
    spark
  }
  def getTablePath = {
    val basePath = sys.env.getOrElse("BASE_PATH", "")
    val fullTablePath = if (host_type == Constants.LOCAL)
      properties.getProperty("local_table_Path") + properties.getProperty("cluster_table_Path")
    else if(host_type == Constants.VM)
      "file://" + basePath + properties.getProperty("cluster_table_Path")
    else
      ""
    println("tablePath : " + fullTablePath)
    fullTablePath
  }
  def getMyEnvPath(myPath:String) = {
    if (host_type == Constants.AWS)  properties.getProperty(myPath + "_s3")
    else properties.getProperty(myPath)
  }
}
