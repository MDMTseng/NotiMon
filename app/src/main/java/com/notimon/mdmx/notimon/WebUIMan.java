package com.notimon.mdmx.notimon;

import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.notimon.mdmx.notimon.CommCH_IF.CommCH_IF;
import com.notimon.mdmx.notimon.CommCH_IF.CommCH_Node;
import com.notimon.mdmx.notimon.JSON_Notifier.JSONARR_Notifier;
import com.notimon.mdmx.notimon.JSON_Notifier.JSONOBJ_Notifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;

/**
 * Created by ctseng on 9/21/15.
 */
public class WebUIMan {

    WebView webView=null;

    WebAppInterface WebUIManCommIf=null;

    CommCH_IF BTMasterCommIf=null;
    CommCH_IF BTSlaveCommIf=null;
    String jSIFName;

    String JsDataFuncCallName=null;
    String JsSysInfoFuncCallName=null;

    CommCH_Node chNode = null;

    enum IncommingDataWhich{
        MDMJsData,
        MDMJsSysInfo
    };




    StatusNotifyMan statusNotiMan=null;
    WebUIMan(WebView webView,String JsIfName,String EntryUrl,JSONOBJ_Notifier sysNotifier)
    {
        statusNotiMan = new StatusNotifyMan(sysNotifier);
        WebUIManCommIf=new WebAppInterface();


        chNode = new CommCH_Node(){

            @Override
            public String SendData(final String url,final JSONObject data){
                WebUIManCommIf.ToWeb(url,data);
                return null;
            }
        };
        InitWebUIMan(webView,JsIfName,EntryUrl);

    }

    boolean InitWebUIMan(WebView webView,String JsIfName,String EntryUrl)
    {

        if(this.webView!=null)return false;

        this.jSIFName=JsIfName;


        JsDataFuncCallName="javascript:"+jSIFName+".ToWeb('";
        JsSysInfoFuncCallName="javascript:"+jSIFName+".SysInfo('";

        this.webView=webView;

        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setAllowFileAccess(true);
        webSettings.setAllowContentAccess(true);
        webSettings.setAllowFileAccessFromFileURLs(true);
        webSettings.setAllowUniversalAccessFromFileURLs(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);

        Log.v("WebUIMan",">>"+this.jSIFName);
        webView.setWebViewClient(new WebViewClient() {

            public void onPageFinished(WebView view, String url) {
                statusNotiMan.setUrl(url);
                Log.v(this.getClass().getName(), "onPageFinished!!!");
            }
        });


        webView.loadUrl(EntryUrl);


        webView.addJavascriptInterface(WebUIManCommIf, jSIFName);
        return true;
    }


    boolean CleanWebUIMan()
    {
        if(this.webView==null)return false;
        webView.clearHistory();
        webView.clearCache(true);
        webView.loadUrl("about:blank");
        webView.freeMemory();
        webView.pauseTimers();
        webView = null;

        return true;
    }


    public CommCH_Node Get_CommIf()
    {
        return chNode;
    }


    private Handler msgHandler = new Handler( ){
        @Override
        public void handleMessage(Message inputMessage) {
            if(webView == null)return;

            IncommingDataWhich which=IncommingDataWhich.values()[inputMessage.what];
            switch(which) {
                case MDMJsData:

                    statusNotiMan.setJsTrafficToWebCount();
                    String data=JsDataFuncCallName+((JSONObject)inputMessage.obj).toString()+"')";

                    Log.v("handleMessage",data);
                    webView.loadUrl(data);
                    break;
                case MDMJsSysInfo:
                    webView.loadUrl(JsSysInfoFuncCallName+(String)inputMessage.obj+"')");
                    break;
            }
        }
    };

    public class WebAppInterface {

        @JavascriptInterface
        public String FromWeb(String DATA) {
            //Log.v("WebAppInterface",">>FromWeb>>"+DATA);
            statusNotiMan.setJsTrafficFromWebCount();
            //Log.v(this.getClass().getName(), DATA);
            try {
                JSONObject jobj=new JSONObject(DATA);
                String url=jobj.optString("url", null);
                JSONObject data=jobj.optJSONObject("data");

                return chNode.RecvData(url, data, null);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }


        public String ToWeb(String url,JSONObject DATA) {
            JSONObject jobj=new JSONObject();
            try {
                jobj.put("url",url);
                jobj.put("data",DATA);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }


            Message msg=msgHandler.obtainMessage();

            msg.obj=jobj;

            msg.what=IncommingDataWhich.MDMJsData.ordinal();

            msgHandler.sendMessage(msg);

            return null;
        }
    }



    private class StatusNotifyMan
    {
        JSONOBJ_Notifier sysNotifier;
        JSONARR_Notifier WebUIPlugins;
        StatusNotifyMan(JSONOBJ_Notifier sysNotifier)
        {
            this.sysNotifier=sysNotifier;
            WebUIPlugins = new JSONARR_Notifier();
            try {
                sysNotifier.put("state","init");
                sysNotifier.put("url",null);
                /*sysNotifier.put("js_traffic_toWeb",0);
                sysNotifier.put("js_traffic_fromWeb",0);*/



                sysNotifier.put("Plugins",WebUIPlugins);

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void setState(String state)
        {
            try {
                sysNotifier.put("state",state);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public void addNewPlugin(String plugin)
        {
            for(int i=0;i<WebUIPlugins.length();i++)
            {
                if(WebUIPlugins.opt(i)==null) {
                    try {
                        WebUIPlugins.put(i, plugin);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    return;
                }
            }

            WebUIPlugins.put(plugin);


        }
        public void removePlugin(String plugin)
        {
            for(int i=0;i<WebUIPlugins.length();i++)
            {
                Log.v("removePlugin::--", "WebUIPlugins.optString("+i+")=="+WebUIPlugins.optString(i));
                if(plugin.contentEquals(WebUIPlugins.optString(i)))
                    try {
                        WebUIPlugins.put(i,null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }


        }

        public void setUrl(String url)
        {
            try {
                sysNotifier.put("url",url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        int jsTrafficCountToWeb=0;

        public void setJsTrafficToWebCount()
        {
           /* try {
                jsTrafficCountToWeb++;
                sysNotifier.put("js_traffic_toWeb",jsTrafficCountToWeb);
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
        }
        int jsTrafficCountFromWeb=0;

        public void setJsTrafficFromWebCount()
        {
           /* try {
                jsTrafficCountFromWeb++;
                sysNotifier.put("js_traffic_fromWeb",jsTrafficCountFromWeb);
            } catch (JSONException e) {
                e.printStackTrace();
            }*/
        }

    }
}
