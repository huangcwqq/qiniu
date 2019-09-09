package com.codo.qiniu.qiniudemospringboot.controller;

import com.codo.qiniu.qiniudemospringboot.service.QiniuService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class QiniuController {
  @Autowired
  private QiniuService qiniuService;

  @RequestMapping(value = "/testUpload", method = RequestMethod.POST)
  @ResponseBody
  public String uploadImage(@RequestParam("file") MultipartFile file, HttpServletRequest request) {

    if(file.isEmpty()) {
      return "error";
    }

    try {
      String fileUrl=qiniuService.saveImage(file);
      return "success, imageUrl = " + fileUrl;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return "fail";
  }

  @RequestMapping(value = "/getToken", method = RequestMethod.GET)
  @ResponseBody
  public Map<String, String> getUploadToken() {
    Map<String, String> data = new HashMap<>();
    // 将上传凭证放入map中
//    data.put("token",qiniuService.getUpToken());
//    data.put("token",qiniuService.callBackToken("测试"));
    data.put("token",qiniuService.getuplodTokenByKey("测试"));
    return data;
  }

  @RequestMapping(value = "/callBack", method = RequestMethod.POST)
  @ResponseBody
  public Map<String, String> callBack(HttpServletRequest request) {
    Map<String, String> data = new HashMap<>();
    System.out.println(request.getHeader("s"));
    System.out.println("回调成功");
    data.put("info","回调成功");
    return data;
  }
}
