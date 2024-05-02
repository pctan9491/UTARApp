package com.example.utarapp;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;

public class TimetableEntry implements Parcelable {
    private String subjectCode;
    private String subjectName;
    private String subjectClass;
    private String subjectDay;
    private String subjectTime;
    private String subjectVenue;
    private Date date;
    private boolean isReplacement;
    private boolean isCancelled;

    private String timetableId;
    // Constructors
    public TimetableEntry() {
    }

    public TimetableEntry(String subjectCode, String subjectName, String subjectClass,
                          String subjectDay, String subjectTime, String subjectVenue, Date date) {
        this.subjectCode = subjectCode;
        this.subjectName = subjectName;
        this.subjectClass = subjectClass;
        this.subjectDay = subjectDay;
        this.subjectTime = subjectTime;
        this.subjectVenue = subjectVenue;
        this.date = date;  // Set date
    }

    // Getters
    public String getSubjectCode() {
        return subjectCode;
    }

    public String getSubjectName() {
        return subjectName;
    }

    public String getSubjectClass() {
        return subjectClass;
    }

    public String getSubjectDay() {
        return subjectDay;
    }

    public String getSubjectTime() {
        return subjectTime;
    }

    public String getSubjectVenue() {
        return subjectVenue;
    }

    public Date getDate() {
        return date;
    }

    // Setters
    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }

    public void setSubjectClass(String subjectClass) {
        this.subjectClass = subjectClass;
    }

    public void setSubjectDay(String subjectDay) {
        this.subjectDay = subjectDay;
    }

    public void setSubjectTime(String subjectTime) {
        this.subjectTime = subjectTime;
    }

    public void setSubjectVenue(String subjectVenue) {
        this.subjectVenue = subjectVenue;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public boolean isReplacement() {
        return isReplacement;
    }

    public void setIsReplacement(boolean isReplacement) {
        this.isReplacement = isReplacement;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    public String getTimetableId() {
        return timetableId;
    }

    public void setTimetableId(String timetableId) {
        this.timetableId = timetableId;
    }

    // Parcelable implementation
    // Parcelable implementation
    protected TimetableEntry(Parcel in) {
        timetableId = in.readString();
        subjectCode = in.readString();
        subjectName = in.readString();
        subjectClass = in.readString();
        subjectDay = in.readString();
        subjectTime = in.readString();
        subjectVenue = in.readString();
        long tmpDate = in.readLong();
        date = tmpDate == -1 ? null : new Date(tmpDate);
        isReplacement = in.readByte() != 0;
        isCancelled = in.readByte() != 0;
    }

    public static final Creator<TimetableEntry> CREATOR = new Creator<TimetableEntry>() {
        @Override
        public TimetableEntry createFromParcel(Parcel in) {
            return new TimetableEntry(in);
        }

        @Override
        public TimetableEntry[] newArray(int size) {
            return new TimetableEntry[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(timetableId);
        dest.writeString(subjectCode);
        dest.writeString(subjectName);
        dest.writeString(subjectClass);
        dest.writeString(subjectDay);
        dest.writeString(subjectTime);
        dest.writeString(subjectVenue);
        dest.writeLong(date != null ? date.getTime() : -1);
        dest.writeByte((byte) (isReplacement ? 1 : 0));
        dest.writeByte((byte) (isCancelled ? 1 : 0));
    }
}
