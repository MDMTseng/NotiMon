package com.notimon.mdmx.notimon.CommCH_IF;

import org.json.JSONObject;

/**
 * Created by ctseng on 8/26/16.
 */
public interface CommCH_IF {

    String getCHName();


    boolean SetDest(CommCH_IF commIf);

    String RecvData(final String url, final JSONObject data, CommCH_IF from);



    String SendData(final String url,final JSONObject data);
}
