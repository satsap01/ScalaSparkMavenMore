//------------->>>> fcalculation\common\PropertyController.scala >>>>--------------
//************************************************************************************************************/
//*** This class reads reads properties file and args() from spark-submit and set respective properties ****/
//************************************************************************************************************/
package com.sat.test.common

import java.io.FileInputStream
import java.util.Properties

object PropertyController
{
 //Global property controller as a common point of access for configs, properties and parameters

 var orderDate: String = ""
 var pathToTemplate = ""
 var emailList: Seq[String] = _
 var properties: Properties = new Properties
 var localTest: Boolean = false
// var PROJECT_VERSION = ""
// var insuranceDb = ""

 def setOrderDate(odate: String): Unit =
 {
 //The ODATE of the run, stored in a single place for easy access
 orderDate = odate
 }

 def loadProperties(path: String): Unit =
 {
 //Loads the environment config as supplied by JVM argument to the driver class
 properties.load(new FileInputStream(path))
// properties.list(System.out)
 }

 def setTemplatePath(path: String): Unit ={
 //set path to reporting template, this is being passed as arg(2) from spark-submit to available report file in cluster
 pathToTemplate = path
 }

 def setemailList(email: String): Unit = {
 // This method will set list of email we want to send email,
 emailList = email.split(",")
 }

 def setProperties(): Unit = {

 }

 def getProperties(key: String): String = {
 return properties.getProperty(key)
 }

 def printProperties(): Unit = {

 }

 def getFieldGroup(fields: Seq[String]): Seq[String] =
 {
 //Field groups are named by their property name, this decodes them back into their field names
 fields.map(f => properties getProperty f)
 }

 def Field(fieldName: String, group: Boolean = false): String =
 {
 println(s"$fieldName has been set to ${properties getProperty fieldName}")
 if(!group) properties getProperty fieldName else getFieldGroup(properties getProperty fieldName split ",") mkString ","
 }

}
