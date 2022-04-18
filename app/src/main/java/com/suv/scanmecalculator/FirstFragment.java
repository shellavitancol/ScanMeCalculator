package com.suv.scanmecalculator;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.TextBlock;
import com.google.android.gms.vision.text.TextRecognizer;
import java.io.File;
import java.util.Stack;

public class FirstFragment extends Fragment {

    private static final int CAMERA_IMAGE_REQUEST = 101;
    private static final int SELECT_PICTURE_REQUEST = 102;
    private static final int MY_CAMERA_PERMISSION_CODE = 100;
    private static final int MY_EXTERNAL_PERMISSION_CODE = 200;
    private String imageName;
    private String imageFolderPath;
    private TextView textview_input;
    private TextView textview_result;
    private TextRecognizer detector;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_first, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());

        //declaring textview to show the expression and answer
        textview_input = view.findViewById(R.id.textview_input);
        textview_result = view.findViewById(R.id.textview_result);
        detector = new TextRecognizer.Builder(this.getContext()).build();
        view.findViewById(R.id.button_first).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                captureImage();
                //openFileSystem();
            }
        });
    }

    @SuppressLint("NewApi")
    public void openFileSystem(){
        //open file system with filter of image only
        //Check permission first before opening filesystem
        if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_EXTERNAL_PERMISSION_CODE);
        }else {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, SELECT_PICTURE_REQUEST);
        }
    }

    @SuppressLint("NewApi")
    public void captureImage() {
        //capture image
        //check permission first for camera and external storage where the image will be saved
        if (getActivity().checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, MY_EXTERNAL_PERMISSION_CODE);
        }else{
            // Create new folder for image saving
            imageFolderPath = Environment.getExternalStorageDirectory().toString()
                    + "/ScanMeCalculator";
            //creating folder if not exist
            File imagesFolder = new File(imageFolderPath);
            imagesFolder.mkdirs();

            // Generate 1 file name (Override succeeding images)
            imageName = "test.jpg";
            // Create image
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(new File(imageFolderPath, imageName)));

            if (getActivity().checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, MY_CAMERA_PERMISSION_CODE);
            }
            else
            {
                startActivityForResult(takePictureIntent, CAMERA_IMAGE_REQUEST);
            }
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // TODO Auto-generated method stub
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CAMERA_IMAGE_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(getActivity(), "Success ", Toast.LENGTH_SHORT).show();
                    String filePath = new File(imageFolderPath, imageName).getPath();
                    ImageToText(filePath);
                } else {
                    Toast.makeText(getActivity(), "Take Picture Failed or canceled",
                            Toast.LENGTH_SHORT).show();
                }
                break;
            case SELECT_PICTURE_REQUEST:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        try {
                            Uri content_describer = data.getData();
                            ImageToText(getPath(content_describer));
                        } catch (Exception e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                }
                break;
        }
    }

    public String getPath(Uri uri) {
        //getting the real filepath if from file system
        Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContext().getContentResolver().query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }



    public void ImageToText(String filePath){
        //Use TextRecognizer to detect characters in image
        TextRecognizer textRecognizer = new TextRecognizer.Builder(getContext()).build();// getApplicationContext()).build();
        Bitmap bitmap = BitmapFactory.decodeFile(filePath);

        Frame imageFrame = new Frame.Builder()
                        .setBitmap(bitmap)// your image bitmap
                        .build();

        String imageText = "";
        SparseArray<TextBlock> textBlocks = textRecognizer.detect(imageFrame);

        for (int i = 0; i < textBlocks.size(); i++) {
            TextBlock textBlock = textBlocks.get(textBlocks.keyAt(i));
            imageText = textBlock.getValue();// return string
            textview_input.setText("Input : "+imageText);
            textview_result.setText("Result : "+ (evaluate(imageText)==0 ? "Invalid expression" : evaluate(imageText)) );
        }
    }


    public int evaluate(String expression){
        boolean validExpression = true;
        char[] token = expression.toCharArray();

        Stack<Integer> operands = new Stack<Integer>();
        Stack<Character> operator = new Stack<Character>();

        for (int i = 0; i < token.length; i++)
        {
            //skip the spaces
            if (token[i]==' '){

            }
            //Check if number
            else if (Character.isDigit(token[i]))
            {
                StringBuffer stringBuffer = new StringBuffer();

                while (i < token.length && Character.isDigit(token[i]))
                    stringBuffer.append(token[i++]);

                operands.push(Integer.parseInt(stringBuffer.toString()));
                i--;
            }
            //Check if operations
            else if (token[i] == '+' ||
                    token[i] == '-' ||
                    token[i] == '*' ||
                    token[i] == '/')
            {
                while (!operator.empty())
                    operands.push(solve(operator.pop(),operands.pop(), operands.pop()));

                operator.push(token[i]);
            }else{
                //invalid expression (not space, not a number, and not an operator)
                validExpression = false;
                break;
            }
        }

        if (validExpression) {
            while (!operator.empty())
                operands.push(solve(operator.pop(),
                        operands.pop(),
                        operands.pop()));

            return operands.pop();
        }else return 0;
    }

    //solve the expression
    public static int solve(char operator, int secondOperand, int firstOperand)
    {
        switch (operator)
        {
            case '+':
                return firstOperand + secondOperand;
            case '-':
                return firstOperand - secondOperand;
            case '*':
                return firstOperand * secondOperand;
            case '/':
                if (secondOperand == 0)
                    throw new
                            UnsupportedOperationException(
                            "Invalid");
                return firstOperand / secondOperand;
        }
        return 0;
    }

}