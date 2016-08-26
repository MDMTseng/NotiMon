package com.notimon.mdmx.notimon;

import android.location.Location;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by MDMx on 8/7/2016.
 */

public class PokeRadarAPI {

    int GetNearByPokemon(final Location location, AsyncPromise promise)
    {
        return  GetNearByPokemon(
                location.getLatitude()-0.01 ,location.getLatitude()+0.01,
                location.getLongitude()-0.01,location.getLongitude()+0.01,promise);
    }
    int GetNearByPokemon(double minLatitude,double maxLatitude,double minLongitude,double maxLongitude,AsyncPromise promise)
    {
        final AsyncPromise promise_ =promise;
        URL url = null;
        try {

            https://www.pokeradar.io/api/v1/submissions?minLatitude=37.34827350704932&maxLatitude=37.34962247306654&minLongitude=-121.90421104431152&maxLongitude=-121.87764644622801
            url = new URL("https://www.pokeradar.io/api/v1/submissions?"+
                    "minLatitude="+minLatitude+
                    "&maxLatitude="+maxLatitude+
                    "&minLongitude="+minLongitude+
                    "&maxLongitude="+maxLongitude);
            //url = new URL("https://cache.fastpokemap.com/?lat="+latitude+"&lng="+longitude);
        }
        catch (MalformedURLException e)
        {
            promise_.reject(-1,e);
            return 0;
        }
        AsyncPromise GET_ACT_Promise= new AsyncPromise(//middle man
                new AsyncPromise.PromiseCB(){//then: success condition
                    public int _(int type, Object obj) {
                        //TODO:(S:3) use this data to generate near by map to reduce Get frequency

                        String sb = (String)obj;


                        return promise_.resolve(type,obj);

                    }
                },
                new AsyncPromise.PromiseCB(){//catch: error condition
                    public int _(int type, Object obj) {
                        promise_.reject(type,obj);
                        return 0;
                    }
                });

        //Log.v("PokeRadarAPI","HttpGetAction(url"+url);


        HashMap<String,String> headerProp = new HashMap<String,String>();
        headerProp.put("Referer","https://www.pokeradar.io/");
        HttpGetAction GETAct = new HttpGetAction(url,headerProp,GET_ACT_Promise,5000);
        return 0;
    }



}
