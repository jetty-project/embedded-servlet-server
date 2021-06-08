Example: Embedded Jetty w/ Servlet 3.1 and Annotation
=====================================================

This is a maven project setup as a WAR packaging, with an EmbedMe class in the test scope that starts an embedded jetty of the
WAR file being produced by this project.

Quick Start
-----------

    $ mvn clean compile exec:exec

Open your web browser to

    http://localhost:8080/  for the root of the webapp


