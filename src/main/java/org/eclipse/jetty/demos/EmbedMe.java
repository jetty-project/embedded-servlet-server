//
//  ========================================================================
//  Copyright (c) Mort Bay Consulting Pty Ltd and others.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.demos;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbedMe
{
    private static final Logger LOG = LoggerFactory.getLogger(EmbedMe.class);

    public static void main(String[] args) throws Exception
    {
        int port = 8080;
        Server server = newServer(port);
        server.start();
        server.join();
    }

    public static Server newServer(int port) throws IOException
    {
        Server server = new Server(port);

        Resource baseResource = findBaseResource(EmbedMe.class.getClassLoader());
        LOG.info("Using BaseResource: {}", baseResource);
        WebAppContext context = new WebAppContext();
        context.setBaseResource(baseResource);
        context.setContextPath("/");
        context.setWelcomeFiles(new String[]{"index.html", "welcome.html"});
        context.setParentLoaderPriority(true);
        server.setHandler(context);
        return server;
    }

    private static Resource findBaseResource(ClassLoader classLoader)
    {
        String webResourceRef = "WEB-INF/html/index.html";

        try
        {
            // Look for resource in classpath (this is the best choice when working with a jar/war archive)
            URL webXml = classLoader.getResource('/' + webResourceRef);
            if (webXml != null)
            {
                URI uri = webXml.toURI().resolve("..").normalize();
                LOG.info("Found WebResourceBase (Using ClassLoader reference) {}", uri);
                return Resource.newResource(uri);
            }
        }
        catch (URISyntaxException | MalformedURLException e)
        {
            throw new RuntimeException("Bad ClassPath reference for: " + webResourceRef, e);
        }

        // Look for resource in common file system paths
        try
        {
            Path pwd = Path.of(System.getProperty("user.dir")).toAbsolutePath();
            Path targetDir = pwd.resolve("target");
            if (Files.isDirectory(targetDir))
            {
                try (Stream<Path> listing = Files.list(targetDir))
                {
                    Path embeddedServletServerDir = listing
                        .filter(Files::isDirectory)
                        .filter((path) -> path.getFileName().toString().startsWith("embedded-servlet-server-"))
                        .findFirst()
                        .orElse(null);
                    if (embeddedServletServerDir != null)
                    {
                        Path resourceFile = embeddedServletServerDir.resolve(webResourceRef);
                        if (Files.isRegularFile(resourceFile))
                        {
                            Path parentDir = resourceFile.getParent();
                            LOG.info("Found WebResourceBase (Using /target/ Path) {}", parentDir);
                            return Resource.newResource(parentDir);
                        }
                    }
                }
            }

            // Try the source path next
            Path srcWebapp = pwd.resolve("src/main/webapp/" + webResourceRef);
            if (Files.exists(srcWebapp))
            {
                Path parentDir = srcWebapp.getParent();
                LOG.info("WebResourceBase (Using /src/main/webapp/ Path) {}", parentDir);
                return Resource.newResource(parentDir);
            }
        }
        catch (Throwable t)
        {
            throw new RuntimeException("Unable to find web resource in file system: " + webResourceRef, t);
        }

        throw new RuntimeException("Unable to find web resource ref: " + webResourceRef);
    }
}
