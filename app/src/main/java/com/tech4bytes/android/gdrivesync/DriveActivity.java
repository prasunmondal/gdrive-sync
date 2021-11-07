package com.tech4bytes.android.gdrivesync;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class DriveActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int PICK_FOLDER = 1004;
    private static final String[] SCOPES = {DriveScopes.DRIVE};
    private static final String PREF_ACCOUNT_NAME = "accountName";
    static String path;
    GoogleAccountCredential mCredential = null;
    java.io.File file2;
    GoogleSignInAccount account;
    Button uploadFileBtn, createFolderBtn, folderPickerBtn;
    String TAG = "tech4bytes";
    private ProgressBar mProgressBar;
    private TextView mTextView;
    private int calledFrom = 0;
    FileManager filemanager = new FileManager();

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drive);

        account = getIntent().getParcelableExtra("ACCOUNT");
        mTextView = findViewById(R.id.drive_status);
        mProgressBar = findViewById(R.id.progress_bar_drive);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

        path = getFilesDir().getAbsolutePath() + "/Template";
        //path of the file that is to be uploaded
        isReadStoragePermissionGranted();

        folderPickerBtn = findViewById(R.id.folder_picker);
        filemanager.readFileData(this);

        folderPickerBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    i.addCategory(Intent.CATEGORY_DEFAULT);
                    startActivityForResult(Intent.createChooser(i, "Choose directory"), PICK_FOLDER);
                }
            }
        });

//
//
////check for permission
//        if(ContextCompat.checkSelfPermission(this,
//                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED){
//            //ask for permission
//            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, READ_EXTERNAL_STORAGE_PERMISSION_CODE);
//        }

        getResultsFromApi();
        // check play service availability, device online status, and signed in account object

    }

    public boolean isReadStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted1");
                return true;
            } else {

                Log.v(TAG, "Permission is revoked1");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 3);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted1");
            return true;
        }
    }


    public void getResultsFromApi() {
        if (!isGooglePlayServicesAvailable()) {
            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (!isDeviceOnline()) {

            Toast.makeText(getApplicationContext(),
                    "No Network Connection Available", Toast.LENGTH_SHORT).show();
            Log.e(this.toString(), "No network connection available.");
        } else {
            //if everything is Ok
            if (calledFrom == 2) {
//                new DriveActivity.MakeDriveRequestTask2(mCredential, DriveActivity.this, null, null).execute();//upload q and responses xlsx files
            }
            if (calledFrom == 1) {
//                new DriveActivity.MakeDriveRequestTask(mCredential, DriveActivity.this).execute();//create app folder in drive
            }
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        if (EasyPermissions.hasPermissions(
                this, android.Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    android.Manifest.permission.GET_ACCOUNTS);
        }
    }


    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Log.e(this.toString(), "This app requires Google Play Services. Please install " +
                            "Google Play Services on your device and relaunch this app.");

                } else {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;
            case PICK_FOLDER:
                try {
                    if (requestCode == PICK_FOLDER) {
                        Log.i("TAG", String.format("Return from DirChooser with result %d",
                                resultCode));
                        Log.i("Test", "Result URI " + data.getData());
                        Log.i("Test", "Result URI " + data.getData().getPath());
                        filemanager.add_folder(this, data.getData().getPath());
                        listFilesInDirectory(data.getData().getPath());
                        uploadAvailableFiles();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }


    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }


    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        Log.e(this.toString(), "Checking if device");
        return (networkInfo != null && networkInfo.isConnected());

    }

    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    void listFilesInDirectory(String directoryPath) {
        String path = directoryPath;
        String actual_path = path.split(":")[1];
        path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + actual_path;
        Log.d("Files", "Path: " + path);
        java.io.File directory = new java.io.File(path);
        java.io.File[] files = directory.listFiles();
        Log.d("Files", "Size: " + files.length);
        for (int i = 0; i < files.length; i++) {
            Log.d("Files", "FileName:" + files[i].getName() + "-" + files[i].isFile() + "-" + files[i].length() + "-" + files[i].lastModified());
        }
    }

    void uploadAvailableFiles() {
        List<LocalFile> files_available = filemanager.files_available;
        for (LocalFile fileReference: files_available) {
            calledFrom = 2;
            getResultsFromApi();
            if (fileReference.file.isFile() && shouldUpload(fileReference)) {
                Log.d("Files", "Uploading file: " + fileReference.file.getName());
                new DriveActivity.MakeDriveRequestTask2(mCredential, DriveActivity.this, fileReference, filemanager).execute();//upload q and responses xlsx filess
            }
        }
    }

    boolean shouldUpload(LocalFile file) {
        String log = "Upload file check: Decision: ";
        boolean decision = true;
        if(file.sync_status == Sync_Status.UPLOAD_COMPLETE) {
            log += "false. Reason: Sync status is already in 'UPLOAD_COMPLETE'";
            decision = false;
        } else {
            log += "true.";
        }
        Log.d("", log);
        return decision;
    }

    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                DriveActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }


    private class MakeDriveRequestTask2 extends AsyncTask<Void, Void, List<String>> {
        private Drive mService = null;
        private Exception mLastError = null;
        private final Context mContext;
        private final java.io.File file;
        private LocalFile fileRef;
        private FileManager fm;


        MakeDriveRequestTask2(GoogleAccountCredential credential, Context context, LocalFile fileRef, FileManager fm) {
            mContext = context;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("SynDrive")
                    .build();
            this.file = fileRef.file;
            this.fileRef = fileRef;
            this.fm = fm;
            // TODO change the application name to the name of your applicaiton
        }

        @Override
        protected List<String> doInBackground(Void... params) {

            try {
                fileRef.sync_status = Sync_Status.UPLOADING;
                fm.writeFileData(mContext);
                uploadFile();
            } catch (Exception e) {
                e.printStackTrace();
                mLastError = e;

                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {

                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            DriveActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.e(this.toString(), "The following error occurred:\n" + mLastError.getMessage());
                }
                Log.e(this.toString(), e + "");
            }


            return null;
        }

        @Override
        protected void onPreExecute() {
            mTextView.setText("");
            mProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgressBar.setVisibility(View.GONE);
            uploadFileBtn.setVisibility(View.VISIBLE);
            createFolderBtn.setVisibility(View.VISIBLE);
            mTextView.setText("Task Completed.");
            fileRef.sync_status = Sync_Status.UPLOAD_COMPLETE;
            try {
                fm.writeFileData(mContext);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        protected void onCancelled() {

            mProgressBar.setVisibility(View.GONE);
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {

                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            DriveActivity.REQUEST_AUTHORIZATION);
                } else {
                    mTextView.setText("The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                mTextView.setText("Request cancelled.");
            }
        }

        // url = file path or whatever suitable URL you want.
        public String getMimeType(String url) {
            String type = null;
            String extension = MimeTypeMap.getFileExtensionFromUrl(url);
            if (extension != null) {
                type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            }
            return type;
        }

        private void uploadFile() throws IOException {
            if(file == null) {
                return;
            }

            File fileMetadata = new File();
            fileMetadata.setName(file.getName());
            fileMetadata.setMimeType(getMimeType(file.getAbsolutePath()));

            // For mime type of specific file visit Drive Doucumentation

            file2 = new java.io.File(file.getAbsolutePath());
            InputStream inputStream = getResources().openRawResource(R.raw.template);
            try {
                FileUtils.copyInputStreamToFile(inputStream, file2);
            } catch (IOException e) {
                e.printStackTrace();
            }

            FileContent mediaContent = new FileContent(getMimeType(file.getAbsolutePath()), file2);


            File file = mService.files().create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute();


            Log.e(this.toString(), "File Created with ID:" + file.getId());

            Toast.makeText(getApplicationContext(),
                    "File created:" + file.getId(), Toast.LENGTH_SHORT).show();
        }
    }


    private class MakeDriveRequestTask extends AsyncTask<Void, Void, List<String>> {
        private Drive mService = null;
        private Exception mLastError = null;
        private final Context mContext;


        MakeDriveRequestTask(GoogleAccountCredential credential, Context context) {

            mContext = context;
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new Drive.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("SynDrive")
                    .build();
            // TODO change the application name to the name of your applicaiton
        }

        @Override
        protected List<String> doInBackground(Void... params) {


            try {
                createFolderInDrive();

            } catch (Exception e) {
                e.printStackTrace();
                mLastError = e;

                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {

                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            DriveActivity.REQUEST_AUTHORIZATION);
                } else {
                    Log.e(this.toString(), "The following error occurred:\n" + mLastError.getMessage());
                }
                Log.e(this.toString(), e + "");
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            mTextView.setText("");
            mProgressBar.setVisibility(View.VISIBLE);

        }

        @Override
        protected void onPostExecute(List<String> output) {
            mProgressBar.setVisibility(View.GONE);
            mTextView.setText("Task Completed.");
        }

        @Override
        protected void onCancelled() {

            mProgressBar.setVisibility(View.GONE);
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {

                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            DriveActivity.REQUEST_AUTHORIZATION);
                } else {
                    mTextView.setText("The following error occurred:\n" + mLastError.getMessage());
                }
            } else {
                mTextView.setText("Request cancelled.");
            }
        }

        private void createFolderInDrive() throws IOException {
            File fileMetadata = new File();
            fileMetadata.setName("Sample Folder");
            fileMetadata.setMimeType("application/vnd.google-apps.folder");

            File file = mService.files().create(fileMetadata)
                    .setFields("id")
                    .execute();
            System.out.println("Folder ID: " + file.getId());

            Log.e(this.toString(), "Folder Created with ID:" + file.getId());
            Toast.makeText(getApplicationContext(),
                    "Folder created:" + file.getId(), Toast.LENGTH_SHORT).show();
        }
    }
}
