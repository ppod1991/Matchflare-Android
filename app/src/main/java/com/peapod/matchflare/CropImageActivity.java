package com.peapod.matchflare;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.mobileconnectors.s3.transfermanager.model.UploadResult;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.edmodo.cropper.CropImageView;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Activity to upload or take a profile picture, crop/rotate it, and upload it to Amazon S3
 */
public class CropImageActivity extends Activity implements Button.OnClickListener {

    //Activity Components
    CropImageView image;
    ImageButton newImageButton;
    ImageButton rotateButton;
    ImageButton uploadButton;
    TextView uploadPercentField;

    //File Variables
    Uri outputFileUri;
    Bitmap bitmap;
    ExifInterface exif;

    //Amazon S3 Variables
    Upload upload;
    TransferManager transferManager;
    ImageProgressListener progressListener;
    CognitoCachingCredentialsProvider cognitoProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        //Get components
        image = (CropImageView) findViewById(R.id.CropImageView);
        newImageButton = (ImageButton) findViewById(R.id.choose_new_image_button);
        rotateButton = (ImageButton) findViewById(R.id.rotate_button);
        uploadButton = (ImageButton) findViewById(R.id.upload_picture_button);
        uploadPercentField = (TextView) findViewById(R.id.upload_percent_field);

        //Set listeners
        newImageButton.setOnClickListener(this);
        rotateButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);

        startImagePickerIntent(); //Retrieve new image

        //Initialize Amazon S3 Credentials
        cognitoProvider = new CognitoCachingCredentialsProvider(
                this,
                "779249472230",
                "us-east-1:08be2bd6-6938-4c95-8602-f2e033821fb6",
                "arn:aws:iam::779249472230:role/Cognito_MatchflareUsersUnauth_DefaultRole",
                "YOUR AUTHENTICATED ARN HERE",
                Regions.US_EAST_1
        );
    }

    @Override
    public void onResume() {
        super.onResume();

        transferManager = new TransferManager(cognitoProvider); //Reconstruct Amazon credentials object

        //Google Analytics
        Tracker t = ((Global) this.getApplication()).getTracker();
        t.setScreenName("CropImageActivity");
        t.send(new HitBuilders.AppViewBuilder().build());
    }


    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.choose_new_image_button) {
            startImagePickerIntent();

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("CropNewImageButtonPressed")
                    .build());
        }
        else if (view.getId() == R.id.rotate_button) {
            rotateImage(90); //Rotate the image by 90 degrees

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("CropRotateImagePressed")
                    .build());
        }
        else if (view.getId() == R.id.upload_picture_button) {
            //Upload the image to Amazon S3
            bitmap = image.getCroppedImage(); //Crop the image
            uploadToS3();

            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.EventBuilder()
                    .setCategory("ui_action")
                    .setAction("button_press")
                    .setLabel("CropDidChoosePicture")
                    .build());
        };
    }

    /*
     * Upload the current image to Amazon S3
     */
    public void uploadToS3() {

        if (transferManager == null) transferManager = new TransferManager(cognitoProvider); //Ensure transfer manager exists

        //Name the file
        String fileName = "profile-pic-" + ((Global)getApplication()).getDeviceID() + "-" + System.currentTimeMillis() + "-android.jpg";

        //Convert the image to a stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);

        //Add meta data
        ObjectMetadata metaData = new ObjectMetadata();
        metaData.addUserMetadata("contact-id",((Global) getApplication()).thisUser.contact_id + "");
        metaData.addUserMetadata("local-file-path",outputFileUri.getPath());
        metaData.addUserMetadata("date", (new Date().toString()));

        //Initiate upload
        upload = transferManager.upload("matchflare-profile-pictures", fileName, bs, metaData);
        progressListener = new ImageProgressListener();
        upload.addProgressListener(progressListener);
    }

    /*
     * Rotates the given image by the specified number of degrees
     * @param degrees The number of degrees to rotate the image by
     */
    public void rotateImage(int degrees) {
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(), bitmap.getHeight(),m,true);
        image.setImageBitmap(null);
        bitmap.recycle();
        bitmap = rotatedBitmap;
        image.setImageBitmap(bitmap,exif);
    };

    /*
     * Starts an intent that instructs the user to take or choose a profile picture
     */
    public void startImagePickerIntent() {

        rotateButton.setVisibility(View.INVISIBLE);

        // Determine the Uuri of camera image to save.
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
        root.mkdirs(); //Make the directory
        final String fname = "matchflare_"+ System.currentTimeMillis() + ".jpg";
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera Intents
        final List<Intent> cameraIntents = new ArrayList<Intent>();
        final Intent captureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        final PackageManager packageManager = getPackageManager();
        final List<ResolveInfo> listCam = packageManager.queryIntentActivities(captureIntent, 0);
        for(ResolveInfo res : listCam) {
            final String packageName = res.activityInfo.packageName;
            final Intent intent = new Intent(captureIntent);
            intent.setComponent(new ComponentName(res.activityInfo.packageName, res.activityInfo.name));
            intent.setPackage(packageName);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, outputFileUri);
            cameraIntents.add(intent);
        }

        // Filesystem Intents
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        startActivityForResult(chooserIntent, 1);
    };

    //Class to listen to the progress of the image upload
    public class ImageProgressListener implements ProgressListener {

        @Override
        public void progressChanged(ProgressEvent progressEvent) {

            final ProgressEvent p = progressEvent;

            //If upload failed...
            if (p.getEventCode() == ProgressEvent.FAILED_EVENT_CODE || p.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                transferManager.shutdownNow();
                transferManager = null;

                //Google analytics
                Tracker t = ((Global) getApplication()).getTracker();
                t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription("Failed to upload image")
                        .setFatal(false)
                        .build());
            };

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    int eventCode = p.getEventCode();

                    if (eventCode == ProgressEvent.FAILED_EVENT_CODE) {
                        Style.makeToast(CropImageActivity.this,"Upload Failed. Try again!");
                        upload.removeProgressListener(progressListener);

                        //Google Analytics
                        Tracker t = ((Global) getApplication()).getTracker();
                        t.send(new HitBuilders.ExceptionBuilder()
                                .setDescription("Failed to upload image")
                                .setFatal(false)
                                .build());
                    }
                    else if (eventCode == ProgressEvent.COMPLETED_EVENT_CODE) { //Upload successful
                        Style.makeToast(CropImageActivity.this,"Woo, Upload Successful!");
                        try {

                            UploadResult uploadResult = upload.waitForUploadResult();
                            String bucket = uploadResult.getBucketName();
                            String image_URL = "https://s3.amazonaws.com/" + bucket + "/" + uploadResult.getKey();

                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("image_URL",image_URL); //Send back the URL of newly uploaded image
                            setResult(RESULT_OK,returnIntent);

                            //Google Analytics
                            Tracker t = ((Global) getApplication()).getTracker();
                            t.send(new HitBuilders.EventBuilder()
                                    .setCategory("ui_action")
                                    .setAction("button_press")
                                    .setLabel("CropUploadSuccessful")
                                    .build());

                            finish(); //End activity
                        }
                        catch (InterruptedException e) {
                            Style.makeToast(CropImageActivity.this,"Upload Failed. Try again!");

                            //Google Analytics
                            Tracker t = ((Global) getApplication()).getTracker();
                            t.send(new HitBuilders.ExceptionBuilder()
                                    .setDescription("Upload Failed: " +
                                            new StandardExceptionParser(CropImageActivity.this, null)
                                                    .getDescription(Thread.currentThread().getName(), e))
                                    .setFatal(false)
                                    .build());
                        }
                        catch (AmazonClientException e) {
                            Style.makeToast(CropImageActivity.this,"Upload Failed. Try again!");

                            //Google Analytics
                            Tracker t = ((Global) getApplication()).getTracker();
                            t.send(new HitBuilders.ExceptionBuilder()
                                    .setDescription("Failed to start chat websocket: " +
                                            new StandardExceptionParser(CropImageActivity.this, null)
                                                    .getDescription(Thread.currentThread().getName(), e))
                                    .setFatal(false)
                                    .build());
                        }

                        upload.removeProgressListener(progressListener);
                    }

                    double percent = upload.getProgress().getPercentTransferred();
                    Log.e("Progress:", percent + "%");
                }
            });

        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_crop_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Processes the result of the image picker intent
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == RESULT_OK) {
            if(requestCode == 1) {
                final boolean isCamera;
                if(data == null) {
                    isCamera = true;
                }
                else {
                    final String action = data.getAction();
                    if(action == null) {
                        isCamera = false;
                    }
                    else {
                        isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                Uri selectedImageUri;
                if(isCamera) {
                    selectedImageUri = outputFileUri;
                }
                else {
                    selectedImageUri = data == null ? null : data.getData();
                    rotateButton.setVisibility(View.VISIBLE);
                }

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

                    //Compress the image
                    int width = bitmap.getWidth();
                    int height = bitmap.getHeight();
                    int MY_IMAGE_SIZE = width*height;
                    final int IMAGE_MAX_SIZE = 900000; // 1.2MP
                    double ratio = MY_IMAGE_SIZE/IMAGE_MAX_SIZE;
                    if (ratio > 1) {
                        bitmap = Bitmap.createScaledBitmap(bitmap, (int) (width/Math.pow(ratio,0.5)), (int) (height/Math.pow(ratio,0.5)), true);
                    }

                    exif = new ExifInterface(selectedImageUri.getPath());
                    image.setImageBitmap(bitmap,exif);
                }
                catch (FileNotFoundException e){
                    Style.makeToast(this,"We can't find that file :( try another one!");
                }
                catch (IOException e) {
                    Style.makeToast(this,"That picture is too hot to handle. Try another one!");
                }

            }
        }
        else {
            //Google Analytics
            Tracker t = ((Global) getApplication()).getTracker();
            t.send(new HitBuilders.ExceptionBuilder()
                    .setDescription("Error retrieving image from image picker intent!")
                    .setFatal(false)
                    .build());
        }
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState)
    {
        super.onRestoreInstanceState(savedInstanceState);
        Intent i = new Intent(this,SplashActivity.class);
        startActivity(i);
        finish();  //Go back to main activity if restored
    }
}
