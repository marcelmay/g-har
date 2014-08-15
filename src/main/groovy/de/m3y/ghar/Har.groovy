package de.m3y.ghar

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Handles HAR (HTTP Archive).
 *
 * See http://www.softwareishard.com/blog/har-12-spec/
 */
class Har {
  def log

  static Har open(InputStream ins) {
    new Har(log: new JsonSlurper().parse(ins).log)
  }

  static Har open(File file) {
    file.withInputStream { open(it) }
  }

  List<Map> pages() {
    log.pages
  }

  List<Map> entries() {
    log.entries
  }

  List<Map> entry(String pageId) {
    entries().grep{it.pageref == pageId}
  }

  List<String> pageIds() {
    pages()*.id
  }

  String toJson() {
    JsonOutput.toJson(['log':log])
  }

  String toPrettyJson() {
    JsonOutput.prettyPrint(toJson())
  }
}