package myproject.spring.schema

import java.io._
import collection.JavaConversions._
import com.github.fge.jsonschema.main.JsonSchema
import com.github.fge.jsonschema.main.JsonSchemaFactory
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation._
import java.net.URLDecoder._
import org.json4s.jackson.JsonMethods._
import org.json4s._
import com.fasterxml.jackson.databind.ObjectMapper
import org.json4s.JsonDSL._
import scala.io.Source
import util.control.Breaks._
import scala.collection.mutable.Map

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
        lines.foreach(line =>
          if(id == line){
            currentSchema = lines.next()
            break
          })
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
  // @RequestMapping(value = Array("/validate/{id}"), method = Array(RequestMethod.POST))
  // @ResponseStatus(HttpStatus.OK)
  // def validate(@PathVariable id: String, @RequestBody fileToValidate: String) : String = {
  //   val jsonToValidate = asJsonNode(compact(render(parse(decode(fileToValidate, "UTF-8")))))
  //
  //   try {
  //       serverAnswer = get("http://localhost:8080/schema/"+id)
  //
  //       val schema : JsonSchema  = asJsonNode(currentSchema)
  //       val validator = factory.getValidator
  //       val processingReport = validator.validate(currentSchema, jsonToValidate)
  //
  //       if (processingReport.isSuccess) {
  //         serverAnswer = compact(render(("action" -> "validateDocument") ~ ("id" -> id)
  //         ~ ("status" -> "SUCCESS")))
  //       } else
  //           serverAnswer = compact(render(("action" -> "validateDocument") ~ ("id" -> id)
  //           ~ ("status" -> "ERROR")))
  //
  //   } catch{
  //     case e: Exception =>
  //       serverAnswer = compact(render(("action" -> "validateDocument") ~ ("id" -> id)
  //       ~ ("status" -> "ERROR")))
  //   }
  //   serverAnswer
  // }

}
