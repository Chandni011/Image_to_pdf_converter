package com.deificdigital.imagetopdfconverter.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.deificdigital.imagetopdfconverter.activities.ImageViewActivity;
import com.deificdigital.imagetopdfconverter.R;
import com.deificdigital.imagetopdfconverter.models.ModelImage;

import java.util.ArrayList;

public class AdapterImage extends RecyclerView.Adapter<AdapterImage.HolderImage>{

    private Context context;
    private ArrayList<ModelImage> imageArrayList;

    public AdapterImage(Context context, ArrayList<ModelImage> imageArrayList) {
        this.context = context;
        this.imageArrayList = imageArrayList;
    }

    @NonNull
    @Override
    public HolderImage onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.row_image,parent,false);
        return new HolderImage(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HolderImage holder, int position) {
        ModelImage modelImage = imageArrayList.get(position);
        Uri imageUri = modelImage.getImageUri();
        Glide.with(context).load(imageUri).placeholder(R.drawable.ic_image_black).into(holder.imageIv);

        holder.checkBox.setOnCheckedChangeListener(null);  // Prevent triggering the listener when updating the state
        holder.checkBox.setChecked(modelImage.isChecked());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, ImageViewActivity.class);
                intent.putExtra("imageUri", ""+imageUri);
                context.startActivity(intent);
            }
        });

        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                modelImage.setChecked(isChecked);
            }
        });

    }
    @Override
    public int getItemCount() {
       return imageArrayList.size();
    }

    class HolderImage extends RecyclerView.ViewHolder{

        ImageView imageIv;
        CheckBox checkBox;

        public HolderImage(@NonNull View itemView) {
            super(itemView);

            imageIv = itemView.findViewById(R.id.imageIv);
            checkBox = itemView.findViewById(R.id.checkBox);
        }
    }
}
