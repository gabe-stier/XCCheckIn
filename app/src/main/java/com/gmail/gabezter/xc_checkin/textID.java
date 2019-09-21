package com.gmail.gabezter.xc_checkin;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

class textID {
    private ArrayList<Integer> countIDa;
    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);

    public textID(){
        countIDa = new ArrayList<>();
    }

    public static int generateViewId() {
        for (;;) {
            final int result = sNextGeneratedId.get();
            // aapt-generated IDs have the high byte nonzero; clamp to the range under that.
            int newValue = result + 1;
            if (newValue > 0x00FFFFFF) newValue = 1; // Roll over to 1, not 0.
            if (sNextGeneratedId.compareAndSet(result, newValue)) {
                return result;
            }
        }
    }

    public ArrayList<Integer> getCountIDa() {
        return countIDa;
    }

    public void setCountIDa(ArrayList<Integer> countIDa) {
        this.countIDa = countIDa;
    }

    public void add(int id){
        countIDa.add(id);
    }
    public void remove(int id){
        countIDa.remove(id);
    }

    public int get(int id){
        return countIDa.get(id);
    }

    public int size(){
        return countIDa.size();
    }
}
