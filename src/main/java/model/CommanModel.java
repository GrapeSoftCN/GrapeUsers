package model;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import common.java.JGrapeSystem.jGrapeFW_Message;
import common.java.nlogger.nlogger;
import common.java.session.session;
import common.java.string.StringHelper;

public class CommanModel {
	private JSONObject _obj = new JSONObject();
	private HashMap<String, Object> map = new HashMap();

	private final Pattern ATTR_PATTERN = Pattern.compile("<img[^<>]*?\\ssrc=['\"]?(.*?)['\"]?\\s.*?>", 2);

	public HashMap<String, Object> AddFixField() {
		this.map.put("itemfatherID", "0");
		this.map.put("itemSort", Integer.valueOf(0));
		this.map.put("deleteable", Integer.valueOf(0));
		this.map.put("visable", Integer.valueOf(0));
		this.map.put("itemLevel", Integer.valueOf(0));
		JSONObject rMode = new JSONObject("chkType", Integer.valueOf(0));
		rMode.put("chkCond", Integer.valueOf(300));
		JSONObject uMode = new JSONObject("chkType", Integer.valueOf(0));
		rMode.put("chkCond", Integer.valueOf(400));
		JSONObject dMode = new JSONObject("chkType", Integer.valueOf(0));
		rMode.put("chkCond", Integer.valueOf(500));
		this.map.put("rMode", rMode.toJSONString());
		this.map.put("uMode", uMode.toJSONString());
		this.map.put("dMode", dMode.toJSONString());
		return this.map;
	}

	public boolean isAdmin() {
		int userType = 0;
		session se = new session();
		JSONObject userInfo = se.getDatas();
		userType = (userInfo != null) && (userInfo.size() != 0) ? Integer.parseInt(userInfo.getString("userType")) : 0;
		return 1000 == userType;
	}

	public boolean isRoot() {
		int userType = 0;
		session se = new session();
		JSONObject userInfo = se.getDatas();
		userType = (userInfo != null) && (userInfo.size() != 0) ? Integer.parseInt(userInfo.getString("userType")) : 0;
		return 10000 == userType;
	}

	public JSONObject AddMap(HashMap<String, Object> map, String info) {
		String key = "";
		JSONObject object = JSONObject.toJSON(info);
		if (map.entrySet() != null) {
			Iterator iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry entry = (Map.Entry) iterator.next();
				key = (String) entry.getKey();
				if (!object.containsKey(key)) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	public String RemoveUrlPrefix(String imageUrl) {
		String imgurl = "";
		if ((imageUrl.equals("")) || (imageUrl == null)) {
			return imageUrl;
		}
		String[] imgUrl = imageUrl.split(",");
		List list = new ArrayList();
		for (String string : imgUrl) {
			if (string.contains("http://")) {
				imgurl = getimage(string);
			}
			list.add(string);
		}
		if (list.size() > 1) {
			imgurl = StringHelper.join(list);
		}
		return imgurl;
	}

	public String getimage(String imageURL) {
		int i = 0;
		if (imageURL.contains("File//upload")) {
			i = imageURL.toLowerCase().indexOf("file//upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File\\upload")) {
			i = imageURL.toLowerCase().indexOf("file\\upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		if (imageURL.contains("File/upload")) {
			i = imageURL.toLowerCase().indexOf("file/upload");
			imageURL = "\\" + imageURL.substring(i);
		}
		return imageURL;
	}

	public JSONObject getImage(JSONObject objimg) {
		for (Iterator localIterator = objimg.keySet().iterator(); localIterator.hasNext();) {
			Object obj = localIterator.next();
			String key = obj.toString();
			String value = objimg.getString(key);
			if (!value.contains("http://")) {
				objimg.put(key, AddUrlPrefix(value));
			}
		}
		return objimg;
	}

	public String AddUrlPrefix(String imageUrl) {
		if ((imageUrl.equals("")) || (imageUrl == null)) {
			return imageUrl;
		}
		String[] imgUrl = imageUrl.split(",");
		List list = new ArrayList();
		for (String string : imgUrl) {
			if (string.contains("http:")) {
				string = getimage(string);
			}
			string = "http://" + getFileHost(1) + string;
			list.add(string);
		}
		return StringHelper.join(list);
	}

	public String AddHtmlPrefix(String Contents) {
		Document doc = Jsoup.parse(Contents);
		if (doc.empty() != null) {
			String imgurl = "http://" + getFileHost(0);
			Elements element = doc.select("img");
			if (element.size() != 0) {
				for (int i = 0; i < element.size(); i++) {
					String attrValue = ((Element) element.get(i)).attr("src");
					if (attrValue.contains("http://")) {
						attrValue = RemoveHtmlPrefix(attrValue);
					}
					((Element) element.get(i)).attr("src", imgurl + attrValue);
				}
				return doc.html();
			}
		}
		return Contents;
	}

	public String RemoveHtmlPrefix(String Contents) {
		List list = getCommonAddr(Contents);
		for (int i = 0; i < list.size(); i++) {
			String temp = (String) list.get(i);
			String string2 = temp.replace(temp, RemoveUrlPrefix(temp));
			if (Contents.contains(temp)) {
				Contents = Contents.replaceAll(temp, string2);
			}
		}
		return Contents;
	}

	private List<String> getCommonAddr(String contents) {
		Matcher matcher = this.ATTR_PATTERN.matcher(contents);
		List list = new ArrayList();
		while (matcher.find()) {
			list.add(matcher.group(1));
		}
		return list;
	}

	public String getFileHost(int signal) {
		String host = null;
		try {
			if ((signal == 0) || (signal == 1))
				host = getAppIp("file").split("/")[signal];
		} catch (Exception e) {
			nlogger.logout(e);
			host = null;
		}
		return host;
	}

	private String getAppIp(String key) {
		String value = "";
		try {
			Properties pro = new Properties();
			pro.load(new FileInputStream("URLConfig.properties"));
			value = pro.getProperty(key);
		} catch (Exception e) {
			nlogger.logout(e);
			value = "";
		}
		return value;
	}

	public String pageShow(JSONArray array, long total, long totalSize, int idx, int pageSize) {
		array = (array != null) && (array.size() != 0) ? array : new JSONArray();
		JSONObject object = new JSONObject();
		object.put("currentPage", Integer.valueOf(idx));
		object.put("pageSize", Integer.valueOf(pageSize));
		object.put("total", Long.valueOf(total));
		object.put("totalSize", Long.valueOf(totalSize));
		object.put("data", array);
		return resultJSONInfo(object);
	}

	public String resultArray(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		this._obj.put("records", array);
		return resultMessage(0, this._obj.toString());
	}

	public String resultJSONInfo(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		this._obj.put("records", object);
		return resultMessage(0, this._obj.toString());
	}

	public String resultmsg(int num) {
		return resultMessage(num, "");
	}

	public String resultMessage(int num, String message) {
		String msg = "";
		switch (num) {
		case 0:
			msg = message;
			break;
		case 1:
			msg = "必填字段为空";
			break;
		case 2:
			msg = "您当前没有权限操作";
			break;
		case 3:
			msg = "内容组名称长度不合法";
			break;
		case 4:
			msg = "该栏目已存在本文章";
			break;
		case 5:
			msg = "该站点已存在本文章";
			break;
		case 6:
			msg = "存在敏感词";
			break;
		case 7:
			msg = "超过限制字数";
			break;
		case 8:
			msg = "没有操作权限";
			break;
		default:
			msg = "其他操作异常";
		}

		return jGrapeFW_Message.netMSG(num, msg);
	}
}