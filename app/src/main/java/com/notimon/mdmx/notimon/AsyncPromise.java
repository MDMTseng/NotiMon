package com.notimon.mdmx.notimon;

/**
 * Created by MDMx on 7/23/2016.
 */
public class AsyncPromise {

    public interface PromiseCB{
        int _(int type, Object obj);
    }
    private PromiseCB then = null;
    private PromiseCB _catch = null;

    private boolean isFulfilled = false;
    private boolean isRejected = false;

    AsyncPromise(PromiseCB then, PromiseCB _catch)
    {
        isFulfilled = false;
        isRejected = false;

        this.then = then;
        this._catch = _catch;

    }

    public boolean isFulfilled()
    {
        return isFulfilled;
    }
    public boolean isRejected()
    {
        return isRejected;
    }
    public boolean isResolved()
    {
        return isFulfilled|isRejected;
    }

    public synchronized  int resolve(int type, Object obj)
    {
        if(isResolved()){
            return -1;
        }
        isFulfilled = true;
        isRejected = false;
        return then._(type, obj);
    }
    public synchronized int reject(int type, Object obj)
    {
        if(isResolved()){
            return -1;
        }
        isFulfilled = false;
        isRejected = true;
        return _catch._(type, obj);
    }


}
