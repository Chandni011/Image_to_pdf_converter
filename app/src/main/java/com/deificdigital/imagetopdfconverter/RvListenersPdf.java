package com.deificdigital.imagetopdfconverter;

import com.deificdigital.imagetopdfconverter.models.ModelPdf;

public abstract class RvListenersPdf {

    public abstract void onPdfClick(ModelPdf modelPdf, int position);
    public abstract void onPdfMoreClick(ModelPdf modelPdf, int position);
}
