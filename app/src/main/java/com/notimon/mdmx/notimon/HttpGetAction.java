package com.notimon.mdmx.notimon;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by MDMx on 7/23/2016.
 */
public class HttpGetAction{
    private URL url = null;
    private HttpURLConnection connection = null;
    private BufferedReader breader = null;
    private StringBuffer sb = new StringBuffer();
    private AsyncPromise ap = null;
    private int timeout_ms;
    static final int ACTIONCode=CONSTANT.PromiseDataEnum.HTTP_GET.ordinal();
    HashMap<String,String> headerProp = null;
    HttpGetAction(URL url, HashMap<String,String> headerProp, AsyncPromise ap, int timeout_ms, boolean newThread)
    {
        this.headerProp=headerProp;
        this.timeout_ms=timeout_ms;
        this.ap = ap;
        this.url = url;
        if(newThread)
            new Thread(r1).start();
        else
            r1.run();
    }

    HttpGetAction(URL url, HashMap<String,String> headerProp, AsyncPromise ap, int timeout_ms)
    {
        this(url,headerProp,ap,timeout_ms,true);
    }

    AsyncPromise GetPromise()
    {
        return ap;
    }

    private Runnable r1=new Runnable () {
        void NetThread()
        {

            /*String res= null;
            try {
                res = getHtmlByGet(url.toString());
                ap.resolve(ACTIONCode, res);
            } catch (IOException e) {
                e.printStackTrace();
                ap.reject(ACTIONCode, e);
            }*/

            char [] buf= new char[100];

            try {
                connection = (HttpURLConnection) url.openConnection();

                if(headerProp !=null)
                for (Map.Entry<String, String> entry : headerProp.entrySet()) {
                    connection.addRequestProperty(entry.getKey(),entry.getValue());
                }
                connection.setConnectTimeout(timeout_ms);
                connection.connect();

                InputStream response = connection.getInputStream();
                breader = new BufferedReader(new InputStreamReader(response));

                String line ="";
                while ((line = breader.readLine())!=null)
                {
                    sb.append(line);
                }
                ap.resolve(ACTIONCode, sb.toString());
                if(breader!=null)breader.close();
            }
            catch(IOException e)
            {
                Log.i("NetThread",e.toString());
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

    public String getHtmlByGet(String _url) throws IOException {

        String result = null;
        HttpClient client = null;

        Log.v("getHtmlByGet",_url);
        try {
            client = new DefaultHttpClient();

            HttpGet get = new HttpGet(_url);


            get.addHeader("Origin","https://www.fastpokemap.com");
            get.addHeader("Referer","https://www.fastpokemap.com/");
            HttpResponse response = client.execute(get);
            HttpEntity resEntity = response.getEntity();

            if (resEntity != null) {
                result = EntityUtils.toString(resEntity);
            }
        } catch (IOException e) {
            Log.v("getHtmlByGet",e.toString());
            client.getConnectionManager().shutdown();
            throw e;
        } finally {
            client.getConnectionManager().shutdown();
        }

        Log.v("getHtmlByGet",result);

        return result;

    }
}
