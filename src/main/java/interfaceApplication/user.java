package interfaceApplication;

import cache.CacheHelper;
import checkCode.checkCodeHelper;
import checkCode.imageCheckCode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import javax.imageio.ImageIO;
import json.JSONHelper;
import model.CommanModel;
import model.userModel;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import security.codec;
import session.session;
import sms.ruoyaMASDB;
import time.TimeHelper;

public class user {
	private userModel usermodel = new userModel();
	private CommanModel model = new CommanModel();
	private HashMap<String, Object> defcol = new HashMap();
	private JSONObject _obj = new JSONObject();
	private String sid = null;
	private session se;
	private JSONObject userInfo = new JSONObject();
	private CacheHelper cache = new CacheHelper();

	public user() {
		this.se = new session();
		this.sid = session.getSID();
		if (this.sid != null)
			this.userInfo = this.se.getDatas();
	}

	public String UserRegister(String userInfo) {
		return this.usermodel.resultMessage(this.usermodel.register(JSONHelper.string2json(userInfo)), "用户注册成功");
	}

	public String getVerifyCode(String phone) {
		String code = checkCodeHelper.getCheckCode(phone, 6);
		code = ruoyaMASDB.sendSMS(phone, "验证码为：" + code + "有效时间为。。。，请在有效时间内进行验证");
		return this.usermodel.resultMessage(code != null ? 0 : 99, "验证码发送成功");
	}

	public String getImageCode() {
		String code = checkCodeHelper.generateVerifyCode(6);
		this.cache.setget(code.toLowerCase(), code);
		byte[] image = imageCheckCode.getCodeimage(code);
		return "data:image/jpeg;base64," + Base64.encodeBase64String(image);
	}

	public String checkVerifyCode(String phone, String code) {
		boolean flag = checkCodeHelper.checkCode(phone, code);
		return this.usermodel.resultMessage(flag ? 0 : 99, "验证成功");
	}

	public String checkImageCode(String code) {
		int tip = 99;
		code = code.toLowerCase();
		if ((code != null) && (!code.equals("")) && (this.cache.get(code) != null)) {
			this.cache.delete(code);
			tip = 0;
		}

		return this.usermodel.resultMessage(tip, "验证成功");
	}

	public String UserLogin(String userInfo) {
		String mString = "";

		String usersinfo = this.usermodel.checkLogin(JSONHelper.string2json(userInfo));
		if ((usersinfo != null) && (!usersinfo.contains("errorcode"))) {
			JSONObject object = JSONObject.toJSON(usersinfo);
			this._obj.put("records", object);
			mString = this.usermodel.resultMessage(0, this._obj.toString());
		} else {
			mString = this.usermodel.resultMessage(9, "");
		}
		return mString;
	}

	public String UserLogout(String UserName) {
		if ((UserName.length() > 1) && (UserName.length() < 128)) {
			this.usermodel.logout(UserName);
		}
		return this.usermodel.resultMessage(0, "退出成功");
	}

	public String UserGetpoint(String userName) {
		String value = String.valueOf(this.usermodel.getpoint_username(userName));
		return this.usermodel.resultMessage(0, value);
	}

	public String UserChangePW(String UserName, String oldPW, String newPW) {
		return this.usermodel.resultMessage(this.usermodel.changePW(UserName, oldPW, newPW), "密码修改成功！");
	}

	public String UserChangePWFront(String UserName, String oldPW, String newPW, int loginmode) {
		return this.usermodel.resultMessage(this.usermodel.changePWs(UserName, oldPW, newPW, loginmode), "密码修改成功！");
	}

	public String UserEdit(String _id, String userInfo) {
		return this.usermodel.resultMessage(this.usermodel.edit(_id, JSONHelper.string2json(userInfo)), "用户信息修改成功");
	}

	public String UserSelect() {
		return this.usermodel.select();
	}

	public String UserSearch(String userinfo) {
		return this.usermodel.select(JSONHelper.string2json(userinfo));
	}

	public String UserFind(String id) {
		return this.usermodel.resultMessage(this.usermodel.select(id));
	}

	public String UserPageFront(String wbid, int idx, int pageSize) {
		return this.usermodel.page(wbid, idx, pageSize, null);
	}

	public String UserPageByFront(String wbid, int idx, int pageSize, String userinfo) {
		return this.usermodel.page(wbid, idx, pageSize, userinfo);
	}

	public String UserPage(int idx, int pageSize) {
		return this.usermodel.page(null, idx, pageSize, null);
	}

	public String UserPageBy(int idx, int pageSize, String userinfo) {
		return this.usermodel.page(null, idx, pageSize, userinfo);
	}

	public String UserDelete(String id) {
		return this.usermodel.resultMessage(this.usermodel.delect(id), "删除成功");
	}

	public String UserBatchDelect(String ids) {
		return this.usermodel.resultMessage(this.usermodel.delect(ids.split(",")), "批量操作成功");
	}

	public String AddLeader(String info) {
		JSONObject object = this.usermodel.AddMap(this.defcol, JSONHelper.string2json(info));
		return this.usermodel.resultMessage(this.usermodel.register(object), "新增用户成功");
	}

	public String FindWbBySid(String wbid, String userid) {
		return this.usermodel.FindWb(wbid, userid);
	}

	public String findByCard(String name, String IDCard) {
		return this.usermodel.findUserByCard(name, IDCard).toString();
	}

	// public String ExcelImport(String filepath)
	// {
	// filepath = codec.DecodeHtmlTag(filepath);
	//
	// JSONArray array = new JSONArray();
	// List list = getAllByExcel(filepath);
	// if (list == null) {
	// return this.usermodel.resultMessage(11, "");
	// }
	// for (JSONObject jsonObject : list) {
	// array.add(jsonObject);
	// }
	// return this.usermodel.Import(array);
	// }

	public String getUserImage() {
		String name = "";
		if ((this.userInfo != null) && (this.userInfo.size() != 0)) {
			name = this.userInfo.getString("name");
		}
		return CreateImage(name);
	}

	private String CreateImage(String name) {
		int width = 200;
		int height = 200;
		Font font = new Font("宋体", 1, 12);
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		String currentTime = TimeHelper.stampToDate(TimeHelper.nowMillis());
		try {
			BufferedImage bi = new BufferedImage(width, height, 6);
			Graphics2D g2 = bi.createGraphics();
			g2.rotate(Math.toRadians(-45.0D), width / 2, height / 2);
			g2.setFont(font);
			g2.setColor(new Color(230, 230, 230));

			g2.drawString(name, 30, 50);
			g2.drawString(currentTime, 30, 80);
			g2.dispose();

			ImageIO.write(bi, "PNG", buffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "data:image/jpeg;base64," + Base64.encodeBase64String(buffer.toByteArray());
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

	private String getFileIp(String key, int sign) {
		String value = "";
		try {
			if ((sign == 0) || (sign == 1))
				value = getAppIp(key).split("/")[sign];
		} catch (Exception e) {
			value = "";
		}
		return value;
	}

	// private List<JSONObject> getAllByExcel(String file) {
	// List list = null;
	// JSONObject object = new JSONObject();
	// try {
	// list = new ArrayList();
	// Workbook rwb = Workbook.getWorkbook(new File(file));
	// Sheet[] value = rwb.getSheets();
	// for (Sheet rs : value) {
	// int clos = rs.getColumns();
	// int rows = rs.getRows();
	// if ((clos == 0) && (rows == 0)) {
	// break;
	// }
	// for (int i = 1; i < rows; i++)
	// for (int j = 1; j < clos; j++) {
	// object.put("name", rs.getCell(j++, i).getContents());
	// object.put("phone", rs.getCell(j++, i).getContents());
	// object.put("IDcard", rs.getCell(j++, i).getContents());
	// list.add(object);
	// }
	// }
	// }
	// catch (Exception e) {
	// e.printStackTrace();
	// list = null;
	// }
	// return list;
	// }

	private String getImageUri(String imageURL) {
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
}