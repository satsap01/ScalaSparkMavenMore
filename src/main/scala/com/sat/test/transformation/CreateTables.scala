package com.sat.test.transformation

import com.sat.test.common.SparkController
import com.sat.test.fields.vars._
import com.sat.test.transformation.AddPartitions.getTablePath
import org.apache.commons.io.FileUtils
import org.apache.spark.sql.{SaveMode, SparkSession}

import java.io.{File, FileInputStream}
import java.util.Properties

object CreateTables {
  def main(args: Array[String]): Unit = {
    val spark = getSparkSession(args)
    tablePath = getTablePath

    //################################################
    // Get the properties from the input file
    //################################################
    val addPartition_tablePath = tablePath + properties.getProperty("addPartition_tablePath")
    val dropDatabase_testdb_queryString = properties.getProperty("dropDatabase_testdb")
    val createDatabase_testdb_queryString = properties.getProperty("createDatabase_testdb")
    val dropTable_users_queryString = properties.getProperty("dropTable_users")
    val createTable_users_queryString = properties.getProperty("createTable_users").replaceAll("\\$tablePath",tablePath)
    val dropTable_updatedUsers_queryString = properties.getProperty("dropTable_updatedUsers")
    val createTable_updatedUsers_queryString = properties.getProperty("createTable_updatedUsers").replaceAll("\\$tablePath",tablePath)
    val showCreateTable_users_queryString=properties.getProperty("showCreateTable_users")
    val showCreateTable_updateUsers_queryString=properties.getProperty("showCreateTable_updatedUsers")
    val selectTable_users_queryString=properties.getProperty("selectTable_users")
    val selectTable_updatedUsers_queryString=properties.getProperty("selectTable_updatedUsers")

    //################################################
    // Drop existing database and physical data
    //################################################
    spark.sql(dropDatabase_testdb_queryString)

    val partitionPath = addPartition_tablePath
    val partitionDir = new File(partitionPath)
    println("partitionPath : " + partitionPath)
    println("partitionDir : " + partitionDir)

    if (partitionDir.exists()) {
      FileUtils.deleteDirectory(partitionDir) // Recursively deletes
      println(s"Deleted partition directory: $partitionPath")
    } else {
      println(s"Partition directory does not exist: $partitionPath")
    }

    //################################################
    //Create database and tables
    //################################################
    spark.sql(createDatabase_testdb_queryString)
    spark.sql(createTable_users_queryString)
    spark.sql(createTable_updatedUsers_queryString)

    spark.sql(showCreateTable_users_queryString).show(false)
    spark.sql(showCreateTable_updateUsers_queryString).show(false)

    spark.sql(selectTable_users_queryString).show()
    spark.sql(selectTable_updatedUsers_queryString).show()

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
    val fullTablePath = if (host_type == "local")
      properties.getProperty("local_table_Path") + properties.getProperty("cluster_table_Path")
    else
      "file://" + properties.getProperty("cluster_table_Path")
    println("tablePath : " + fullTablePath)
    fullTablePath
  }
}
