package com.example.utarapp;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.NonNull;

public class QRCodeData implements Parcelable {
    private String subject;
    private String classType;
    private String startTime;
    private String venue;
    private String date;

    public QRCodeData(String subject, String classType, String startTime, String venue, String date){
        this.subject=subject;
        this.classType=classType;
        this.startTime=startTime;
        this.venue=venue;
        this.date=date;
    }
    public String getSubject(){
        return subject;
    }
    public void setSubject(String subject){
        this.subject=subject;
    }
    public String getClassType(){
        return classType;
    }
    public void setClassType(String classType){
        this.classType=classType;
    }
    public String getStartTime(){
        return startTime;
    }
    public void setStartTime(String startTime){
        this.startTime=startTime;
    }
    public String getVenue(){
        return venue;
    }
    public void setVenue(String venue){
        this.venue=venue;
    }
    public String getDate(){
        return date;
    }
    public void setDate(String date){
        this.date=date;
    }
    protected QRCodeData(Parcel in) {
        subject=in.readString();
        classType=in.readString();
        startTime=in.readString();
        venue=in.readString();
        date = in.readString();
        Log.d("QRCodeData", "Unparceling: Date is " + date); // Debugging
    }

    public static final Creator<QRCodeData> CREATOR = new Creator<QRCodeData>() {
        @Override
        public QRCodeData createFromParcel(Parcel in) {
            return new QRCodeData(in);
        }

        @Override
        public QRCodeData[] newArray(int size) {
            return new QRCodeData[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(subject);
        dest.writeString(classType);
        dest.writeString(startTime);
        dest.writeString(venue);
        dest.writeString(date);
        Log.d("QRCodeData", "Parceling: Date is " + date); // Debugging
    }
}
