package com.gmail.gabezter.xc_checkin;

import android.widget.EditText;

import java.util.ArrayList;

public class colCount {
    private ArrayList<Object> col;

    public colCount() {
        col = new ArrayList<>();
    }

    public ArrayList<Object> getCol() {
        return col;
    }

    public Object getCol(int loc) {
        return col.get(loc);
    }

    public void add(Object o) {
        col.add(o);
    }

    public int size() {
        return col.size();
    }

    public void clear(){
        col.clear();
    }
}