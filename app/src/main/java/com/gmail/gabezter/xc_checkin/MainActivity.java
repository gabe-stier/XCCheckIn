package com.gmail.gabezter.xc_checkin;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    final String TAG = "XC CHECK IN";
    final ArrayList<String> listSchools = new ArrayList<>();
    final ArrayList<String> listSchoolsR = new ArrayList<>();
    final ArrayList<String> listSchoolsC = new ArrayList<>();
    final ArrayList<String> listRaces = new ArrayList<>();
    final colCount colCount = new colCount();
    final String welcomeScreenShownPref = "welcomeScreenShown";
    String meet = "";
    File meetFile;
    Uri uri;
    Date todayDate = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String date = formatter.format(todayDate);
    SharedPreferences mPrefs;

    private static void requestWritePermission(final Context context) {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(context)
                    .setMessage("This app needs permission to write a file.")
                    .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                    }).show();

        } else {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread thread, Throwable e) {
                        handleUncaughtException(thread, e);
                        Log.e("ME", "ME");
                    }
                });

        requestWritePermission(MainActivity.this);
        setContentView(R.layout.activity_main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        Boolean welcomeScreenShown = mPrefs.getBoolean(welcomeScreenShownPref, false);

        if (!welcomeScreenShown) {
            String whatsNewTitle = "Change Log:";
            String whatsNewText = "•" + "Added the ability to add schools on the fly.";
            new AlertDialog.Builder(this).setIcon(android.R.drawable.ic_dialog_alert).setTitle(whatsNewTitle).setMessage(whatsNewText).setPositiveButton(
                    "Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    }).show();
            SharedPreferences.Editor editor = mPrefs.edit();
            editor.putBoolean(welcomeScreenShownPref, true);
            editor.apply(); // Very important to save the preference
        }

        meetFile = new File(getFilesDir() + "/meet.csv");
        if (!meetFile.exists()) {
            importFile();
        } else {
            if (meetFile.exists()) {
                readSchools(readFromFile());
                Log.e("Clerk", "onCreate: READ FILE");
            } else {
                showFileChooser();
            }
        }
        TextView currentMeet = findViewById(R.id.selectedMeet);
        currentMeet.setText(meet);

        final Button total = findViewById(R.id.btnTotal);
        total.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                int[] total = getTotal();
                AlertDialog.Builder totalMessage = new AlertDialog.Builder(MainActivity.this);
                totalMessage.setTitle("Total Count");
                StringBuilder message = new StringBuilder();
                message.append("Total count for " + listRaces.get(0) + ": " + total[0]);
                for (int i = 1; i < listRaces.size(); i++) {
                    message.append('\n');
                    message.append("Total count for " + listRaces.get(i) + ": " + total[i]);
                }
                totalMessage.setMessage(message.toString());
                totalMessage.show();
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
//                if(imm.isActive())
//                    imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            }
        });

    }

    private void createFile() {
        Toast.makeText(MainActivity.this, "Sdcard not Exists", Toast.LENGTH_LONG).show();
        AlertDialog.Builder addMeet = new AlertDialog.Builder(MainActivity.this);
        addMeet.setTitle("No meet.csv found!");
        addMeet.setMessage("No meet.csv file found. Please insert an SD card with a file named: meet.csv \nTo see how the file is to be formatted look in How to Use tab in the menu.");
        addMeet.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        addMeet.show();


        try {
            InputStream is = getResources().openRawResource(R.raw.meet);
            OutputStream os = new FileOutputStream(meetFile);
            byte[] data = new byte[is.available()];
            is.read(data);
            os.write(data);
            is.close();
            os.close();
        } catch (IOException e) {
            e.printStackTrace();

        }

    }

    private void readSchools(String[] schools) {
        listSchools.clear();
        listSchoolsR.clear();
        listSchoolsC.clear();
        listRaces.clear();
        meet = schools[0];
        meet = meet.replace(",", " ");
        TextView currentMeet = findViewById(R.id.selectedMeet);
        currentMeet.setText(meet);
        String clear = schools[1].replace("Schools,", "");
        String[] races = clear.split(",");
        for (int i = 1; i < races.length; i++) {
            listRaces.add(races[i]);
        }
        for (int i = 2; i < schools.length; i++) {
            Log.i(Integer.toString(i), schools[i]);
            String[] tokens = schools[i].split(",");

            listSchools.add(tokens[0]);
            if (tokens.length >= 2 && tokens[1].length() > 0) {
                listSchoolsR.add(tokens[1]);
            } else {
                listSchoolsR.add("0");
            }
            if (tokens.length >= 3 && tokens[2].length() > 0) {
                listSchoolsC.add(tokens[2]);
            } else {
                listSchoolsC.add("0");
            }
        }
        boxesCreate(listSchools.toArray(new String[0]), listSchoolsR, listSchoolsC, listRaces.toArray(new String[0]));
    }

    //TODO boxesCreate
    private void boxesCreate(String[] schools, ArrayList<String> numsG, ArrayList<String> numsB, String[] colTitle) {
        TableLayout ll = findViewById(R.id.boxesTable);
        TextView boxNum;
        TextView boxSchool;
        TextView boxRunner;
        EditText boxRunnerT;
        colCount.clear();

        for (int l = 0; l < colTitle.length; l++) {
            textID boxId = new textID();
            colCount.add(boxId);
        }

        String[] sNumsG = numsG.toArray(new String[0]);
        String[] sNumsB = numsB.toArray(new String[0]);
        ll.removeAllViewsInLayout();
        TableRow row = new TableRow(this);
        boxNum = new TextView(MainActivity.this);
        boxSchool = new TextView(MainActivity.this);

        boxNum.setText("Box");
        boxSchool.setText("School");
        boxNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        boxSchool.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        boxNum.setGravity(Gravity.CENTER);
        boxSchool.setGravity(Gravity.CENTER);

        row.addView(boxNum);
        row.addView(boxSchool);
        for (String title : colTitle) {
            boxRunner = new TextView(MainActivity.this);
            boxRunner.setText(title);
            boxRunner.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            boxRunner.setGravity(Gravity.CENTER);
            row.addView(boxRunner);
        }
        ll.addView(row);
        int i = 1;
        int si = i;
        for (String school : schools) {
            si = i++;

            if (sNumsG[si - 1].equals("0")) sNumsG[si - 1] = "";
            if (sNumsB[si - 1].equals("0")) sNumsB[si - 1] = "";

            TableRow ros = new TableRow(MainActivity.this);
            boxNum = new TextView(MainActivity.this);
            boxSchool = new TextView(MainActivity.this);


            boxNum.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            boxSchool.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

            boxNum.setText(Integer.toString(si));
            boxSchool.setText(school);

            boxNum.setGravity(Gravity.CENTER);
            boxSchool.setGravity(Gravity.CENTER);

            ros.addView(boxNum);
            ros.addView(boxSchool);
            for (int j = 0; j < colTitle.length; j++) {
                textID idCon = (textID) colCount.getCol(j);
                boxRunnerT = new EditText(MainActivity.this);
                boxRunnerT.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
                boxRunnerT.setSingleLine();
                boxRunnerT.setHint("0");

                int id;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                    id = textID.generateViewId();
                } else {
                    id = View.generateViewId();
                }
                idCon.add(id);
                boxRunnerT.setId(id);
                boxRunnerT.setInputType(InputType.TYPE_CLASS_NUMBER);
                boxRunnerT.setSingleLine();
                ros.addView(boxRunnerT);
            }
            ll.addView(ros);
        }
    }

    //TODO TOTAL
    private int[] getTotal() {
        int[] totalFinal = new int[colCount.size()];
        int total;
        int t;
        String c;
        listSchoolsC.clear();
        for (int i = 0; i < colCount.size(); i++) {
            total = 0;
            textID id = (textID) colCount.getCol(i);
            for (int k = 0; k < id.size(); k++) {
                EditText count = findViewById(id.get(k));
                c = count.getText().toString();
                if (c.equals("")) c = "0";
                listSchoolsC.add(c);
                t = total;
                total = t + Integer.parseInt(c);
                Log.d(TAG, "getTotal: " + i + "," + k + "                 " + c);
            }
            Log.d(TAG, "Total: " + total);
            totalFinal[i] = total;
        }
        return totalFinal;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        MenuItem mi = menu.findItem(R.id.fileMenu);
        getMenuInflater().inflate(R.menu.sub_file_menu, mi.getSubMenu());
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        Intent help = new Intent(this, HowToUse.class);
        switch (item.getItemId()) {
            case R.id.howToUse:
                startActivity(help);
                return true;
            case R.id.importFile:
                importFile();
                return true;
            case R.id.addSchool:
                addSchoolButton();
                return true;
            case R.id.createFile:
                createFile();
                return true;
                /*
            case R.id.exportFile:
                exportFile();
                return true;
                */
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showFileChooser() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        //intent.setType("*/*");      //all files
        intent.setType("text/comma-separated-values");
        String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);

        try {
            startActivityForResult(Intent.createChooser(intent, "Select a File to Upload"), 1);
            Log.e("Clerk", "File Pick");
        } catch (android.content.ActivityNotFoundException ex) {
            // Potentially direct the user to the Market with a Dialog
            Toast.makeText(this, "Please install a File Manager.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            uri = data.getData();
            try {
                readSchools(readTextFromUri(uri));
                setMeetFile(uri);
                Log.e("Clerk", uri.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private String[] readTextFromUri(Uri uri) throws IOException {
        InputStream inputStream = getContentResolver().openInputStream(uri);
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                inputStream));
        String line;
        ArrayList<String> text = new ArrayList<>();

        while ((line = reader.readLine()) != null) {
            text.add(line);
        }
        inputStream.close();
        reader.close();
        return text.toArray(new String[0]);
    }

    public void appendLog(StackTraceElement[] text) {
        File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/log.txt");
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try {
            Log.e("Create", logFile.getAbsolutePath());
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            for (StackTraceElement s : text) {
                buf.append(s.toString());
                buf.newLine();
            }
            buf.close();
            Toast.makeText(this, "Clerk Check crashed. Created log file in downloads folder." + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleUncaughtException(Thread thread, Throwable e) {

        // The following shows what I'd like, though it won't work like this.
        appendLog(e.getStackTrace());
        appendLog(e.getCause().getStackTrace());
        thread.interrupt();
        // Add some code logic if needed based on your requirement
    }

    private void setMeetFile(Uri uriF) {
        File file = new File(getFilesDir() + "/meet.csv");
        try {
            String[] texts = readTextFromUri(uriF);
            FileWriter fw = new FileWriter(file);
            PrintWriter writer = new PrintWriter(fw);
            for (String text : texts) {
                writer.println(text);
                Log.i(file.toString(), "setMeetFile: " + text);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void importFile() {
        showFileChooser();
    }

    /*  // For future versions possibly
        private void exportFile(){

            String[] mFile = readFromFile();
            String name = mFile[0].replace(",","") + "-" + date;
            name = name.replace("/", "-");
            File toFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + name +".csv");
            if(!(toFile.exists())) {
                try {
                    FileWriter fw = new FileWriter(toFile);
                    PrintWriter writer = new PrintWriter(fw);
                    for(String text : mFile){
                        writer.println(text);
                    }
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else {
                toFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + name +".csv");
                try {
                    FileWriter fw = new FileWriter(toFile);
                    PrintWriter writer = new PrintWriter(fw);
                    for(String text : mFile){
                        writer.println(text);
                    }
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            goToFile(toFile.toString() , name);
        }
    */
    private String[] readFromFile() {

        ArrayList<String> text = new ArrayList<>();

        try {
            InputStream inputStream = new FileInputStream(new File(getFilesDir() + "/meet.csv"));

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;

                while ((receiveString = bufferedReader.readLine()) != null) {
                    text.add(receiveString);
                }

                inputStream.close();
            }
        } catch (IOException e) {
        }
        return text.toArray(new String[0]);
    }

    private void goToFile(final String loc, String name) {
        AlertDialog.Builder toFile = new AlertDialog.Builder(MainActivity.this);
        toFile.setTitle("Exported");
        toFile.setMessage("This meet has been exported to the Downloads folder and is named: " + name);
       /* toFile.setPositiveButton("Go to File", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri.Builder uriFile = new Uri.Builder();
                uriFile.scheme("content");
                uriFile.path(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath()+"/");
                Uri file = uriFile.build();
                Intent goToFile = new Intent(Intent.ACTION_VIEW);
                goToFile.setDataAndType(file, "resource/folder");
                startActivity(goToFile);
            }
        });*/
        toFile.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        toFile.setCancelable(false);
        toFile.show();

    }

    private void addSchoolButton() {
        final AlertDialog.Builder addSchool = new AlertDialog.Builder(MainActivity.this);
        final EditText input = new EditText(MainActivity.this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        addSchool.setView(input);
        addSchool.setTitle("Add School");
        addSchool.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addSchool(input.getText().toString());
            }
        });
        addSchool.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                input.setText("");
                dialog.cancel();
            }
        });
        addSchool.show();
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive())
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    private void addSchool(String school) {
        listSchools.add(school);
        listSchoolsC.add("0");
        listSchoolsR.add("0");
        boxesCreate(listSchools.toArray(new String[0]), listSchoolsR, listSchoolsC, listRaces.toArray(new String[0]));
    }
}