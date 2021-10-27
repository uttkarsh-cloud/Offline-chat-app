package com.pk.inchat;

import android.graphics.Bitmap;

import java.io.File;


public class MessageInstance {
    public static final int DATA_IMAGE = 2;
    public static final int DATA_TEXT = 1;

    public boolean send;
    public String message;
    public Bitmap imageBitmap;
    public String macAddress;
    public String userName;
    public int dataType;
    public Object data;
    public String time;

    public MessageInstance() {
        message = null;
        imageBitmap = null;

    }

    public String getMessage() {
        return message;
    }

    public Bitmap getImage() {
        return imageBitmap;
    }


    public String getMacAddress() {
        return macAddress;
    }

    public String getUserName() {
        return userName;
    }

    public boolean getSide() {
        return send;
    }

    public int getDataType() {
        return dataType;
    }

    public Object getData() {
        return data;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }
    public void setData(Object data) {
        this.data = data;
    }
    public void setDataType(int type) {
        this.dataType = type;
    }

    public MessageInstance(boolean side, String message, String macAddress) {
        this.send = side;
        this.message = message;
        this.macAddress = macAddress;
        imageBitmap = null;

    }

    public MessageInstance(boolean side, String message) {
        this.send = side;
        this.message = message;
        imageBitmap = null;

    }

    public MessageInstance(boolean side, Bitmap imageBitmap) {
        this.send = side;
        this.message = null;
        this.imageBitmap = imageBitmap;

    }


}
