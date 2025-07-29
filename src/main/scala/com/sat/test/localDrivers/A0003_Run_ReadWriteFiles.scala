package com.sat.test.localDrivers

import com.sat.test.fields.Constants
import com.sat.test.transformation.ReadWriteFiles

object A0003_Run_ReadWriteFiles extends App {
 println( "Hello World!" )
 val odate = "2025-07-16"
 val host_type = Constants.LOCAL
 val args1 = Array(odate,s"src/main/resources/dev/input_readWriteFiles.properties", host_type)
 ReadWriteFiles.main(args1)
}
