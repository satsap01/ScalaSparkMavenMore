package com.sat.test.transformation

import com.sat.test.common._
import com.sat.test.fields.vars._
import org.apache.spark.sql._

import java.io.FileInputStream
import java.util.Properties

import org.apache.spark.sql.functions._
import java.time.format.DateTimeFormatter
import java.io.File
import org.apache.commons.io.FileUtils

object AddPartitions {
  def main(args: Array[String]): Unit = {
    val spark = getSparkSession(args)
    tablePath = getTablePath
    val addPartition_tablePath = tablePath + properties.getProperty("addPartition_tablePath")
    val alterDropPartition_updatedUsers = properties.getProperty("alterDropPartition_updatedUsers").replaceAll("\\$odate",odate)
    val repairTable_updatedUsers = properties.getProperty("repairTable_updatedUsers")
    val showPartition_updatedUsers = properties.getProperty("showPartition_updatedUsers")
    val selectTable_updatedUsers = properties.getProperty("selectTable_updatedUsers").replaceAll("\\$odate",odate)

   //########################################################
    //Drop odated partition and its physical data
    //########################################################
    println(s"Processing data for date_processed = $odate")

//    val partitionPath = s"$addPartition_tablePath/date_processed=$odate"
    val strippedPartitionPath = addPartition_tablePath.stripPrefix("file://")
    val partitionPath = s"$strippedPartitionPath/date_processed=$odate"

    println(s"Raw partitionPath: '$partitionPath'")
    println("Partition path bytes: " + partitionPath.getBytes("UTF-8").mkString(" "))


    val partitionDir = new File(partitionPath)
    if (partitionDir.exists()) {
      FileUtils.deleteDirectory(partitionDir) // Recursively deletes
      println(s"Deleted partition directory: $partitionPath")
    } else {
      println(s"Partition directory does not exist: $partitionPath")
    }

    //########################################################
    //Prepare new partition data
    //########################################################
    val parts = odate.split("-")
    val year = parts(0).toInt    // 2025
    val month = parts(1).toInt   // 7
    val day = parts(2).toInt     // 27
    val total = year + month + day  // 2025 + 7 + 27 = 2059
    println(s"Total: $total")

    //########################################################
    //Add physical data and add new partition
    //########################################################
    val updatedDF = spark.table("testdb.users").withColumn("id", col("id") + total).withColumn("date_processed", lit(odate))
    updatedDF.show()
    updatedDF.coalesce(1).write.mode("append").option("delimiter", ",").format("csv").partitionBy("date_processed").save(s"$addPartition_tablePath/")
    spark.sql(repairTable_updatedUsers)

    //########################################################
    //Verify data
    //########################################################
    spark.sql(showPartition_updatedUsers).show(false)
    spark.sql(selectTable_updatedUsers).show()

    spark.stop()
  }
  def getSparkSession(args: Array[String]): SparkSession = {
    properties = new Properties
    odate = args(0)
    input_properties = args(1)
    host_type = args(2)
    properties.load(new FileInputStream(input_properties))
    println("host_type : " + host_type + " : ")
    val spark = if (host_type == "local") SparkController.startLocalTest() else SparkController.start()
    spark
  }
  def getTablePath = {
    val basePath = sys.env.getOrElse("BASE_PATH", "")
    val fullTablePath = if (host_type == "local")
      properties.getProperty("local_table_Path") + properties.getProperty("cluster_table_Path")
    else
      "file://" + basePath + properties.getProperty("cluster_table_Path")
    println("tablePath : " + fullTablePath)
    fullTablePath
  }
}
