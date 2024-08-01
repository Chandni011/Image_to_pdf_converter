package com.deificdigital.imagetopdfconverter.activities;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.deificdigital.imagetopdfconverter.R;
import com.deificdigital.imagetopdfconverter.adapters.AdapterPdfView;
import com.deificdigital.imagetopdfconverter.models.ModelPdfView;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PdfViewActivity extends AppCompatActivity {

    private RecyclerView pdfViewRv;

    private String pdfUri;
    private static final String TAG = "PDF_VIEW_TAG";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pdf_view);

        getSupportActionBar().setTitle("PDF Viewer");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        pdfUri =getIntent().getStringExtra("pdfUri");
        Log.d(TAG, "onCreate: pdf" +pdfUri);
        loadPdfPages();

        pdfViewRv =findViewById(R.id.pdfViewRv);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private PdfRenderer.Page mCurrentPage = null;
    private void loadPdfPages() {
        Log.d(TAG, "loadPdfPages: ");
        ArrayList<ModelPdfView> pdfViewArrayList =new ArrayList<>();
        //AdapterPdfView adapterPdfView = new AdapterPdfView(this, pdfViewArrayList);
        //pdfViewRv.setAdapter(adapterPdfView);

        File file = new File(Uri.parse(pdfUri).getPath());
        try {
            getSupportActionBar().setSubtitle(file.getName());
            
        }catch (Exception e){
            Log.e(TAG, "loadPdfPages: ", e);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Handler handler =new Handler(Looper.getMainLooper());

        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ParcelFileDescriptor parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
                    PdfRenderer nPdfRenderer = new PdfRenderer(parcelFileDescriptor);
                    int pageCount = nPdfRenderer.getPageCount();

                    if (pageCount>0){
                        Log.d(TAG, "run: No pages in pdf");
                    }
                    else {
                        Log.d(TAG, "run: Have pages in Pdf file");

                        for (int i=0; i<pageCount; i++){
                            if (mCurrentPage != null){
                                mCurrentPage.close();
                            }
                            mCurrentPage = nPdfRenderer.openPage(i);
                            Bitmap bitmap = Bitmap.createBitmap(mCurrentPage.getWidth(), mCurrentPage.getHeight(), Bitmap.Config.ARGB_8888);
                            mCurrentPage.render(bitmap,null,null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                            pdfViewArrayList.add(new ModelPdfView(Uri.parse(pdfUri), (i+1), pageCount, bitmap));
                        }
                    }
                }catch (Exception e){
                    Log.e(TAG, "run: ", e);
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "run: UI Thread");
                       // adapterPdfView.notifyDataSetChanged();
                    }
                });
            }
        });
    }


    @Override
    public boolean onSupportNavigateUp() {
        getSupportActionBar();
        return super.onSupportNavigateUp();
    }
}