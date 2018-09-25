package example

import com.sun.jna.Native
import org.graalvm.nativeimage.c.`type`.CTypeConversion
import org.jooq.SQLDialect
import org.jooq.conf.{ParamType, Settings}
import org.jooq.impl.DSL
import org.postgresql.core.Parser
import play.api.libs.json.Json

object Hello {

  def main(args: Array[String]): Unit = {
//    System.load(new java.io.File("hello-rs/target/debug/libhello.dylib").getAbsolutePath)
//    println(new java.io.File("hello-rs/target/debug/libhello.dylib").getAbsolutePath)
//    testJna()

    testGraalsCApi()
  }

  def testJna(): Unit = {
    val currentDir = System.getProperty("user.dir")
    System.setProperty("jna.debug_load.jna", "true")
    System.setProperty("jna.boot.library.path", s"$currentDir/jnalib/")
    System.setProperty("jna.debug_load", "true")
    System.setProperty("jna.library.path", s"$currentDir")
    val library = Native.loadLibrary("hello", classOf[RustInterfaceJna])
    library.printHello()
  }

  def testGraalsCApi(): Unit = {
    RustInterfaceGraal.printHello()

    val hello = CTypeConversion.toJavaString(RustInterfaceGraal.hello())
    println(s"hello returned: $hello")

    val formattedHello = CTypeConversion.toJavaString(RustInterfaceGraal.formatHello(CTypeConversion.toCString("Marcus").get()))
    println(s"formatHello returned: $formattedHello")

    testStructViaGraal()
    testJsonViaGraal()
    testSqlViaGraal()
    testTransaction()
    testTransactionRollback()
  }

  def testStructViaGraal(): Unit = {
    println("about to create a struct from Scala")
    val byValue = RustInterfaceGraal.newCounterByValue()
    require(byValue.isNull) // don't know why that is

    val struct = RustInterfaceGraal.newCounterByReference()
    println("created the struct. Now calling a method on it")
    println(s"count is: ${struct.getCount}")
    RustInterfaceGraal.increment(struct)
    println(s"count is: ${struct.getCount}")
    RustInterfaceGraal.increment(struct)
    println(s"count is: ${struct.getCount}")
  }

  def testJsonViaGraal(): Unit = {
    val json = Json.obj("message" -> "hello from Scala")
    println(s"passing a JSON string from Scala: ${json.toString}")
    val result = RustInterfaceGraal.processJson(CTypeConversion.toCString(json.toString()).get())
    println("got the following JSON from Rust")
    val jsonResult = Json.parse(CTypeConversion.toJavaString(result))
    println(jsonResult.toString())
  }

  def testSqlViaGraal(): Unit = {
    println("about to test sql")
    val cResult = RustInterfaceGraal.readFromDb(CTypeConversion.toCString("").get())
    val result  = CTypeConversion.toJavaString(cResult)
    println(s"sql result is: $result")

    println("trying with jooq")
    import org.jooq.impl.DSL.{field, name, table}
    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .select()
      .from(table("posts"))
      .where(field("id").in("?", "?"))

//    val rawSqlString = query.getSQL(ParamType.NAMED).replace(":", "$")
    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql
    println(s"raw sql string: $rawSqlString")
//    println(query.getSQL(ParamType.INDEXED))
//    println("-" * 50)
//    println(query.getSQL(ParamType.NAMED))
//    println("-" * 50)
//    println(query.getSQL(ParamType.NAMED_OR_INLINED))
//    println("-" * 50)
//    println(query.getSQL(ParamType.INLINED))
//    println("-" * 50)
    val paramsString = Json
      .arr(
        Json.obj("discriminator" -> "Int", "value" -> 1),
        Json.obj("discriminator" -> "Int", "value" -> 2)
      )
      .toString()
//    val cResult2 = RustInterfaceGraal.sqlQuery(
//      CTypeConversion.toCString(rawSqlString).get(),
//      CTypeConversion.toCString(paramsString).get()
//    )
//    val resultAsString = CTypeConversion.toJavaString(cResult2)
//    println(s"sql result is: $resultAsString")
//
//    val jsonResultSet = JsonResultSet.fromString(resultAsString).get
//    while (jsonResultSet.next()) {
//      println(s"body: ${jsonResultSet.getString("body")}")
//      println(s"id: ${jsonResultSet.getInt("id")}")
//      println(s"published: ${jsonResultSet.getBoolean("published")}")
//      println(s"title: ${jsonResultSet.getString("title")}")
//    }

    println("*" * 100)
    println("      Testing the CustomJdbcDriver !!!")
    // testing custom jdbc driver
    val driver     = CustomJdbcDriver()
    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setInt(0, 1)
    ps.setInt(1, 2)

    val rs = ps.executeQuery()
    while (rs.next()) {
      println(s"body: ${rs.getString("body")}")
      println(s"id: ${rs.getInt("id")}")
      println(s"published: ${rs.getBoolean("published")}")
      println(s"title: ${rs.getString("title")}")
    }
  }

  def testTransaction(): Unit = {
    println("Test transaction")
    import org.jooq.impl.DSL.{field, name, table}
    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .insertInto(table("posts"))
      .columns(field("title"), field("body"), field("published"))
      .values("?", "?", "?")

    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql

    val driver     = CustomJdbcDriver()
    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setString(0, "Test")
    ps.setString(1, "TestBody")
    ps.setBoolean(2, true)

    connection.setAutoCommit(false)
    ps.execute()
    connection.commit()
//    connection.close()
  }

  def testTransactionRollback(): Unit = {
    println("Test transaction rollback")
    import org.jooq.impl.DSL.{field, name, table}
    val sql = DSL.using(SQLDialect.POSTGRES, new Settings().withRenderFormatted(true))
    val query = sql
      .insertInto(table("posts"))
      .columns(field("title"), field("body"), field("published"))
      .values("?", "?", "?")

    val standardConformingStrings  = true
    val withParameters             = true
    val splitStatements            = true
    val isBatchedReWriteConfigured = false
    val rawSqlString =
      Parser.parseJdbcSql(query.getSQL(), standardConformingStrings, withParameters, splitStatements, isBatchedReWriteConfigured).get(0).nativeSql

    val driver     = CustomJdbcDriver()
    val connection = driver.connect("postgres://postgres:prisma@localhost/", null)
    val ps         = connection.prepareStatement(rawSqlString)
    ps.setString(0, "Test")
    ps.setString(1, "TestBody")
    ps.setBoolean(2, true)

    connection.setAutoCommit(false)
    ps.execute()
    connection.rollback()
//    connection.close()
  }
}
