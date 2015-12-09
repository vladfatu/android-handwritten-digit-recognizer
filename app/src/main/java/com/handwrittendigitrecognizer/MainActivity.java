package com.handwrittendigitrecognizer;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.ml.nn.ModelReader;
import com.ml.nn.analyzers.MNISTAnalyzer;
import com.ml.nn.model.Model;

import org.ejml.simple.SimpleMatrix;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_IMAGE_CAPTURE = 1;

    private ImageView thumbnailImageView;
    private TextView digitTextView;
    private Bitmap lastDigitImage;
    private Model model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        thumbnailImageView = ImageView.class.cast(findViewById(R.id.thumbnailImageView));
        digitTextView = TextView.class.cast(findViewById(R.id.digitTextView));

        BackgroundTask task = new BackgroundTask();
        task.execute();
    }

    private void initialiseModel() {
        try {
            readModel();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onTakePhotoClicked(View v) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    private void readModel() throws IOException {
        ModelReader modelReader = new ModelReader();
        model = modelReader.readModel();
    }

    private int getDigitFromBitmap() {
        Bitmap processedDigitImage = Bitmap.createScaledBitmap(lastDigitImage, 28, 28, false);
        double[][] imageData = doGreyScale(processedDigitImage);//toGrayScale(lastDigitImage);
        SimpleMatrix input = new SimpleMatrix(imageData);
        printMatrix(input);
        input.reshape(784, 1);
        SimpleMatrix outputVector = model.validate(input);
        outputVector.print();
        MNISTAnalyzer analyzer = new MNISTAnalyzer();
        int digit = analyzer.getDigit(outputVector);
        System.out.println("Detected digit: " + digit);
        return digit;
    }

    private void printMatrix(SimpleMatrix mat) {
        for (int i = 0; i < 28; i++) {
            for (int j = 0; j < 28; j++) {
                if (mat.get(i, j) > 0) {
                    System.out.print(" ");
                } else {
                    System.out.print("0");
                }
            }
            System.out.println();
        }
    }

    public double[][] doGreyScale(Bitmap src) {
        // constant factors
        final double GS_RED = 0.299;
        final double GS_GREEN = 0.587;
        final double GS_BLUE = 0.114;

        // create output bitmap
        Bitmap bmOut = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        // pixel information
        int A, R, G, B;
        int pixel;

        // get image size
        int width = src.getWidth();
        int height = src.getHeight();

        double[][] greyData = new double[height][width];

        // scan through every single pixel
        for (int x = 0; x < width; ++x) {
            for (int y = 0; y < height; ++y) {
                // get one pixel color
                pixel = src.getPixel(x, y);
                // retrieve color of all channels
                A = Color.alpha(pixel);
                R = Color.red(pixel);
                G = Color.green(pixel);
                B = Color.blue(pixel);
                // take conversion up to one single value
                R = G = B = (int) (GS_RED * R + GS_GREEN * G + GS_BLUE * B);
                // set new pixel color to output bitmap
                int composedColor = Color.argb(A, R, G, B);
                int color = 255 - R;
                if (color < 130) {
                    color = 0;
                }
                bmOut.setPixel(x, y, composedColor);
                greyData[y][x] = color;
            }
        }
        thumbnailImageView.setImageBitmap(bmOut);
        // return final image
        return greyData;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            lastDigitImage = (Bitmap) extras.get("data");
            thumbnailImageView.setImageBitmap(lastDigitImage);
            digitTextView.setText(Integer.toString(getDigitFromBitmap()));
        }
    }

    private class BackgroundTask extends AsyncTask<Void, Void, Void> {
        private ProgressDialog dialog;

        public BackgroundTask() {
            dialog = new ProgressDialog(MainActivity.this);
            dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage(MainActivity.this.getString(R.string.loading));
            dialog.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
        }

        @Override
        protected Void doInBackground(Void... params) {
            System.out.println("Started reading model");
            initialiseModel();
            System.out.println("finished reading model");

            return null;
        }

    }


}
