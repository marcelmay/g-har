package de.m3y.ghar

/**
 * Tests Har.
 */
class HarTest extends GroovyTestCase {

  void testHar() {
    resourceByName('softwareishard.com.har').withInputStream {
      def har = Har.open(it)
      assert har.log.pages.size() == 2
      assert har.log.pages.size() == har.pages().size()
      assert har.log.entries.size() == 40
      assert har.log.entries.size() == har.entries().size()
      assert har.pageIds() == ['page_46155', 'page_26935']
      assert har.entries('page_46155').size() == 20
      assert har.entries('page_26935').size() == 20
      har.pageIds().each { pageId ->
        har.entries(pageId).each {
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
      resourceByName(resourceName).withInputStream {
        Har.open(it).validate()
      }
    }
  }

  void testAppend() {
    Har har1, har2

    resourceByName('softwareishard.com.har').withInputStream { har1 = Har.open(it) }
    resourceByName('softwareishard.com.har').withInputStream { har2 = Har.open(it) }

    har2.mapPageIds { it + '_new' }

    Har har3 = har1.clone()
    har3.append(har2)

    assert har3.pages().size() == har1.pages().size() + har2.pages().size()
    assert har3.entries().size() == har1.entries().size() + har2.entries().size()
    [har1, har2].each { har ->
      har.pages().each { page ->
        assert har3.pages().find { it.id == page.id }
        assert har3.entries(page.id) == har.entries(page.id)
      }
    }
  }

  private URL resourceByName(String resourceName) {
    Thread.currentThread().contextClassLoader.getResource(resourceName)
  }

}
