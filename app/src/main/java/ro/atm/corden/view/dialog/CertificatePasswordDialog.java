package ro.atm.corden.view.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;

import ro.atm.corden.R;

public class CertificatePasswordDialog extends AppCompatDialogFragment {
    private CertificatePasswordDialogListener mListener;
    private EditText mPassword;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_certificate_password, null);
        mPassword = view.findViewById(R.id.password);
        builder.setView(view)
                .setTitle("Enter certificate password!")
                .setPositiveButton("Ok", (dialog, which) -> {
                    mListener.onPassword(mPassword.getText().toString());
                });
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (CertificatePasswordDialogListener) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
    }

    public interface CertificatePasswordDialogListener {
        void onPassword(String password);
    }
}
