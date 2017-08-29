package interfaceApplication;

import apps.appsProxy;
import java.util.HashMap;
import json.JSONHelper;
import model.RolesModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import session.session;

public class roles
{
  private RolesModel rolesModel = new RolesModel();
  private HashMap<String, Object> defcol = new HashMap();
  private session se;
  private String sid = null;
  private JSONObject userinfo = new JSONObject();

  public roles() {
    this.se = new session();
    this.sid = session.getSID();
    if (this.sid != null) {
      this.userinfo = this.se.getDatas();
    }
    this.defcol.put("ownid", Integer.valueOf(appsProxy.appid()));
    this.defcol.put("sort", Integer.valueOf(0));
    this.defcol.put("fatherid", Integer.valueOf(0));
    this.defcol.put("wbid", (this.userinfo != null) && (this.userinfo.size() != 0) ? this.userinfo.get("currentWeb").toString() : "");
    this.defcol.put("plv", Integer.valueOf(1000));
  }

  public String RoleInsert(String roleInfo) {
    JSONObject object = this.rolesModel.addMap(this.defcol, JSONHelper.string2json(roleInfo));
    return this.rolesModel.resultMessage(this.rolesModel.insert(object), "新增角色成功");
  }

  public String RoleUpdate(String id, String roleInfo) {
    return this.rolesModel.resultMessage(this.rolesModel.update(id, JSONHelper.string2json(roleInfo)), "修改角色成功");
  }

  public String RoleUpdateBatch(String arraystring)
  {
    int code = 99;
    JSONArray array = JSONArray.toJSONArray(arraystring);
    if (array.size() != 0) {
      for (int i = 0; i < array.size(); i++) {
        if (code != 0) {
          return this.rolesModel.resultMessage(2, "");
        }
        code = this.rolesModel.update((JSONObject)array.get(i));
      }
    }
    return this.rolesModel.resultMessage(code, "设置顺序或层级成功");
  }

  public String RoleSearch(String roleInfo) {
    return this.rolesModel.select(JSONHelper.string2json(roleInfo));
  }

  public String RoleDelete(String id) {
    return this.rolesModel.resultMessage(this.rolesModel.delete(id), "角色删除成功");
  }

  public String RoleBatchDelete(String ids) {
    return this.rolesModel.resultMessage(this.rolesModel.delete(ids.split(",")), "批量删除成功");
  }

  public String RolePage(int idx, int pageSize) {
    return this.rolesModel.page(idx, pageSize);
  }

  public String RolePageBy(int idx, int pageSize, String roleInfo) {
    return this.rolesModel.page(idx, pageSize, JSONHelper.string2json(roleInfo));
  }

  public String RoleSetSort(String id, int sort) {
    return this.rolesModel.resultMessage(this.rolesModel.setsort(id, sort), "设置排序值成功");
  }

  public String RoleSetFatherId(String id, String fatherid) {
    return this.rolesModel.resultMessage(this.rolesModel.setFatherId(id, fatherid), "上级用户组设置成功");
  }

  public String RoleSetPlv(String id, String plv) {
    return this.rolesModel.resultMessage(this.rolesModel.setPlv(id, plv), "权限设置成功");
  }

  public String getRole(String ugid)
  {
    return this.rolesModel.getRole(ugid);
  }
}