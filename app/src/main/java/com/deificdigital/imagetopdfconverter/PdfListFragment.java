package com.deificdigital.imagetopdfconverter;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.deificdigital.imagetopdfconverter.activities.PdfViewActivity;
import com.deificdigital.imagetopdfconverter.adapters.AdapterPdf;
import com.deificdigital.imagetopdfconverter.models.ModelPdf;

import java.io.File;
import java.util.ArrayList;

public class PdfListFragment extends Fragment {

    private RecyclerView pdfRv;

    private Context mContext;
    private ArrayList<ModelPdf> pdfArrayList;
    private AdapterPdf adapterPdf;

    private static final String TAG = "PDF_LIST_TAG";

    public PdfListFragment() {
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
        return inflater.inflate(R.layout.fragment_pdf_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pdfRv = view.findViewById(R.id.pdfRv);
        loadPdfDocument();
    }
    private void loadPdfDocument(){
        pdfArrayList = new ArrayList<>();
        adapterPdf = new AdapterPdf(mContext, pdfArrayList, new RvListenersPdf() {
            @Override
            public void onPdfClick(ModelPdf modelPdf, int position) {
                Intent intent = new Intent(mContext, PdfViewActivity.class);
                intent.putExtra("pdfUri", "" + modelPdf.getUri());
                startActivity(intent);
            }

            @Override
            public void onPdfMoreClick(ModelPdf modelPdf, int position, AdapterPdf.HolderPdf holder) {
                showMoreOptions(modelPdf, holder);
            }
        });
        pdfRv.setAdapter(adapterPdf);

        File folder = new File(mContext.getExternalFilesDir(null), Constants.PDF_FOLDER);

        if (folder.exists()){
            File[] files = folder.listFiles();
            Log.d(TAG, "loadPdfDocument: Files Count: "+ files.length);

            for (File fileEntry: files){
                Log.d(TAG, "loadPdfDocument: File Name"+fileEntry.getName());
                Uri uri = Uri.fromFile(fileEntry);
                ModelPdf modelPdf = new ModelPdf(fileEntry, uri);
                pdfArrayList.add(modelPdf);
                adapterPdf.notifyItemInserted(pdfArrayList.size());
            }
        }
    }

    private void showMoreOptions(ModelPdf modelPdf, AdapterPdf.HolderPdf holder) {

        Log.d(TAG, "showMoreOptions: ");

        PopupMenu popupMenu = new PopupMenu(mContext, holder.moreBtn);
        popupMenu.getMenu().add(Menu.NONE,0,0,"Rename");
        popupMenu.getMenu().add(Menu.NONE,1,1,"Delete");
        popupMenu.getMenu().add(Menu.NONE,2,2,"Share");

        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == 0){
                    pdfRename(modelPdf);
                } else if (itemId == 1) {
                    pdfDelete(modelPdf);
                }
                else if (itemId == 2){
                    sharePdf(modelPdf);
                }
                return true;
            }
        });
    }

    private void pdfRename(ModelPdf modelPdf){
        Log.d(TAG, "pdfRename: ");

        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_rename, null);
        EditText pdfNewNameEt = view.findViewById(R.id.pdfNewNameEt);
        Button renameBtn = view.findViewById(R.id.renameBtn);

        String previousName = ""+modelPdf.getFile().getName();
        Log.d(TAG, "pdfRename: previousName"+previousName);
        
        pdfNewNameEt.setText(previousName);
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setView(view);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        renameBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String newName = pdfNewNameEt.getText().toString().trim();
                Log.d(TAG, "onClick: newName"+newName);

                if (newName.isEmpty()){
                    Toast.makeText(mContext, "Enter Name", Toast.LENGTH_SHORT).show();
                }
                else {
                    try {
                        File newFile = new File(mContext.getExternalFilesDir(null),Constants.PDF_FOLDER+ "/" + ".pdf");

                        modelPdf.getFile().renameTo(newFile);
                        Toast.makeText(mContext, "Renamed successfully....", Toast.LENGTH_SHORT).show();

                    }catch (Exception e){
                        Log.e(TAG, "onClick: ", e);
                        Toast.makeText(mContext, "Failed to rename due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    alertDialog.dismiss();
                }
            }
        });

    }
    private void pdfDelete(ModelPdf modelPdf){
        Log.d(TAG, "pdfDelete: ");

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setTitle("Delete file")
                .setMessage("Are you sure you want to delete the" +modelPdf.getFile().getName())
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        try{
                            modelPdf.getFile().delete();
                            Toast.makeText(mContext, "Delete Successfully", Toast.LENGTH_SHORT).show();
                            loadPdfDocument();

                        }catch (Exception e){
                            Log.e(TAG, "onClick: ", e);
                            Toast.makeText(mContext, "Failed to delete due to"+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void sharePdf(ModelPdf modelPdf) {
        Log.d(TAG, "sharePdf: ");
        File file = modelPdf.getFile();

        // Ensure the file exists
        if (!file.exists()) {
            Toast.makeText(mContext, "File does not exist", Toast.LENGTH_SHORT).show();
            return;
        }

        Uri uri = FileProvider.getUriForFile(mContext, "com.deificdigital.imagetopdfconverter.fileprovider", file);

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_STREAM, uri);

        mContext.startActivity(Intent.createChooser(intent, "Share Pdf"));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, you can perform the file operations now
            } else {
                // Permission denied
                Toast.makeText(mContext, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
            }
        }
    }


}