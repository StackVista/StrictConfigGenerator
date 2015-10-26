package com.stackstate.configgenerator

import java.io.File
import com.stackstate.configgenerator.ConfigGenerator.GeneratedConfigFiles
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigException

import _root_.scalaz.Failure
import _root_.scalaz.Success
import scala.collection.JavaConversions._
import scalaz._
import scalaz.syntax.applicative._
import scalaz.syntax.validation._
import scalaz.Validation.FlatMap._

object ConfigGenerator extends App {
  type GeneratedConfigFiles = ValidationNel[String, List[File]]

  def printUsageAndQuit(error : String) = {
    println(
      s"""Error: $error
         |
         |The generator transforms template file(s) into configuration files using variable values from a variable file.
         |Every variable in the template file(s) must present in the variables file.
         |Every variable in the variables file must be referenced at least once.
         |
         |Usage: <template file or directory> <variables file> <output dir>
         |
         |1st param - The template file or directory containing template files.
         |            Variables to substitute shall be inserted in "##<variable-name>##" format.
         |            Example: sever.address = ##server##
         |2nd param - The variables file containing the variables used for generating config in the "key = value" format.
         |            Example: server = http://google.com
         |2nd param - The output directory to which the config(s) should be generated.
      """.
        stripMargin)
    System.exit(1)
  }

  println("The Strict Config Generator")

  if (args.length != 3) {
    printUsageAndQuit("Missing command line arguments.")
  }

  val templateFile = new File(args(0))
  val variablesFile = new File(args(1))
  val outputPath = new File(args(2))


  new ConfigGenerator().generateConfig(templateFile, variablesFile, outputPath) match {
    case Failure(e) =>
      println(s"Errors:\n${e.list.mkString("\n")}")
      System.exit(3)
    case Success(generatedFiles) =>
      println(s"Config generated: ${generatedFiles.map(_.getName).mkString(", ")}")
  }
}

class ConfigGenerator {
  val varRegex = """\##(.*?)##""".r
  val EOL = "\n"

  def getFileTree(f: File): List[File] =
    if (f.isDirectory) f.listFiles.toList
    else List(f)

  private def check(b : Boolean, failMsg : String) : ValidationNel[String, Unit] =
    (if (b) failMsg.failure else ().success).toValidationNel

  def generateConfig(templateFile: File, variablesFile: File, outputPath: File): GeneratedConfigFiles = {
    val result =
      (check(!templateFile.exists(), s"${templateFile.getAbsolutePath} does not exist.") |@|
      check(outputPath.isFile, s"${outputPath.getAbsolutePath} must be either a directory or not exist, but is a file.") |@|
      check(!variablesFile.isFile, s"${variablesFile.getAbsolutePath} does not exist or is not a file.")) { (_, _, _) =>
        List[File]()
      }
    for {
      _ <- result
      variables       <- parseVariables(variablesFile)
      allVariables     = variables.entrySet.map(_.getKey)
      allUsedVariables = getFileTree(templateFile).flatMap(getAllUsedVariables).toSet
      unDefinedVariables = allUsedVariables.filterNot(variables.hasPath)
      unUsedVariables     = allVariables.diff(allUsedVariables)
      _ <-
      (check(unDefinedVariables.nonEmpty, s"Variables: '${unDefinedVariables.mkString(",")}' are not defined!") |@|
        check(unUsedVariables.nonEmpty, s"Variables: '${unUsedVariables.mkString(",")}' are not used!")) { (_, _) =>
          List[File]()
        }
    } yield getFileTree(templateFile).map { configTemplate =>
      val configFile = new File(outputPath.getAbsolutePath + File.separatorChar + configTemplate.getName)
      generateConfigFile(variables, configTemplate, configFile)
      configFile
    }
  }

  def readTemplateFile(templateFile : File) = scala.io.Source.fromFile(templateFile, "utf-8").getLines()

  def generateConfigFile(variables : Config, configTemplate : File, configFile: File) : Unit = {
    configFile.getParentFile.mkdirs()
    if (!configFile.exists) configFile.createNewFile()
    scala.tools.nsc.io.File(configFile).writeAll(generateConfigFile(variables, configTemplate).mkString(EOL))
  }

  def generateConfigFile(variables : Config, configTemplate : File) : Iterator[String] = {
    readTemplateFile(configTemplate).map { line =>
      varRegex.replaceAllIn(line, m => variables.getString(m.group(1)))
    }
  }

  def getAllUsedVariables(templateFile : File) : Set[String] = {
    readTemplateFile(templateFile).foldLeft(Set[String]()) { (usedVariables, line) =>
      usedVariables ++ varRegex.findAllIn(line).matchData.map(_.group(1)).toSet
    }
  }

  def parseVariables(variablesFile: File) : ValidationNel[String, Config] = {
    try {
      ConfigFactory.parseFile(variablesFile).success
    } catch {
      case e : ConfigException => s"Error parsing $variablesFile: ${e.getMessage}".failureNel[Config]
    }
  }
}