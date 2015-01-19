package com.ukuke.gl.sensegrab;

import java.lang.Object;

/**
 * Created by gildoandreoni on 19/01/15.
 */

public class ServiceComponent {
    public String dysplayName;
    public boolean exists;
    int imageID;

    ServiceComponent(String dysplayName, boolean exists) {
        this.dysplayName = dysplayName;
        this.exists = exists;
        if (this.exists) {
            imageID = R.drawable.ic_check_grey600_36dp;
        }
        else {
            imageID = R.drawable.ic_close_grey600_36dp;
        }
    }
}
