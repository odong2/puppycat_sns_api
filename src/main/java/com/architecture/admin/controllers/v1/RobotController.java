package com.architecture.admin.controllers.v1;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@RequiredArgsConstructor
@RestController
public class RobotController {

    @ResponseBody
    @RequestMapping("/robots.txt")
    public void robotsBlock(HttpServletResponse response) throws IOException {

        response.getWriter().write("User-agent: *\nDisallow: /\n");
    }
}
