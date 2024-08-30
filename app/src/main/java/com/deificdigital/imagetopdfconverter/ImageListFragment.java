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
import androidx.core.app.ActivityCompat;
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
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.deificdigital.imagetopdfconverter.adapters.AdapterImage;
import com.deificdigital.imagetopdfconverter.models.ModelImage;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImageListFragment extends Fragment {

    private static final String TAG = "Image List Tag";
    private static final int REQUEST_STORAGE_PERMISSION = 200;
    private static final int REQUEST_CAMERA_PERMISSION = 100;

    private Context mContext;

    private FloatingActionButton addImageFab;
    private RecyclerView imagesRv;
    private Uri imageUri = null;
    private ArrayList<ModelImage> allImageArrayList;
    private AdapterImage adapterImage;
    private ProgressDialog progressDialog;
    ImageView imageItemDelete, imageItemPdf;
    private List<Uri> imageUris = new ArrayList<>();

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


        addImageFab = view.findViewById(R.id.addImageFab);
        imagesRv = view.findViewById(R.id.imagesRv);
        progressDialog = new ProgressDialog(mContext);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);
        imageItemDelete = view.findViewById(R.id.images_item_delete);
        imageItemPdf = view.findViewById(R.id.images_item_pdf);

        loadImages();
        addImageFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showInputImageDialog();
            }
        });

        imageItemDelete.setOnClickListener(v -> {
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
        });

        imageItemPdf.setOnClickListener(v -> {
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
        });
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    private void convertImagesToPdf(boolean convertAll){
        Log.d(TAG, "convertImagesToPdf: convertAll: "+convertAll);

        progressDialog.setMessage("Converting to pdf");
        progressDialog.show();

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executorService.execute(() -> {
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

                        int pageWidth = 595; // A4 width at 300 DPI
                        int pageHeight = 842; // A4 height at 300 DPI

// Step 2: Calculate the scaling factor to fit the bitmap within the page
                        float scaleFactorWidth = (float) pageWidth / bitmap.getWidth();
                        float scaleFactorHeight = (float) pageHeight / bitmap.getHeight();
                        float scaleFactor = Math.min(scaleFactorWidth, scaleFactorHeight);

                        int scaledWidth = Math.round(bitmap.getWidth() * scaleFactor);
                        int scaledHeight = Math.round(bitmap.getHeight() * scaleFactor);
                        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, scaledWidth, scaledHeight, true);

                        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pageWidth, pageHeight, i + 1).create();

                        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

                        Paint paint = new Paint();
                        paint.setColor(Color.WHITE);
                        Canvas canvas = page.getCanvas();
                        canvas.drawPaint(paint);

                        int xPos = (pageWidth - scaledWidth) / 2;
                        int yPos = (pageHeight - scaledHeight) / 2;
                        canvas.drawBitmap(scaledBitmap, xPos, yPos, null);

                        pdfDocument.finishPage(page);
                        bitmap.recycle();
                    }
                    catch (Exception e){
                        Log.e(TAG, "run: ",e);
                    }
                }
                pdfDocument.writeTo(fileOutputStream);
                pdfDocument.close();
                fileOutputStream.close();
            }
            catch (Exception e){
                Toast.makeText(mContext, ""+e.getMessage(), Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
                Log.e(TAG, "run: ",e);
            }

            handler.post(() -> {
                Log.d(TAG, "run: Converted....");
                progressDialog.dismiss();
                Toast.makeText(mContext, "Converted....", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void deleteImages(boolean deleteAll) {
        ArrayList<ModelImage> imagesToDeleteList = new ArrayList<>();

        if (deleteAll) {
            imagesToDeleteList.addAll(allImageArrayList);
        } else {
            for (ModelImage image : allImageArrayList){
                if (image.isChecked()) {
                    imagesToDeleteList.add(image);
                }
            }
        }

        for (ModelImage image : imagesToDeleteList) {
            try {
                String pathOfImageToDelete = image.getImageUri().getPath();
                if (pathOfImageToDelete != null) {
                    File file = new File(pathOfImageToDelete);
                    if (file.exists() && file.delete()) {
                        Log.d(TAG, "deleteImages: Image deleted: " + pathOfImageToDelete);
                    } else {
                        Log.d(TAG, "deleteImages: Failed to delete image: " + pathOfImageToDelete);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "deleteImages: ", e);
            }
            image.setChecked(false);
        }
        allImageArrayList.removeAll(imagesToDeleteList);
        adapterImage.notifyDataSetChanged();

        Toast.makeText(mContext, "Deleted", Toast.LENGTH_SHORT).show();
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
//        popupMenu.getMenu().add(Menu.NONE,1,1,"Camera");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Gallery");
        popupMenu.show();

        popupMenu.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
//            if (itemId == 1){
//                Log.d(TAG, "onMenuItemClick: Camera is clicked, check if camera permissions are granted or not.");
//                if (checkCameraPermission()){
//                    pickImageCamera();
//                }
//                else {
//                    requestCameraPermissions.launch(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE});
//                }
//            }
            if (itemId == 2){
                Log.d(TAG, "onMenuItemClick: Camera is clicked, check if camera permissions are granted or not.");
                if (checkStoragePermission()){
                    pickImageGallery();
                }
                else {
                    requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }
            }
            return true;
        });
    }

    private void pickImageGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            if (data.getClipData() != null) {
                                // Multiple images were selected
                                int count = data.getClipData().getItemCount();
                                for (int i = 0; i < count; i++) {
                                    Uri imageUri = data.getClipData().getItemAt(i).getUri();
                                    Log.d(TAG, "onActivityResult: Picked Image Gallery " + imageUri);
                                    saveImageToAppLevelDirectory(imageUri);
                                    ModelImage modelImage = new ModelImage(imageUri, false);
                                    allImageArrayList.add(modelImage);
                                    adapterImage.notifyItemInserted(allImageArrayList.size());
                                }
                            } else if (data.getData() != null) {
                                // Single image was selected
                                Uri imageUri = data.getData();
                                Log.d(TAG, "onActivityResult: Picked Image Gallery " + imageUri);
                                saveImageToAppLevelDirectory(imageUri);
                                ModelImage modelImage = new ModelImage(imageUri, false);
                                allImageArrayList.add(modelImage);
                                adapterImage.notifyItemInserted(allImageArrayList.size());
                            }
                        }
                    } else {
                        Toast.makeText(mContext, "Cancelled....", Toast.LENGTH_SHORT).show();
                    }
                }
                private void checkPermissions() {
                    if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions((Activity) mContext, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    }
                }
            }
    );

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CAMERA_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with camera access
                } else {
                    // Permission denied, show a message to the user
                }
                break;
            case REQUEST_STORAGE_PERMISSION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, proceed with storage access
                } else {
                    // Permission denied, show a message to the user
                }
                break;
        }
    }

//    private void pickImageCamera(){
//        Log.d(TAG, "pickImageCamera: ");
//        ContentValues contentValues = new ContentValues();
//        contentValues.put(MediaStore.Images.Media.TITLE, "Temp Image Title");
//        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Temp Image Description");
//        imageUri = mContext.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
//        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
//        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//        cameraActivityResultLauncher.launch(intent);
//    }

//    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
//            new ActivityResultContracts.StartActivityForResult(),
//            new ActivityResultCallback<ActivityResult>() {
//                @Override
//                public void onActivityResult(ActivityResult result) {
//                    if (result.getResultCode() == Activity.RESULT_OK){
//                        Log.d(TAG,"onActivityResult: Picked Image camera:"+imageUri);
//                        imageUris.add(imageUri);
//                        saveImageToAppLevelDirectory(imageUri);
//                    }
//                    else{
//                        Toast.makeText(mContext, "Cancelled....", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//    );

    private boolean checkStoragePermission(){
        Log.d(TAG, "checkStoragePermission: ");
        boolean result = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        return result;
    }

    private ActivityResultLauncher<String> requestStoragePermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
                @Override
                public void onActivityResult(Boolean isGranted) {
                    Log.d(TAG, "onActivityResult: isGranted"+isGranted);

                    if (isGranted){
                        pickImageGallery();
                    }
                    else {
                        Toast.makeText(mContext, "Permission Denied", Toast.LENGTH_SHORT).show();
                    }

                }
            }
    );

//    private void requestPermissions() {
//        if (!checkCameraPermission()) {
//            requestCameraPermissions.launch(new String[] {
//                    Manifest.permission.CAMERA,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//            });
//        } else {
//            pickImageCamera();
//        }
//    }

    private boolean checkCameraPermission(){
        Log.d(TAG, "checkCameraPermission: ");
        boolean cameraResult = ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean storageResult = ContextCompat.checkSelfPermission(mContext, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        return cameraResult && storageResult;
    }
    
    private ActivityResultLauncher<String[]> requestCameraPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            new ActivityResultCallback<Map<String, Boolean>>() {

                @Override
                public void onActivityResult(Map<String, Boolean> result){
                    Log.d(TAG, "onActivityResult: "+result.toString());

                    boolean areAllGranted = true;
                    for (boolean isGranted: result.values()){
                        Log.d(TAG, "onActivityResult: isGranted" +isGranted);
                        areAllGranted = areAllGranted && isGranted;
                    }
                    if (areAllGranted){
                        Log.d(TAG, "onActivityResult: All Granted e.g. Camera and Storage");
                    }
                    else {
                        Log.d(TAG, "onActivityResult: Camera or Storage or Both Denied");
                        Toast.makeText(mContext, "Camera or Storage or both permissions denied.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );
}