package com.deificdigital.imagetopdfconverter.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentTransaction;

import com.deificdigital.imagetopdfconverter.ImageListFragment;
import com.deificdigital.imagetopdfconverter.PdfListFragment;
import com.deificdigital.imagetopdfconverter.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        bottomNavigationView =findViewById(R.id.bottom_nav);

        //bottomNavigationView.setItemIconSize(getResources().getDimensionPixelSize(R.dimen.bottom_navigation_icon_size));

        // Set text appearance for active and inactive states
        //bottomNavigationView.setItemTextAppearanceActive(R.style.BottomNavigationTextStyle);
        //bottomNavigationView.setItemTextAppearanceInactive(R.style.BottomNavigationTextStyle);

        // Set item text color
        //bottomNavigationView.setItemTextColor(getResources().getColorStateList(R.color.grey));

        // Set item icon tint
        //bottomNavigationView.setItemIconTintList(getResources().getColorStateList(R.color.purple));
        loadImagesFragment();


      //  ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
        //    Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
          //  v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            //return insets;

        //});


        bottomNavigationView.setOnItemSelectedListener(item -> {

            int itemId = item.getItemId();

            if (itemId == R.id.bottom_menu_images){
                loadImagesFragment();
            }
            else if (itemId == R.id.bottom_menu_pdfs){
                loadPdfsFragment();
            }
            return true;
        });
    }
    private void loadImagesFragment(){
        setTitle("Image list");
        ImageListFragment imageListFragment = new ImageListFragment();
        FragmentTransaction fragmentTransaction =getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, imageListFragment,"Image list fragment");
        fragmentTransaction.commit();
    }
    private void loadPdfsFragment(){
        setTitle("PDFs list");
        PdfListFragment pdfListFragment = new PdfListFragment();
        FragmentTransaction fragmentTransaction =getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.frameLayout, pdfListFragment,"PDFs list fragment");
        fragmentTransaction.commit();
    }
}