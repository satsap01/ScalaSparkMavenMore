package com.sat.test.transformation

import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(classOf[Suite])
@Suite.SuiteClasses(Array(
  classOf[CreateTablesTest],
  classOf[AddPartitionsTest],
  classOf[ReadWriteFilesTest]
))
class OrderedSuiteTest