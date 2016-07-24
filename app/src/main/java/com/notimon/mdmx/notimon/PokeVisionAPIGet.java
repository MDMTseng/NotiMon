package com.notimon.mdmx.notimon;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by MDMx on 7/24/2016.
 */
public class PokeVisionAPIGet {

    int GetNearByPokemon(float latitude,float longitude,AsyncPromise promise)
    {

        URL url = null;
        try {
            url = new URL("https://pokevision.com/map/data/"+latitude+"/"+longitude);
        }
        catch (MalformedURLException e)
        {}


        HttpGetAction GETAct = new HttpGetAction(url,promise);
        return 0;
    }


}
