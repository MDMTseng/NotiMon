package com.notimon.mdmx.notimon;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

/**
 * Created by MDMx on 7/24/2016.
 */
public class ServiceManger <ServiceClassType extends Service> {

    Context context;
    private mServiceConnection ServiceConn;
    Class<? extends Service> serviceClass = null;
    boolean isServiceBounded;
    ServiceManger(Context context, Class<? extends Service> serviceClass)
    {
        this.context=context;
        this.serviceClass= serviceClass;
        isServiceBounded = false;
        ServiceConn = new mServiceConnection();
    }


    private class mServiceConnection implements ServiceConnection
    {
        private ServiceClassType serv = null;
        @Override
        public void onServiceDisconnected(ComponentName name) {

            if(serv == null)return;
            ServiceManger.this.onServiceDisconnected(name);
            serv = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            NotiMonService._Binder myBinder = (NotiMonService._Binder) service;
            serv = (ServiceClassType)myBinder.getService();
            ServiceManger.this.onServiceConnected(serv);
        }

        public ServiceClassType GetService(){return serv;}
        //public ServiceClassType rmService(ServiceClassType serv){ this.serv = serv;}
    }


    public void startBindService(int BindServiceFlags){

        Intent intent = new Intent(context,serviceClass);
        context.startService(intent);
        isServiceBounded=context.bindService(intent, ServiceConn, BindServiceFlags);
    }

    public void unbindService()
    {
        if(isServiceBounded) {
            context.unbindService(ServiceConn);
            isServiceBounded=false;

        }
    }
    public void stopService()
    {
        context.stopService(new Intent(context, serviceClass));
        ServiceConn.onServiceDisconnected(null);
        //Hack: onServiceDisconnected is usually not getting called, not able to handle Service kill it self
    }

    ServiceClassType GetService(){return ServiceConn.GetService();}

    void onServiceConnected(ServiceClassType service)
    {

    }


    void onServiceDisconnected(ComponentName name)
    {

    }
}
