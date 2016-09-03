package com.notimon.mdmx.notimon.Plugins;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import com.notimon.mdmx.notimon.CommCH_IF.CommCH_IF;
import com.notimon.mdmx.notimon.JSON_Notifier.JSONOBJ_Notifier;
import com.notimon.mdmx.notimon.WebUIMan;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ctseng on 8/26/16.
 */
public class DevOrienation {




    Context context;
    enum sensorStatus{
        off,
        prepare,
        on
    }

    CommIF commIF=new CommIF("Orientation");
    public CommCH_IF getCommCH_IF()
    {
        return commIF;
    }



    OrientationSensorEventManager OrientationSensorMan = null;


    public DevOrienation(Context context)
    {
        this.context=context;
    }

    public boolean start(Context context)
    {
        if( OrientationSensorMan==null )
        {
            OrientationSensorMan = new OrientationSensorEventManager();
            return true;
        }

        return false;

    }

    public sensorStatus getCurrentStatus()
    {
        return OrientationSensorMan.status;

    }

    public boolean stop(Context context)
    {
        if( OrientationSensorMan==null )
        {
            return false;
        }

        OrientationSensorMan.deInit_Sensor();
        OrientationSensorMan=null;
        return true;

    }



    class CommIF  implements CommCH_IF
    {
        String CHName;
        CommCH_IF destination;

        JSONObject Orientation_status;
        CommIF(String CHName)
        {
            Orientation_status = new JSONObject();
            try {
                Orientation_status.put("status",sensorStatus.off.ordinal());
            } catch (JSONException e) {
                e.printStackTrace();
            }
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
            String []urlArr = url.split("/");

            Log.i("urlArr.length",urlArr.length+"");
            if(urlArr.length == 1)
            {
                if(urlArr[0]=="enable")
                {
                    Log.i("DevOrienation",url);
                    start(context);
                }else
                if(urlArr[0]=="disable"){
                    stop(context);

                }
            }
            Log.i("DevOrienation",url);
            return null;
        }

        @Override
        public String SendData(String url, JSONObject data) {
            return destination.SendData(url,data);
        }


        public void updateOrientation(sensorStatus status, SensorEvent event )
        {
            try {
                Orientation_status.put("status",status.ordinal());
                if(event==null) {
                    Orientation_status.remove("SensorType");
                    Orientation_status.remove("x");
                    Orientation_status.remove("y");
                    Orientation_status.remove("z");
                }else
                {
                    Orientation_status.put("SensorType", event.sensor.getName());
                    Orientation_status.put("x", event.values[0]);
                    Orientation_status.put("y", event.values[1]);
                    Orientation_status.put("z", event.values[2]);
                }
                SendData("value", Orientation_status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }


    class OrientationSensorEventManager implements SensorEventListener
    {

        sensorStatus status;
        OrientationSensorEventManager()
        {
            init_Sensor();
        }
        private SensorManager mSensorManager;
        private Sensor mAccelerometer;
        void init_Sensor()
        {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
            if (mAccelerometer != null){
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                status = sensorStatus.prepare;
                commIF.updateOrientation(status, null);
            }
            else {
            }
        }

        void deInit_Sensor()
        {
            status = sensorStatus.off;
            mSensorManager.unregisterListener(this);
            mAccelerometer = null;
            mSensorManager = null;
            commIF.updateOrientation(status, null);
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            status = sensorStatus.on;
            commIF.updateOrientation(status, event);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    }



}
