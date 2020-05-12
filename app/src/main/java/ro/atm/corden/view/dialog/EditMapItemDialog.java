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
import androidx.appcompat.app.AppCompatDialog;
import androidx.appcompat.app.AppCompatDialogFragment;

import com.google.android.material.textfield.TextInputLayout;

import petrov.kristiyan.colorpicker.ColorPicker;
import ro.atm.corden.R;
import ro.atm.corden.model.map.MapItem;

public class EditMapItemDialog extends AppCompatDialogFragment {
    public static final String TAG = "EditMapItemDialog";

    private String mName;
    private String mDesc;
    private int mColor;
    private String mID;
    private String itemType;

    private MapItem mMapItem;


    private TextInputLayout mInputTextLayoutName;
    private TextInputLayout mInputTextLayoutDescription;
    private Button mColorPickerButton;
    private EditMapDialogListener listener;

    public EditMapItemDialog(String itemId, String type, String name, String description, int color) {
        mName = name;
        mDesc = description;
        mColor = color;
    }

    public EditMapItemDialog(MapItem mMapItem, String id) {
        this.mMapItem = mMapItem;
        this.mID = id;
    }

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
                        listener.cancelEdit();
                    }
                })
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = mInputTextLayoutName.getEditText().getText().toString();
                        String description = mInputTextLayoutDescription.getEditText().getText().toString();
                        mMapItem.setName(name);
                        mMapItem.setDescription(description);
                        mMapItem.setColor(mColor);
                        listener.editedMap(mID, mMapItem);
                    }
                });

        mInputTextLayoutName = view.findViewById(R.id.text_input_name);
        mInputTextLayoutDescription = view.findViewById(R.id.text_input_description);
        mColorPickerButton = view.findViewById(R.id.buttonColorPicker);

        this.mInputTextLayoutName.getEditText().setText(mMapItem.getName());
        this.mInputTextLayoutDescription.getEditText().setText(mMapItem.getDescription());
        mColorPickerButton.setBackgroundColor(mMapItem.getColor());

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
            listener = (EditMapDialogListener) context;
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

    }

    public interface EditMapDialogListener {
        void editedMap(String id, MapItem mapItem);
        void cancelEdit();
    }
}
