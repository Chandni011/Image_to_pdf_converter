package com.deificdigital.imagetopdfconverter;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.deificdigital.imagetopdfconverter.adapters.AdapterImage;
import com.deificdigital.imagetopdfconverter.models.ModelImage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageListFragment extends Fragment {

    private static final String TAG = "Image List Tag";
    private static final int STORAGE_REQUEST_CODE = 100;
    private static final int CAMERA_REQUEST_CODE = 101;

    private String[] cameraPermissions;
    private String[] storagePermissions;
    private Context mContext;

    private FloatingActionButton addImageFab;
    private RecyclerView imagesRv;
    private Uri imageUri = null;
    private ArrayList<ModelImage> allImageArrayList;
    private AdapterImage adapterImage;
    private ProgressDialog progressDialog;

    public ImageListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mContext = context;
        super.onAttach(context);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_image_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        addImageFab = view.findViewById(R.id.addImageFab);
        imagesRv = view.findViewById(R.id.imagesRv);
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        loadImages();
        addImageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputImageDialog();
            }
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_images, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.images_item_delete) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext); // Use 'this' instead of 'mContext' if inside an Activity
            builder.setTitle("Delete Images")
                    .setMessage("Are you sure you want to delete All/Selected images?")
                    .setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteImages(true);
                        }
                    })
                    .setNeutralButton("Delete Selected", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            deleteImages(false);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            return true; // Return true as the event is handled
        } else if (itemId == R.id.images_item_pdf) {
            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Convert to pdf")
                    .setMessage("Convert all/selected images into pdf")
                    .setPositiveButton("Convert All", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            convertImagesToPdf(true);
                        }
                    })
                    .setNeutralButton("Convert selected", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            convertImagesToPdf(false);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
            
        }
        return super.onOptionsItemSelected(item);
    }

    private void convertImagesToPdf(boolean convertAll){
        Log.d(TAG, "convertImagesToPdf: convertAll: "+convertAll);

        progressDialog.setMessage("Converting to pdf");
        progressDialog.show();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "run: BG work start....");
                ArrayList<ModelImage> imagesToPdfList = new ArrayList<>();
                if (convertAll){
                    imagesToPdfList = allImageArrayList;
                }
                else {
                    for (int i = 0; i < allImageArrayList.size(); i++){
                        if (allImageArrayList.get(i).isChecked()){
                            imagesToPdfList.add(allImageArrayList.get(i));
                        }
                    }
                }
                Log.d(TAG, "run: imagesToPdfList size"+ imagesToPdfList.size());

                try {
                    File root = new File(mContext.getExternalFilesDir(null), Constants.PDF_FOLDER);
                    root.mkdirs();

                    long timestamp = System.currentTimeMillis();
                    String fileName = "PDF_"+timestamp+".pdf";

                    Log.d(TAG, "run: fileName"+ fileName);

                    File file = new File(root, fileName);

                    FileOutputStream fileOutputStream =new FileOutputStream(file);
                    PdfDocument pdfDocument = new PdfDocument();

                    for (int i=0; i<imagesToPdfList.size(); i++){
                        Uri imageToAdInPdfUri = imagesToPdfList.get(i).getImageUri();

                        try {
                            Bitmap bitmap;
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(mContext.getContentResolver(), imageToAdInPdfUri));
                            }
                            else
                            {
                                bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), imageToAdInPdfUri);
                            }
                            bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false);

                            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i+1).create();
                            PdfDocument.Page page = pdfDocument.startPage(pageInfo);


                            Paint paint = new Paint();
                            paint.setColor(Color.WHITE);
                            Canvas canvas = page.getCanvas();
                            canvas.drawPaint(paint);
                            canvas.drawBitmap(bitmap,0f,0f,null);

                            pdfDocument.finishPage(page);
                            bitmap.recycle();
                        }
                        catch (Exception e){
                            Log.e(TAG, "run: ",e);
                        }
                    }
                    pdfDocument.writeTo(fileOutputStream);
                    pdfDocument.close();
                }
                catch (Exception e){
                    Toast.makeText(mContext, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                    progressDialog.dismiss();
                    Log.e(TAG, "run: ",e);
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "run: Converted....");
                        progressDialog.dismiss();
                        Toast.makeText(mContext, "Converted....", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void deleteImages(boolean deleteAll){
        ArrayList<ModelImage> imagesToDeleteList = new ArrayList<>();
        if (deleteAll){
            imagesToDeleteList = allImageArrayList;
            allImageArrayList.clear();
        }
        else{
            for (ModelImage image: allImageArrayList){
                if (image.isChecked()){
                    imagesToDeleteList.add(image);
                }
            }
        }

        for (ModelImage image: allImageArrayList){
            try {
                String pathOfImageToDelete = image.getImageUri().getPath();
                File file = new File(pathOfImageToDelete);
                if (file.exists()){
                    boolean isDeleted = file.delete();
                    Log.d(TAG, "deleteImages: isDeleted"+isDeleted);
                }
                allImageArrayList.remove(image);

            }catch (Exception e){
                Log.e(TAG, "deleteImages: ",e);
            }
        }
        Toast.makeText(mContext, "Deleted", Toast.LENGTH_SHORT).show();
        adapterImage.notifyDataSetChanged();
        loadImages();
    }

    private void loadImages(){
        Log.d(TAG, "loadImages: ");
        allImageArrayList = new ArrayList<>();
        adapterImage = new AdapterImage(mContext, allImageArrayList);
        imagesRv.setAdapter(adapterImage);
        File folder = new File(mContext.getExternalFilesDir(null), Constants.IMAGES_FOLDER);
        if (folder.exists()){
            Log.d(TAG, "loadImages: ");
            File[] files = folder.listFiles();
            if (files != null){
                Log.d(TAG, "loadImages: Folder exist and have images");

                for (File file: files){
                    Log.d(TAG, "loadImages: fileName:"+file.getName());
                    Uri imqgeUri = Uri.fromFile(file);
                    ModelImage modelImage = new ModelImage(imqgeUri, false);
                    allImageArrayList.add(modelImage);
                    adapterImage.notifyItemInserted(allImageArrayList.size());
                }
            }
            else {
                Log.d(TAG, "loadImages: Folder exist but empty");
            }
        }
        else {
            Log.d(TAG, "loadImages: Folder doesn't exist");
        }
    }

    private void saveImageToAppLevelDirectory(Uri imageUriToBeSaved){
        Log.d(TAG, "saveImageToAppLevelDirectory: ");
        try{
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(mContext.getContentResolver(), imageUriToBeSaved));
            }
            else{
                bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(),imageUriToBeSaved);
            }
            File directory = new File(mContext.getExternalFilesDir(null), Constants.IMAGES_FOLDER);
            directory.mkdirs();
            long timestamp = System.currentTimeMillis();
            String filename = timestamp+ ".jpeg";
            File file = new File(mContext.getExternalFilesDir(null), ""+ Constants.IMAGES_FOLDER+ "/"+ filename);
            try {
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100,fos);
                fos.flush();
                fos.close();
                Log.d(TAG, "saveImageToAppLevelDirectory: Image Saved");
                Toast.makeText(mContext, "Image Saved", Toast.LENGTH_SHORT).show();

            }catch (Exception e){
                Log.e(TAG, "saveImageToAppLevelDirectory: ",e);
                Log.d(TAG, "saveImageToAppLevelDirectory: Failed to save image due to "+e.getMessage());
                Toast.makeText(mContext, "Failed to save image due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
            }

        }catch (Exception e){
            Log.e(TAG, "saveImageToAppLevelDirectory: ",e);
            Log.d(TAG, "saveImageToAppLevelDirectory: Failed to prepare image due to "+e.getMessage());
            Toast.makeText(mContext, "Failed to prepare image due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputImageDialog(){
        Log.d(TAG, "showInputImageDialog: ");
        PopupMenu popupMenu = new PopupMenu(mContext, addImageFab);
        popupMenu.getMenu().add(Menu.NONE,1,1,"Camera");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int itemId = menuItem.getItemId();
                if (itemId == 1){
                    Log.d(TAG, "onMenuItemClick: Camera is clicked, check if camera permissions are granted or not.");
                    if (checkCameraPermission()){
                        pickImageCamera();
                    }
                    else {
                        requestCameraPermissions();
                    }
                } else if (itemId == 2){
                    Log.d(TAG, "onMenuItemClick: Camera is clicked, check if camera permissions are granted or not.");
                    if (checkStoragePermission()){
                        pickImageGallery();
                    }
                    else {
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: Picked Image Gallery" +imageUri);
                        saveImageToAppLevelDirectory(imageUri);
                        ModelImage modelImage = new ModelImage(imageUri, false);
                        allImageArrayList.add(modelImage);
                        adapterImage.notifyItemInserted(allImageArrayList.size());
                    }
                    else{
                        Toast.makeText(mContext, "Cancelled....", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera(){
        Log.d(TAG, "pickImageCamera: ");
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Temp Image Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Image Description");
        imageUri = mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Log.d(TAG, "onActivityResult: Picked Image camera:"+imageUri);
                        saveImageToAppLevelDirectory(imageUri);
                    }
                    else{
                        Toast.makeText(mContext, "Cancelled....", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission(){
        Log.d(TAG, "checkStoragePermission: ");
        boolean result = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    private void requestStoragePermission(){
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        Log.d(TAG, "checkCameraPermission: ");
        boolean cameraResult = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storageResult = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return cameraResult && storageResult;
    }

    private void requestCameraPermissions(){
        Log.d(TAG, "requestCameraPermissions: ");
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted){
                        pickImageCamera();
                        Log.d(TAG, "onRequestPermissionsResult: Both permissions (Camera and Gallery) are ganted, we can launch camera intent ");
                    }
                    else{
                        Log.d(TAG, "onRequestPermissionsResult: Camera & Storage Permissions are required");
                        Toast.makeText(mContext, "Camera & Storage Permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }
                else{
                    Log.d(TAG, "onRequestPermissionsResult: Cancelled....");
                    Toast.makeText(mContext, "Cancelled....", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){
                        Log.d(TAG, "onRequestPermissionsResult: Storage permission granted, we can launch gallery intent");
                        pickImageGallery();
                    }
                    else{
                        Toast.makeText(mContext, "Storage Permission is required", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "onRequestPermissionsResult: Storage permission, we can launch gallery intent.");
                    }
                }
                else{
                    Log.d(TAG, "onRequestPermissionsResult: Cancelled....");
                    Toast.makeText(mContext, "Cancelled....", Toast.LENGTH_SHORT).show();
                }
            }
            break;
        }
    }
}