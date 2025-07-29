package com.sat.test.localDrivers

import com.sat.test.transformation.AddPartitions

object A0002_Run_AddPartitions extends App {
 println( "Hello World!" )
 val odate = "2025-07-12"
 val host_type = "local"
 val args1 = Array(odate,s"src/main/resources/dev/input_addPartitions.properties", host_type)
 AddPartitions.main(args1)
}
