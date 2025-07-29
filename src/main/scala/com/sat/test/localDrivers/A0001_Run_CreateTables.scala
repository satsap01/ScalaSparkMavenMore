package com.sat.test.localDrivers

import com.sat.test.transformation.{CreateTables, AddPartitions}

object A0001_Run_CreateTables extends App {
 println( "Hello World!" )
 val odate = "2025-07-13"
 val host_type = "local"
 val args1 = Array(odate,s"src/main/resources/dev/input_createTables.properties", host_type)
 CreateTables.main(args1)
}
