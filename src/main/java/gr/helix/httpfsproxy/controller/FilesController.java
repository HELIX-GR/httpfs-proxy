package gr.helix.httpfsproxy.controller;

import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping(path = "/files")
public class FilesController
{
    @Autowired
    @Qualifier("httpClient")
    CloseableHttpClient httpClient;
}
