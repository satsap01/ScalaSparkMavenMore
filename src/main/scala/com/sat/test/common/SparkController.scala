//------------->>>> fcalculation\common\SparkController.scala >>>>--------------
//******************************************************************************************/
//*** This common class, and used to create, start and stop Spark session ***************/
//*****************************************************************************************/
package com.sat.test.common

import com.sat.test.fields.Constants
import org.apache.spark.sql.SparkSession
object SparkController {

 var spark : SparkSession = _
 var testStarted : Boolean = false

 def start(): SparkSession = {
  //Starting the process requires establishing the Spark and Hive contexts
  spark = SparkSession
    .builder()
    .appName(Constants.app_name)
    .config("hive.exec.dynamic.partition", "true")
    .config("hive.exec.dynamic.partition.mode", "nonstrict")
    .enableHiveSupport()
    .getOrCreate()
  spark.sparkContext.setLogLevel("ERROR")

  println("Spark Session started")
  spark
 }

 def startLocalTest(): SparkSession = {
   println("we start Local Test")

  spark = SparkSession
   .builder()
   .appName(Constants.app_name)
   .master("local[1]")
   .config("spark.executor.memory", "2g")
   .config("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
   .config("hive.exec.scratchdir", "C:\\tmp\\hive")
   .config("hive.exec.dynamic.partition", "true")
   .config("hive.exec.dynamic.partition.mode", "nonstrict")
   .config("spark.hadoop.hadoop.native.lib", "false") // Ensure native lib is off
//    .config("spark.sql.warehouse.dir", "file:///E:/#2 Technical/Spark/Main/ScalaSparkMavenMore/1_Data/Resources/spark-warehouse")
    .enableHiveSupport()
   .getOrCreate()
   spark.sparkContext.setLogLevel("OFF")

   testStarted = true
   println("SPARK SESSION LOCAL TEST STARTED ---")
   spark
 }

 def stop(): Unit = {
 //Terminates the SparkSession
 spark.stop()
 }

 def stageName(name: String): Unit = {
 //This is used to display the name of the current functionality being used in the Spark UI
 spark.sparkContext.setLocalProperty("callSite.short", name)
 }

 def stageDescription(description: String): Unit = {
 //This is used to display information about the current process in the Spark UI
 spark.sparkContext.setLocalProperty("callSite.long", description)
 }
}
