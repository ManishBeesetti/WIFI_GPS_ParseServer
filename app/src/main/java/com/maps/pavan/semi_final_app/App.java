package com.maps.pavan.semi_final_app;

import android.app.Application;

import com.parse.Parse;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

//        The Below code is used to establish the connection with a cloud database
//        it consists of application ID
//                       client key
//                       Server Address
//         of the server on with the cloud database is deployed on.

        Parse.initialize(new Parse.Configuration.Builder(this)
                .applicationId("***************************")
                // if desired
                .clientKey("**************************")
                .server("*****************************")
                .build()
        );


    }


}