package interfaceApplication;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import cache.CacheHelper;
import check.checkHelper;
import database.DBHelper;
import database.db;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import json.JSONHelper;
import nlogger.nlogger;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import security.codec;
import time.TimeHelper;

public class wechatUser
{
  private DBHelper opHelper;

  public wechatUser()
  {
    this.opHelper = new DBHelper(appsProxy.configValue().get("db").toString(), "UserOpenId");
  }

  private db openIdBind() {
	  return opHelper.bindApp();
  }

  public String insertOpenId(String info)
  {
    CacheHelper helper = new CacheHelper();
    int code = 99;
    JSONObject object = JSONHelper.string2json(info);
    if (object != null) {
      try {
        object.remove("type");
        if (!object.containsKey("isdelete")) {
          object.put("isdelete", "0");
        }
        if (!object.containsKey("time")) {
          object.put("time", TimeHelper.nowMillis());
        }
        object.put("kickTime", "");
        if (!checkHelper.checkMobileNumber(object.get("phone").toString())) {
          return resultMessage(2);
        }
        String openid = object.get("openid").toString();
        String userinfo = 
          appsProxy.proxyCall("/GrapeWechat/Wechat/getUserInfo/s:" + openid, null, "")
          .toString();
        if (JSONHelper.string2json(userinfo) != null) {
          String message = JSONHelper.string2json(userinfo).get("message").toString();
          String records = JSONHelper.string2json(message).get("records").toString();
          String headimgurl = "";
          if (records.contains("headimgurl")) {
            headimgurl = JSONHelper.string2json(records).get("headimgurl").toString();
          }
          object.put("headimgurl", headimgurl);
          code = openIdBind().data(object).insertOnce() != null ? 0 : 99;
          nlogger.logout("headimgurl:" + headimgurl);
          if (code == 0)
          {
            if (helper.get(openid + "Info") != null) {
              helper.delete(openid + "Info");
              helper.setget(openid + "Info", FindOpenId(openid));
            }
          }
        }
      } catch (Exception e) {
        nlogger.logout(e);
        code = 99;
      }
    }
    return resultMessage(code, "实名认证成功");
  }

  public String UpdateInfo(String openid, String info)
  {
    int code = 99;
    try {
      JSONObject object = JSONHelper.string2json(info);
      String headimg = (String)object.get("headimgurl");
      headimg = codec.DecodeHtmlTag(headimg);
      object.put("headimgurl", codec.decodebase64(headimg));
      if (object != null)
        code = openIdBind().eq("openid", openid).data(object).update() != null ? 0 : 99;
    }
    catch (Exception e) {
      nlogger.logout(e);
      code = 99;
    }
    return resultMessage(code, "修改数据成功");
  }

  public String FindOpenId(String openid) {
    System.out.println(openid);
    JSONObject object = openIdBind().eq("openid", openid).find();
    return resultMessage(object);
  }

  public String FindById(String id) {
    JSONObject object = openIdBind().eq("_id", new ObjectId(id)).find();
    return resultMessage(object);
  }

  public String KickUser(String openid, String info)
  {
    JSONObject object = JSONHelper.string2json(info);
    if (object != null) {
      String message = FindOpenId(openid);
      if (JSONHelper.string2json(message) != null) {
        String record = JSONHelper.string2json(message).get("message").toString();
        if (!"".equals(record)) {
          if (object.containsKey("content")) {
            object.remove("content");
          }
          int code = openIdBind().eq("openid", openid).data(object).update() != null ? 0 : 99;
          return resultMessage(code, "操作成功");
        }
      }
    }
    return resultMessage(4);
  }

  public String page(int ids, int pageSize)
  {
    JSONObject object = null;
    try {
      object = new JSONObject();
      JSONArray array = openIdBind().page(ids, pageSize);
      object.put("totalSize", Integer.valueOf((int)Math.ceil(openIdBind().count() / pageSize)));
      object.put("currentPage", Integer.valueOf(ids));
      object.put("pageSize", Integer.valueOf(pageSize));
      object.put("data", array);
    } catch (Exception e) {
      nlogger.logout(e);
      object = null;
    }
    return resultMessage(object);
  }

  public String pageby(int ids, int pageSize, String info)
  {
    JSONObject object = null;
    JSONObject objects = JSONHelper.string2json(info);
    if (objects != null) {
      try {
        openIdBind().and();
        for (Iterator localIterator = objects.keySet().iterator(); localIterator.hasNext(); ) { Object obj = localIterator.next();
          if (obj.equals("_id")) {
            openIdBind().eq("_id", new ObjectId(objects.get("_id").toString()));
          }
          openIdBind().eq(obj.toString(), objects.get(obj.toString()));
        }

        JSONArray array = openIdBind().dirty().page(ids, pageSize);
        object = new JSONObject();
        object.put("totalSize", Integer.valueOf((int)Math.ceil(openIdBind().count() / pageSize)));
        openIdBind().clear();
        object.put("pageSize", Integer.valueOf(pageSize));
        object.put("currentPage", Integer.valueOf(ids));
        object.put("data", array);
      } catch (Exception e) {
        nlogger.logout(e);
        object = null;
      }
    }
    return resultMessage(object);
  }

  public String unkick() {
    int code = 99;
    try {
      int totalSize = (int)Math.ceil(openIdBind().eq("isdelete", "1").count() / 10.0D);
      for (int i = 0; i < totalSize; i++) {
        JSONArray array = openIdBind().page(i, 10);
        code = judge(array);
      }
    } catch (Exception e) {
      nlogger.logout(e);
      code = 99;
    }
    return resultMessage(code, "解封成功");
  }

  private String callHost() {
    return getAppIp("host").split("/")[0];
  }

  private String getAppIp(String key) {
    String value = "";
    try {
      Properties pro = new Properties();
      pro.load(new FileInputStream("URLConfig.properties"));
      value = pro.getProperty(key);
    } catch (Exception e) {
      value = "";
    }
    return value;
  }

  private int judge(JSONArray array)
  {
    int code = 99;
    if (array.size() != 0) {
      try {
        for (int i = 0; i < array.size(); i++) {
          JSONObject object = (JSONObject)array.get(i);
          if ("0".equals(object.get("kickTime").toString())) {
            break;
          }
          long opTime = Long.parseLong(object.get("time").toString());
          long kickTime = Long.parseLong(object.get("kickTime").toString());
          String totalTime = TimeHelper.stampToDate(kickTime * 3600L * 24L * 1000L + opTime).split(" ")[0];
          String currentTime = TimeHelper.stampToDate(TimeHelper.nowMillis()).split(" ")[0];
          if (!totalTime.equals(currentTime)) {
            break;
          }
          object.put("isdelete", "0");
          object.put("kickTime", "");
          object.put("time", TimeHelper.nowMillis());
          code = openIdBind().eq("openid", object.get("openid").toString()).data(object).update() != null ? 0 : 
            99;
        }
      } catch (Exception e) {
        nlogger.logout(e);
        code = 99;
      }
    }
    return code;
  }

  private String resultMessage(int num) {
    return resultMessage(num, "");
  }

  private String resultMessage(JSONObject object)
  {
    System.out.println(object);
    JSONObject obj = new JSONObject();
    if (object == null) {
      object = new JSONObject();
    }
    obj.put("records", object);
    return resultMessage(0, obj.toString());
  }

  private String resultMessage(int num, String message) {
    String msg = "";
    switch (num) {
    case 0:
      msg = message;
      break;
    case 1:
      msg = "必填字段为空";
      break;
    case 2:
      msg = "手机号格式错误";
      break;
    case 3:
      msg = "身份证号格式错误";
      break;
    case 4:
      msg = "用户不存在";
      break;
    case 5:
      msg = "服务次数已达到上限，实名认证失败，请稍候再试";
      break;
    default:
      msg = "其他操作异常";
    }

    return jGrapeFW_Message.netMSG(num, msg);
  }
}