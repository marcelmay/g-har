package de.m3y.ghar

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

/**
 * Handles HAR (HTTP Archive).
 *
 * Supports
 * <ul>
 *   <li>parsing of JSON HAR content provided as input stream or file</li>
 *   <li>easily processing of HAR structure, including navigation helper for related pages and entries</li>
 *   <li>cloning for duplication</li>
 *   <li>appending several HARs into a single archive</li>
 * </ul>
 *
 * See http://www.softwareishard.com/blog/har-12-spec/
 */
class Har implements Cloneable {
  /** Parsed HAR log as root reference. */
  def log

  /**
   * Creates a Har instance by stream.
   *
   * @param ins the input stream providing JSON HAR content for parsing.
   * @return the newly created Har.
   */
  static Har open(InputStream ins) {
    new Har(log: new JsonSlurper().parse(ins).log)
  }

  /**
   * Creates a Har instance by file.
   *
   * @param file the input file providing JSON HAR content for parsing.
   * @return the newly created Har.
   */
  static Har open(File file) {
    file.withInputStream { open(it) }
  }

  /**
   * Appends the HAR pages and entries.
   *
   * Note: Does not handle page id/ref collisions!
   *
   * @param anotherHar the other har to integrate
   */
  void append(Har anotherHar) {
    pages().addAll(anotherHar.pages())
    entries().addAll(anotherHar.entries())
  }

  /**
   * Maps all page ids to a new page id, for all entries and pages.
   *
   * Useful when merging HARs, to avoid id collisions.
   *
   * @param closure a closure receiving the old page id as parameter and returning the new page id.
   *
   * Example, appending '_new' to all page ids:
   * <code>
   *   Har.open('foo.har').mapPageIds{ it + '_new' }
   * </code>
   */
  void mapPageIds(Closure<String> closure) {
    pages().each { page ->
      def oldId = page.id
      page.id = closure.call(oldId) // new page id
      entries(oldId).each { entry ->
        entry.pageref = page.id // new, updated page id
      }
    }
  }

  /**
   * Validates against http://www.softwareishard.com/blog/har-12-spec/ and throws assertions.
   *
   * The validation is a best effort.
   */
  void validate() {
    Validator.validate(this)
  }

  /**
   * Lists all HAR pages.
   *
   * @return list of pages, or an empty list.
   */
  List<Map> pages() {
    log.pages
  }

  /**
   * Lists all HAR entries.
   *
   * @return list of entries, or an empty list.
   */
  List<Map> entries() {
    log.entries
  }

  /**
   * Returns all entries for given page id.
   *
   * @param pageId the page id.
   * @return the entries found or an empty list.
   */
  List<Map> entries(String pageId) {
    entries().grep { it.pageref == pageId }
  }

  /**
   * Returns all page ids.
   *
   * Note: Looks only at page entries.
   *
   * @return the page ids or an empty list.
   */
  List<String> pageIds() {
    pages()*.id
  }

  /**
   * Converts to JSON presentation.
   *
   * @see #toPrettyJson()
   * @return the JSON presentation.
   */
  String toJson() {
    JsonOutput.toJson(['log': log])
  }

  /**
   * Converts to prettified JSON presentation.
   *
   * @see #toJson()
   * @return the prettified JSON presentation.
   */
  String toPrettyJson() {
    JsonOutput.prettyPrint(toJson())
  }

  @Override
  Har clone() throws CloneNotSupportedException {
    new ByteArrayInputStream(toJson().getBytes('UTF-8')).withStream {
      Har.open(it)
    }
  }

/**
 * Validates against http://www.softwareishard.com/blog/har-12-spec/ and throws assertions.
 *
 * The validation is a best effort.
 */
  static class Validator {
    static validate(Har har) {
      def log = har.log
      //    assert log.version is optional. If empty, 1.1 is assumed
      assert log.creator.name
      assert log.creator.version
      //    log.creator.version is optional
      assert log.browser.name
      assert log.browser.version
      //    log.browser.version is optional
      // pages is optional
      //    assert log.pages
      log.pages?.each { validatePage(it) }
      log.entries.each { entry ->
        //      assert entry.pageref is optional
        assert entry.startedDateTime
        assert entry.time
        assert entry.time
        assert entry.request.method
        assert entry.request.url
        assert entry.request.httpVersion
        validateEntryCookies(entry.request.cookies)
        validateEntryHeaders(entry.request.headers)
        assert entry.request.queryString.size() >= 0 // Array can be empty
        entry.request.queryString.each { query ->
          assert query.name
          assert query.value
          //        assert query.comment is optional
        }
        if (entry.request.method == 'POST') {
          validateEntryPostRequest(entry.request)
        }
        assert entry.request.headersSize >= -1
        assert entry.request.bodySize >= -1
        //      assert entry.request.comment is optional

        assert entry.response.status
        assert entry.response.statusText
        assert entry.response.httpVersion
        validateEntryCookies(entry.response.cookies)
        validateEntryHeaders(entry.response.headers)

        assert entry.response.content
        assert entry.response.content.size >= -1 // Can be 0 for eg HTTP 204
        //      assert entry.response.content.compression is optional
        assert entry.response.content.mimeType
        //      assert entry.response.content.text is optional
        //      assert entry.response.content.encoding is optional, since 1.2
        //      assert entry.response.content.comment is optional, since 1.2

        if (entry.response.status == '302') {
          assert entry.response.redirectURL
        }
        assert entry.response.headersSize
        assert entry.response.bodySize >= 0
        //      assert entry.response.comment is optional

        validateEntryCache(entry.cache)

        validateTimings(entry.timings, entry.request.url.toUpperCase().startsWith('HTTPS'), entry.time)

        //      assert entry.serverIPAddress is optional, since 1.2
        //      assert entry.connection is optional, since 1.2
        //      assert entry.comment is optional, since 1.2
      }
      //    assert log.comment is optional, since 1.2
    }

    static validateEntryPostRequest(request) {
      assert request.postData
      assert request.postData.mimeType
      assert request.postData.params.size() >= 0
      request.postData.params.each { param ->
        assert param.name
        //          assert param.value is optional
        //          assert param.fileName is optional
        //          assert param.contentType is optional
        //          assert param.comment is optional, since 1.2
      }
      assert request.postData.text
      //        assert request.postData.comment is optional, since 1.2
    }

    static validateEntryCache(cache) {
      if (cache) {
        //        assert entry.cache.beforeRequest is optional
        def cacheRequestValidator = { req ->
          //          assert req.expire is optionals
          assert req.lastAccess
          //          assert req.eTag is optional
          assert req.hitCount
          //          assert req.comment is optional

        }
        if (cache.beforeRequest) {
          cacheRequestValidator(cache.beforeRequest)
        }
        if (cache.afterRequest) {
          cacheRequestValidator(cache.afterRequest)
        }
        //        assert entry.cache.afterRequest is optional
        //        assert entry.cache.comment is optional, since 1.2
      }
    }

    static validateEntryCookies(cookies) {
      assert cookies.size() >= 0
      cookies.each { cookie ->
        assert cookie.name
        assert cookie.value
        //        assert cookie.path is optional
        //        assert cookie.domain is optional
        //        assert cookie.expires is optional
        //        assert cookie.httpOnly is optional
        //        assert cookie.secure is optional, since 1.2
        //        assert cookie.comment is optional, since 1.2
      }
    }

    static validateEntryHeaders(headers) {
      assert headers.size() >= 0
      headers.each { header ->
        assert header.name
        assert header.value
        //        assert header.comment is optional
      }
    }

    static validateTimings(timings, isSsl, totalTime) {
      assert timings.blocked >= -1 // number, optional
      assert timings.dns >= -1 // number, optional
      assert timings.connect >= -1 // number, optional
      assert timings.containsKey('send') && timings.send >= 0 // number
      assert timings.containsKey('wait') && timings.wait >= 0 // number
      assert timings.containsKey('receive') && timings.receive >= 0 // number
      if (isSsl) {
        assert timings.ssl >= -1 // number, optional
      }
      assert totalTime == timings.grep { it.value > 0 }.sum { it.value }
      //      assert entry.timings.comment is optional, since 1.2
    }

    static validatePage(page) {
      assert page.startedDateTime
      assert page.id
      assert page.title
      assert page.pageTimings
      //      assert page.pageTimings.onContentLoad is optional
      //      assert page.pageTimings.onLoad is optional
      //      assert page.pageTimings.comment is optional, since 1.2
      //      assert page.comment is optional
    }
  }
}