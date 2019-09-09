package com.codo.qiniu.qiniudemospringboot.service;

import com.alibaba.fastjson.JSONObject;
import com.codo.qiniu.qiniudemospringboot.util.FileUtil;
import com.qiniu.common.QiniuException;
import com.qiniu.common.Zone;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class QiniuService {
  private static final Logger logger = LoggerFactory.getLogger(QiniuService.class);

  // 设置好账号的ACCESS_KEY和SECRET_KEY
  private static String ACCESS_KEY = "###################";
  private static String SECRET_KEY = "###################";
  // 要上传的空间
  private static String bucketname = "####";

  // private Bucket
  private static String privateBucket = "privateBucket";

  // 回调地址
  private static String callbackURL = "callBackURL";

  //publicIMGURL
  private static String publicIMGURL = "IMGURL";

  // priviteIMGURL
  private static String priviteIMGURL = "priviteIMGURL";

  // 密钥配置
  Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);
  // 构造一个带指定Zone对象的配置类,不同的七云牛存储区域调用不同的zone
  Configuration cfg = new Configuration(Zone.zone0());
  // ...其他参数参考类注释
  UploadManager uploadManager = new UploadManager(cfg);

  // 静态加载配置文件中的数据
  static {
    try {
      FileInputStream in = new FileInputStream(
          "E:\\Qiniu\\qiniudemo-springboot\\src\\main\\resources\\application.properties");
      Properties properties = new Properties();
      properties.load(in);
      ACCESS_KEY = properties.getProperty("AccessKey");
      SECRET_KEY = properties.getProperty("SecretKey");
      bucketname = properties.getProperty("publicBucket");
      privateBucket = properties.getProperty("privateBucket");
      callbackURL = properties.getProperty("CallBackURL");
      publicIMGURL = properties.getProperty("publicIMGURL");
      priviteIMGURL = properties.getProperty("priviteIMGURL");
      in.close();
    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // 简单上传，使用默认策略，只需要设置上传的空间名就可以了
  public String getUpToken() {
    return auth.uploadToken(bucketname);
  }

  public String callBackToken(String key){
    // 使用StringMap工具类拼装参数
    StringMap putPolicy = new StringMap();

    putPolicy.put("scope", bucketname);


    putPolicy.put("deadline", (System.currentTimeMillis() / 1000L + 3600));

    // type : String
    putPolicy.put("returnBody",
            "{\"key\": $(key), \"hash\": $(etag), \"w\": $(imageInfo.width), \"h\": $(imageInfo.height)}");

    // type : String
    putPolicy.put("callbackBody",
            "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"w\":\"$(imageInfo.width)\",\"h\":\"$(imageInfo.height)\"}");

    // type : String
    putPolicy.put("callbackUrl",
            "http://127.0.0.1:8080/callBack");

    putPolicy.put("callbackBodyType", "application/json");

    // 第三步： 生成一个上传凭证

    return auth.uploadToken(bucketname, key, (System.currentTimeMillis() / 1000L + 3600), putPolicy);
  }

  public static Map<String, String> getUploadToken(int bocket, String key) {
    Map<String, String> data = new HashMap<>();

    // 将上传凭证放入map中
    data.put("token", getuplodTokenByKey(key));

    // 图片地址
    data.put("key", getUrl(bocket, key));
    return data;
  }

  public String saveImage(MultipartFile file) throws IOException {
    try {
      int dotPos = file.getOriginalFilename().lastIndexOf(".");
      if (dotPos < 0) {
        return null;
      }
      String fileExt = file.getOriginalFilename().substring(dotPos + 1).toLowerCase();
      // 判断是否是合法的文件后缀
      if (!FileUtil.isFileAllowed(fileExt)) {
        return null;
      }

      String fileName = UUID.randomUUID().toString().replaceAll("-", "") + "." + fileExt;
      // 调用put方法上传
      Response res = uploadManager.put(file.getBytes(), fileName, getUpToken());
      // 打印返回的信息
      if (res.isOK() && res.isJson()) {
        // 返回这张存储照片的地址
        return publicIMGURL + JSONObject.parseObject(res.bodyString()).get("key");
      } else {
        logger.error("七牛异常:" + res.bodyString());
        return null;
      }
    } catch (QiniuException e) {
      // 请求失败时打印的异常的信息
      logger.error("七牛异常:" + e.getMessage());
      return null;
    }
  }

  public static String getuplodTokenByKey(String key) {

    // 第一步： 先生成一个Auth验证
    Auth auth = Auth.create(ACCESS_KEY, SECRET_KEY);

    // 第二步： 使用StringMap工具类拼装参数
    StringMap putPolicy = new StringMap();

    /**
     * 指定上传的目标资源空间 Bucket 和资源键 Key（最大为 750 字节）。有三种格式：
     *   1. <bucket>，表示允许用户上传文件到指定的 bucket。在这种格式下文件只能新增
     *       （分片上传需要指定insertOnly为1才是新增，否则也为覆盖上传），若已存在同名资源（且文件内容/etag不一致），
     *       上传会失败；若已存在资源的内容/etag一致，则上传会返回成功。
     *
     *
     *   2. <bucket>:<key>，表示只允许用户上传指定 key 的文件。
     *       在这种格式下文件默认允许修改，若已存在同名资源则会被覆盖。
     *       如果只希望上传指定 key 的文件，并且不允许修改，那么可以将下面的 insertOnly 属性值设为 1。
     *
     *
     *   3. <bucket>:<keyPrefix>，表示只允许用户上传指定以 keyPrefix 为前缀的文件，
     *       当且仅当 isPrefixalScope 字段为 1 时生效，isPrefixalScope 为 1 时无法覆盖上传。
     *
     * type : String
     */
    putPolicy.put("scope", bucketname);

    // 上传凭证有效截止时间。Unix时间戳，单位为秒。该截止时间为上传完成后，在七牛空间生成文件的校验时间，
    // 而非上传的开始时间，一般建议设置为上传开始时间 + 3600s
    // type : uint32
    putPolicy.put("deadline", (System.currentTimeMillis() / 1000L + 3600));

    // Web 端文件上传成功后，浏览器执行 303 跳转的 URL。通常用于表单上传。文件上传成功后会跳转到
    // <returnUrl>?upload_ret=<queryString>，<queryString>包含 returnBody 内容。
    // 如不设置 returnUrl，则直接将 returnUrlnBody 的内容返回给客户端。
    // type : String
//        putPolicy.put("returnUrl","");

    /**
     *上传成功后，自定义七牛云最终返回給上传端（在指定 returnUrl 时是携带在跳转路径参数中）的数据。
     * 支持魔法变量和自定义变量。returnBody 要求是合法的 JSON 文本。
     * 例如 {"key": $(key), "hash": $(etag), "w": $(imageInfo.width), "h": $(imageInfo.height)}。
     */
    // type : String
    putPolicy.put("returnBody",
        "{\"key\": $(key), \"hash\": $(etag), \"w\": $(imageInfo.width), \"h\": $(imageInfo.height)}");

    /**
     *
     * 上传成功后，七牛云向业务服务器发送 Content-Type: application/x-www-form-urlencoded 的 POST 请求。
     * 业务服务器可以通过直接读取请求的 query 来获得该字段，支持魔法变量和自定义变量。
     * callbackBody 要求是合法的 url query string。
     * 例如key=$(key)&hash=$(etag)&w=$(imageInfo.width)&h=$(imageInfo.height)。
     * 如果callbackBodyType指定为application/json，则callbackBody应为json格式，
     * 例如:{"key":"$(key)","hash":"$(etag)","w":"$(imageInfo.width)","h":"$(imageInfo.height)"}。
     * */
    // type : String
    putPolicy.put("callbackBody",
        "{\"key\":\"$(key)\",\"hash\":\"$(etag)\",\"w\":\"$(imageInfo.width)\",\"h\":\"$(imageInfo.height)\"}");

    //上传成功后，七牛云向业务服务器发送回调通知 callbackBody 的 Content-Type。
    // 默认为 application/x-www-form-urlencoded，也可设置为 application/json。
    // type : String
    putPolicy.put("callbackBodyType", "application/json");

    // 第三步： 生成一个上传凭证

    return auth.uploadToken(bucketname, key, (System.currentTimeMillis() / 1000L + 3600), putPolicy);
  }

  /**
   * 得到资源的访问地址，也是下载地址
   *
   * @param bocket 指定是在共有仓库还是私有仓库中，0为私有仓库，非0为共有仓库
   * @param key 资源的key
   * @return 资源的访问地址
   */
  private static String getUrl(int bocket, String key) {
    if (bocket == 0) {
      return getstaticDownloadToken(priviteIMGURL + key);
    } else {
      return publicIMGURL + key;
    }
  }

  /**
   * 获得一个私有空间下载的凭证,传入的参数是按照公开仓库访问方式生成的URL
   *
   * @param baseUrl 按照公开仓库生成URL的方式生成的URL
   */
  public static String getstaticDownloadToken(String baseUrl) {
    Auth auth = Auth.create(ACCESS_KEY,SECRET_KEY);
    return auth.privateDownloadUrl(baseUrl);
  }

}
