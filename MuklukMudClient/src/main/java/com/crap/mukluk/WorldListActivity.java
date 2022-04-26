package com.crap.mukluk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;

// Initial activity that displays the list of defined worlds
public class WorldListActivity extends ListActivity
{
    private static final String TAG = "WordListActivity";

    private Context _context;
    private WorldDbAdapter _worldDbAdapter;
    private ListView _listView;
    private int _worldToDeleteID = -1;
    private AlertDialog _alertDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.world_list);

        _context = getApplicationContext();

        _worldDbAdapter = new WorldDbAdapter(_context);
        _worldDbAdapter.open();

        _listView = getListView();
        registerForContextMenu(_listView);

        _listView.setOnItemClickListener(
            new OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> arg0, View view, int position, long id)
            {
                World world = _worldDbAdapter.getWorld((int) id);

                if (world.host.trim().length() == 0 || world.host.trim().length() == 0)
                    Toast.makeText(_context, _context.getString(R.string.toast_world_host_or_port_not_set), Toast.LENGTH_SHORT).show();
                else
                    startWorldConnection(world);
            }
        });
    }

    @Override
    public void onResume()
    {
        super.onResume();
        fillWorldList();

        if (_listView.getCount() == 0)
            showAddConfirmationDialog();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(_context);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (_alertDialog != null)
            _alertDialog.dismiss();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        _worldDbAdapter.close();
        _worldDbAdapter = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.world_list_menu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.world_list_context_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_item_new_world:
                startAddWorldActivity();
                return true;
            case R.id.menu_item_global_settings:
                startGlobalSettingsActivity();
                return true;
            case R.id.menu_item_export_worlds:
                exportWorlds();
                return true;
            case R.id.menu_item_import_worlds:
                importWorlds();
                return true;
            case R.id.menu_item_about:
                startAbout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();

        switch (item.getItemId())
        {
            case R.id.menu_item_edit_world :
                startEditWorldActivity(_worldDbAdapter.getWorld((int) info.id));
                return true;
            case R.id.menu_item_copy_world :
                World worldToCopy = _worldDbAdapter.getWorld((int) info.id);
                worldToCopy.name += " (Copy)";
                _worldDbAdapter.addWorldToDatabase(worldToCopy); // id is ignored so this is safe
                fillWorldList();
                return true;
            case R.id.menu_item_clear_world_cache :
                showCacheClearConfirmationDialog((int) info.id);
                return true;
            case R.id.menu_item_delete_world :
                showDeleteConfirmationDialog((int) info.id);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    // called when a sub-activity returns
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            World worldReturned;

            switch (requestCode)
            {
                case AddEditWorldActivity.ADD_WORLD :
                    worldReturned = (World) data.getExtras().getParcelable("world");
                    _worldDbAdapter.addWorldToDatabase(worldReturned);
                    Toast.makeText(_context, _context.getString(R.string.toast_changes_saved), Toast.LENGTH_SHORT).show();
                    break;
                case AddEditWorldActivity.EDIT_WORLD :
                    worldReturned = (World) data.getExtras().getParcelable("world");
                    _worldDbAdapter.updateWorldInDatabase(worldReturned);
                    Toast.makeText(_context, _context.getString(R.string.toast_changes_saved), Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    @Override
    // intercept back button
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK && ((WorldListAdapter) getListAdapter()).getConnectedWorlds() > 0)
        {
            showBackButtonDialog();
            return false;
        }

        return super.onKeyDown(keyCode, event);
    }

    private void fillWorldList()
    {
        setListAdapter(new WorldListAdapter(getApplicationContext(), _worldDbAdapter.getAllWorlds()));
    }

    private void exportWorlds()
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            final World[] worlds = _worldDbAdapter.getAllWorlds();

            if (worlds != null && worlds.length > 0)
            {
                _alertDialog = new AlertDialog.Builder(this).create();
                LayoutInflater inflater = (LayoutInflater) _context.getSystemService(LAYOUT_INFLATER_SERVICE);
                View layout = inflater.inflate(R.layout.text_dialog, (ViewGroup) findViewById(R.id.layout_text_dialog_root));
                _alertDialog.setView(layout);

                _alertDialog.setTitle(getString(R.string.dialog_title_export_worlds));
                ((TextView) layout.findViewById(R.id.label_value)).setText(getString(R.string.dialog_message_export_worlds));

                final EditText textInput = (EditText) layout.findViewById(R.id.text_value);
                textInput.setText("/mukluk/mukluk.worlds");

                _alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.button_okay), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        String enteredFile = textInput.getText().toString();
                        File exportFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + enteredFile);

                        if (exportFile.exists())
                            exportFile.delete();

                        if (!exportFile.getParentFile().exists())
                            exportFile.getParentFile().mkdirs();

                        ArrayList<SerializableWorld> serWorlds = new ArrayList<SerializableWorld>();

                        for (World w : worlds)
                            serWorlds.add(new SerializableWorld(w));

                        try
                        {
                            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(exportFile));
                            out.writeObject(serWorlds);
                            out.close();

                            Toast.makeText(getApplicationContext(), getString(R.string.toast_export_success, exportFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
                        }
                        catch (FileNotFoundException ex)
                        {
                            Log.e(TAG, ex.toString());
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_export_failure), Toast.LENGTH_LONG).show();
                        }
                        catch (IOException ex)
                        {
                            Log.e(TAG, ex.toString());
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_export_failure), Toast.LENGTH_LONG).show();
                        }
                    }
                });

                _alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                });

                _alertDialog.show();
            }
            else
            {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_export_no_worlds), Toast.LENGTH_LONG).show();
            }
        }
        else
        {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_export_failure), Toast.LENGTH_LONG).show();
        }
    }

    private void importWorlds()
    {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
        {
            _alertDialog = new AlertDialog.Builder(this).create();
            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(LAYOUT_INFLATER_SERVICE);
            View layout = inflater.inflate(R.layout.text_dialog, (ViewGroup) findViewById(R.id.layout_text_dialog_root));
            _alertDialog.setView(layout);

            _alertDialog.setTitle(getString(R.string.dialog_title_import_worlds));
            ((TextView) layout.findViewById(R.id.label_value)).setText(getString(R.string.dialog_message_import_worlds));

            final EditText textInput = (EditText) layout.findViewById(R.id.text_value);
            textInput.setText("/mukluk/mukluk.worlds");

            _alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.button_okay), new DialogInterface.OnClickListener()
            {
                @SuppressWarnings("unchecked")
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    File initialPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath());
                    String enteredFile = textInput.getText().toString();
                    File importFile = new File(initialPath.getAbsolutePath() + enteredFile);
                    ObjectInputStream in = null;

                    if (importFile.exists())
                    {
                        try
                        {
                            ArrayList<SerializableWorld> worlds;
                            in = new ObjectInputStream(new FileInputStream(importFile));

                            try
                            {
                                worlds = (ArrayList<SerializableWorld>) in.readObject();

                                for (SerializableWorld w : worlds)
                                    _worldDbAdapter.addWorldToDatabase(w.getAsWorld());

                                fillWorldList();
                            }
                            catch (ClassNotFoundException ex)
                            {
                                // this shouldn't happen
                                Log.e(TAG, ex.toString());
                            }
                        }
                        catch (IOException ex)
                        {
                            Toast.makeText(getApplicationContext(), getString(R.string.toast_import_failure), Toast.LENGTH_LONG).show();
                        }
                    }
                    else
                    {
                        Toast.makeText(getApplicationContext(), getString(R.string.toast_import_invalid_file, importFile.getAbsoluteFile()), Toast.LENGTH_LONG).show();
                    }
                }
            });

            _alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel), new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {}
            });

            _alertDialog.show();
        }
        else
        {
            Toast.makeText(getApplicationContext(), getString(R.string.toast_import_failure), Toast.LENGTH_LONG).show();
        }
    }

    private void startAbout()
    {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    private void startAddWorldActivity()
    {
        Intent intent = new Intent(this, AddEditWorldActivity.class);
        startActivityForResult(intent, AddEditWorldActivity.ADD_WORLD);
    }

    private void startEditWorldActivity(World world)
    {
        Intent intent = new Intent(this, AddEditWorldActivity.class);
        intent.putExtra("world", world);
        startActivityForResult(intent, AddEditWorldActivity.EDIT_WORLD);
    }

    private void startGlobalSettingsActivity()
    {
        Intent intent = new Intent(this, GlobalSettingsActivity.class);
        startActivity(intent);
    }

    private void startWorldConnection(World world)
    {
        Intent intent = new Intent(this, WorldConnectionActivity.class);
        intent.putExtra("world", world);
        intent.putExtra("createdFromWorldList", true);
        startActivity(intent);
    }

    private void showBackButtonDialog()
    {
        _alertDialog = new AlertDialog.Builder(this).create();
        _alertDialog.setTitle(_context.getString(R.string.dialog_title_back));
        _alertDialog.setMessage(_context.getString(R.string.dialog_message_back));
        _alertDialog.setButton(_context.getString(R.string.dialog_button_back_disconnect),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    Intent intent = new Intent(getApplicationContext(), WorldConnectionService.class);
                    stopService(intent);
                    finish();
                }
            });
        _alertDialog.setButton2(_context.getString(R.string.button_cancel),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which) {}
            });
        _alertDialog.setButton3(_context.getString(R.string.dialog_button_back_stay_connected),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    finish();
                }
            });
        _alertDialog.show();
    }

    private void showAddConfirmationDialog()
    {
        _alertDialog = new AlertDialog.Builder(this).create();
        _alertDialog.setTitle(_context.getString(R.string.dialog_title_no_worlds));
        _alertDialog.setMessage(_context.getString(R.string.dialog_message_no_worlds));
        _alertDialog.setButton(_context.getString(R.string.button_okay),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    startAddWorldActivity();
                }
        });
        _alertDialog.setButton2(_context.getString(R.string.button_cancel),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    return;
                }
            });
        _alertDialog.show();
    }

    private void showCacheClearConfirmationDialog(int worldID)
    {
        final World world = _worldDbAdapter.getWorld(worldID);
        final String worldName = world.name.trim().length() == 0 ? "[Unnamed World]" : world.name;
        _alertDialog = new AlertDialog.Builder(this).create();
        _alertDialog.setTitle(String.format(_context.getString(R.string.dialog_title_cache), worldName));
        _alertDialog.setMessage(_context.getString(R.string.dialog_message_cache));
        _alertDialog.setButton(_context.getString(R.string.button_okay),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    deleteFile(world.getCommandHistoryCacheFileName());
                    deleteFile(world.getConsoleCacheFileName());
                    deleteFile(world.getConsoleStyleCacheJsonFileName());
                    deleteFile(world.getConsoleStyleCacheJacksonJsonFileName());
                    deleteFile(world.getURLCacheFileName());
                    Toast.makeText(_context, String.format(getString(R.string.toast_cache_cleared), worldName), Toast.LENGTH_LONG).show();
                }
            });
        _alertDialog.setButton2(_context.getString(R.string.button_cancel),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    return;
                }
            });
        _alertDialog.show();
    }

    private void showDeleteConfirmationDialog(int worldID)
    {
        _worldToDeleteID = worldID;

        World world = _worldDbAdapter.getWorld(worldID);
        _alertDialog = new AlertDialog.Builder(this).create();
        _alertDialog.setTitle(world.name.trim().length() == 0 ? "[Unnamed World]" : world.name);
        _alertDialog.setMessage(_context.getString(R.string.dialog_message_delete_world_confirmation));
        _alertDialog.setButton(_context.getString(R.string.button_okay),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    int currentStatus = WorldConnectionService.getWorldConnectionStatus(_worldToDeleteID);

                    if (currentStatus == WorldConnectionService.STATUS_CONNECTING || currentStatus == WorldConnectionService.STATUS_CONNECTED)
                    {
                        Toast.makeText(_context, getString(R.string.toast_deleted_connected_world), Toast.LENGTH_LONG).show();
                    }
                    else
                    {
                        World worldBeingDeleted = _worldDbAdapter.getWorld(_worldToDeleteID);

                        deleteFile(worldBeingDeleted.getConsoleCacheFileName());
                        deleteFile(worldBeingDeleted.getCommandHistoryCacheFileName());
                        deleteFile(worldBeingDeleted.getURLCacheFileName());
                        deleteFile(worldBeingDeleted.getConsoleStyleCacheJsonFileName());
                        deleteFile(worldBeingDeleted.getConsoleStyleCacheJacksonJsonFileName());
                        _worldDbAdapter.deleteWorldFromDatabase(_worldToDeleteID);

                        _worldToDeleteID = -1;
                        fillWorldList();
                    }
                }
            });
        _alertDialog.setButton2(_context.getString(R.string.button_cancel),
            new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                    _worldToDeleteID = -1;
                    return;
                }
            });
        _alertDialog.show();
    }
}