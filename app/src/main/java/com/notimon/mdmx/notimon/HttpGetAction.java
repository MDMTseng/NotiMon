package com.notimon.mdmx.notimon;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Created by MDMx on 7/23/2016.
 */
public class HttpGetAction{
    private URL url = null;
    private HttpURLConnection connection = null;
    private BufferedReader breader = null;
    private StringBuffer sb = new StringBuffer();
    private AsyncPromise ap = null;
    static final int ACTIONCode=CONSTANT.PromiseDataEnum.HTTP_GET.ordinal();

    HttpGetAction(URL url,AsyncPromise ap,boolean newThread)
    {
        this.ap = ap;
        this.url = url;
        if(newThread)
            new Thread(r1).start();
        else
            r1.run();
    }

    HttpGetAction(URL url,AsyncPromise ap)
    {
        this(url,ap,true);
    }

    AsyncPromise GetPromise()
    {
        return ap;
    }

    private Runnable r1=new Runnable () {
        void NetThread()
        {
            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream response = connection.getInputStream();
                breader = new BufferedReader(new InputStreamReader(response));

                String line ="";
                while ((line = breader.readLine())!=null)
                {
                    sb.append(line);
                }
                ap.resolve(ACTIONCode, sb);
                if(breader!=null)breader.close();
            }
            catch(IOException e)
            {
                ap.reject(ACTIONCode, e);
            }
            finally {
                if(connection!=null)connection.disconnect();
            }
        }
        public void run() {
            NetThread();
        }
    };



}
