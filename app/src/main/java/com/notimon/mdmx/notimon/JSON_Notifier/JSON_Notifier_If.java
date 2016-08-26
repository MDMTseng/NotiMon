package com.notimon.mdmx.notimon.JSON_Notifier;

import java.util.Objects;

/**
 * Created by MDMx on 7/28/2016.
 */
public interface JSON_Notifier_If {
    public void setParent(JSON_Notifier_If parent, String nameForSelf);
    public void noticeParent(Object from,String key,Object value);
}
