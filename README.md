# README - HttpFS Proxy

An authenticating proxy in front of an HttpFS/HDFS service

## Overview

The HttpFS-Proxy service is an authenticating proxy in front of an HttpFS/HDFS service. It offers a REST API for manipulating files on 
an HDFS filesystem, simplifying the underlying webhdfs API exposed from the HttpFS/HDFS service.

It acts as a gateway service (similarly to the what HttpFS does) i.e. it does not require that a client has network access to the Hadoop cluster.

The full documentation for the HTTP API (of the latest non-snapshot version) can be found at https://helix-gr.github.io/httpfs-proxy/

## Build

Build as a ordinary Maven project:

    mvn clean package

## Generate REST API documentation

Run integration tests which will generate snippets to be used for documentation purposes:
    
    mvn -P integration-tests verify

Build documentation:
    
    mvn asciidoctor:process-asciidoc@generate-docs

