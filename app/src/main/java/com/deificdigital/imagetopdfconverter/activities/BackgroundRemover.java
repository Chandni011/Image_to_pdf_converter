package com.deificdigital.imagetopdfconverter.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.loader.content.CursorLoader;

import com.deificdigital.imagetopdfconverter.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class BackgroundRemover extends AppCompatActivity {

    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final int PICK_IMAGE = 100;
    ImageView image;
    FloatingActionButton imageButton;
    Bitmap processedBitmap;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_background_remover);

        image = findViewById(R.id.imageIv);
        imageButton = findViewById(R.id.addImageFab);

        if (image == null) {
            Log.e("BackgroundRemover", "ImageView is null. Check the ID and layout.");
        }

        imageButton.setOnClickListener(v -> openGallery());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    private void openGallery() {
        Intent gallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI);
        startActivityForResult(gallery, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && requestCode == PICK_IMAGE) {
            assert data != null;
            Uri imageUri = data.getData();
            image.setImageURI(imageUri);

            removeBackground(imageUri);

            ImageView convert = findViewById(R.id.ivConvert);
            convert.setOnClickListener(v -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(BackgroundRemover.this);
                builder.setTitle("Remove Background")
                        .setMessage("Remove the Background of Image.")
                        .setPositiveButton("Remove", (dialog, which) -> removeBackground(imageUri))
                        .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                        .show();
            });
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void removeBackground(Uri imageUri) {
        new AsyncTask<Uri, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Uri... uris) {
                try {
                    String apiKey = "4ogFA9kxNSw8aPx1RVoUbhDE";
                    File imageFile = new File(getRealPathFromURI(uris[0]));
                    OkHttpClient client = new OkHttpClient();

                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image_file", imageFile.getName(),
                                    RequestBody.create(imageFile, MediaType.parse("image/jpeg")))
                            .addFormDataPart("size", "auto")
                            .build();

                    Request request = new Request.Builder()
                            .url("https://api.remove.bg/v1.0/removebg")
                            .addHeader("X-Api-Key", apiKey)
                            .post(requestBody)
                            .build();

                    Response response = client.newCall(request).execute();
                    System.out.println("Response Code: " + response.code());
                    if (response.isSuccessful()) {
                        InputStream inputStream = response.body().byteStream();
                        return BitmapFactory.decodeStream(inputStream);
                    } else {
                        Log.e("BackgroundRemover", "Failed to remove background: " + response.message());
                    }

                } catch (Exception e) {
                    Log.e("BackgroundRemover", "Error during API request", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                // UI operations must be performed on the main thread
                if (result != null) {
                    processedBitmap = result;
                    // Post the UI update to the main thread using Handler
                    new Handler(Looper.getMainLooper()).post(() -> {
                        image.setImageBitmap(processedBitmap);
                        showDownloadDialog();
                    });
                } else {
                    // Handle the failure on the main thread using Handler
                    new Handler(Looper.getMainLooper()).post(() ->
                            Toast.makeText(BackgroundRemover.this, "Failed to remove background", Toast.LENGTH_SHORT).show()
                    );
                }
            }
        }.execute(imageUri);

    }
    private void showDownloadDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Download Image")
                .setMessage("Do you want to download the image?")
                .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // Android 10 and above: Save directly using scoped storage
                            saveImageToLocal(processedBitmap);
                        } else {
                            // Android 9 and below: Check for storage permission
                            if (ActivityCompat.checkSelfPermission(BackgroundRemover.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                                saveImageToLocal(processedBitmap);
                            } else {
                                ActivityCompat.requestPermissions(BackgroundRemover.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                            }
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void saveImageToLocal(Bitmap bitmap) {
        String imageFileName = "no_bg_" + System.currentTimeMillis() + ".png";
        FileOutputStream fos;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DISPLAY_NAME, imageFileName);
                values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
                values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/YourAppFolder");

                Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

                if (uri != null) {
                    fos = (FileOutputStream) getContentResolver().openOutputStream(uri);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    fos.close();
                    Toast.makeText(this, "Image saved to Pictures/YourAppFolder", Toast.LENGTH_SHORT).show();
                }
            } else {
                // For Android 9 and below
                File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + "/YourAppFolder");
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }
                File imageFile = new File(storageDir, imageFileName);
                fos = new FileOutputStream(imageFile);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.close();
                Toast.makeText(this, "Image saved to " + imageFile.getAbsolutePath(), Toast.LENGTH_SHORT).show();
                galleryAddPic(imageFile.getAbsolutePath());
            }
        } catch (Exception e) {
            Log.e("BackgroundRemover", "Error saving image", e);
        }
    }

    private void galleryAddPic(String imagePath) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DATA, imagePath);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }
}