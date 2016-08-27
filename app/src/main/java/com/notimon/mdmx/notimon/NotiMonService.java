package com.notimon.mdmx.notimon;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.notimon.mdmx.notimon.CommCH_IF.CommCH_IF;
import com.notimon.mdmx.notimon.JSON_Notifier.JSONOBJ_Notifier;

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
    class mLocationUpdater extends LocationUpdater
    {
        private Location lastLocation = null;
        private int GPS_Status = -1;
        mLocationUpdater(Context context) {
            super(context);
            GPS_Status = -1;
        }
        public void onLocationChanged(Location location) {
            if (location != null){
                lastLocation = location;

                GPS_Status = LocationProvider.AVAILABLE;
                statusNotifyMan.updateGPS(GPS_Status,location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if(status == LocationProvider.OUT_OF_SERVICE)
            {
                lastLocation = null;
            }
            GPS_Status = status;
            statusNotifyMan.updateGPS(GPS_Status,lastLocation);
            Log.i("SuperMap", "onStatusChanged provider:" +provider + " status: " +
                    status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            lastLocation = null;
            GPS_Status =LocationProvider.TEMPORARILY_UNAVAILABLE;;
            statusNotifyMan.updateGPS(GPS_Status,null);
            updateNotification("Location service is on...",-1);
            Log.i("SuperMap", "onProviderEnabled provider:" +provider );
        }

        @Override
        public void onProviderDisabled(String provider) {
            lastLocation = null;
            GPS_Status = LocationProvider.OUT_OF_SERVICE;
            statusNotifyMan.updateGPS(GPS_Status,lastLocation);
            Log.i("SuperMap", "onProviderDisabled provider:" +provider );
            updateNotification("Location service is off...",-1);
                /*Intent gpsOptionsIntent = new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(gpsOptionsIntent);*/

        }
        int getGPSStatus()
        {
            return GPS_Status;
        }


        Location getLastLocation()
        {
            return lastLocation;
        }

        protected void close() {
            super.close();

            GPS_Status =LocationProvider.OUT_OF_SERVICE;;

            statusNotifyMan.updateGPS(GPS_Status,null);

        }

    }



    mLocationUpdater locationUpdater = null;




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


        locationUpdater = new mLocationUpdater(this);
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
                    final Location catcheLastLocation=locationUpdater.getLastLocation();
                    if(locationUpdater!=null&&catcheLastLocation!=null)
                    {

                        final AsyncPromise GET_ACT_Promise= new AsyncPromise(

                                new AsyncPromise.PromiseCB(){//then: success condition
                                    public int _(int type, Object obj) {
                                        handlePokemonJsonInfo(catcheLastLocation, (String) obj);
                                        return 0;
                                    }
                                },
                                new AsyncPromise.PromiseCB(){//catch: error condition
                                    public int _(int type, Object obj) {
                                        Log.i(TAG, "catch::"+obj.toString());
                                        updateNotification("PokeRadar service is not reachable...",-1);
                                        return 0;
                                    }
                                });
                        Log.i(TAG, "GetNearByPokemon>>");
                        pokeAPI.GetNearByPokemon(catcheLastLocation,GET_ACT_Promise);
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
        //Log.v(">>>>","nearByPokemonInfo_str::"+nearByPokemonInfo_str);
        JSONObject nearByPokemonInfo = null;

        try {
            nearByPokemonInfo = new JSONObject(nearByPokemonInfo_str.toString());
            Latest_nearByPokemonInfo = nearByPokemonInfo;
            JSONArray pokemonArr = nearByPokemonInfo.getJSONArray("data");

            int length=pokemonArr.length();
            int PokeCount=0;


            int SelectedPokemons_len=(SelectedPokemons==null)?0:SelectedPokemons.length();

            for(int i=0;i<length;i++)
            {
                JSONObject pokemonData= (JSONObject)pokemonArr.opt(i);

                int pokemonID=pokemonData.optInt("pokemonId");
                boolean matchedPokemon=false;
                for(int j=0;j<SelectedPokemons_len;j++)
                {
                    JSONObject listEle= SelectedPokemons.optJSONObject(j);
                    if(listEle.optInt("id")==pokemonID)
                    {
                        int rating = listEle.optInt("rating");
                        if( rating == 0 )break;
                        pokemonData.put("rating",listEle.optInt("rating"));//TODO: put rating in the select list
                        matchedPokemon=true;
                        break;
                    }
                }
                if(!matchedPokemon)
                {
                    pokemonArr.put(i,null);//an workaround way to delete
                    continue;
                }


                double lon,lat;
                lon=pokemonData.optDouble("longitude");
                lat=pokemonData.optDouble("latitude");

                GeoUtil.GeoDiffToXY_approx(tmpCoor,
                        lastLocation.getLatitude(),lastLocation.getLongitude(),
                        lat,lon
                        );
                double dist=Math.sqrt(tmpCoor.x*tmpCoor.x+tmpCoor.y*tmpCoor.y);
                if(dist>300)//Out of range
                {
                    pokemonArr.put(i,null);//an workaround way to delete
                }
                else
                {
                    pokemonData.put("pokemonId",pokemonData.optInt("pokemonId"));//Pokemonradar ID starts from 1
                    pokemonData.put("x2o",tmpCoor.x);
                    pokemonData.put("y2o",tmpCoor.y);
                    if(matchedPokemon)
                        PokeCount++;
                }
            }
            Log.v(">>>>",PokeCount+"");
            updateNotification(PokeCount+":Around You",-1);

            NotiMonServIF.SendNearbyPokemonUpdateNotify(nearByPokemonInfo);

        } catch (JSONException e) {
            e.printStackTrace();
        }
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

        if(locationUpdater !=null)
            locationUpdater.close();
        locationUpdater=null;

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

    class MainComm_IF  implements CommCH_IF
    {
        String CHName;
        CommCH_IF destination;
        MainComm_IF(String CHName)
        {
            this.CHName=CHName;
        }
        @Override
        public String getCHName() {
            return CHName;
        }


        @Override
        public boolean SetDest(CommCH_IF commIf) {
            destination=commIf;
            return true;
        }

        @Override
        public String RecvData(String url, JSONObject data, CommCH_IF from) {

            Log.v("ServRecvData::",url);
            String[] urlArr = url.split("/");

            if(urlArr[0].contentEquals("GET"))
            {
                if(urlArr.length<3)return null;

                if(urlArr[2].contentEquals("NearByPokemon"))
                {
                    return SendData(url+"/RSP",Latest_nearByPokemonInfo,this.destination);
                }



                return null;
            }


            if(urlArr[0].contentEquals("GET"))
            {
                if(urlArr.length<2)return null;

                if(urlArr[1].contentEquals("NearByPokemon"))
                {
                    return SendData(url+"/RSP",Latest_nearByPokemonInfo,this.destination);
                }



                return null;
            }

            if(urlArr[0].contentEquals("GPS"))
            {
                if(urlArr.length<3)return null;

                if(urlArr[2].contentEquals("true"))
                {
                    if(locationUpdater ==null)
                        locationUpdater = new mLocationUpdater(NotiMonService.this);
                }
                else
                {
                    if(locationUpdater !=null)
                    {
                        locationUpdater.close();
                    }
                    locationUpdater =null;
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

        @Override
        public String SendData(String url, JSONObject data, CommCH_IF to) {
            if(to == null)return null;
            return to.RecvData(url,data,this);
        }



        public String SendPokemonUpdateNotify(int number) {

            try {
                long time= System.currentTimeMillis();
                JSONObject jobj = new JSONObject();
                jobj.put("number",number);
                jobj.put("time",time);
                return SendData("PokemonUpdateNotify",jobj,this.destination);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return null;
        }

        public String SendNearbyPokemonUpdateNotify(JSONObject pokemonInfo) {

            return SendData("PokemonUpdateNotify",pokemonInfo,this.destination);
        }
    }


    private class StatusNotifyMan
    {
        JSONOBJ_Notifier sysNotifier;
        JSONObject GPS_status;
        JSONObject Orientation_status;
        StatusNotifyMan(JSONOBJ_Notifier sysNotifier)
        {
            this.sysNotifier=sysNotifier;
            GPS_status = new JSONObject();
            Orientation_status = new JSONObject();
            try {
                sysNotifier.put("GPS_status",GPS_status);
                sysNotifier.put("Orientation_status",Orientation_status);
                updateGPS(locationUpdater.getGPSStatus(),null);
                updateOrientation(0,null);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public synchronized  void updateGPS(int status,Location loca) {

            try {
                GPS_status.put("status",status);

                if(loca!=null) {
                    GPS_status.put("provider", loca.getProvider());
                    GPS_status.put("longitude", loca.getLongitude());
                    GPS_status.put("latitude", loca.getLatitude());
                    GPS_status.put("altitude", loca.getAltitude());
                    GPS_status.put("accuracy", loca.getAccuracy());
                }
                else
                {
                    GPS_status.remove("longitude");
                    GPS_status.remove("latitude");
                    GPS_status.remove("altitude");
                    GPS_status.remove("accuracy");
                    GPS_status.remove("provider");
                }
                sysNotifier.put("GPS_status",GPS_status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        public synchronized  void updateOrientation(int status,SensorEvent event) {

            try {
                Orientation_status.put("status",status);
                if(event!=null) {
                    Orientation_status.put("SensorType", event.sensor.getName());
                    Orientation_status.put("x", event.values[0]);
                    Orientation_status.put("y", event.values[1]);
                    Orientation_status.put("z", event.values[2]);
                }
                else
                {
                    Orientation_status.remove("SensorType");
                    Orientation_status.remove("x");
                    Orientation_status.remove("y");
                    Orientation_status.remove("z");
                }

                sysNotifier.put("Orientation_status",Orientation_status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }
}