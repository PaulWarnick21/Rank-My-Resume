package me.paulwarnick.rankmyresume;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.camera2.TotalCaptureResult;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import hod.api.hodclient.HODApps;
import hod.api.hodclient.HODClient;
import hod.api.hodclient.IHODClientCallback;


public class MainActivity extends AppCompatActivity implements IHODClientCallback, View.OnClickListener {

    public String recognizedText = "";
    TextView mainTextView;
    TextView mainTextViewSkills;
    TextView maintextviewExperience;
    TextView maintextviewEtiquette;
    TextView total_score;
    TextView score1;
    TextView score2;
    TextView score3;
    Button mainButton;
    HODClient hodClient;
    public int buggerCount = 0;

    private static String logtag = "CameraApp";
    private static int TAKE_PICTURE = 1;
    private Uri imageUri;

    public double skillScore = 0;
    public double experienceScore = 0;
    public double etiquetteScore = 0;
    public double totalOverallScore = 0;

    public TextView textView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button cameraButton = (Button)findViewById(R.id.button_camera);
        cameraButton.setOnClickListener(cameraListener);

        hodClient = new HODClient("308154c7-252b-4acc-9f5c-ae5d8dbe1f1f", this);

        mainTextViewSkills = (TextView) findViewById(R.id.main_textviewSkills);
        mainTextViewSkills.setEnabled(false);
        maintextviewExperience = (TextView) findViewById(R.id.main_textviewExperience);
        maintextviewExperience.setEnabled(false);
        maintextviewEtiquette = (TextView) findViewById(R.id.main_textviewEtiquette);
        maintextviewEtiquette.setEnabled(false);

        total_score = (TextView) findViewById(R.id.total_score);
        score1 = (TextView) findViewById(R.id.score1);
        score2 = (TextView) findViewById(R.id.score2);
        score3 = (TextView) findViewById(R.id.score3);
    }

    private View.OnClickListener cameraListener = new View.OnClickListener() {
        public void onClick(View v) {
            takePhoto(v);
        }
    };

    private void takePhoto(View v){
            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
            File photo = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "YHack.jpg");
            imageUri = Uri.fromFile(photo);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
            startActivityForResult(intent, TAKE_PICTURE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        mainTextView = (TextView) findViewById(R.id.main_textview);
        mainTextView.setText("Scanning Image...");
        doOCR();
/*
        if (resultCode == Activity.RESULT_OK) {
            Uri selectedImage = imageUri;
            Log.d("CameraApp", "CameraApp "+imageUri);
            getContentResolver().notifyChange(selectedImage, null);

            try {
                bitmap = MediaStore.Images.Media.getBitmap(cr, selectedImage);
                imageView.setImageBitmap(bitmap);*/
                //Toast.makeText(MainActivity.this, selectedImage.toString(), Toast.LENGTH_LONG).show();
/*            }

            catch (Exception e) {
                Log.e(logtag, e.toString());
            }*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private File getLatestFilefromDir(String dirPath){
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        if (files == null || files.length == 0) {
            return null;
        }

        File lastModifiedFile = files[0];
        for (int i = 1; i < files.length; i++) {
            if (lastModifiedFile.lastModified() < files[i].lastModified()) {
                lastModifiedFile = files[i];
            }
        }
        return lastModifiedFile;
    }

    private void doOCR() {
        String hodApp = HODApps.OCR_DOCUMENT;
        Map<String, Object> params = new HashMap<String, Object>();


        //params.put("file", getLatestFilefromDir("/storage/emulated/0/DCIM/Camera/").toString());
        params.put("file", "/storage/emulated/0/YHack/YHack Tester Resume.jpg");
        params.put("mode", "document_photo");

        hodClient.PostRequest(params, hodApp, HODClient.REQ_MODE.ASYNC);
    }

    @Override
    public void requestCompletedWithJobID(String response) {
        try {
            JSONObject mainObject = new JSONObject(response);
            if (!mainObject.isNull("jobID")) {
                String jobID = mainObject.getString("jobID");
                hodClient.GetJobResult(jobID);
            }
        } catch (Exception ex) {
            ;//HandleException(response);
            mainTextView.setText("JobID Request Error");
        }
    }

    @Override
    public void requestCompletedWithContent(String response) {
        try {
            if (response.charAt(2) == 'a') {
                JSONObject mainObject = new JSONObject(response);
                JSONArray textBlockArray = mainObject.getJSONArray("actions");
                int count = textBlockArray.length();
                if (count > 0) {
                    for (int i = 0; i < count; i++) {
                        JSONObject actions = textBlockArray.getJSONObject(i);
                        JSONObject result = actions.getJSONObject("result");
                        if (!result.isNull("text_block")) {
                            JSONArray textArray = result.getJSONArray("text_block");
                            count = textArray.length();
                            if (count > 0) {
                                for (int n = 0; n < count; n++) {
                                    JSONObject texts = textArray.getJSONObject(n);
                                    recognizedText += texts.getString("text");
                                }
                            }
                        }
                    }
                }

                mainTextView.setText("Scan Complete");
                mainTextViewSkills.setEnabled(true);
                buggerCount++;
                doSkillCount();

            }

            if (response.charAt(2) == 'e') {
                JSONObject mainObject = new JSONObject(response);
                JSONArray entitiesArray = mainObject.getJSONArray("entities");
                int count = entitiesArray.length();
                int i = 0;
                String companies_eng = "";
                String professions = "";
                String universities = "";
                String profanities = "";
                String internet = "";

                if (count > 0) { // TODO Set up scoring system
                    for (i = 0; i < count; i++) {
                        JSONObject entity = entitiesArray.getJSONObject(i);
                        String type = entity.getString("type");
                        if (type.equals("companies_eng")) {
                            companies_eng += entity.getString("normalized_text") + "\n";
                        }
                        else if (type.equals("professions")) { // TODO if doctor PERFECT SCORE
                            professions += entity.getString("normalized_text") + "\n";
                        }
                        else if (type.equals("universities")) {
                            universities += entity.getString("normalized_text") + "\n";
                        }
                        else if (type.equals("profanities")) {
                            profanities += entity.getString("normalized_text") + "\n";
                        }
                        else if (type.equals("internet")) {
                            internet += entity.getString("normalized_text") + "\n";
                        }
                    }

                    String[] bigCompaniesArray = {
                            "Palantir Technologies", "Apple Inc", "Google Inc",
                            "Facebook Inc", "QUALCOMM Inc", "LinkedIn Corp",
                            "Hewlett-Packard Co", "International Business Machines Corp",
                            "Microsoft Corp", "Intel Corp", "Bloomberg"};
                    for (int j = 0; j < bigCompaniesArray.length; j++) {
                        if (companies_eng.contains(bigCompaniesArray[j])) { experienceScore = experienceScore + 3; }
                    }

                    String[] regularCompaniesArray = {
                            "software engineer", "software developer"};
                    for (int j = 0; j < regularCompaniesArray.length; j++) {
                        if (professions.contains(regularCompaniesArray[j])) { experienceScore = experienceScore + 1; }
                    }

                    String[] universitiesArray = {
                            "Harvard University", "University of Waterloo",
                            "Yale University", "Brown University",
                            "Columbia University", "Cornell University",
                            "Dartmouth College", "University of Pennsylvania",
                            "Princeton University"};
                    for (int j = 0; j < universitiesArray.length; j++) {
                        if (universities.contains(universitiesArray[j])) { experienceScore = experienceScore + 5; }
                    }

                    if (!(profanities.equals(""))) { experienceScore = experienceScore - 2; }

                    if (!(internet.equals(""))) { experienceScore = experienceScore + 1; }

                    // if (professions.contains("doctor")) { experienceScore = experienceScore + 1000;} // TODO Implement later for demo purposes

                    experienceScore = experienceScore * 0.65;
                    score2.setText(String.valueOf(experienceScore));
                    buggerCount++;
                    maintextviewExperience.setText("Experience Evaluated");
                    maintextviewEtiquette.setEnabled(true);
                    doSentiment();
                }
            }

            else if (buggerCount == 2){
                JSONObject mainObject = new JSONObject(response);
                JSONArray positiveArray = mainObject.getJSONArray("positive");
                JSONArray negativeArray = mainObject.getJSONArray("negative");
                int countPos = positiveArray.length();
                int countNeg = negativeArray.length();
                etiquetteScore = (countPos - countNeg) * 0.1;
                score3.setText(String.valueOf(etiquetteScore));
                //textView3.setText(String.valueOf(etiquetteScore));
                //mainTextView.setText(String.valueOf(etiquetteScore));

                maintextviewEtiquette.setText("Etiquette Evaluated");
                totalOverallScore = skillScore + experienceScore + etiquetteScore;
                if (totalOverallScore <= 10) {
                    total_score.setText(String.valueOf(totalOverallScore));
                }

                else {
                    total_score.setText(10);
                }
            }
        }

        catch (Exception ex) {
            // handle exception
            mainTextView.setText("Server Side Error");
            Log.e("OCR", "Exception: " + Log.getStackTraceString(ex));
        }
    }

    private void doSkillCount() { // TODO add in skills loading bar here
        String[] skillsArray = { // TODO maybe add any more common langauges
                "Java", "JavaScript", "Python",
                "PHP", "Visual Basic", "C++",
                "C", "Ruby", "Perl", "Objective-C",
                "Swift", "C#", "SQL", "MongoDB",
                "Scala", "Haskell", "XML",
                "HTML", "CSS", ".NET", "ASP"};

        for (int i = 0; i < skillsArray.length; i++) {
            if (recognizedText.contains(skillsArray[i])) { skillScore++; }
        }

        skillScore = skillScore * 0.25;
        score1.setText(String.valueOf(skillScore));

        mainTextViewSkills.setText("Skills Evaluated");
        maintextviewExperience.setEnabled(true);
        doEntityExtraction();
    }

    private void doEntityExtraction() {
        String hodApp = HODApps.ENTITY_EXTRACTION;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("text", recognizedText);
        Map<String, String> entity_array = new HashMap<String, String>();
        entity_array.put("entity_type", "companies_eng,professions,universities,profanities,internet,internet_email,person_name_component_eng");
        params.put("arrays", entity_array);
        hodClient.GetRequest(params, hodApp, HODClient.REQ_MODE.SYNC);
    }

    private void doSentiment() {
        String hodApp = HODApps.ANALYZE_SENTIMENT;
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("text", recognizedText);
        hodClient.GetRequest(params, hodApp, HODClient.REQ_MODE.SYNC);
    }

    @Override
    public void onErrorOccurred(String errorMessage) {
        // handle error if any
        mainTextView.setText(errorMessage);
    }

    @Override
    public void onClick(View v) {
        //mainButton.setEnabled(false);
        //mainTextView = (TextView) findViewById(R.id.main_textview);
        //mainTextView.setText("Scanning Image...");
        //doOCR();
    }
}