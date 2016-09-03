package com.notimon.mdmx.notimon.Plugins;

import android.content.Context;
import android.location.Location;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

import com.notimon.mdmx.notimon.CommCH_IF.CommCH_IF;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by ctseng on 8/30/16.
 */
public class LocationProvidorCH {



    CommIF commIF=new CommIF("Location");
    public CommCH_IF getCommCH_IF()
    {
        return commIF;
    }


    enum LocaStatus{
        off,
        prepare,
        on
    }



    class mLocationUpdater extends LocationUpdater
    {
        private Location lastLocation = null;
        private LocaStatus Location_status =LocaStatus.off;
        public mLocationUpdater(Context context) {
            super(context);
            Location_status =LocaStatus.off;
        }
        public void onLocationChanged(Location location) {
            if (location != null){
                lastLocation = location;

                Location_status = LocaStatus.on;
                commIF.updateGPS(Location_status,location);
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if(status == LocationProvider.OUT_OF_SERVICE)
            {
                lastLocation = null;
            }
            Location_status = LocaStatus.prepare;

            commIF.updateGPS(Location_status,lastLocation);


            Log.i("SuperMap", "onStatusChanged provider:" +provider + " status: " +
                    status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            lastLocation = null;
            Location_status =LocaStatus.off;
            Log.i("SuperMap", "onProviderEnabled provider:" +provider );
        }

        @Override
        public void onProviderDisabled(String provider) {
            lastLocation = null;
            Location_status =LocaStatus.off;
            commIF.updateGPS(Location_status,lastLocation);
            Log.i("SuperMap", "onProviderDisabled provider:" +provider );
            //updateNotification("Location service is off...",-1);
                /*Intent gpsOptionsIntent = new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(gpsOptionsIntent);*/

        }
        LocaStatus getGPSStatus()
        {
            return Location_status;
        }


        Location getLastLocation()
        {
            return lastLocation;
        }

        protected void close() {
            super.close();

            Location_status =LocaStatus.off;

            commIF.updateGPS(Location_status,null);

        }

    }

    public boolean start(Context context)
    {
        if( locationUpdater==null )
        {
            locationUpdater = new mLocationUpdater(context);
            return true;
        }

        return false;
    }

    public boolean stop(Context context)
    {
        if( locationUpdater==null )
        {
            return false;
        }

        locationUpdater.close();
        locationUpdater=null;
        return true;

    }

    mLocationUpdater locationUpdater = null;


    public LocationProvidorCH(Context context)
    {
        //locationUpdater = new mLocationUpdater(context);
    }


    class CommIF  implements CommCH_IF
    {
        String CHName;
        CommCH_IF destination;

        JSONObject Location_status;
        CommIF(String CHName)
        {
            Location_status = new JSONObject();
            try {
                Location_status.put("status",LocaStatus.off.ordinal());
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

            Log.i("LocationProvidorCH",url+">>"+data.toString());
            return null;
        }

        @Override
        public String SendData(String url, JSONObject data) {
            return destination.SendData(url,data);
        }

        public synchronized  void updateGPS(LocaStatus status,Location loca) {

            try {
                Location_status.put("status",status.ordinal());

                if(loca!=null) {
                    Location_status.put("provider", loca.getProvider());
                    Location_status.put("longitude", loca.getLongitude());
                    Location_status.put("latitude", loca.getLatitude());
                    Location_status.put("altitude", loca.getAltitude());
                    Location_status.put("accuracy", loca.getAccuracy());
                }
                else
                {
                    Location_status.remove("longitude");
                    Location_status.remove("latitude");
                    Location_status.remove("altitude");
                    Location_status.remove("accuracy");
                    Location_status.remove("provider");
                }
                SendData("Location_status",Location_status);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

    }


}
