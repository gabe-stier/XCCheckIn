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
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TabHost;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {


    String meet = "";

    ArrayList<Integer> countIDa = new ArrayList<>();
    File meetFile;

    private static final AtomicInteger sNextGeneratedId = new AtomicInteger(1);
    final ArrayList<String> listSchools = new ArrayList<>();
    final ArrayList<String> listSchoolsR = new ArrayList<>();
    final ArrayList<String> listSchoolsC = new ArrayList<>();

    Uri uri;

    Date todayDate = Calendar.getInstance().getTime();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
    String date = formatter.format(todayDate);

    SharedPreferences mPrefs;
    final String welcomeScreenShownPref = "welcomeScreenShown";



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
                new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException (Thread thread, Throwable e) {
                        handleUncaughtException (thread, e);
                        Log.e("ME", "ME");
                    }
                });

        requestWritePermission(MainActivity.this);
        setContentView(R.layout.activity_main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        // second argument is the default to use if the preference can't be found
        Boolean welcomeScreenShown = mPrefs.getBoolean(welcomeScreenShownPref, false);

        if (!welcomeScreenShown) {
            // here you can launch another activity if you like
            // the code below will display a popup

            String whatsNewTitle = "Change Log:";
            String whatsNewText = "•" + " Fixed a bug that could appear if \\ was used. \n" + "•" + " Cleaned up the menu a little.";
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

        meetFile = new File(getFilesDir()+"/meet.csv");
        if (!meetFile.exists()) {
                importFile();
        }else{
            if(meetFile.exists()){
                readSchools(readFromFile());
                Log.e("Clerk", "onCreate: READ FILE" );
            }else{
                showFileChooser();
            }
        }
        TextView currentMeet = findViewById(R.id.selectedMeet);
        currentMeet.setText(meet);

        final TabHost tabHost = findViewById(R.id.tabHost);
        tabHost.setup();

        TabHost.TabSpec spec = tabHost.newTabSpec("Schools");
        spec.setContent(R.id.schools);
        spec.setIndicator("Schools");
        tabHost.addTab(spec);

        spec = tabHost.newTabSpec("Boxes");
        spec.setContent(R.id.boxes);
        spec.setIndicator("Boxes");
        tabHost.addTab(spec);


        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(tabHost.getApplicationWindowToken(), 0);
            }
        });

        final Button total = findViewById(R.id.btnTotal);
        total.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    TextView num = findViewById(R.id.textNumber);
                    num.setText(Integer.toString(getTotal()));
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(tabHost.getApplicationWindowToken(), 0);
            }
        });
        final Button newSchool = findViewById(R.id.addSchool);
        newSchool.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final EditText input = new EditText(MainActivity.this);
                input.setHint("School Name");
                input.setSingleLine();
                input.requestFocus();
                InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

                AlertDialog.Builder addMeet = new AlertDialog.Builder(MainActivity.this);
                addMeet.setTitle("Add School");
                addMeet.setView(input);
                addMeet.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String filename = input.getText().toString();
                            Toast.makeText(getApplicationContext(), input.getText() + " has been added.", Toast.LENGTH_LONG).show();
                            listSchools.add(filename);
                            listSchoolsR.add("0");
                            listSchoolsC.add("0");
                            input.setText(null);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                            schools(listSchools.toArray(new String[0]), listSchoolsR.toArray(new String[0]));
                            boxesCreate(listSchools.toArray(new String[0]));
                         //   save();
                            alterDocument();
                        }
                    });
                    addMeet.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            input.setText(null);
                            InputMethodManager imm = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                            imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        }
                    });
                addMeet.setCancelable(false);
                addMeet.show();
        }});

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

    private void readSchools(String[] schools){
        listSchools.clear();
        listSchoolsR.clear();
        listSchoolsC.clear();
        meet = schools[0];
        meet = meet.replace("," ," ");
        TextView currentMeet = findViewById(R.id.selectedMeet);
        currentMeet.setText(meet);
        for(int i = 2; i < schools.length; i++){
            Log.i(Integer.toString(i), schools[i]);
            String[] tokens = schools[i].split(",");

            listSchools.add(tokens[0]);
            if(tokens.length >= 2 && tokens[1].length() >0){
            listSchoolsR.add(tokens[1]);}else{
                listSchoolsR.add("0");
            }
            if(tokens.length >= 3 && tokens[2].length() > 0){
                listSchoolsC.add(tokens[2]);
            }else{
                listSchoolsC.add("0");
            }
        }
        schools(listSchools.toArray(new String[0]), listSchoolsR.toArray(new String[0]));
        boxesCreate(listSchools.toArray(new String[0]), listSchoolsC);
    }

    private void schools(String[] schools, String[] runners){
        TableLayout ll = findViewById(R.id.schoolsTable);
        TextView boxNum;
        TextView boxSchool;

        TableRow row = new TableRow(this);
        boxNum = new TextView(MainActivity.this);
        boxSchool = new TextView(MainActivity.this);

        boxNum.setText("School");
        boxSchool.setText("Registered");

        boxNum.setGravity(Gravity.CENTER);
        boxSchool.setGravity(Gravity.CENTER);

        ll.removeAllViewsInLayout();

        row.addView(boxNum);
        row.addView(boxSchool);
        ll.addView(row);

        int i = 0;
        countIDa.clear();
        for (String school : schools) {
            TableRow ros = new TableRow(MainActivity.this);
            boxNum = new TextView(MainActivity.this);
            boxSchool = new TextView(MainActivity.this);

            boxNum.setText(school);
            boxSchool.setText(runners[i]);
            i++;

            boxNum.setGravity(Gravity.CENTER);
            boxSchool.setGravity(Gravity.CENTER);

            ros.addView(boxNum);
            ros.addView(boxSchool);
            ll.addView(ros);
        }
    }

    //TODO boxesCreate
    private void boxesCreate(String[] schools) {
        TableLayout ll = findViewById(R.id.boxesTable);
        TextView boxNum;
        TextView boxSchool;
        TextView boxRunner;
        EditText boxRunnerT;

        TableRow row = new TableRow(this);
        boxNum = new TextView(MainActivity.this);
        boxSchool = new TextView(MainActivity.this);
        boxRunner = new TextView(MainActivity.this);

        boxNum.setText("Box");
        boxSchool.setText("School");
        boxRunner.setText("Number of Runners");

        boxNum.setGravity(Gravity.CENTER);
        boxSchool.setGravity(Gravity.CENTER);
        boxRunner.setGravity(Gravity.CENTER);

        ll.removeAllViewsInLayout();

        row.addView(boxNum);
        row.addView(boxSchool);
        row.addView(boxRunner);
        ll.addView(row);

        int i = 1;
        int si = i;
        countIDa.clear();
        for (String school : schools) {
            si = i++;

            TableRow ros = new TableRow(MainActivity.this);
            boxNum = new TextView(MainActivity.this);
            boxSchool = new TextView(MainActivity.this);
            boxRunnerT = new EditText(MainActivity.this);

            boxNum.setText(Integer.toString(si));
            boxSchool.setText(school);
            boxRunnerT.setSingleLine();
            boxRunnerT.setHint("0");

            boxNum.setGravity(Gravity.CENTER);
            boxSchool.setGravity(Gravity.CENTER);
            int id;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                id = generateViewId();
                countIDa.add(id);
            } else {
                id = View.generateViewId();
                countIDa.add(id);
            }

            boxRunnerT.setId(id);

            boxRunnerT.setInputType(InputType.TYPE_CLASS_NUMBER);
            boxRunnerT.setSingleLine();

            ros.addView(boxNum);
            ros.addView(boxSchool);
            ros.addView(boxRunnerT);
            ll.addView(ros);
        }
    }

    private void boxesCreate(String[] schools, ArrayList<String> nums){
        TableLayout ll = findViewById(R.id.boxesTable);
        TextView boxNum;
        TextView boxSchool;
        TextView boxRunner;
        EditText boxRunnerT;
        String[] sNums = nums.toArray(new String[0]);

        TableRow row = new TableRow(this);
        boxNum = new TextView(MainActivity.this);
        boxSchool = new TextView(MainActivity.this);
        boxRunner = new TextView(MainActivity.this);

        boxNum.setText("Box");
        boxSchool.setText("School");
        boxRunner.setText("Number of Runners");

        boxNum.setGravity(Gravity.CENTER);
        boxSchool.setGravity(Gravity.CENTER);
        boxRunner.setGravity(Gravity.CENTER);

        ll.removeAllViewsInLayout();

        row.addView(boxNum);
        row.addView(boxSchool);
        row.addView(boxRunner);
        ll.addView(row);

        int i = 1;
        int si = i;
        countIDa.clear();
        for (String school : schools) {
            si = i++;

            if(sNums[si-1].equals("0")) sNums[si-1] = "";

            TableRow ros = new TableRow(MainActivity.this);
            boxNum = new TextView(MainActivity.this);
            boxSchool = new TextView(MainActivity.this);
            boxRunnerT = new EditText(MainActivity.this);

            boxNum.setText(Integer.toString(si));
            boxSchool.setText(school);
            boxRunnerT.setSingleLine();
            boxRunnerT.setText(sNums[si-1]);
            boxRunnerT.setHint("0");

            boxNum.setGravity(Gravity.CENTER);
            boxSchool.setGravity(Gravity.CENTER);
            int id;

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
                id = generateViewId();
                countIDa.add(id);
            } else {
                id = View.generateViewId();
                countIDa.add(id);
            }

            boxRunnerT.setId(id);

            boxRunnerT.setInputType(InputType.TYPE_CLASS_NUMBER);
            boxRunnerT.setSingleLine();

            ros.addView(boxNum);
            ros.addView(boxSchool);
            ros.addView(boxRunnerT);
            ll.addView(ros);
        }
    }

    //TODO TOTAL
    private int getTotal(){
        int total = 0;
        int t;
        String c;
        List<EditText> nums = new ArrayList<>();
        listSchoolsC.clear();
        for(int i = 0; i < countIDa.size(); i++) {
            EditText count = findViewById(countIDa.get(i));
            nums.add(count);
            c = count.getText().toString();
            if(c.equals(""))c="0";
            listSchoolsC.add(c);
            t = total;
            total = t + Integer.parseInt(c);
        }
       // save();
        alterDocument();
        return total;
    }

    private static int generateViewId() {
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
            case R.id.createFile:
                createFile();
                return true;
            case R.id.exportFile:
                exportFile();
                return true;
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
            try{
                readSchools(readTextFromUri(uri));
                setMeetFile(uri);
                Log.e("Clerk", uri.toString());
            }catch (IOException e){e.printStackTrace();}
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

    private void alterDocument() {

        String[] schools = listSchools.toArray(new String[0]);
        String[] schoolsR = listSchoolsR.toArray(new String[0]);
        String[] schoolsC = listSchoolsC.toArray(new String[0]);
        File file = new File(getFilesDir() + "/meet.csv");

        try {
            String[] reply = readFromFile();
            ArrayList<String> list = new ArrayList<>();

            list.add(reply[0]);
            list.add(reply[1]);
            for (int i = 0; i < schools.length; i++){
                Log.e(Integer.toString(i), schools[i]+","+schoolsR[i]+","+schoolsC[i]);
                list.add(schools[i]+","+schoolsR[i]+","+schoolsC[i]);
            }
            OutputStream outputStream =new FileOutputStream(file);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));


            for(String line: list){
                writer.write(line);
                writer.newLine();
            }

            writer.close();
            outputStream.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void appendLog(StackTraceElement[] text){
        File logFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/log.txt");
        if (!logFile.exists())
        {
            try
            {
                logFile.createNewFile();

            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        try
        {
            Log.e("Create", logFile.getAbsolutePath());
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            for (StackTraceElement s : text){
                buf.append(s.toString());
                buf.newLine();
            }
            buf.close();
            Toast.makeText(this, "Clerk Check crashed. Created log file in downloads folder." + logFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleUncaughtException (Thread thread, Throwable e) {

        // The following shows what I'd like, though it won't work like this.
        appendLog(e.getStackTrace());
        appendLog(e.getCause().getStackTrace());
        thread.interrupt();
        // Add some code logic if needed based on your requirement
    }

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

    private void setMeetFile(Uri uriF){
        File file = new File(getFilesDir()+"/meet.csv");
        try {
            String[] texts = readTextFromUri(uriF);
            FileWriter fw = new FileWriter(file);
            PrintWriter writer = new PrintWriter(fw);
            for(String text : texts){
                writer.println(text);
                Log.i(file.toString(), "setMeetFile: " + text);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void importFile(){
        showFileChooser();
    }

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

    private String[] readFromFile() {

        ArrayList<String> text = new ArrayList<>();

        try {
            InputStream inputStream = new FileInputStream(new File(getFilesDir() + "/meet.csv"));

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString;

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    text.add(receiveString);
                }

                inputStream.close();
            }
        }catch (IOException e){}
        return text.toArray(new String[0]);
    }

    private void goToFile(final String loc, String name){
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

}