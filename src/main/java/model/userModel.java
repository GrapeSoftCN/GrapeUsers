package model;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import check.checkHelper;
import database.db;
import interfaceModel.GrapeTreeDBModel;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import json.JSONHelper;
import nlogger.nlogger;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import rpc.execRequest;
import security.codec;
import session.session;

public class userModel {
	private GrapeTreeDBModel users;
	private CommanModel model = new CommanModel();
	private JSONObject _obj = new JSONObject();
	private JSONObject UserInfo = null;
	private session se = new session();
	private String sid = null;
	private Map<String, String> ssessionmap = new Hashtable();
	private String userid = "";
	private String currentWeb = "";

	public userModel() {
		this.users = new GrapeTreeDBModel();
		this.users.form("userList");
		this.sid = ((String) execRequest.getChannelValue("GrapeSID"));
		if (this.sid != null) {
			this.UserInfo = this.se.getDatas();
			if ((this.UserInfo != null) && (this.UserInfo.size() != 0)) {
				this.userid = this.UserInfo.getMongoID("_id");
				this.currentWeb = this.UserInfo.get("currentWeb").toString();
			}
		}
	}

	private GrapeTreeDBModel bind() {
		return (GrapeTreeDBModel) this.users.bindApp();
	}

	private HashMap<String, Object> getInitData() {
		HashMap defcol = this.model.AddFixField();
		defcol.put("sex", Integer.valueOf(1));
		defcol.put("birthday", Integer.valueOf(0));
		defcol.put("point", Integer.valueOf(0));
		defcol.put("cash", Double.valueOf(0.0D));
		defcol.put("ownid", Integer.valueOf(0));
		defcol.put("time", null);
		defcol.put("lasttime", Integer.valueOf(0));
		defcol.put("ugid", Integer.valueOf(0));
		defcol.put("state", Integer.valueOf(0));
		defcol.put("plv", Integer.valueOf(100));
		defcol.put("IDcard", "");
		defcol.put("userheadImg", "");
		defcol.put("wbid", this.currentWeb);
		defcol.put("loginCount", Integer.valueOf(0));
		return defcol;
	}

	public int register(JSONObject _userInfo) {
		int code = 99;
		if (_userInfo != null) {
			try {
				String userName = _userInfo.get("id").toString();
				if (!checkUserName(userName)) {
					return 2;
				}
				if (findUserNameByID(userName) != null) {
					return 3;
				}
				if (_userInfo.containsKey("email")) {
					String email = _userInfo.get("email").toString();
					if (!checkEmail(email)) {
						return 4;
					}
					if (findUserNameByEmail(email) != null) {
						return 5;
					}
				}
				if (_userInfo.containsKey("mobphone")) {
					String phoneno = _userInfo.get("mobphone").toString();
					if (!checkMobileNumber(phoneno)) {
						return 6;
					}
					if (findUserNameByMoblie(phoneno) != null) {
						return 7;
					}
				}

				String secpassword = secPassword(_userInfo.get("password").toString());
				_userInfo.replace("password", secpassword);
				_userInfo = AddMap(getInitData(), _userInfo);
				Object object = bind().data(_userInfo).insertOnce();
				code = object != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public String checkLogin(JSONObject userinfo) {
		JSONObject object = null;
		int loginMode = 0;
		String username = "";
		String password = "";
		try {
			if (userinfo.containsKey("loginmode")) {
				loginMode = Integer.parseInt(userinfo.get("loginmode").toString());
			}
			switch (loginMode) {
			case 0:
				username = userinfo.get("id").toString();
				if (!checkUserName(username)) {
					return resultMessage(2, "");
				}
				if (!userinfo.containsKey("password"))
					break;
				password = userinfo.get("password").toString();

				break;
			case 1:
				username = userinfo.get("email").toString();
				if (!checkEmail(username)) {
					return resultMessage(4, "");
				}
				if (!userinfo.containsKey("password"))
					break;
				password = userinfo.get("password").toString();

				break;
			case 2:
				username = userinfo.get("mobphone").toString();
				if (!checkMobileNumber(username)) {
					return resultMessage(6, "");
				}
				if (!userinfo.containsKey("password"))
					break;
				password = userinfo.get("password").toString();

				break;
			case 3:
				username = userinfo.get("IDcard").toString();
				password = userinfo.get("name").toString();
				break;
			case 4:
				String name = userinfo.get("name").toString();
				if (userinfo.containsKey("password")) {
					password = userinfo.get("password").toString();
				}
				JSONObject rs = getIDCard(name, password);
				username = rs == null ? null : rs.get("IDcard").toString();
				password = name;
			}

			object = null;
			if (username != null) {
				object = new JSONObject();
				object = login(username, password, loginMode);
			}
		} catch (Exception e) {
			e.printStackTrace();
			object = null;
		}
		return object != null ? object.toString() : null;
	}

	private JSONObject login(String username, String password, int loginMode) {
		String sid = "";
		String _checkField = "";
		String field = "password";
		switch (loginMode) {
		case 0:
			_checkField = "id";
			break;
		case 1:
			_checkField = "email";
			break;
		case 2:
			_checkField = "mobphone";
			break;
		case 3:
		case 4:
			_checkField = "IDcard";
			field = "name";
		}

		if (field.equals("password")) {
			password = codec.md5(password);
		}
		JSONObject object = bind().eq(_checkField, username).eq(field, password).find();
		if (object != null) {
			String wbid = object.get("wbid").toString();
			String ugid = object.get("ugid").toString();
			JSONArray array = getWbID(wbid);
			if (array == null) {
				array = new JSONArray();
			}
			object.remove("wbid");
			object.remove("password");
			wbid = wbid.split(",")[0];
			object.put("currentWeb", wbid);
			object.put("webinfo", array);
			object.put("rolename", getRole(ugid));
			se = session.createSession(username, object, 86400L);
			sid = se._getSID();
			object.put("sid", sid);

			JSONObject loginData = AddCount(_checkField, username);
			String _id = ((JSONObject) object.get("_id")).getString("$oid");
			edit(_id, loginData);
		}
		return object;
	}

	private JSONObject AddCount(String checkField, String value) {
		JSONObject obj = new JSONObject();
		long times = 0L;
		try {
			JSONObject logincount = bind().eq(checkField, value).field("logincount").limit(1).find();
			if ((logincount != null) && (logincount.size() != 0)) {
				String values = String.valueOf(logincount.get("logincount"));
				if (values.contains("$numberLong")) {
					logincount = JSONObject.toJSON(values);
					values = (logincount != null) && (logincount.size() != 0) ? logincount.getString("$numberLong")
							: "0";
				}
				times = Long.parseLong(values) + 1L;
			}
		} catch (Exception e) {
			nlogger.logout(e);
		}
		obj.put("logincount", Long.valueOf(times));
		return obj;
	}

	private JSONArray getWbID(String wbid) {
		JSONArray webs = null;

		if (wbid.equals("0")) {
			return webs;
		}

		try {
			String webinfo = appsProxy.proxyCall("/GrapeWebInfo1/WebInfo/WebFindById/s:" + wbid).toString();

			JSONObject rs = JSONHelper.string2json(webinfo);
			if (rs != null) {
				rs = JSONHelper.string2json(rs.get("message").toString());
				if (rs != null) {
					String records = rs.get("records").toString();
					JSONArray array = JSONArray.toJSONArray(records);
					if ((array != null) && (array.size() > 0)) {
						webs = new JSONArray();
						JSONObject objects = null;
						JSONObject objid = null;
						int i = 0;
						for (int len = array.size(); i < len; i++) {
							JSONObject object2 = (JSONObject) array.get(i);
							objid = (JSONObject) object2.get("_id");
							objects = new JSONObject();
							objects.put("wbid", objid.get("$oid").toString());
							objects.put("wbname", object2.get("title").toString());
							objects.put("wbgid", object2.get("wbgid").toString());
							webs.add(objects);
						}
					}
				}
			}
		} catch (Exception e) {
			webs = null;
		}
		return webs;
	}

	private String getRole(String ugid) {
		String name = "";

		String temp = appsProxy.proxyCall("/GrapeUser1/roles/getRole/" + ugid).toString();
		JSONObject tempObj = JSONObject.toJSON(temp);
		if ((tempObj != null) && (tempObj.size() != 0)) {
			tempObj = JSONObject.toJSON(tempObj.getString("message"));
		}
		if ((tempObj != null) && (tempObj.size() != 0)) {
			tempObj = JSONObject.toJSON(tempObj.getString("records"));
		}
		if ((tempObj != null) && (tempObj.size() != 0)) {
			name = tempObj.getString("name");
		}
		return name;
	}

	public void logout(String sid) {
		String GrapeSID = session.getSID();
		if ((GrapeSID == null) && (!this.ssessionmap.containsKey(sid))) {
			GrapeSID = (String) this.ssessionmap.get(sid);
		}

		this.se.deleteSession();
	}

	public long getpoint_username(String username) {
		long rl = 0L;
		JSONObject rs = bind().eq("id", username).field("point").find();
		if (rs != null) {
			rl = Long.parseLong(rs.get("point").toString());
		}
		return rl;
	}

	public int changePW(String id, String oldPW, String newPW) {
		if (checkUser(id, oldPW)) {
			return 9;
		}
		JSONObject object = new JSONObject();
		object.put("password", secPassword(newPW));
		object = bind().eq("id", id).eq("password", codec.md5(oldPW)).data(object).update();
		return object != null ? 0 : 99;
	}

	public int changePWs(String id, String oldPW, String newPW, int loginmode) {
		JSONObject object = new JSONObject();
		String _checkField = "";
		switch (loginmode) {
		case 0:
			_checkField = "id";
			break;
		case 1:
			_checkField = "email";
			break;
		case 2:
			_checkField = "mobphone";
			break;
		case 3:
			if (!checkHelper.checkPersonCardID(id)) {
				object = getIDCard(id, oldPW);
				id = object == null ? null : object.get("IDcard").toString();
			}
			_checkField = "IDcard";
		}

		if (checkUsers(_checkField, id, oldPW)) {
			return 9;
		}
		JSONObject obj = new JSONObject();
		obj.put("password", secPassword(newPW));
		object = bind().eq(_checkField, id).eq("password", secPassword(oldPW)).data(obj).update();
		return object != null ? 0 : 99;
	}

	private boolean checkUsers(String field, String value, String pw) {
		return bind().eq(field, value).eq("password", secPassword(pw)).find() == null;
	}

	public int edit(String _id, JSONObject userInfo) {
		int code = 99;
		if (userInfo != null) {
			try {
				if ((userInfo.containsKey("id")) && (!checkUserName(userInfo.get("id").toString()))) {
					return 2;
				}

				if ((userInfo.containsKey("email")) && (!checkEmail(userInfo.get("email").toString()))) {
					return 4;
				}

				if ((userInfo.containsKey("mobphone")) && (!checkMobileNumber(userInfo.get("mobphone").toString()))) {
					return 6;
				}

				if (userInfo.containsKey("password")) {
					userInfo.remove("password");
				}
				JSONObject object = bind().eq("_id", new ObjectId(_id)).data(userInfo).update();
				code = object != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int Update(String id, String ownid, JSONObject object) {
		return bind().eq("_id", new ObjectId(id)).eq("ownid", ownid).data(object).update() != null ? 0 : 99;
	}

	public String select() {
		JSONArray array = null;
		try {
			array = new JSONArray();
			array = bind().limit(20).select();
		} catch (Exception e) {
			nlogger.logout(e);
			array = null;
		}
		return resultMessage(array);
	}

	public String select(JSONObject userInfo) {
		JSONArray array = null;
		if (userInfo != null) {
			try {
				array = new JSONArray();
				for (Iterator localIterator = userInfo.keySet().iterator(); localIterator.hasNext();) {
					Object object2 = localIterator.next();
					if (object2.equals("_id")) {
						bind().eq("_id", new ObjectId(userInfo.get(object2).toString()));
					}
					bind().eq(object2.toString(), userInfo.get(object2.toString()));
				}
				array = bind().limit(20).select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return resultMessage(array);
	}

	public JSONObject select(String id) {
		JSONObject object = bind().eq("id", id).find();
		return object != null ? object : null;
	}

	public String page(int idx, int pageSize) {
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		if (this.UserInfo != null) {
			db db = bind();
			try {
				if (isAdmin()) {
					array = db.page(idx, pageSize);
				} else {
					db.eq("wbid", (String) this.UserInfo.get("currentWeb"));
					array = db.dirty().page(idx, pageSize);
				}
				object.put("totalSize", Integer.valueOf((int) Math.ceil(db.count() / pageSize)));
			} catch (Exception e) {
				nlogger.logout(e);
				object.put("totalSize", Integer.valueOf(0));
			} finally {
				db.clear();
			}
			object.put("currentPage", Integer.valueOf(idx));
			object.put("pageSize", Integer.valueOf(pageSize));
			object.put("data", array);
		}
		return resultMessage(object);
	}

	public String page(int idx, int pageSize, JSONObject userInfo) {
		db db = bind();
		JSONObject object = new JSONObject();
		JSONArray array = new JSONArray();
		try {
			if (userInfo != null) {
				for (Iterator localIterator = userInfo.keySet().iterator(); localIterator.hasNext();) {
					Object object2 = localIterator.next();
					if ("_id".equals(object2.toString())) {
						db.eq("_id", new ObjectId(userInfo.get("_id").toString()));
					}
					db.eq(object2.toString(), userInfo.get(object2.toString()));
				}
				array = db.dirty().page(idx, pageSize);
				object.put("totalSize", Integer.valueOf((int) Math.ceil(db.count() / pageSize)));
			} else {
				object.put("totalSize", Integer.valueOf(0));
			}
		} catch (Exception e) {
			nlogger.logout(e);
			object.put("totalSize", Integer.valueOf(0));
		} finally {
			db.clear();
		}
		object.put("currentPage", Integer.valueOf(idx));
		object.put("pageSize", Integer.valueOf(pageSize));
		object.put("data", array);
		return resultMessage(object);
	}

	public String page(String wbid, int idx, int pageSize, String userInfo) {
		JSONObject object = new JSONObject();
		JSONArray array = null;
		long total = 0L;
		long totalSize = 0L;
//		if (isAdmin())
//			wbid = null;
//		else {
//			wbid = currentWeb;
//		}
//		db db = getDB(wbid, userInfo);
		db db = getDB(currentWeb, userInfo);
		array = db.dirty().page(idx, pageSize);
		totalSize = db.dirty().pageMax(pageSize);
		total = db.count();
		db.clear();
		object.put("total", Long.valueOf(total));
		object.put("totalSize", Long.valueOf(totalSize));
		object.put("currentPage", Integer.valueOf(idx));
		object.put("pageSize", Integer.valueOf(pageSize));
		object.put("data", array != null ? array : new JSONArray());
		return resultMessage(object);
	}

	public boolean isAdmin() {
		int userType = 0;
		session se = new session();
		JSONObject userInfo = se.getDatas();
		userType = (userInfo != null) && (userInfo.size() != 0) ? Integer.parseInt(userInfo.getString("userType")) : 0;
		return 10000 == userType;
	}

	private db getDB(String wbid, String condString) {
		db db = bind();
		if ((condString != null) && (condString.equals(""))) {
			JSONArray array = JSONArray.toJSONArray(condString);
			if ((array != null) && (array.size() != 0)) {
				db.where(array);
			}
		}
		if ((wbid != null) && (wbid.equals(""))) {
			db.eq("wbid", wbid);
		}
		return db;
	}

	public int delect(String id) {
		int code = 99;
		try {
			JSONObject object = bind().eq("_id", new ObjectId(id)).delete();
			code = object != null ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int delect(String[] arr) {
		int code = 99;
		try {
			bind().or();
			for (int i = 0; i < arr.length; i++) {
				bind().eq("_id", new ObjectId(arr[i]));
			}
			long codes = bind().deleteAll();
			code = Integer.parseInt(String.valueOf(codes)) == arr.length ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public JSONObject findUserNameByID(String userName) {
		return bind().eq("id", userName).find();
	}

	public JSONObject findUserNameByEmail(String email) {
		return bind().eq("email", email).find();
	}

	public JSONObject findUserNameByMoblie(String phoneno) {
		return bind().eq("mobphone", phoneno).find();
	}

	public JSONObject findUserByCard(String name, String IDCard) {
		return bind().eq("name", name).eq("IDCard", IDCard).find();
	}

	public boolean checkUser(String id, String pw) {
		pw = secPassword(pw);
		return bind().eq("id", id).eq("password", pw).find() == null;
	}

	public boolean checkEmail(String email) {
		return checkHelper.checkEmail(email);
	}

	public boolean checkMobileNumber(String mobile) {
		return checkHelper.checkMobileNumber(mobile);
	}

	public boolean checkUserName(String userName) {
		String regex = "([a-z]|[A-Z]|[0-9]|[\\u4e00-\\u9fa5])+";
		Pattern p = Pattern.compile(regex);
		Matcher m = p.matcher(userName);
		return (userName.length() >= 7) && (userName.length() <= 15) && (m.matches());
	}

	public JSONObject AddMap(HashMap<String, Object> map, JSONObject object) {
		if (map.entrySet() != null) {
			Iterator iterator = map.entrySet().iterator();
			while (iterator.hasNext()) {
				Map.Entry entry = (Map.Entry) iterator.next();
				if (!object.containsKey(entry.getKey())) {
					object.put(entry.getKey(), entry.getValue());
				}
			}
		}
		return object;
	}

	public String secPassword(String passwd) {
		return codec.md5(passwd);
	}

	public String Import(JSONArray array) {
		int code = 0;
		for (int i = 0; i < array.size(); i++) {
			if (code == 0)
				code = bind().data((JSONObject) array.get(i)).insertOnce() != null ? 0 : 99;
			else {
				code = 99;
			}
		}
		return resultMessage(code, "导入excel成功");
	}

	private JSONObject getIDCard(String name, String password) {
		JSONObject object = bind().eq("name", name)
				.eq("password", !password.equals("") ? codec.md5(password) : password).field("IDcard").find();
		return object != null ? object : null;
	}

	private JSONObject FindByPrimary(String _id) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", new ObjectId(_id)).field("wbid").find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return object;
	}

	public String FindWb(String wbid, String userid) {
		int code = 99;

		JSONObject object = FindByPrimary(userid);
		if (object != null) {
			try {
				String tempwbid = (String) object.get("wbid");
				if (!"".equals(tempwbid)) {
					wbid = String.join(",", new CharSequence[] { wbid, tempwbid });
				}
				object.put("wbid", wbid);
				code = bind().eq("_id", new ObjectId(userid)).data(object).update() != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return resultMessage(code, "设置网站管理员成功");
	}

	public String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		this._obj.put("records", object);
		return resultMessage(0, this._obj.toString());
	}

	public String resultMessage(JSONArray array) {
		if (array == null) {
			array = new JSONArray();
		}
		this._obj.put("records", array);
		return resultMessage(0, this._obj.toString());
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
			msg = "用户名格式错误";
			break;
		case 3:
			msg = "用户名已存在";
			break;
		case 4:
			msg = "email格式错误";
			break;
		case 5:
			msg = "email已存在";
			break;
		case 6:
			msg = "手机号格式错误";
			break;
		case 7:
			msg = "手机号已存在";
			break;
		case 8:
			msg = "该用户已登录";
			break;
		case 9:
			msg = "登录信息填写错误";
			break;
		case 10:
			msg = "没有操作权限";
			break;
		case 11:
			msg = "获取excel文件内容失败";
			break;
		case 12:
			msg = "验证码，请重新获取验证码";
			break;
		default:
			msg = "其他操作异常";
		}

		return jGrapeFW_Message.netMSG(num, msg);
	}
}