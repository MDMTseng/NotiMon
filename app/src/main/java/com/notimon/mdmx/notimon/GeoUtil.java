package com.notimon.mdmx.notimon;


/**
 * Created by MDMx on 8/7/2016.
 */
public class GeoUtil {
    final static double EARTH_RADIOUS_m = 6371008;
    public static class Coor{
        double x,y;
    }
    static void GeoDiffToXY_approx(Coor coor,double lat_O,double lon_O,double lat,double lon) {//in short range aproximate
        lat_O = Math.toRadians(lat_O);
        lon_O = Math.toRadians(lon_O);
        lat = Math.toRadians(lat);
        lon = Math.toRadians(lon);

        coor.x=Math.cos(lat_O)*(lon-lon_O)*EARTH_RADIOUS_m;
        coor.y=(lat-lat_O)*EARTH_RADIOUS_m;

    }
}
