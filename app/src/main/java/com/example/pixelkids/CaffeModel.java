package com.example.pixelkids;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;

public class CaffeModel {
    private Net net;

    CaffeModel(String protxt, String caffeModel){
        net = Dnn.readNetFromCaffe(protxt,caffeModel);
    }

    public float[] prediccion(Mat imagen){
        Mat inputBlob = Dnn.blobFromImage(imagen, 1.0, new Size(224, 224),
                new Scalar(104, 117, 123), false, false);

        net.setInput(inputBlob);
        Mat outputBlob = net.forward();

        float[] predicciones = new float[(int) (outputBlob.total() * outputBlob.channels())];
        Core.norm(outputBlob,outputBlob, Core.NORM_L2);

        return  predicciones;
    }
}
