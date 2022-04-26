package com.crap.mukluk;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;

public class AddEditWorldActivity extends Activity
{
    //private static final String TAG = "AddEditWorldActivity";
    public static final int ADD_WORLD = 1;
    public static final int EDIT_WORLD = 2;

    private boolean settingsChanged;

    private Context context;
    private World worldToEdit = null;
    private EditText txtWorldName;
    private EditText txtWorldHost;
    private EditText txtWorldPort;
    private Spinner spnEncoding;
    private CheckBox chkLogging;
    private CheckBox chkAnsiColor;
    private EditText txtPostLoginCommand;
    private Button saveButton;
    private Button cancelButton;

    private ArrayAdapter<String> adapter;
    private String previousEncodingSetting = null;

    // Called when "OK" is clicked on the save confirmation box
    DialogInterface.OnClickListener saveChangesOkayListener = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            saveChanges();
        }
    };

    // Called when "Don't Save" is clicked on the save confirmation box
    DialogInterface.OnClickListener saveChangesDontSaveListener = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            cancelChanges();
        }
    };

    // Called when "Cancel" is clicked on the save confirmation box
    DialogInterface.OnClickListener saveChangesCancelListener = new DialogInterface.OnClickListener()
    {
        public void onClick(DialogInterface dialog, int which)
        {
            // don't do anything
        }
    };

    // Called when text is changed in name, host or post
    private TextWatcher updateSaveButtonStatus = new TextWatcher()
    {
        @Override
        public void afterTextChanged(Editable s)
        {
            toggleButtons();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };

    // Called when anything text-based is changed
    private TextWatcher updateChangedStatus = new TextWatcher()
    {
        @Override
        public void afterTextChanged(Editable s)
        {
            settingsChanged = true;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}
    };

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_edit_world);

        context = getApplicationContext();

        saveButton = (Button) findViewById(R.id.button_save_editing);
        saveButton.setOnClickListener(
            new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                saveChanges();
            }
        });

        cancelButton = (Button) findViewById(R.id.button_cancel_editing);
        cancelButton.setOnClickListener(
            new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                cancelChanges();
            }
        });

        txtWorldName = (EditText) findViewById(R.id.text_world_name);
        txtWorldName.addTextChangedListener(updateSaveButtonStatus);
        txtWorldName.addTextChangedListener(updateChangedStatus);

        txtWorldHost = (EditText) findViewById(R.id.text_world_address);
        txtWorldHost.addTextChangedListener(updateSaveButtonStatus);
        txtWorldHost.addTextChangedListener(updateChangedStatus);

        txtWorldPort = (EditText) findViewById(R.id.text_world_port);
        txtWorldPort.addTextChangedListener(updateSaveButtonStatus);
        txtWorldPort.addTextChangedListener(updateChangedStatus);

        adapter = new ArrayAdapter(context, android.R.layout.simple_spinner_item, Utility.getSupportedCharsetNames());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spnEncoding = (Spinner) findViewById(R.id.spinner_encoding);
        spnEncoding.setAdapter(adapter);
        spnEncoding.setOnItemSelectedListener(new OnItemSelectedListener()
        {
            @Override
            public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3)
            {
                int newPos = arg2;

                // only if this isn't a new world and the new setting does not match one on the old world
                if (previousEncodingSetting != null && newPos != adapter.getPosition(previousEncodingSetting))
                    settingsChanged = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {}
        });

        chkLogging = (CheckBox) findViewById(R.id.checkbox_logging);
        chkLogging.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                settingsChanged = true;
                toggleButtons();
            }
        });

        chkAnsiColor = (CheckBox) findViewById(R.id.checkbox_ansi_color);
        chkAnsiColor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
            {
                settingsChanged = true;
                toggleButtons();
            }
        });

        txtPostLoginCommand = (EditText) findViewById(R.id.text_post_login_command);
        txtPostLoginCommand.addTextChangedListener(updateChangedStatus);

        Bundle extras = getIntent().getExtras();

        if (extras == null)
        {
            // adding a new world
            saveButton.setEnabled(false);
            spnEncoding.setSelection(adapter.getPosition("UTF-8"));
            chkAnsiColor.setChecked(true);
        }
        if (extras != null)
        {
            // Editing an existing world
            worldToEdit = (World) extras.getParcelable("world");

            if (worldToEdit.name != null && worldToEdit.name.length() > 0)
                txtWorldName.setText(worldToEdit.name);

            if (worldToEdit.host != null && worldToEdit.host.length() > 0)
                txtWorldHost.setText(worldToEdit.host);

            if (worldToEdit.port > 0)
                txtWorldPort.setText(String.valueOf(worldToEdit.port));

            if (worldToEdit.loggingEnabled)
                chkLogging.setChecked(true);

            if (worldToEdit.ansiColorEnabled)
            {
                chkAnsiColor.setChecked(true);
            }

            if (worldToEdit.postLoginCommands.size() > 0)
                txtPostLoginCommand.setText(worldToEdit.postLoginCommands.get(0));

            spnEncoding.setSelection(adapter.getPosition(worldToEdit.encoding.name()));
            previousEncodingSetting = worldToEdit.encoding.name();

            saveButton.setEnabled(true);
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();

        settingsChanged = false;
    }

    @Override
    // intercept back button if changes made
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            if (settingsChanged &&
                    (txtWorldName.getText().toString().trim().length() == 0 ||
                     txtWorldHost.getText().toString().trim().length() == 0 ||
                     txtWorldPort.getText().toString().trim().length() == 0))
            {
                showSaveMissingDataConfirmationDialog();
                return false;
            }

            if (settingsChanged)
            {
                showSaveConfirmationDialog();
                return false;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    private void cancelChanges()
    {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void saveChanges()
    {
        if (worldToEdit == null)
            worldToEdit = new World(); // no dbID if a brand new world. this is okay

        worldToEdit.name = txtWorldName.getText().toString();
        worldToEdit.host = txtWorldHost.getText().toString();
        worldToEdit.port = txtWorldPort.getText().toString().trim().length() == 0 ? 0 : Integer.parseInt(txtWorldPort.getText().toString());
        worldToEdit.loggingEnabled = chkLogging.isChecked();
        worldToEdit.ansiColorEnabled = chkAnsiColor.isChecked();
        worldToEdit.setCharset((String) spnEncoding.getSelectedItem());

        worldToEdit.postLoginCommands.clear();
        String postLoginCommand = txtPostLoginCommand.getText().toString().trim();

        if (postLoginCommand.length() > 0)
        {
            worldToEdit.postLoginCommands.add(postLoginCommand);
        }

        Bundle bundle = new Bundle();
        bundle.putParcelable("world", worldToEdit);

        Intent intent = new Intent();
        intent.putExtras(bundle);
        setResult(RESULT_OK, intent);
        finish();
    }

    // set the save and cancel buttons on or off depending on if all necessary data is filled out
    private void toggleButtons()
    {
        if (txtWorldName.getText().toString().trim().length() > 0 && txtWorldHost.getText().toString().trim().length() > 0 && txtWorldPort.getText().toString().trim().length() > 0)
        {
            saveButton.setEnabled(true);
        }
        else
        {
            saveButton.setEnabled(false);
        }
    }

    private void showSaveConfirmationDialog()
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(context.getString(R.string.dialog_title_unsaved_changes));
        alertDialog.setMessage(String.format(context.getString(R.string.dialog_message_unsaved_changes), txtWorldName.getText().toString()));
        alertDialog.setButton(context.getString(R.string.button_save), saveChangesOkayListener);
        alertDialog.setButton2(context.getString(R.string.button_cancel), saveChangesCancelListener);
        alertDialog.setButton3(context.getString(R.string.button_dont_save), saveChangesDontSaveListener);
        alertDialog.show();
    }

    private void showSaveMissingDataConfirmationDialog()
    {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();
        alertDialog.setTitle(context.getString(R.string.dialog_title_missing_data_unsaved_changes));
        alertDialog.setMessage(context.getString(R.string.dialog_message_missing_data_unsaved_changes));
        alertDialog.setButton(context.getString(R.string.button_save), saveChangesOkayListener);
        alertDialog.setButton2(context.getString(R.string.button_cancel), saveChangesCancelListener);
        alertDialog.setButton3(context.getString(R.string.button_dont_save), saveChangesDontSaveListener);
        alertDialog.show();
    }
}
