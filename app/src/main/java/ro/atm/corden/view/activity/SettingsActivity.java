package ro.atm.corden.view.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerationAndroid;

import java.util.ArrayList;
import java.util.List;

import ro.atm.corden.R;
import ro.atm.corden.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding mBinding;
    private List<CameraEnumerationAndroid.CaptureFormat> mFrontCameraFormats;
    private List<CameraEnumerationAndroid.CaptureFormat> mBackCameraFormats;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_settings);

        setAdapterForSpinner();
        setListenerForSpinner();

    }

    /**
     * Sets the adapter for spinners
     * Finds the supported camera formats for build in camera (back camera and front camera)
     */
    private void setAdapterForSpinner() {
        // find resolution list for build in camera
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        mFrontCameraFormats = enumerator.getSupportedFormats("0");
        mBackCameraFormats = enumerator.getSupportedFormats("1");

        List<String> adapterFrontSpinner = new ArrayList<>();
        List<String> adapterBackSpinner = new ArrayList<>();

        SharedPreferences sharedPreferences = this.getSharedPreferences(getString(R.string.settingSharedPreferences), Context.MODE_PRIVATE);
        int heightFront = Integer.parseInt(sharedPreferences.getString(getString(R.string.frontCameraHeight), "0"));
        int widthFront = Integer.parseInt(sharedPreferences.getString(getString(R.string.frontCameraWidth), "0"));
        int positionFront = -1;

        int heightBack = Integer.parseInt(sharedPreferences.getString(getString(R.string.backCameraHeight), "0"));
        int widthBack = Integer.parseInt(sharedPreferences.getString(getString(R.string.backCameraWidth), "0"));
        int positionBack = -1;

        for (CameraEnumerationAndroid.CaptureFormat captureFormat : mFrontCameraFormats) {
            adapterFrontSpinner.add(String.format("%s x %s", captureFormat.width, captureFormat.height));
            if(captureFormat.width == widthFront && captureFormat.height == heightFront){
                positionFront = mFrontCameraFormats.indexOf(captureFormat);
            }
        }
        for (CameraEnumerationAndroid.CaptureFormat captureFormat : mBackCameraFormats) {
            adapterBackSpinner.add(String.format("%s x %s", captureFormat.width, captureFormat.height));
            if(captureFormat.width == widthBack && captureFormat.height == heightBack){
                positionBack = mFrontCameraFormats.indexOf(captureFormat);
            }
        }
        ArrayAdapter arrayFrontAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, adapterFrontSpinner);
        arrayFrontAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter arrayBackAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, adapterBackSpinner);
        arrayBackAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mBinding.backResolutions.setAdapter(arrayBackAdapter);
        mBinding.frontResolutions.setAdapter(arrayFrontAdapter);


        if(positionBack != -1){
            mBinding.backResolutions.setSelection(positionBack);
        }
        if(positionFront != -1){
            mBinding.frontResolutions.setSelection(positionFront);
        }
    }

    /**
     * Set on selected item listener.
     * When user sets a new resolution, the application saves it in shared preferences
     */
    private void setListenerForSpinner() {
        SharedPreferences.Editor sharedPref = this.getSharedPreferences(getString(R.string.settingSharedPreferences), Context.MODE_PRIVATE).edit();

        mBinding.frontResolutions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CameraEnumerationAndroid.CaptureFormat captureFormat = mFrontCameraFormats.get(position);
                sharedPref.putString(getString(R.string.frontCameraHeight), String.valueOf(captureFormat.height));
                sharedPref.putString(getString(R.string.frontCameraWidth), String.valueOf(captureFormat.width));
                sharedPref.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mBinding.backResolutions.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                CameraEnumerationAndroid.CaptureFormat captureFormat = mBackCameraFormats.get(position);
                sharedPref.putString(getString(R.string.backCameraHeight), String.valueOf(captureFormat.height));
                sharedPref.putString(getString(R.string.backCameraWidth), String.valueOf(captureFormat.width));
                sharedPref.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }
}
