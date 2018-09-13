package example

import com.sun.jna.Native
import org.graalvm.nativeimage.c.`type`.CTypeConversion

object Hello {

  def main(args: Array[String]): Unit = {
    testJna()

//    testGraalsCApi()
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

  def testJsonViaGraal(): Unit = {}
}
