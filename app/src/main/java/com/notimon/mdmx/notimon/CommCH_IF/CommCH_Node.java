package com.notimon.mdmx.notimon.CommCH_IF;

import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by ctseng on 8/26/16.
 */
public class CommCH_Node implements CommCH_IF {

    HashMap<Object,CommCH_IF> PluginMap=null;

    public boolean SetDest(CommCH_IF commIf){return false;}
    public String getCHName(){return null;}



    boolean addCH(CommCH_IF CommIf)
    {
        if(CommIf.getCHName()==null||CommIf.getCHName().length()==0||
                PluginMap.containsKey(CommIf.getCHName()))return false;

        Log.v("NEW plugin", CommIf.getCHName());
        PluginMap.put(CommIf.getCHName(), CommIf);
        return true;
    }

    boolean rmCH(CommCH_IF CommIf)
    {
        CommCH_IF cIF=PluginMap.remove(CommIf.getCHName());
        if(cIF!=null)
            return true;
        return false;
    }

    @Override
    public String RecvData(final String url,final  JSONObject data,CommCH_IF from)//may from other thread
    {
        int idxOfSlash=url.indexOf('/');
        String submoduleName=null;
        String rest_url=null;
        if(idxOfSlash==-1)
        {
            submoduleName=url;
            rest_url= null;
        }
        else
        {
            submoduleName=url.substring(0,idxOfSlash);
            rest_url=url.substring(idxOfSlash+1,url.length());
        }

        CommCH_IF commIF=PluginMap.get(submoduleName);

        return SendData(rest_url, data ,commIF);

    }



    @Override
    public String SendData(final String url,final JSONObject data,CommCH_IF to){

        return to.RecvData(url,data,this);
    }
}
