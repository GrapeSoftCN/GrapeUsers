package model;

import JGrapeSystem.jGrapeFW_Message;
import apps.appsProxy;
import database.db;
import interfaceModel.GrapeDBSpecField;
import interfaceModel.GrapeTreeDBModel;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import json.JSONHelper;
import nlogger.nlogger;
import org.bson.types.ObjectId;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import session.session;

public class RolesModel {
	private GrapeTreeDBModel role;
	private GrapeDBSpecField gdbField;
	private String wbid;
	private JSONObject _obj = new JSONObject();

	private GrapeTreeDBModel bind() {
		return (GrapeTreeDBModel) role.bindApp();
	}

	public RolesModel() {
		role = new GrapeTreeDBModel();
		gdbField = new GrapeDBSpecField();
        gdbField.importDescription(appsProxy.tableConfig("roles"));
        role.descriptionModel(gdbField);
        role.bindApp();
	}

	public int insert(JSONObject object) {
		int code = 99;
		if (object != null) {
			JSONObject obj = new session().getDatas();
			if ((object.containsKey("wbid")) && ("".equals(object.get(this.wbid).toString())) && (obj != null))
				object.put("wbid", obj.get("currentWeb").toString());
			try {
				if (select(object.get("name").toString()) != null) {
					return 2;
				}
				Object object2 = bind().data(object).insertOnce();
				code = object2 != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int update(String id, JSONObject object) {
		int code = 99;
		JSONObject obj = null;
		try {
			if ((object != null) && (getPlv(id) != 0))
				try {
					obj = new JSONObject();
					obj = bind().eq("_id", new ObjectId(id)).data(object).update();
					code = obj != null ? 0 : 99;
				} catch (Exception e) {
					nlogger.logout(e);
					code = 99;
				}
			else
				code = 4;
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int update(JSONObject object) {
		int code = 99;
		JSONObject _obj = new JSONObject();
		if (object != null) {
			try {
				JSONObject obj = (JSONObject) object.get("_id");
				if ((object.containsKey("fatherid")) && (object.containsKey("sort"))) {
					_obj.put("fatherid", object.get("fatherid"));
					_obj.put("sort", Integer.valueOf(Integer.parseInt(object.get("sort").toString())));
					code = bind().eq("_id", obj.get("$oid").toString()).data(_obj).update() != null ? 0 : 99;
				} else if (object.containsKey("fatherid")) {
					code = setFatherId(object.get("$oid").toString(), object.get("fatherid").toString());
				} else {
					code = setsort(object.get("$oid").toString(), Integer.parseInt(object.get("sort").toString()));
				}
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public String select(JSONObject object) {
		JSONArray array = null;
		if (object != null) {
			try {
				array = new JSONArray();
				for (Iterator localIterator = object.keySet().iterator(); localIterator.hasNext();) {
					Object object2 = localIterator.next();
					bind().eq(object2.toString(), object.get(object2.toString()));
				}
				array = bind().limit(20).select();
			} catch (Exception e) {
				nlogger.logout(e);
				array = null;
			}
		}
		return resultMessage(array);
	}

	public String select(String name) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("name", name).find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	public String page(int idx, int pageSize) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			JSONArray array = bind().page(idx, pageSize);
			object = new JSONObject();
			object.put("totalSize", Integer.valueOf((int) Math.ceil(bind().count() / pageSize)));
			object.put("currentPage", Integer.valueOf(idx));
			object.put("pageSize", Integer.valueOf(pageSize));
			object.put("data", array);
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	public String page(int idx, int pageSize, JSONObject object) {
		JSONObject obj = null;
		if (object != null) {
			try {
				obj = new JSONObject();
				for (Iterator localIterator = object.keySet().iterator(); localIterator.hasNext();) {
					Object object2 = localIterator.next();
					bind().eq(object2.toString(), object.get(object2.toString()));
				}
				JSONArray array = bind().dirty().page(idx, pageSize);
				obj = new JSONObject();
				obj.put("totalSize", Integer.valueOf((int) Math.ceil(bind().count() / pageSize)));
				obj.put("currentPage", Integer.valueOf(idx));
				obj.put("pageSize", Integer.valueOf(pageSize));
				obj.put("data", array);
			} catch (Exception e) {
				nlogger.logout(e);
				object = null;
			} finally {
				bind().clear();
			}
		}
		return resultMessage(object);
	}

	public int delete(String id) {
		int code = 99;
		JSONObject object = null;
		try {
			if (getPlv(id) != 0) {
				object = new JSONObject();
				object = bind().eq("_id", new ObjectId(id)).delete();
				code = object != null ? 0 : 99;
			}
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int delete(String[] arr) {
		int code = 99;
		try {
			int plv = 0;
			bind().or();
			for (int i = 0; i < arr.length; i++) {
				plv = getPlv(arr[i]);
				if (plv != 0) {
					bind().eq("_id", new ObjectId(arr[i]));
				}
			}
			long codes = bind().deleteAll();
			code = Integer.parseInt(String.valueOf(codes)) == (plv == 0 ? arr.length : arr.length - 1) ? 0 : 99;
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
	}

	public int setsort(String id, int num) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("sort", Integer.valueOf(num));
		if (object != null) {
			try {
				JSONObject obj = bind().eq("_id", new ObjectId(id)).data(object).update();
				code = obj != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int setFatherId(String id, String fatherid) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("fatherid", fatherid);
		if (object != null) {
			try {
				JSONObject obj = bind().eq("_id", new ObjectId(id)).data(object).update();
				code = obj != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public int setPlv(String id, String plv) {
		int code = 99;
		JSONObject object = new JSONObject();
		object.put("plv", plv);
		if (object != null) {
			try {
				JSONObject obj = bind().eq("_id", new ObjectId(id)).data(object).update();
				code = obj != null ? 0 : 99;
			} catch (Exception e) {
				nlogger.logout(e);
				code = 99;
			}
		}
		return code;
	}

	public String getRole(String ugid) {
		JSONObject object = null;
		try {
			object = new JSONObject();
			object = bind().eq("_id", ugid).find();
		} catch (Exception e) {
			nlogger.logout(e);
			object = null;
		}
		return resultMessage(object);
	}

	private int getPlv(String id) {
		int code = 99;
		try {
			String roleplv = getRole(id);
			JSONObject object = JSONHelper.string2json(roleplv);
			if (object != null) {
				object = JSONHelper.string2json(object.get("message").toString());
				if (object != null) {
					object = JSONHelper.string2json(object.get("records").toString());
				}
				code = Integer.parseInt(object.get("plv").toString());
			}
		} catch (Exception e) {
			nlogger.logout(e);
			code = 99;
		}
		return code;
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

	public JSONObject addMap(HashMap<String, Object> map, JSONObject object) {
		JSONObject obj = null;
		if (object != null) {
			try {
				obj = object;
				if (map.entrySet() != null) {
					Iterator iterator = map.entrySet().iterator();
					while (iterator.hasNext()) {
						Map.Entry entry = (Map.Entry) iterator.next();
						if (!obj.containsKey(entry.getKey()))
							obj.put(entry.getKey(), entry.getValue());
					}
				}
			} catch (Exception e) {
				nlogger.logout(e);
				obj = null;
			}
		}
		return obj;
	}

	private String resultMessage(JSONObject object) {
		if (object == null) {
			object = new JSONObject();
		}
		this._obj.put("records", object);
		return resultMessage(0, this._obj.toString());
	}

	private String resultMessage(JSONArray array) {
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
			msg = "设置排序或层级失败";
			break;
		case 3:
			msg = "设置排序或层级失败";
			break;
		case 4:
			msg = "无法操作该条数据";
			break;
		case 5:
			msg = "没有创建数据权限，请联系管理员进行权限调整";
			break;
		case 6:
			msg = "没有修改数据权限，请联系管理员进行权限调整";
			break;
		case 7:
			msg = "没有删除数据权限，请联系管理员进行权限调整";
			break;
		default:
			msg = "其他操作异常";
		}

		return jGrapeFW_Message.netMSG(num, msg);
	}
}