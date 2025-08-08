package com.sat.test.transformation

import com.sat.test.fields.vars
import com.sat.test.fields.vars.{input_properties, properties}
import org.junit.Test
import org.scalatest.funsuite.AnyFunSuite

import java.io.FileInputStream
import java.util.Properties

class AddPartitionsTest extends AnyFunSuite {
  val basePath = sys.env.getOrElse("BASE_PATH", "")
  def initFunctions() = {
    input_properties = "src/main/resources/dev/input_addPartitions.properties"
    properties = new Properties
    properties.load(new FileInputStream(input_properties))
    val osName = System.getProperty("os.name").toLowerCase
    vars.host_type = if (osName.contains("windows")) "local" else "vm"
  }

  @Test
  def getSparkSession_value(): Unit = {
    val odate = "2025-07-12"
//    val host_type = "local"
    val args = Array(odate,s"src/main/resources/dev/input_addPartitions.properties", vars.host_type)
    val spark = AddPartitions.getSparkSession(args)
  }

  @Test
  def getTablePath_value(): Unit = {
    initFunctions()
    if (vars.host_type == "local") {
      val result = AddPartitions.getTablePath
      println(">>>>>>>>>>>>>>>>>>>" + result)
      assert(result.equals("E:/7_spark_out/home/vagrant"))
    }
    else {
      val result = AddPartitions.getTablePath
      println(">>>>>>>>>>>>>>>>>>>" + result + "   :   <<<<<<<<<<<<<<<< " + basePath)
//      assert(result.equals("file:///home/vagrant"))
      assert(result.equals(s"file://${basePath}/home/vagrant"))
    }
  }

  @Test
  def main_value(): Unit = {
    val odate = "2025-07-12"
    initFunctions()
//    val host_type = "local"
    println("vars.host_type : " + vars.host_type + " :")
    val args = Array(odate,s"src/main/resources/dev/input_addPartitions.properties", vars.host_type)
    AddPartitions.main(args)
  }

  /*
  test("transformData should convert strings to uppercase") {
    val input = Seq("hello", "world").toDS()
    val expected = Seq("HELLO", "WORLD").toDS()

    val result = SparkApp.transformData(input)

    assert(result.collect().sameElements(expected.collect()))
  }

  test("getInputOutputPaths should return correct paths for local") {
    val props = new Properties()
    props.setProperty("input_Path_local", "/tmp/input")
    props.setProperty("output_Path_local", "/tmp/output")

    val (input, output) = SparkApp.getInputOutputPaths(props, "local")

    assert(input == "/tmp/input")
    assert(output == "/tmp/output")
  }

  test("getInputOutputPaths should return correct paths for s3") {
    val props = new Properties()
    props.setProperty("input_Path_s3", "s3://bucket/input")
    props.setProperty("output_Path_s3", "s3://bucket/output")

    val (input, output) = SparkApp.getInputOutputPaths(props, "s3")

    assert(input == "s3://bucket/input")
    assert(output == "s3://bucket/output")
  }
*/

}
