package ro.atm.corden.view.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputLayout;

import petrov.kristiyan.colorpicker.ColorPicker;
import ro.atm.corden.R;
import ro.atm.corden.util.adapter.UserMapAdapter;
import ro.atm.corden.util.websocket.Repository;

/**  */
public class SaveMapItemDialog extends AppCompatDialogFragment {
    private static final String TAG = "SaveMapItemDialog";

    private TextInputLayout mInputTextLayoutName;
    private TextInputLayout mInputTextLayoutDescription;
    private Button mColorPickerButton;

    private int mColor = 0;

    private SaveMapDialogListener listener;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_save_map_item, null);

        builder.setView(view)
                .setTitle("Save map detail")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        listener.cancel();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = mInputTextLayoutName.getEditText().getText().toString();
                        String description = mInputTextLayoutDescription.getEditText().getText().toString();
                        listener.saveMap(name, description, mColor);
                    }
                });

        mInputTextLayoutName = view.findViewById(R.id.text_input_name);
        mInputTextLayoutDescription = view.findViewById(R.id.text_input_description);
        mColorPickerButton = view.findViewById(R.id.buttonColorPicker);

        mColorPickerButton.setOnClickListener(v -> {
            final ColorPicker colorPicker = new ColorPicker(getActivity());
            colorPicker.setOnChooseColorListener(new ColorPicker.OnChooseColorListener() {
                @Override
                public void onChooseColor(int position, int color) {
                    Log.d(TAG, "Color choosed " + color);
                }

                @Override
                public void onCancel() {
                    Log.d(TAG, "onCancel");
                    // put code
                }
            })
                    .addListenerButton("Choose", new ColorPicker.OnButtonListener() {
                        @Override
                        public void onClick(View v, int position, int color) {
                            Log.d(TAG, "onClick, position: " + position + " color: " + color);
                            mColor = color;
                            mColorPickerButton.setBackgroundColor(color);
                            colorPicker.dismissDialog();
                        }
                    })
                    .disableDefaultButtons(true)
                    .setTitle("Choose a color")
                    .setColumns(4)
                    .show();
        });

        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        try {
            listener = (SaveMapDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + "must implement SaveMapDialogListener");
        }
    }

    public interface SaveMapDialogListener{
        void saveMap(String name, String description, int color);
        void cancel();
    }
}
