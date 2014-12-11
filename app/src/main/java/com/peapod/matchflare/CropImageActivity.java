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
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.event.ProgressEvent;
import com.amazonaws.event.ProgressListener;
import com.amazonaws.mobileconnectors.cognito.CognitoSyncManager;
import com.amazonaws.mobileconnectors.cognito.Dataset;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.mobileconnectors.s3.transfermanager.model.UploadResult;
import com.amazonaws.regions.Regions;

import com.amazonaws.services.s3.model.ObjectMetadata;
import com.edmodo.cropper.CropImageView;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class CropImageActivity extends Activity implements Button.OnClickListener {

    Uri outputFileUri;
    CropImageView image;
    Button newImageButton;
    Button rotateButton;
    Button uploadButton;
    Bitmap bitmap;
    ExifInterface exif;
    Upload upload;
    TextView uploadPercentField;
    TransferManager transferManager;
    ImageProgressListener progressListener;

    CognitoCachingCredentialsProvider cognitoProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_crop_image);

        image = (CropImageView) findViewById(R.id.CropImageView);
        newImageButton = (Button) findViewById(R.id.choose_new_image_button);
        rotateButton = (Button) findViewById(R.id.rotate_button);
        uploadButton = (Button) findViewById(R.id.upload_picture_button);

        newImageButton.setOnClickListener(this);
        rotateButton.setOnClickListener(this);
        uploadButton.setOnClickListener(this);

        uploadPercentField = (TextView) findViewById(R.id.upload_percent_field);

        startImagePickerIntent();

        cognitoProvider = new CognitoCachingCredentialsProvider(
                this, // get the context for the current activity
                "779249472230",
                "us-east-1:08be2bd6-6938-4c95-8602-f2e033821fb6",
                "arn:aws:iam::779249472230:role/Cognito_MatchflareUsersUnauth_DefaultRole",
                "YOUR AUTHENTICATED ARN HERE",
                Regions.US_EAST_1
        );

        //Log.d("LogTag", "my ID is " + cognitoProvider.getIdentityId());


    }

    @Override
    public void onResume() {
        super.onResume();
        transferManager = new TransferManager(cognitoProvider);
    }


    @Override
    public void onClick(View view) {

        if (view.getId() == R.id.choose_new_image_button) {
            startImagePickerIntent();
        }
        else if (view.getId() == R.id.rotate_button) {
            rotateImage(90);
        }
        else if (view.getId() == R.id.upload_picture_button) {
            bitmap = image.getCroppedImage();
            uploadToS3();
        };
    }

    public void uploadToS3() {
        if (transferManager == null) {
            transferManager = new TransferManager(cognitoProvider);
        }

        String fileName = "profile-pic-" + ((Global)getApplication()).getDeviceID() + "-" + System.currentTimeMillis() + "-android.jpg";

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, bos);
        byte[] bitmapdata = bos.toByteArray();
        ByteArrayInputStream bs = new ByteArrayInputStream(bitmapdata);

        ObjectMetadata metaData = new ObjectMetadata();
        metaData.addUserMetadata("contact-id",((Global) getApplication()).thisUser.contact_id + "");
        metaData.addUserMetadata("local-file-path",outputFileUri.getPath());
        metaData.addUserMetadata("date", (new Date().toString()));

        upload = transferManager.upload("matchflare-profile-pictures", fileName, bs, metaData);
        progressListener = new ImageProgressListener();
        upload.addProgressListener(progressListener);
        uploadPercentField.setVisibility(View.VISIBLE);
//        Dataset dataset = syncClient.openOrCreateDataset('myDataset');
//        dataset.put("myKey", "myValue");
//        dataset.synchronize(this, syncCallback);
    }

    public class ImageProgressListener implements ProgressListener {
        @Override
        public void progressChanged(ProgressEvent progressEvent) {

            final ProgressEvent p = progressEvent;

            if (p.getEventCode() == ProgressEvent.FAILED_EVENT_CODE || p.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                transferManager.shutdownNow();
                transferManager = null;
            };

            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    int eventCode = p.getEventCode();

                    if (eventCode == ProgressEvent.FAILED_EVENT_CODE) {
                        Style.makeToast(CropImageActivity.this,"Upload Failed. Try again!");
                        uploadPercentField.setVisibility(View.INVISIBLE);
                        upload.removeProgressListener(progressListener);
                    }
                    else if (eventCode == ProgressEvent.COMPLETED_EVENT_CODE) {
                        Style.makeToast(CropImageActivity.this,"Woo, Upload Successful!");
                        uploadPercentField.setVisibility(View.INVISIBLE);
                        try {
                            UploadResult uploadResult = upload.waitForUploadResult();
                            String bucket = uploadResult.getBucketName();
                            String image_URL = "https://s3.amazonaws.com/" + bucket + "/" + uploadResult.getKey();

                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("image_URL",image_URL);
                            setResult(RESULT_OK,returnIntent);
                            finish();

                        }
                        catch (InterruptedException e) {
                            Style.makeToast(CropImageActivity.this,"Upload Failed. Try again!");
                        }
                        catch (AmazonClientException e) {
                            Style.makeToast(CropImageActivity.this,"Upload Failed. Try again!");
                        }

                        upload.removeProgressListener(progressListener);
                    }

                    double percent = upload.getProgress().getPercentTransferred();
                    Log.e("Progress:", percent + "%");
                    uploadPercentField.setText(percent + "%");
                }
            });

        }
    }

    public void rotateImage(int degrees) {
        Matrix m = new Matrix();
        m.postRotate(degrees);
        Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(), bitmap.getHeight(),m,true);
        image.setImageBitmap(null);
        bitmap.recycle();
        bitmap = rotatedBitmap;
        image.setImageBitmap(bitmap,exif);
    };

    public void startImagePickerIntent() {
        // Determine Uri of camera image to save.
        rotateButton.setVisibility(View.INVISIBLE);
        final File root = new File(Environment.getExternalStorageDirectory() + File.separator + "MyDir" + File.separator);
        root.mkdirs();

        final String fname = "matchflare_"+ System.currentTimeMillis() + ".jpg";
        final File sdImageMainDirectory = new File(root, fname);
        outputFileUri = Uri.fromFile(sdImageMainDirectory);

        // Camera.
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

        // Filesystem.
        final Intent galleryIntent = new Intent();
        galleryIntent.setType("image/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);

        // Chooser of filesystem options.
        final Intent chooserIntent = Intent.createChooser(galleryIntent, "Select Source");

        // Add the camera options.
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, cameraIntents.toArray(new Parcelable[]{}));

        startActivityForResult(chooserIntent, 1);
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_crop_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(resultCode == RESULT_OK)
        {
            if(requestCode == 1)
            {
                final boolean isCamera;
                if(data == null)
                {
                    isCamera = true;
                }
                else
                {
                    final String action = data.getAction();
                    if(action == null)
                    {
                        isCamera = false;
                    }
                    else
                    {
                        isCamera = action.equals(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                    }
                }

                Uri selectedImageUri;
                if(isCamera)
                {
                    selectedImageUri = outputFileUri;
                }
                else
                {
                    selectedImageUri = data == null ? null : data.getData();
                    rotateButton.setVisibility(View.VISIBLE);
                }

                try {
                    bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);

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
    }
}
