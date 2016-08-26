package com.notimon.mdmx.notimon.JSON_Notifier;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

/**
 * Created by MDMx on 7/28/2016.
 */
public class JSONOBJ_Notifier extends JSONObject implements JSON_Notifier_If{

    public JSONObject put(String name, boolean value) throws JSONException {
        JSONObject obj = super.put(name,value);
        noticeParent(null,name,value);
        return obj;
    }

    public JSONObject put(String name, double value) throws JSONException {
        JSONObject obj = super.put(name,value);
        noticeParent(null,name,value);
        return obj;
    }

    public JSONObject put(String name, int value) throws JSONException {
        JSONObject obj = super.put(name,value);
        noticeParent(null,name,value);
        return obj;
    }

    public JSONObject put(String name, long value) throws JSONException {
        JSONObject obj = super.put(name,value);
        noticeParent(null,name,value);
        return obj;
    }

    public Object remove(String name) {
        Object obj = super.remove(name);
        if(obj!=null) {
            if(obj instanceof JSON_Notifier_If)
            {
                ((JSON_Notifier_If) obj).setParent(null,null);
            }
            noticeParent(null, name, "undefined");
        }
        return obj;
    }

    public JSONObject put(String name, Object value) throws JSONException {

        if(value instanceof JSON_Notifier_If)
        {
            JSON_Notifier_If jobjN = (JSON_Notifier_If)value;
            jobjN.setParent(this,name);
        }
        JSONObject obj = super.put(name,value);

        noticeParent(null,name,value);
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
            key = "."+key;
        if(parent instanceof JSONARR_Notifier)
            parent.noticeParent(this,"["+Name+"]"+key,value);
        else if(parent instanceof JSONOBJ_Notifier)
            parent.noticeParent(this,"."+Name+key,value);

    }


    public static Object GetCachedData(JSONObject obj,String Path)
    {
        if(Path.charAt(0)=='.')
            Path = Path.substring(1);
        Path =Path.replace("]",".");
        String[] Sections = Path.split("\\.");

        Object jobj = obj;

        try{
            for(String Sec :Sections)
            {
                if(Sec.contains("["))
                {
                    String[] ArrSecs = Sec.split("\\[");
                    if(ArrSecs[0]!=null)
                    {
                        jobj=((JSONObject)jobj).opt(ArrSecs[0]);
                    }


                    for(int i=1; i<ArrSecs.length;i++){

                        int idx=Integer.parseInt(ArrSecs[i]);
                        jobj=((JSONArray)jobj).opt(idx);
                    }
                }
                else
                {

                    jobj=((JSONObject)jobj).opt(Sec);
                    //this.get();
                }
            }

            return jobj;
        }
        catch (Exception e)
        {
            Log.i("GetCachedData___e_",e.toString());
            return null;
        }
    }
}
