package com.example.dbbr;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.opencsv.CSVReader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private EditText editTextDBBR;
    private Button uploadButton, updateButton, deleteButton, backupButton, restoreButton;
    private DBHelper dbHelper;
    private ListView listViewData;
    private ArrayAdapter<String> adapter;
    private int selectedItemId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        myPermissions();

        dbHelper = new DBHelper(this);

        editTextDBBR = findViewById(R.id.editTextDBBR);
        uploadButton = findViewById(R.id.uploadButton);
        updateButton = findViewById(R.id.updateButton);
        deleteButton = findViewById(R.id.deleteButton);
        backupButton = findViewById(R.id.backupButton);
        restoreButton = findViewById(R.id.restoreButton);
        listViewData = findViewById(R.id.listViewData);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1);
        listViewData.setAdapter(adapter);
        listViewData.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the text from EditText
                String userInput = editTextDBBR.getText().toString();

                // Insert the text into the database
                insertDataIntoDatabase(userInput);

                // Optionally, clear the EditText after uploading
                editTextDBBR.getText().clear();
                displayData();

            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedItemId != -1) {
                    String updatedText = editTextDBBR.getText().toString();
                    updateDataInDatabase(selectedItemId, updatedText);
                    editTextDBBR.getText().clear();
                    displayData();
                    selectedItemId = -1; // Reset selected item after updating
                    listViewData.clearChoices();
                } else {
                    Toast.makeText(MainActivity.this, "Select an item to update", Toast.LENGTH_SHORT).show();
                }
            }
        });

        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteAllData();
                displayData();
                listViewData.clearChoices();
            }
        });

        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createBackup();
            }
        });

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                restoreBackup();
                displayData();
            }
        });


        listViewData.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                selectedItemId = (int) id;
                String selectedItemText = adapter.getItem(position);
                editTextDBBR.setText(selectedItemText);
            }
        });

        displayData();
    }

    private void insertDataIntoDatabase(String data) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_DATA, data);

        // Insert the data into the table
        db.insert(DBHelper.TABLE_NAME, null, values);

        // Close the database connection
        db.close();
    }

    private void updateDataInDatabase(int itemId, String newData) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(DBHelper.COLUMN_DATA, newData);

        db.update(DBHelper.TABLE_NAME, values, DBHelper.COLUMN_ID + "=?", new String[]{String.valueOf(itemId)});

        db.close();
    }

    private void deleteAllData() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DBHelper.TABLE_NAME, null, null);
        db.close();
    }


    private void createBackup() {
        try {

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            File folder = new File(path, "/DBBackup");
            //File backupDir = new File(Environment.getExternalStorageDirectory(), "DBBackup");
            if (!folder.exists()) {
                folder.mkdirs();
            }

            File backupFile = new File(folder, "database_backup.csv");

            if (backupFile.exists()) {
                backupFile.delete();
            }

            FileWriter writer = new FileWriter(backupFile);
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_NAME, null);

            while (cursor.moveToNext()) {
                @SuppressLint("Range") String userData = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_DATA));
                writer.append(userData);
                writer.append("\n");
            }

            writer.close();
            cursor.close();
            db.close();

            Toast.makeText(this, "Backup created successfully", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error creating backup", Toast.LENGTH_SHORT).show();
        }
    }

    private void restoreBackup() {
        try {
            File filePathAndName = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS+"/DBBackup/"+"database_backup.csv");
            File backupFile = new File(String.valueOf(filePathAndName));


            //File backupFile = new File(Environment.getExternalStorageDirectory() + "/DBBackup/database_backup.csv");

            if (backupFile.exists()) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                db.execSQL("DELETE FROM " + DBHelper.TABLE_NAME);

                CSVReader reader = new CSVReader(new FileReader(backupFile.getAbsolutePath()));
                String[] nextLine;

                while ((nextLine = reader.readNext()) != null) {
                    String data = nextLine[0]; // Assuming CSV contains only one column
                    insertDataIntoDatabase(data);
                }

                reader.close();
                db.close();

                Toast.makeText(this, "Backup restored successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Backup file not found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException | SQLException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error restoring backup", Toast.LENGTH_SHORT).show();
        }
    }

    private void displayData() {
        adapter.clear();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + DBHelper.TABLE_NAME, null);

        if (cursor.moveToFirst()) {
            do {
                @SuppressLint("Range") String userData = cursor.getString(cursor.getColumnIndex(DBHelper.COLUMN_DATA));
                adapter.add(userData);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
    }

    //permission code here =========================================================================
    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permission ->{
                boolean allGranted = true;

                for (Boolean isGranted : permission.values()){
                    if (!isGranted){
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted){
                    // All is granted
                } else {
                    // All is not granted
                }

            });

    private boolean myPermissions(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){

            String[] permissions = new String[]{
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_AUDIO,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.CAMERA,
            };


            List<String> permissionsTORequest = new ArrayList<>();
            for (String permission : permissions){
                if (ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                    permissionsTORequest.add(permission);
                }
            }

            if (permissionsTORequest.isEmpty()){
                // All permissions are already granted
                Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT).show();


            } else {
                String[] permissionsArray = permissionsTORequest.toArray(new String[0]);
                boolean shouldShowRationale = false;

                for (String permission : permissionsArray){
                    if (shouldShowRequestPermissionRationale(permission)){
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (shouldShowRationale){
                    new AlertDialog.Builder(this)
                            .setMessage("Please allow all permissions")
                            .setCancelable(false)
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermissionLauncher.launch(permissionsArray);
                                }
                            })

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();

                } else {
                    requestPermissionLauncher.launch(permissionsArray);
                }


            }


        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {

            if(!Environment.isExternalStorageManager()){
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.addCategory("android.intent.category.DEFAULT");
                    intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                    startActivityIfNeeded(intent, 101);
                }catch (Exception e){
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    startActivityIfNeeded(intent, 101);
                }
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String[] permissions = new String[]{
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,

            };


            List<String> permissionsTORequest = new ArrayList<>();
            for (String permission : permissions){
                if (ContextCompat.checkSelfPermission(this,permission) != PackageManager.PERMISSION_GRANTED){
                    permissionsTORequest.add(permission);
                }
            }

            if (permissionsTORequest.isEmpty()){
                // All permissions are already granted
                Toast.makeText(this, "All permissions are already granted", Toast.LENGTH_SHORT).show();


            } else {
                String[] permissionsArray = permissionsTORequest.toArray(new String[0]);
                boolean shouldShowRationale = false;

                for (String permission : permissionsArray){
                    if (shouldShowRequestPermissionRationale(permission)){
                        shouldShowRationale = true;
                        break;
                    }
                }

                if (shouldShowRationale){
                    new AlertDialog.Builder(this)
                            .setMessage("Please allow all permissions")
                            .setCancelable(false)
                            .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    requestPermissionLauncher.launch(permissionsArray);
                                }
                            })

                            .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            })
                            .show();

                } else {
                    requestPermissionLauncher.launch(permissionsArray);
                }
            }
        }
        return true;
    }
    //permission code end  =========================================================================



}