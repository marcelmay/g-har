package de.m3y.ghar

/**
 * Tests Har.
 */
class HarTest extends GroovyTestCase {

  void testHar() {
    Thread.currentThread().contextClassLoader.getResource('softwareishard.com.har').withInputStream {
      def har = Har.open(it)
      assert har.log.pages.size() == 2
      assert har.log.pages.size() == har.pages().size()
      assert har.log.entries.size() == 40
      assert har.log.entries.size() == har.entries().size()
      assert har.pageIds() == ['page_46155', 'page_26935']
      assert har.entry('page_46155').size() == 20
      assert har.entry('page_26935').size() == 20
      har.pageIds().each { pageId ->
        har.entry(pageId).each {
          assert it.pageref == pageId
        }
      }

      def file = new File('build/foo.har')
      file.withOutputStream { it << har.toJson() }

      def har2 = Har.open(file)
      assert har2.pageIds() == har.pageIds()

      // Manipulate
      har.pages().each { page ->
        page.title = page.id + ' ' + page.title
      }
      har.pages().each { assert it.title.startsWith(it.id) }
    }
  }

  void testValidate() {
    ['browser-blocking-time.har', 'google.com.har', 'inline-scripts-block.har', 'softwareishard.com.har']
            .each { resourceName ->
      Thread.currentThread().contextClassLoader.getResource(resourceName).withInputStream {
        Har.open(it).validate()
      }
    }
  }

}
