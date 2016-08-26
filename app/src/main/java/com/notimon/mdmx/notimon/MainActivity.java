package com.notimon.mdmx.notimon;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.webkit.WebView;

import com.notimon.mdmx.notimon.JSON_Notifier.JSONARR_Notifier;
import com.notimon.mdmx.notimon.JSON_Notifier.JSONOBJ_Notifier;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class MainActivity extends AppCompatActivity {

    ServiceManger<NotiMonService> servMan = null;

    MainComm_IF MainIF=new MainComm_IF("MainIF");

    class MainComm_IF  implements WebUIMan.CommCH_IF
    {
        String CHName;
        WebUIMan.CommCH_IF destination;
        MainComm_IF(String CHName)
        {
            this.CHName=CHName;
        }
        @Override
        public String getCHName() {
            return CHName;
        }

        @Override
        public boolean SetDest(WebUIMan.CommCH_IF commIf) {
            destination=commIf;
            return true;
        }

        @Override
        public String RecvData(String url, JSONObject data, WebUIMan.CommCH_IF from) {


            String []urlArr = url.split("/");

            if(url.startsWith("NotiMonService/enable/"))
            {
                if(urlArr[2].contentEquals("true"))
                {

                    Log.i("NotiMonService/enable/ ", "servMan:"+servMan.toString());
                    if(servMan.GetService()!=null)return null;
                    servMan.startBindService(Context.BIND_NOT_FOREGROUND);
                }
                else if(urlArr[2].contentEquals("false"))
                {
                    if(servMan.GetService()==null)return null;

                    servMan.GetService().setContext(null);
                    servMan.unbindService();
                    servMan.stopService();
                }
                return null;
            }

            if(url.contentEquals("WebUIAPP_Store/POST"))
            {
                SharedPreferences settings = getSharedPreferences("WebUIAPP_STORAGE", 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("store",data.toString());
                Log.i("WebUIAPP_Store/POST>> ", "data:"+data.toString());
                editor.commit();//save WebUI store
                return null;
            }


            Log.i(CHName+"_RECV",url+">>"+data.toString());
            return null;
        }

        @Override
        public String SendData(String url, JSONObject data, WebUIMan.CommCH_IF to) {

            return to.RecvData(url,data,this);
        }


        public String SendSystemStatusChange(String systemUrl ,Object data) {
            try {
                JSONObject obj = new JSONObject();
                obj.put("sysUrl",systemUrl);
                obj.put("value",data);
                return destination.RecvData("SystemStatusChange",obj,this);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String SendWebUIStore() {
            JSONObject obj= new JSONObject();
            try {
                SharedPreferences settings = getSharedPreferences("WebUIAPP_STORAGE", 0);
                String storeStr=settings.getString("store",null);
                Log.i("SendWebUIStore ", "storeStr:"+storeStr);

                if(storeStr==null)
                    obj=new JSONObject();
                else
                    obj=new JSONObject(storeStr);

                return destination.RecvData("InitWebUI",obj,this);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String GetWebUI_APP_Store() {
            return destination.RecvData("WebUIAPP_Store/GET",null,this);
        }
    }
    WebUIMan wvMan;


    class SystemState_Notifier extends JSONOBJ_Notifier
    {
        public boolean en_notify = false;

        @Override
        public void noticeParent(Object from,String key, Object value) {
            if(!en_notify)return;
            if(from == null)
                key = "."+key;

            MainIF.SendSystemStatusChange(key,value);

            if(key.contentEquals(".webUI.url"))
            {
                WebUIReady();
            }
            //Log.i("systemStatusJObj>>",this.GetCachedData(key).toString());

        }


    }

    SystemState_Notifier systemStatusJObj = null;
    StatusNotifyMan statusNotifyMan = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        WebView WView = (WebView)  findViewById(R.id.webView);


        systemStatusJObj = new SystemState_Notifier();

        try {
            JSONOBJ_Notifier activity = new JSONOBJ_Notifier();
            JSONOBJ_Notifier webUI = new JSONOBJ_Notifier();
            systemStatusJObj.put("activity",activity);
            systemStatusJObj.put("webUI",webUI);
            statusNotifyMan = new StatusNotifyMan(activity);
            wvMan = new WebUIMan(WView,"WebViewIf","file:///android_asset/MainUI/index.html",webUI);
            wvMan.AddPlugin(MainIF);
            systemStatusJObj.en_notify = true;


            //Log.i("systemStatusJObj>>",systemStatusJObj.GetCachedData("activity.APP_Info").toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();  // Always call the superclass method first




    }


    void WebUIReady()
    {

        servMan = new ServiceManger<NotiMonService>(this,NotiMonService.class)
        {
            void onServiceConnected(NotiMonService service)
            {
                statusNotifyMan.setServiceRunning(true);
                wvMan.AddPlugin(service.getCommCH_IF());
                service.setContext(MainActivity.this);
                try {
                    systemStatusJObj.put("service",this.GetService().getSysStatusNotifier());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Log.i("ServiceManger NotiMon", "Service:"+service.getPackageName()+" onConnected");
            }
            void onServiceDisconnected(ComponentName name)
            {
                statusNotifyMan.setServiceRunning(false);

                wvMan.RemovePlugin(this.GetService().getCommCH_IF());
                this.GetService().setContext(null);
                systemStatusJObj.remove("service");
                Log.i("ServiceManger NotiMon", "Service:"+name+" onDestroy");
            }
        };
        MainIF.SendWebUIStore();
        MainIF.SendSystemStatusChange(".service",JSONOBJ_Notifier.GetCachedData(systemStatusJObj,".service"));
        PermissionRequestSM(0, true);
    }


    @Override
    public void onPause() {
        MainIF.GetWebUI_APP_Store();
        Log.i("onPause",":onPauseonPauseonPauseonPause");
        super.onPause();
    }

    @Override
    public void onBackPressed() {
    }

    @Override
    public void onDestroy() {
        wvMan.CleanWebUIMan();
        servMan.unbindService();
        super.onDestroy();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        boolean fulfuillAllPermissions = true;
        for(int i=0; i<permissions.length ; i++)//str : permissions)
        {
            statusNotifyMan.setPermission(permissions[i].replace(".","_"),grantResults[i]==PackageManager.PERMISSION_GRANTED);
            fulfuillAllPermissions &= (grantResults[i]==PackageManager.PERMISSION_GRANTED);
            Log.i("onReqPermissionsRes",permissions[i] +":"+ (grantResults[i]==PackageManager.PERMISSION_GRANTED));

        }
        Log.i("fulfuillAllPermissions",":"+fulfuillAllPermissions);
        PermissionRequestSM(requestCode,fulfuillAllPermissions);
    }

    void PermissionRequestSM(int state, boolean cascadeResult)
    {
        if(cascadeResult==false)
        {
            Log.v("PermissionRequestSM","ERROR: permission is not enough");
        }
        switch(state)
        {
            case 0:ActivityCompat.requestPermissions( this, new String[] {  Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET  },state+1);
                break;
            default:
                if(cascadeResult&&false)
                    servMan.startBindService(Context.BIND_NOT_FOREGROUND);
                break;
        }
    }



    private class StatusNotifyMan
    {
        JSONOBJ_Notifier sysNotifier;
        JSONObject APP_Info;
        JSONOBJ_Notifier permissions;
        StatusNotifyMan(JSONOBJ_Notifier sysNotifier)
        {
            this.sysNotifier=sysNotifier;
            permissions = new JSONOBJ_Notifier();
            APP_Info = new JSONObject();
            try {
                sysNotifier.put("APP_Info",APP_Info);
                APP_Info.put("version","v0.0.1");
                APP_Info.put("pkgName",MainActivity.this.getPackageName());
                APP_Info.put("Name",getApplicationName(MainActivity.this));




                sysNotifier.put("serviceRunning",false);
                sysNotifier.put("permissions",permissions);


            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public  String getApplicationName(Context context) {
            int stringId = context.getApplicationInfo().labelRes;
            return context.getString(stringId);
        }

        public void setServiceRunning(boolean isRunning)
        {
            try {
                sysNotifier.put("serviceRunning",isRunning);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }


        public void setPermission(String permission,boolean granted)
        {
            try {
                permissions.put(permission,granted);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public JSONOBJ_Notifier getNotifier()
        {
            return sysNotifier;
        }

    }
}
