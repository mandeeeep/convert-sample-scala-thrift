package scriptgen.thrift

import scriptgen.thrift.BaseConverter.TYPE_MAPS

import scala.io.Source

/**
  * Created by mandeep on 10/26/17.
  */
object Execute extends App{
  BaseConverter.init("examples/Blog/blog.txt")

//  val regexCommentRemove = "((['\"])(?:(?!\\2|\\\\).|\\\\.)*\\2)|\\/\\/[^\\n]*|\\/\\*(?:[^*]|\\*(?!\\/))*\\*\\/"
//  val x = Source.fromResource("examples/Blog/blog.txt").mkString
//  println(x.replaceAll(regexCommentRemove,""))


}
