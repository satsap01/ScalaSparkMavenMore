package com.sat.test.transformation

import com.sat.test.common._
import com.sat.test.fields.Constants
import com.sat.test.fields.vars._
import org.apache.spark.sql._

import java.io.FileInputStream
import java.util.Properties
import org.apache.spark.sql.functions._

import java.time.format.DateTimeFormatter
import java.io.File
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.SparkSession


object AddPartitions {
  def main(args: Array[String]): Unit = {
    val spark = getSparkSession(args)
    tablePath = getTablePath
    val addPartition_tablePath = tablePath
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

    if (host_type == Constants.AWS) deleteAWSFiles(partitionPath,spark) else deleteNonAWSFiles(partitionPath)

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
    val updatedDF = spark.table("testdb.users").withColumn("eid", col("eid") + total).withColumn("date_processed", lit(odate))
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
    val fullTablePath = if (host_type == Constants.LOCAL)
      properties.getProperty("local_table_Path") + properties.getProperty("cluster_table_Path") +  properties.getProperty("addPartition_tablePath")
    else if (host_type == Constants.VM)
      "file://" + basePath + properties.getProperty("cluster_table_Path") +  properties.getProperty("addPartition_tablePath")
    else
      properties.getProperty("addPartition_tablePath_s3")
    println("tablePath : " + fullTablePath)
    fullTablePath
  }

  def deleteNonAWSFiles(partitionPath:String) ={
    val partitionDir = new File(partitionPath)
    if (partitionDir.exists()) {
      FileUtils.deleteDirectory(partitionDir) // Recursively deletes
      println(s"Deleted partition directory: $partitionPath")
    } else {
      println(s"Partition directory does not exist: $partitionPath")
    }
  }

  def deleteAWSFiles(partitionPath:String,spark:SparkSession) ={
    import org.apache.hadoop.fs.{FileSystem, Path}

    val s3PartitionPath = new Path(partitionPath)
    val fs = s3PartitionPath.getFileSystem(spark.sparkContext.hadoopConfiguration)

    println(s"Checking partition path on FS: ${fs.getUri} - $partitionPath")

    if (fs.exists(s3PartitionPath)) {
      fs.delete(s3PartitionPath, true)
      println(s"Deleted S3 partition directory: $partitionPath")
    } else {
      println(s"S3 Partition directory does not exist: $partitionPath")
    }
  }
}
