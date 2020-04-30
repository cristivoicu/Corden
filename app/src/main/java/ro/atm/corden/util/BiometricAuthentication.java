package ro.atm.corden.util;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.biometric.BiometricPrompt.AuthenticationCallback;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;


/**
 * It is a helper class for biometric authentication
 * If no fingerprint sensor is available, user must use a pin code.
 */
public class BiometricAuthentication {
    public boolean canAuthenticate = false;
    public boolean hasNoHardware = false;
    public boolean isNoOneEnrolled = false;
    public boolean isHardwareUnavailable = false;

    private AppCompatActivity appCompatActivity;
    private Executor mExecutor;
    private BiometricPrompt mBiometricPrompt;
    private BiometricManager mBiometricManager;
    private androidx.biometric.BiometricPrompt.PromptInfo mPromptInfo;

    public BiometricAuthentication(AppCompatActivity appCompatActivity) {
        this.appCompatActivity = appCompatActivity;
        mExecutor = ContextCompat.getMainExecutor(appCompatActivity);
        mBiometricPrompt = new BiometricPrompt(appCompatActivity,
                mExecutor,
                new AuthenticationCallback() {
            @Override
            public void onAuthenticationError(int errorCode,
                                              @NonNull CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
                Toast.makeText(appCompatActivity,
                        "Authentication error: " + errString, Toast.LENGTH_SHORT)
                        .show();
            }

            @Override
            public void onAuthenticationSucceeded(
                    @NonNull BiometricPrompt.AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                Toast.makeText(appCompatActivity,
                        "Authentication succeeded!", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                Toast.makeText(appCompatActivity, "Authentication failed",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
        setBiometricPromptInfo();
    }

    private void setBiometricPromptInfo(){
        mPromptInfo = new androidx.biometric.BiometricPrompt.PromptInfo.Builder()
                .setTitle("Biometric authentication")
                .setSubtitle("Please use your fingerprint")
                .setNegativeButtonText("Use application PIN")
                .build();
    }

    private boolean[] checkAvailability(){
        BiometricManager biometricManager = BiometricManager.from(appCompatActivity);
        switch (biometricManager.canAuthenticate()) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                canAuthenticate = true;
                break;
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                hasNoHardware = true;
                break;
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                isHardwareUnavailable = true;
                break;
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                isNoOneEnrolled = true;
                break;
        }
        return new boolean[]{canAuthenticate, hasNoHardware, isHardwareUnavailable, isNoOneEnrolled};
    }

    public void authenticate(){
        checkAvailability();
        if(canAuthenticate){
            mBiometricPrompt.authenticate(mPromptInfo);
        }
    }
}
