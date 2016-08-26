package com.notimon.mdmx.notimon.JSON_Notifier;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Created by MDMx on 7/28/2016.
 */
public class JSONARR_Notifier extends JSONArray implements JSON_Notifier_If{

    public JSONArray put(boolean value) {
        JSONArray obj = super.put(value);
        noticeParent(null,""+(obj.length()-1),value);
        return obj;
    }

    public JSONArray put(double value) throws JSONException {
        JSONArray obj = super.put(value);
        noticeParent(null,""+(obj.length()-1),value);
        return obj;
    }

    public JSONArray put(int value) {
        JSONArray obj = super.put(value);
        noticeParent(null,""+(obj.length()-1),value);
        return obj;
    }

    public JSONArray put(long value) {
        JSONArray obj = super.put(value);
        noticeParent(null,""+(obj.length()-1),value);
        return obj;
    }
    public JSONArray put(Object value) {
        JSONArray obj = super.put(value);
        if(value instanceof JSON_Notifier_If)
        {
            JSON_Notifier_If jobjN = (JSON_Notifier_If)value;
            jobjN.setParent(this,""+(obj.length()-1));
        }
        noticeParent(null,""+(obj.length()-1),value);
        return obj;
    }

    public JSONArray put(int index, boolean value) throws JSONException {
        noticeParent(null,""+index,value);
        JSONArray obj = super.put(index,value);
        return obj;
    }

    public JSONArray put(int index, double value) throws JSONException {
        JSONArray obj = super.put(index,value);
        noticeParent(null,""+index,value);
        return obj;
    }

    public JSONArray put(int index, int value) throws JSONException {
        JSONArray obj = super.put(index,value);
        noticeParent(null,""+index,value);
        return obj;
    }

    public JSONArray put(int index, long value) throws JSONException {
        JSONArray obj = super.put(index,value);
        noticeParent(null,""+index,value);
        return obj;
    }

    public JSONArray put(int index, Object value) throws JSONException {
        if(value instanceof JSON_Notifier_If)
        {
            JSON_Notifier_If jobjN = (JSON_Notifier_If)value;
            jobjN.setParent(this,""+index);
        }
        JSONArray obj = super.put(index,value);

        noticeParent(null,""+index,value);
        return obj;
    }
    JSON_Notifier_If parent = null;
    String Name = null;
    @Override
    public void setParent(JSON_Notifier_If parent, String nameForSelf) {
        this.parent = parent;
        Name = nameForSelf;
    }

    @Override
    public void noticeParent(Object from,String key, Object value) {
        if(parent == null)return;

        if(from == null)
            key = "["+key+"]";

        if(parent instanceof JSONARR_Notifier)
            parent.noticeParent(this,"["+Name+"]"+key,value);
        else if(parent instanceof JSONOBJ_Notifier)
            parent.noticeParent(this,"."+Name+key,value);

    }
}
