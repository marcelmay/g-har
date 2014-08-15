A HAR (HTTP Archive) Helper
---------------------------

g-har helps working with [HAR](http://www.softwareishard.com/blog/har-12-spec/) files.

Groovy example
--------------
```groovy
new File('src/test/resources/softwareishard.com.har').withInputStream{
   har = Har.open(it)

   // content references the parsed JSON directly
   assert har.log.pages.size() == 2 

   // Some helpers
   assert har.pageIds() == ['page_46155', 'page_26935']
   assert har.entry('page_46155').size() == 20

   // Print all URLs
   har.pageIds().each { pageId ->
       har.entry(pageId).each {
           println( it.request.url )
       }
   }

   // Manipulate title
   har.pages().each{ page ->
     page.title = page.id + ' ' + page.title
   }

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

```gradle clean install```


License
-------
[Apache License, Version 2.0](LICENSE)

HAR sample files are from the excellent [HAR Viewer](https://github.com/janodvarko/harviewer) project and are under [New BSD license](https://code.google.com/p/harviewer/).
