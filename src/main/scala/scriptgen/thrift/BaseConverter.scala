package scriptgen.thrift

/**
  * Created by mandeep on 10/26/17.
  */
object BaseConverter {
  //READ

  val APPEND_SCHEMA_POSTFIX = "ThriftModel"

  val TYPE_MAPS: Map[String, (String, Boolean)] = Map("Option" -> ("optional", false),
    "String" -> ("string", false),
    "Seq" -> ("list", true),
    "List" -> ("list", true),
    "Map" -> ("map", true),
    "Set" -> ("set", true),
    "Int" -> ("i32", false),
    "Long" -> ("i64", false),
    "Boolean" -> ("bool", false),
    //"Byte" -> "byte",
    //"Byte[]" -> "binary",
    "Short" -> ("i16", false))


  def init(file: String) = {
    val output = fieldConversions(convert(read(file)))
    println(output)
  }

  def read(file: String): String = {
    import scala.io.Source
    // ((['"])(?:(?!\2|\\).|\\.)*\2)|\/\/[^\n]*|\/\*(?:[^*]|\*(?!\/))*\*\/ REMOVE MULTI AND SINGLE LINE COMMENTS
    // \/\*([\s\S]*?)\*\/ REMOVE ONLY MULTI LINE COMMENTS
    val regexCommentRemove = """\/\*([\s\S]*?)\*\/"""
    val x = Source.fromResource(file).mkString
    x.replaceAll(regexCommentRemove,"")
  }

  def convert(text: String): Seq[Seq[String]] = {
    //READ whole file, look for double line breaks
    val rx =
      """(?m)\s*^(?:([-+=#_])\1+|\s)+$\s*|(?:\r?\n\n)+"""
    val lines = text.split(rx)
    var grouped: Seq[Seq[String]] = Nil
    for (x <- lines) {
      var temp: Seq[String] = Nil
      if (x.contains("case class")) {
        val regexRemoveSingleComment = """((['"])(?:(?!\2|\\).|\\.)*\2)|\/\/[^\n]*|\/\*(?:[^*]|\*(?!\/))*\*\/"""
        val removedSingleComments = x.replaceAll(regexRemoveSingleComment,"")
        temp = temp ++ removedSingleComments.split(",").toSeq.filterNot(o => o.trim().contains("\n"))
        grouped = grouped :+ temp
        temp = Nil
      }
    }
    grouped
  }

  def fieldConversions(grouped: Seq[Seq[String]]): String = {
    var aClass: Seq[String] = Nil
    for (x <- grouped) {
      var counterY: Int = 0
      for (y <- x) {
        if (counterY == 0) {
          //ITS HEAD
          counterY = counterY + 1
          //CHECK if it's only label or has fields
          val (head: String, isOnlyHead: Boolean, fieldInHead: String) = checkHead(y)
          aClass = aClass :+ "struct " + head + APPEND_SCHEMA_POSTFIX + "{"
          if (isOnlyHead == false) {
            //Here if, HEAD HAS FIELD as well
            val fieldInfos = retrieveFieldInfo(fieldInHead.trim)
            aClass = aClass :+ counterY + ":" + fieldInfos._2 + " " + fieldInfos._1 + ","
            counterY = counterY + 1
          } else {
            ""
          }
        } else {
          val d = counterY
          if (counterY > x.length - 1) {
            val fieldInfos = retrieveFieldInfo(y)
            aClass = aClass :+ counterY + ":" + fieldInfos._2 + " " + fieldInfos._1 + "}"
          } else {
            val fieldInfos = retrieveFieldInfo(y)
            aClass = aClass :+ counterY + ":" + fieldInfos._2 + " " + fieldInfos._1 + ","
            counterY = counterY + 1
          }
        }
      }
    }
    (aClass.mkString("\n"))
  }

  def checkHead(str: String): (String, Boolean, String) = {
    val trimmedStr = str.trim
    val whereTheNameEnds = trimmedStr.indexOf("(")
    val theName = trimmedStr.substring(10, whereTheNameEnds).trim
    val head = trimmedStr.substring(whereTheNameEnds + 1, trimmedStr.length).trim
    if (head.length > 0) {
      //HEAD has classname as well as first field
      (theName, false, head)
    } else {
      //HEAD doesn't have first field
      (theName, true, "")
    }
  }

  def retrieveFieldInfo(str: String): (String, String) = {
    val splitFieldInfo = str.split(":")
    val fieldName = splitFieldInfo(0).trim
    val fieldTypeInfo: String = splitFieldInfo(1).trim
    val fieldTypeInfoWithoutDefaultValue: String = fieldTypeInfo.split("=")(0)
    //NEED TO RETHINK OPTIONAL TYPE PLACEMENTS, AND MORE COMPlEX TYPE INFERENCE
    var needToClose: Boolean = false
    var startCloser: Int = 0
    var convertedField: String = ""
    val allTypes: Seq[String] = fieldTypeInfoWithoutDefaultValue.trim.split("[^0-9A-Za-z\\,]").filterNot(str => str.equals(""))
    //println(allTypes.mkString("\n"))
    for (x <- allTypes) {
      //IF CUSTOM TYPE just leave as is
      if (!x.contains(",")) {
        var exec = {
          if (TYPE_MAPS.keySet.exists(_ == x)) {
            val xx = TYPE_MAPS(x)
            //KEY in MAP exists
            " " + xx._1 + {
              if (xx._2) {
                needToClose = true
                startCloser = startCloser + 1
                "<"
              }
              else {
                ""
              }
            }
          } else {
            " " + x
          }
        }
        convertedField = convertedField + exec
      } else {
        val mixedTypes = x.split(",")
        var withCommaFields = ""
        for (y <- mixedTypes) {
          withCommaFields = withCommaFields + {
            if (TYPE_MAPS.keySet.exists(_ == y)) {
              val xx = TYPE_MAPS(y)
              xx._1 + " "
            } else {
              x + " "
            }
          }
        }
        convertedField = convertedField + withCommaFields.trim().replaceAll(" ", ",")
      }
    }
    for (o <- 1 to startCloser) {
      convertedField = convertedField + ">"
    }
    (fieldName, convertedField)
  }
}
