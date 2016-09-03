package com.notimon.mdmx.notimon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.notimon.mdmx.notimon.CommCH_IF.CommCH_IF;
import com.notimon.mdmx.notimon.CommCH_IF.CommCH_Node;
import com.notimon.mdmx.notimon.JSON_Notifier.JSONOBJ_Notifier;
import com.notimon.mdmx.notimon.Plugins.DevOrienation;
import com.notimon.mdmx.notimon.Plugins.LocationProvidorCH;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by MDMx on 7/24/2016.
 */

public class NotiMonService extends Service {

    private static final String TAG = "NotiMonService";

    private boolean isRunning  = false;


    private JSONArray SelectedPokemons=null;

    private int iCounter;

    PokeRadarAPI pokeAPI;




    Thread periodicTask;
    void SetiCounter(int value)
    {
        iCounter = value;
    }


    public JSONOBJ_Notifier getSysStatusNotifier()
    {
        return systemStatusJObj;
    }

    JSONOBJ_Notifier  systemStatusJObj = null;
    StatusNotifyMan statusNotifyMan = null;

    public String loadJSONFromAsset(String assetPath) {
        String json = null;
        try {
            InputStream is = this.getAssets().open(assetPath);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return json;
    }

    JSONArray loadIdMapFromWebUIStore()
    {
        try {
            SharedPreferences settings = getSharedPreferences("WebUIAPP_STORAGE", 0);
            String storeStr=settings.getString("store",null);

            if(storeStr==null)
                return null;
            JSONObject obj=new JSONObject(storeStr);
            JSONArray seleMap = (JSONArray)JSONOBJ_Notifier.GetCachedData(obj,".reduxStore.AppData.PokemonSelectData.pokemonSelectList");
            JSONArray seleMap_x = new JSONArray();

            int seleMapLen=seleMap.length();
            for(int i=0;i<seleMapLen;i++)
            {
                JSONObject ele = seleMap.getJSONObject(i);
                if(ele.optInt("rating")>0)
                    seleMap_x.put(ele);
            }
            return seleMap_x;

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return null;
    }
    @Override
    public void onCreate() {
       //loadJSONFromAsset();
        SelectedPokemons = loadIdMapFromWebUIStore();


        systemStatusJObj = new JSONOBJ_Notifier();
        statusNotifyMan = new StatusNotifyMan(systemStatusJObj);
        iCounter = 0;
        isRunning = true;
        pokeAPI = new PokeRadarAPI();
        runAsForeground();
        periodicTask = get_periodicTask();
        periodicTask.start();

        //OrientationSensorMan = new OrientationSensorEventManager();

        updateNotification(this.getString(R.string.app_name),-1);
        Log.i(TAG, "Service onCreate");
    }


    Thread get_periodicTask()
    {

        //Always write your long running tasks in a separate thread, to avoid ANR
        return new Thread(new Runnable() {
            @Override
            public void run() {

                while (true) {

                    try {
                        Thread.sleep(10000);
                    } catch (Exception e) {
                        break;
                    }
                    if(isRunning){
                        Log.i(TAG, "Service running"+ (iCounter++));
                    }
                    else
                    {
                        break;
                    }


                }
            }
        });
    }

    JSONObject Latest_nearByPokemonInfo = null;
    GeoUtil.Coor tmpCoor= new GeoUtil.Coor();
    private synchronized int handlePokemonJsonInfo(Location lastLocation, String nearByPokemonInfo_str) {
        JSONObject nearByPokemonInfo = null;

        return 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "Service onStartCommand");
        return Service.START_STICKY;
    }




    private NotificationManager mNotificationManager =null;

    NotificationCompat.Builder mBuilder = null;
    private void runAsForeground(){
        if(mNotificationManager == null) {
            mNotificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }

        mBuilder = new NotificationCompat.Builder(this);
        Notification notification=mBuilder.build();

        //startForeground(54545, notification);

    }

    private void updateNotification(String message, int progress) {

        if(mNotificationManager == null)
            return;

        final Intent notificationIntent = new Intent(getApplicationContext(),
                MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.
                setContentText(message).
                setSmallIcon(R.drawable.update).
                setCategory(message).setContentTitle(getString(R.string.app_name)).setContentIntent(pendingIntent);
        if(progress!=-1)
        {
            mBuilder.setProgress(1000,progress,false);
        }

        //startForeground(54545, mBuilder.build());
        mNotificationManager.notify(54545, mBuilder.build());
    }


    @Override
    public IBinder onBind(Intent arg0) {
        Log.i(TAG, "Service onBind");
        return new _Binder();
    }
    @Override

    public boolean onUnbind(Intent intent) {

        Log.i(TAG, "Service onUnbind");return false;
    }
    @Override

    public void onRebind(Intent intent) {
        Log.i(TAG, "Service onRebind");
    }

    @Override
    public void onDestroy() {
        periodicTask.interrupt();

        periodicTask=null;
        mNotificationManager.cancelAll();
        mNotificationManager = null;
        isRunning = false;
        Log.i(TAG, "Service onDestroy");
        stopSelf();
    }

    public class _Binder extends Binder {
        NotiMonService getService() {
            return NotiMonService.this;
        }
    }


    Context context;
    public void setContext(Context context) {
        if(context!=null)
        {
            Log.i("NotiMonService","runs under context:"+context.toString());
        }
        else
        {

            Log.i("NotiMonService","runs under no context:");
        }
        this.context=context;
    }

    public CommCH_IF getCommCH_IF()
    {
        return NotiMonServIF;
    }


    MainComm_IF NotiMonServIF=new MainComm_IF("NotiMonServIF");


    class MainComm_IF  extends CommCH_Node
    {
        String CHName;

        DevOrienation devOriCH =new DevOrienation(NotiMonService.this);
        LocationProvidorCH locaCH=new LocationProvidorCH(NotiMonService.this);
        MainComm_IF(String CHName)
        {
            this.CHName=CHName;
            addCH(devOriCH.getCommCH_IF());//Add device orientation submodule
            addCH(locaCH.getCommCH_IF());//Add location submodule
        }
        @Override
        public String getCHName() {
            return CHName;
        }

        @Override
        public String SendData(final String url,final JSONObject data){

            //Hijeck data in subCH



            return super.SendData(url,data);
        }

        @Override
        public String RecvData_default(String url, JSONObject data, CommCH_IF from) {

            Log.v("ServRecvData::",url);
            String[] urlArr = url.split("/");

            if(urlArr[0].contentEquals("GET"))
            {
                if(urlArr.length<3)return null;

                if(urlArr[2].contentEquals("NearByPokemon"))
                {
                }



                return null;
            }
            if(urlArr[0].contentEquals("pokemonselect"))
            {
                if(urlArr.length<2)return null;

                if(urlArr[1].contentEquals("set"))
                {
                    SelectedPokemons=data.optJSONArray("list");
                    Log.v("pokemonselect::",SelectedPokemons.toString());
                }
                return null;
            }

            Log.i(CHName+"_RECV",url+">>"+data.toString());
            JSONObject jobjx = new JSONObject();
            try {
                jobjx.put(CHName,from);

                return jobjx.toString();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

    }


    private class StatusNotifyMan
    {
        JSONOBJ_Notifier sysNotifier;
        StatusNotifyMan(JSONOBJ_Notifier sysNotifier)
        {
            this.sysNotifier=sysNotifier;
        }

    }
}