package com.example.pixelkids;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;

import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Map;
import java.util.TreeMap;

public class ReconocimientoEdad {
    private Interpreter interpreter;
    //Cargar el modelo de prediccion

    private int INT_SIZE;

    //Escala que se usa para imagen
    private float IMAGEN_STD = 255.0f;
    private float IMAGEN_MEAN = 0;

    //se usa para configurar la GPU y el subproceso para Interpreter
    private GpuDelegate gpuDelegate = null;

    private int height = 0;
    private int width = 0;

    //Se usa para cargar haar-cascade de opencv
    private CascadeClassifier cascadeClassifier;

    //El constructor lo llamaremos en el OnCreate de CamaraActivity
    ReconocimientoEdad(AssetManager assetManager, Context context, String modePath, int intputSize) throws IOException{
        //definir GPU y número de subprocesos para el intérprete
        Interpreter.Options options = new Interpreter.Options();
        gpuDelegate = new GpuDelegate();
        options.addDelegate(gpuDelegate);
        options.setNumThreads(4); //Establecer el numero de hilos para el telefono
        //si el teléfono tiene más hilos, la velocidad de fotogramas será alta

        //cargar modelo
        interpreter = new Interpreter(loadModelFile(assetManager, modePath),options);

        //Comprobar que se ha cargado el modelo en el interprete
        Log.d("Reconocimiento Edad","CNN model is loaded");

        //Cargar el modelo haar-cascade
        InputStream is =  context.getResources().openRawResource(R.raw.haarcascade_frontalface_default);
        File cascadeDir = context.getDir("cascade", Context.MODE_PRIVATE);
        File mCascadeFile =  new File(cascadeDir, "haarcascade_frontalface_default");
        FileOutputStream os = new FileOutputStream(mCascadeFile);

        byte[] buffer = new byte[4096];
        int byteRead;

        while((byteRead = is.read(buffer)) != -1){
            os.write(buffer,0, byteRead);
        }

        is.close();
        os.close();

        cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
        //Para saber si se ha cargado el xml en el cascade
        Log.d("ReconocimientoEdad", "CascadeClassifier cargado");


    }

    public Mat reconocimientoImagen(Mat matImage, Context context){

        //rotar imagen
        Core.flip(matImage.t(), matImage, 1);

        //Escalar a grises
        Mat grayScaleImage = new Mat();
        Imgproc.cvtColor(matImage, grayScaleImage,Imgproc.COLOR_RGBA2GRAY);

        height = grayScaleImage.height();
        width = grayScaleImage.width();

        int absoluteFaceSize = (int) (height*0.1);
        MatOfRect faces =  new MatOfRect();

        //Comprobación si la cascada esta a cargada
        if(cascadeClassifier != null){
            cascadeClassifier.detectMultiScale(grayScaleImage,faces,1.1,2,2,
                    new Size(absoluteFaceSize,absoluteFaceSize), new Size() );
        }

        for(Rect face : faces.toList()){
            //Dibujar Rectangulo
                                      //primer  utlimo punto
            Imgproc.rectangle(matImage,face.tl(),face.br(),new Scalar(0,255,0,255),2);
            Rect roi = new Rect((int)face.tl().x, (int)face.tl().y,
                    (int)(face.br().x)-(int)(face.tl().x),
                    (int)(face.br().y)-(int)(face.tl().y));

            Mat recortar = new Mat(grayScaleImage, roi);
            Mat recortarRgba = new Mat(matImage,roi);

            Bitmap bitmap = null;
            bitmap = Bitmap.createBitmap(recortarRgba.cols(),recortarRgba.rows(),Bitmap.Config.ARGB_8888);

            //Convertir un mat a bitmap
            Utils.matToBitmap(recortarRgba,bitmap);
            Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap,96,96, false);

            //Conversion de bitmap a buffer
            ByteBuffer byteBuffer = conversionBitmapAbyteBuffer(scaleBitmap);
            Object[] input = new Object[1];
            input[0] = byteBuffer;

            Map<Integer,Object> outputMap = new TreeMap<>();
            float[][] edad = new float[1][1];
            float[][] genero = new float[1][1];

            outputMap.put(0,edad);
            outputMap.put(0,genero);

            //Prediccion
            interpreter.runForMultipleInputsOutputs(input, outputMap);

            Object oEdad = outputMap.get(0);

            //Extraer valor del objeto
            int edadValor= (int)(float) Array.get(Array.get(oEdad,0),0);

            Imgproc.putText(recortarRgba, "Edad: " + edadValor, new Point(10,20),1,1.5,
                   new Scalar(0,0,255,255),2);
            //Control de edad y genero
            Log.d("ReconocimientoEdad", "edad: " + edadValor);
            Toast.makeText(context, "Edad: " + edadValor, Toast.LENGTH_SHORT).show();

            //Reemplazo de la cara original por la recortada
            recortarRgba.copyTo(new Mat(matImage, roi));
        }

        //antes de retornarlo se vuelve a girar
        Core.flip(matImage.t(), matImage,0);
        return matImage;
    }

    private ByteBuffer conversionBitmapAbyteBuffer(Bitmap scaleBitmap) {
        ByteBuffer byteBuffer;
        int sizeImage = 96;

        byteBuffer = ByteBuffer.allocateDirect(4*1*sizeImage*sizeImage*3);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[sizeImage*sizeImage];
        scaleBitmap.getPixels(intValues,0,scaleBitmap.getWidth(),0,0,scaleBitmap.getWidth(),scaleBitmap.getHeight());
        int pixel = 0;

        for(int i = 0; i < sizeImage; i++){
            for(int j = 0; j < sizeImage; j++){
                final int val = intValues[pixel++];
                byteBuffer.putFloat((((val>>16)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>8)&0xFF))/255.0f);
                byteBuffer.putFloat((((val>>0xFF)&0xFF))/255.0f);
            }
        }

        return  byteBuffer;
    }

    private MappedByteBuffer  loadModelFile(AssetManager assetManager, String modePath) throws IOException {
        //
        AssetFileDescriptor assetFileDescriptor = assetManager.openFd(modePath);
        FileInputStream inputStream = new FileInputStream(assetFileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long starOffSet = assetFileDescriptor.getStartOffset();
        long declaredLength = assetFileDescriptor.getDeclaredLength();

        return fileChannel.map(FileChannel.MapMode.READ_ONLY, starOffSet,declaredLength);
    }
}
