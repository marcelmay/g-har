A HAR (HTTP Archive) Helper
---------------------------

g-har helps working with [HAR](http://www.softwareishard.com/blog/har-12-spec/) files.

Supports

 *   parsing of JSON HAR content provided as input stream or file
 *   easily processing of HAR structure, including navigation helper for related pages and entries
 *   cloning for duplication
 *   appending several HARs into a single archive
 *   basic HAR validation support

Groovy example
--------------
```groovy
@Grab('de.m3y.ghar:g-har:1.0')
@GrabResolver(name='bintray', root='http://dl.bintray.com/marcel-may/maven/')

import de.m3y.ghar.Har

new File('src/test/resources/softwareishard.com.har').withInputStream{
   har = Har.open(it)

   // content references the parsed JSON directly
   assert har.log.pages.size() == 2 

   // Some helpers
   assert har.pageIds() == ['page_46155', 'page_26935']
   assert har.entries('page_46155').size() == 20
   har.validate() // Validations using assertions

   // Print all URLs
   har.pageIds().each { pageId ->
       har.entries(pageId).each {
           println( it.request.url )
       }
   }

   // Manipulate title
   har.pages().each{ page ->
     page.title = page.id + ' ' + page.title
   }
   
   // Clone and append
   Har har2 = har.clone()
   har2.mapPageIds{ it + '_new' } // page_001 => page_001_new
   har.append(har2)

   // Save as har
   new File('foo.har') << har.toJson()
   new File('bar.har') << har.toPrettyJson()
}
```

Building from source
--------------------

### Gradle

  The project requires Gradle for building from source. If you do not have Gradle installed yet, have a look at
  the [Gradle homepage](http://gradle.org).


### Compiling and creating JARs

To compile and create the JARs, run

```gradle clean test codenarcMain codenarcTest install```


License
-------
[Apache License, Version 2.0](LICENSE)

HAR sample files are from the excellent [HAR Viewer](https://github.com/janodvarko/harviewer) project and are under [New BSD license](https://code.google.com/p/harviewer/).
