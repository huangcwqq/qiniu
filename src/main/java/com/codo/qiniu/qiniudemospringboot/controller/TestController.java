package com.codo.qiniu.qiniudemospringboot.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class TestController {

  @RequestMapping("/test2")
  public String test2(Model m) {
    return "test2";
  }

  @RequestMapping("/test")
  public String test(Model m) {
    return "test";
  }
}
