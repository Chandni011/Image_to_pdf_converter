package com.deificdigital.imagetopdfconverter.models;

import android.net.Uri;

public class ModelImage {
    Uri imageUri;
    boolean checked;

    public ModelImage(Uri imageUri, boolean checked) {
        this.imageUri = imageUri;
        this.checked = checked;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModelImage that = (ModelImage) o;
        return imageUri.equals(that.imageUri);
    }

    @Override
    public int hashCode() {
        return imageUri.hashCode();
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }
}
