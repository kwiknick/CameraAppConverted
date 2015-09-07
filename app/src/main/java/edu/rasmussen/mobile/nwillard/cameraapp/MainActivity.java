package edu.rasmussen.mobile.nwillard.cameraapp;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends Activity {
    // name of file
    private EditText name;
    private EditText location;
    private final int TAKE_PICTURE = 1;
    private Uri imageUri;

    // filenames
    private String Photo;
    private String tempPhoto;

    private String gpsPos = "";
    private LocationManager locMgr;
    private LocationListener netListener;
    private LocationListener gpsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = (EditText) findViewById(R.id.editName);
        name.setText("");
        location = (EditText) findViewById(R.id.editLocation);
        location.setText("");
        location.setEnabled(false);
        gpsPos = "";

        // create temporary directory to store image files taken by camera (not saved)
        String tempDirPath = getFilesDir().getAbsolutePath() + File.separator + "tempData";
        File tempDir = new File(tempDirPath);

        if (!tempDir.exists())
            tempDir.mkdirs();

        // remove all temp image files if not empty, when application starts
        String[] children = tempDir.list();

        for (int i = 0; i < children.length; i++) {
            new File(tempDir, children[i]).delete();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    private void StopLocation() {
        if (netListener != null) locMgr.removeUpdates(netListener);
        if (gpsListener != null) locMgr.removeUpdates(gpsListener);
        Log.w("CameraExample", "Closing Down GPS");
    }

    @Override
    protected void onStop() {
        super.onStop();
        StopLocation();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            // called after user hits check mark in camera content provider, may want to remove empty file if user does
            // not click checkmark
            case TAKE_PICTURE:
                if (resultCode == Activity.RESULT_OK) {
                    // get image from media store content provider
                    Uri selectedImage = imageUri;
                    getContentResolver().notifyChange(selectedImage, null);

                    // where to display the image
                    ImageView imageView = (ImageView) findViewById(R.id.preview);
                    ContentResolver cr = getContentResolver();

                    // update location
                    locMgr = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    netListener = new MyLocationListener();
                    gpsListener = new MyLocationListener();

                    if (locMgr.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                        locMgr.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, netListener);
                    }

                    locMgr.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, gpsListener);

                    // create bitmap to show in imageview
                    Bitmap bitmap;
                    try {
                        bitmap = android.provider.MediaStore.Images.Media.getBitmap(cr, selectedImage);
                        imageView.setImageBitmap(bitmap);


                        // move file to temp directory
                        tempPhoto = File.separator + "tempData" + File.separator + name.getText().toString() + ".jpg";
                        File temp = new File(getFilesDir(), Photo);
                        temp.renameTo(new File(getFilesDir(), tempPhoto));

                        Toast.makeText(this, new File(getFilesDir(), tempPhoto).getAbsolutePath(), Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Failed to load", Toast.LENGTH_SHORT).show();
                        Log.e("Camera", e.toString());
                    }
                }
        }
    }

    // snap button clicked
    public void onSnapButton(View v) {
        // if the user entered a name, need name to save file
        if (name.getText().length() > 1) {
            location.requestFocus();

            // set the name of the filename to jpg extension
            Photo = name.getText().toString() + ".jpg";

            // storing the image file in local storage to app, NOT always a good idea, better to save to sdcard
            // in order to do this, mediastore has to have access to that directory, so set it
            // see http://stackoverflow.com/questions/5252193/trouble-writing-internal-memory-android
            try {
                FileOutputStream fos = openFileOutput(Photo, Context.MODE_WORLD_WRITEABLE);
                fos.close();
            } catch (Exception e) {
            }

            File photo = new File(getFilesDir(), Photo);
            Log.e("Taking", "filename  " + Photo + "    " + photo.getPath());

            // start camera, send it name of file for where to store image
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photo));
            imageUri = Uri.fromFile(photo);
            startActivityForResult(intent, TAKE_PICTURE);

        } else {
            Toast.makeText(MainActivity.this, "Sorry Need a Name first !", Toast.LENGTH_LONG).show();
        }
    }

    // save button clicked
    public void onSaveButton(View v) {
        if (location.getText().length() > 1 && name.getText().length() > 1) {
            Toast.makeText(MainActivity.this, "Creating File:  " + name.getText().toString(), Toast.LENGTH_LONG).show();
            Log.w("CameraExample", "Creating:  [" + name.getText().toString() + "]    " + gpsPos + "   :  " + Photo);

            // move temp file to files directory (permanent storage)
            File temp = new File(getFilesDir(), tempPhoto);
            temp.renameTo(new File(getFilesDir(), Photo));

        } else {
            Toast.makeText(MainActivity.this, "Sorry Need a picture and Location First !", Toast.LENGTH_LONG).show();
        }
    }

    // LOCATION stuff
    private class MyLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location loc) {
            location.setText("");
            String cityName = null;
            Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());

            List<Address> addresses;
            try {
                addresses = gcd.getFromLocation(loc.getLatitude(), loc.getLongitude(), 1);

                if (addresses.size() > 0)
                    cityName = addresses.get(0).getLocality() + ", " + addresses.get(0).getAdminArea();
            } catch (IOException e) {
                e.printStackTrace();
            }

            gpsPos = loc.getLatitude() + " # " + loc.getLongitude();
            location.setText(cityName);
            StopLocation();
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }
    }
}
