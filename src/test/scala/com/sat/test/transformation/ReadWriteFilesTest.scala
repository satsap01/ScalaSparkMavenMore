package com.sat.test.transformation

import com.sat.test.fields.{Constants, vars}
import com.sat.test.fields.vars.{input_properties, properties}
import org.scalatest.funsuite.AnyFunSuite
import org.junit.Test

import java.io.FileInputStream
import java.util.Properties

class ReadWriteFilesTest extends AnyFunSuite {
  def initFunctions() = {
    input_properties = "src/main/resources/dev/input_readWriteFiles.properties"
    properties = new Properties
    properties.load(new FileInputStream(input_properties))
  }

  @Test
  def getTablePath_value(): Unit = {
    initFunctions()
    vars.host_type = "local"
    var result = ReadWriteFiles.getTablePath
    println(">>>>>>>>>>>>>>>>>>>" + result)
    assert(result.equals("E:/7_spark_out/home/vagrant"))
    vars.host_type = "vm"

    result = ReadWriteFiles.getTablePath
    println(">>>>>>>>>>>>>>>>>>>" + result)
    assert(result.equals("file:///home/vagrant"))
  }
  @Test
  def getMyEnvPath_value(): Unit = {
    initFunctions()
  //  getSparkSession_value()
    vars.host_type = Constants.LOCAL
    var result  = ReadWriteFiles.getMyEnvPath("filePath_input")
    println(">>>>>>>>>>>>>>>>>>>" + result)
    assert(result.equals("/1_Data/Resources/csv/input.csv"))

    vars.host_type = Constants.AWS
    result  = ReadWriteFiles.getMyEnvPath("filePath_input")
    println(">>>>>>>>>>>>>>>>>>>" + result)
    assert(result.equals("s3://sat-ailmt-general/input.txt"))
  }
  @Test
  def getSparkSession_value(): Unit = {
    val odate = "2025-07-12"
    val host_type = "local"
    val args = Array(odate,s"src/main/resources/dev/input_readWriteFiles.properties", host_type)
    val spark = ReadWriteFiles.getSparkSession(args)
  }

  @Test
  def main_value(): Unit = {
    val odate = "2025-07-12"
    val host_type = "local"
    val args = Array(odate,s"src/main/resources/dev/input_readWriteFiles.properties", host_type)
    ReadWriteFiles.main(args)
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
