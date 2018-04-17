package myproject.spring.schema

import java.io._
import java.net.URLDecoder._
import scala.io.Source
import util.control.Breaks._
import collection.JavaConversions._
import org.json4s.jackson.JsonMethods._
import org.json4s._
import org.json4s.JsonDSL._

import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.core.report.ProcessingMessage

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.scala.DefaultScalaModule

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation._


@RestController
class AppController {

  var serverAnswer : String = null
  var currentSchema : String = null
  var factory : JsonSchemaFactory = JsonSchemaFactory.byDefault()
  var mapper : ObjectMapper = new ObjectMapper()


  // Upload schema file
  @RequestMapping(value = Array("/schema/{id}"), method = Array(RequestMethod.POST))
  @ResponseStatus(HttpStatus.CREATED)
  @ResponseBody
  def upload(@RequestBody file: String, @PathVariable id : String): String  ={
    try {
      val source = compact(render(parse(decode(file, "UTF-8"))))

    // verify JSON schema format
      if(asJsonNode(source) != null){
    // the file with all the schemas will be stored in the user home directory
        val path = System.getProperty("user.home")

    // verify that schema with id given isn't in schemaLibrary.txt
        var idAlreadyUsed = false
        if(new java.io.File(path+"/schemaLibrary.txt").exists)
          breakable {
            val lines = Source.fromFile(path+"/schemaLibrary.txt").getLines
            lines.foreach(line =>
              if(id == line){
                idAlreadyUsed = true
                break
              })
          }

        if(idAlreadyUsed)
          serverAnswer = compact(render(("action" -> "DownloadSchema") ~ ("id" -> id)
          ~ ("status" -> "ERROR")  ~ ("message" -> "ID ALREADY USED")))
        else{
          val fileW = new PrintWriter(new BufferedWriter(new FileWriter(path+"/schemaLibrary.txt", true )))
          fileW.println(id)
          fileW.println(source)
          fileW.close
          serverAnswer = compact(render(("action" -> "uploadSchema") ~ ("id" -> id)
          ~ ("status" -> "SUCCESS")))
        }

      }

    } catch {
      case e: Exception => {
        val ret = ("action" -> "uploadSchema") ~ ("id" -> id) ~ ("status" -> "ERROR") ~ ("message" -> "INVALID JSON")
        println(compact(render(ret)))
        serverAnswer = compact(render(ret))
      }
    }
    serverAnswer
  }

  // Download the schema from the file
  @RequestMapping(value = Array("/schema/{id}"), method = Array(RequestMethod.GET))
  @ResponseStatus(HttpStatus.OK)
  def download(@PathVariable id : String) : String = {
    try {
      currentSchema = null
      val path = System.getProperty("user.home")
      val lines = Source.fromFile(path+"/schemaLibrary.txt").getLines

      // Get schema with id
      breakable {
        lines.foreach{line =>
          if(id == line){
            currentSchema = lines.next()
            break
          }
        }
      }

      if(currentSchema == null)
        serverAnswer = compact(render(("action" -> "DownloadSchema") ~ ("id" -> id)
        ~ ("status" -> "ERROR")  ~ ("message" -> "NO SCHEMA MATCH")))
      else
        serverAnswer = compact(render(("action" -> "DownloadSchema") ~ ("id" -> id)
        ~ ("status" -> "SUCCESS")))

    } catch{
      case e: Exception =>
        serverAnswer = compact(render(("action" -> "DownloadSchema") ~ ("id" -> id)
        ~ ("status" -> "ERROR")))
    }
    serverAnswer
  }

  // validate json
  @RequestMapping(value = Array("/validate/{id}"), method = Array(RequestMethod.POST))
  @ResponseStatus(HttpStatus.OK)
  def validate(@PathVariable id: String, @RequestBody fileToValidate: String) : String = {

    mapper.registerModule(DefaultScalaModule)
    mapper.configure(SerializationFeature.WRITE_NULL_MAP_VALUES, false)
    mapper.registerModule(DefaultScalaModule)


    try {

        serverAnswer = download(id)
        var jsonToValidate = parse(decode(fileToValidate, "UTF-8"))

      // clear file to validate : take off fields with null values
        var result = mapper.writeValueAsString(jsonToValidate.values)

        val jsonToValidateChecked : JsonNode = mapper.readTree(result)
        val nodeSchema : JsonNode = mapper.readTree(currentSchema)
        val schema : JsonSchema = factory.getJsonSchema(nodeSchema)
        val processingReport : ProcessingReport = schema.validate(jsonToValidateChecked)

        if (processingReport.isSuccess) {
          serverAnswer = compact(render(("action" -> "validateDocument") ~ ("id" -> id)
          ~ ("status" -> "SUCCESS")))
        } else{
          // get all errors from ProcessingReport value
          val report = processingReport.iterator()
          var message = "Message : "

          while(report.hasNext){
            var reportMessage : ProcessingMessage = report.next()
            message = message + reportMessage.asJson().get("message").asText() + ". ";
            }
          serverAnswer = compact(render(("action" -> "validateDocument") ~ ("id" -> id)
          ~ ("status" -> "ERROR") ~ ("message" -> message)))
        }

    } catch{
      case e: Exception =>
        serverAnswer = compact(render(("action" -> "validateDocument") ~ ("id" -> id)
        ~ ("status" -> "ERROR")))
    }
    currentSchema = null
    serverAnswer
  }

}
