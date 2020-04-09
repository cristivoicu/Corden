package ro.atm.corden.view.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

public class EditMapItemDialog extends AppCompatDialogFragment {
    public static final String TAG = "EditMapItemDialog";

    private EditMapDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (EditMapDialogListener) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

    }

    public interface EditMapDialogListener {

    }
}
