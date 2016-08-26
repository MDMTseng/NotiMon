package com.notimon.mdmx.notimon;

import android.location.Location;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by MDMx on 7/24/2016.
 */
public class FastPokeMapAPI {

    int GetNearByPokemon(final Location location, AsyncPromise promise)
    {
        return  GetNearByPokemon(location.getLatitude(),location.getLongitude(),promise);
    }
    int GetNearByPokemon(double latitude,double longitude,AsyncPromise promise)
    {
        final AsyncPromise promise_ =promise;
        URL url = null;
        Log.v("FastPokeMapAPI","AsyncPromise");
        try {
            url = new URL("https://api.fastpokemap.com/?lat="+latitude+"&lng="+longitude);
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

        Log.v("FastPokeMapAPI","HttpGetAction(url"+url);


        HashMap<String,String> headerProp = new HashMap<String,String>();
        headerProp.put("Origin","https://www.fastpokemap.com");
        headerProp.put("Referer","https://www.fastpokemap.com/");
        HttpGetAction GETAct = new HttpGetAction(url,headerProp,GET_ACT_Promise,5000);
        return 0;
    }



}
