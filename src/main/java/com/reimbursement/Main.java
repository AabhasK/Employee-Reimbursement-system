package com.reimbursement;

import java.io.File;

import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.WebResourceRoot;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.servlets.DefaultServlet;
import org.apache.catalina.startup.Tomcat;
import org.apache.catalina.webresources.DirResourceSet;
import org.apache.catalina.webresources.StandardRoot;
import org.apache.tomcat.util.scan.StandardJarScanner;
import org.apache.tomcat.util.http.Rfc6265CookieProcessor;

public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws LifecycleException {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(new File("target/tomcat").getAbsolutePath());
        tomcat.setPort(PORT);
        Connector connector = tomcat.getConnector();
        connector.setProperty("address", System.getenv().getOrDefault("APP_BIND", "127.0.0.1"));
        if (Boolean.parseBoolean(System.getenv("APP_HTTPS_ONLY"))) {
            connector.setScheme("https");
            connector.setSecure(true);
            connector.setProxyPort(443);
        }
        tomcat.setAddDefaultWebXmlToWebapp(false);

        Context context = tomcat.addWebapp("", new File("src/main/webapp").getAbsolutePath());
        Rfc6265CookieProcessor cookies = new Rfc6265CookieProcessor();
        cookies.setSameSiteCookies("lax");
        context.setCookieProcessor(cookies);

        // Only the default servlet for CSS and other static files. Pages come from Thymeleaf, not JSP.
        Tomcat.addDefaultMimeTypeMappings(context);
        Tomcat.addServlet(context, "default", new DefaultServlet());
        context.addServletMappingDecoded("/", "default");

        // Point Tomcat at our compiled classes so it finds the @WebServlet and @WebFilter classes
        WebResourceRoot resources = new StandardRoot(context);
        resources.addPreResources(new DirResourceSet(resources, "/WEB-INF/classes",
                new File("target/classes").getAbsolutePath(), "/"));
        context.setResources(resources);
        ((StandardJarScanner) context.getJarScanner()).setScanManifest(false);

        tomcat.start();
        System.out.println("Expense claim system running at http://localhost:" + PORT);
        tomcat.getServer().await();
    }
}
