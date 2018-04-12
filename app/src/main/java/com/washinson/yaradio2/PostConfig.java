package com.washinson.yaradio2;

/**
 * Created by User on 12.04.2018.
 */

public class PostConfig {
    StringBuilder cur = new StringBuilder();

    void put(String f, String s){
        if(cur.length() == 0) cur.append(f).append("=").append(s);
        else cur.append("&").append(f).append("=").append(s);
    }

    @Override
    public String toString() {
        return cur.toString();
    }
}
