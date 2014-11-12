package com.whitepages

import sbt._
import Keys.dependencyClasspath
import Keys.Classpath
import scala.tools.nsc.io.File
import complete.DefaultParsers._
import complete.Parser

object VieView extends Plugin {

  private case class pinfo(path: String, prefix: String, ver: String, pack: String, id: String)

  private def parsePath(path: String) = {
    println(path)
    val i = path.replace("-SNAPSHOT","*SNAPSHOT").lastIndexOf("-")
    val j = path.replace("/bundles/", "/jars/").indexOf("/jars/")
    val prefix = path.substring(0, j)
    val parts = prefix.split("/")
    val pack = parts(parts.size - 2).replace(".scala", "").replace(".", "/")
    val ver = path.substring(i + 1).replace(".jar", "")
    val id = parts(parts.size - 1) + "-" + ver
    pinfo(path, prefix, ver, pack, id)
  }

  private def viewAct(args: Seq[String], v: Classpath) {
    val paths = v.files map (_.getPath)
    assert(args.size > 0)
    val selected = paths filter {
      path => args.forall(path.contains(_))
    } filter (_.endsWith(".jar")) map (parsePath(_))
    if (args.size > 0 && args(0) == "*") {
      println("self NYI")
    } else if (selected.size == 0) {
      println("Nothing selected")
    } else if (selected.size > 1) {
      println("Too many")
      for ((pp, i) <- selected.zipWithIndex) {
        println("[" + (i + 1) + "] " + pp)
      }
    } else {
      val select = selected(0)
      // TODO make sure doc jar exists else report not available error
      val f = File(select.prefix + "/docs/html-" + select.ver)
      if (!f.exists) {
        val in = select.prefix + "/docs/" + select.id + "-javadoc.jar"
        val out = select.prefix + "/docs/html-" + select.ver
        println("IN="+in)
        java.lang.Runtime.getRuntime.exec("mkdir " + out).waitFor()
        java.lang.Runtime.getRuntime.exec("tar -xf " + in + " -C " + out).waitFor()
      }
      java.lang.Runtime.getRuntime().exec("open " + select.prefix + "/docs/html-" +
        select.ver + "/" + "index.html").waitFor()
      val f1 = File(select.prefix + "/docs/html-" + select.ver + "/thrift")
      if (f1.exists) {
        java.lang.Runtime.getRuntime().exec("open " + select.prefix + "/docs/html-" +
          select.ver + "/thrift").waitFor()
      }
      val f2 = File(select.prefix + "/docs/html-" + select.ver + "/thriftdoc/index.html")
      if (f2.exists) {
        java.lang.Runtime.getRuntime().exec("open " + select.prefix + "/docs/html-" +
          select.ver + "/thriftdoc/" + "index.html").waitFor()
      }
      println("opening doc for " + select.path)
    }
  }

  val view = InputKey[Unit]("view", "view from doc jar")

  /*
  val view1 = view <<= inputTask {
    (argTask: TaskKey[Seq[String]]) =>
      (argTask, dependencyClasspath in Runtime) map {
        (args: Seq[String], v1: Classpath) => viewAct(args, v1)
      }
  }
  */

  // Define a parser that will parse user input into a sequence of strings.
  val argParser: Parser[Seq[String]] = spaceDelimited("<arg>")

  // A setting which will define your task.
  val view1 = view :=  {
    // Parses the input for this input task, and returns the value
    val args = argParser.parsed
    // Can directly access task values (although computed asynchronously)
    val cp = (dependencyClasspath in Runtime).value
    // Call the method you had before
    viewAct(args, cp)
  }

  lazy val viewSettings = Seq(view1)

}

