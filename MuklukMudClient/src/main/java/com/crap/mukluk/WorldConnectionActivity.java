package com.crap.mukluk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.Vibrator;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnLongClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class WorldConnectionActivity extends Activity
{
    private static final Pattern urlPattern = Pattern.compile(
                "(?i)\\b((?:https?://|www\\d{0,3}[.]|[a-z0-9.\\-]+[.][a-z]{2,4}/)(?:[^\\s()<>]+|\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\))+(?:\\(([^\\s()<>]+|(\\([^\\s()<>]+\\)))*\\)|[^\\s`!()\\[\\]{};:'\".,<>?]))",
                Pattern.CASE_INSENSITIVE);
    private static final Pattern httpPattern = Pattern.compile("(http)", Pattern.CASE_INSENSITIVE);
    private static final String bellCharacter = "\u0007";

    private static String tag;

    private Menu activityMenu;

    private AlertDialog alertDialog = null;
    private Messenger messenger = new Messenger(new MessagesFromWorldHandler());
    private BlockingQueue<WorldMessage> messagesFromWorldQueue;
    private BlockingQueue<WorldMessage> messagesToWorldQueue;
    private MessagesFromWorldProcessor messagesFromWorldProcessor = null;
    private int echoColor;
    private int lastKnownStatus = WorldConnectionService.STATUS_NOT_CONNECTED;
    private int bindChances;

    private World world;
    private FixedSizeList<CharSequence> commandHistory;
    private FixedSizeList<CharSequence> urlHistory;
    private ConsoleTextHistory consoleHistory;

    private EditText txtCommandEntry;
    private Spinner spnAutoSay;
    private Button btnEnter;
    private TextView txtConsole;
    private ScrollView scrollConsole;
    private RelativeLayout layout;

    private WakeLock screenOnLock = null;
    private boolean autoScroll = true;
    private boolean autoSayEnabled;
    private boolean echoCommandsToConsole;
    private boolean verifyHistoryNavigation;
    private boolean vibrateOnBell;
    private boolean loggingEnabled;
    private boolean ansiColorEnabled;
    private File logPath;
    private File logFile;
    private int textSizeInDP;

    GestureDetector flingGestureDetector = new GestureDetector(new CommandHistoryFlingListener());

    GestureDetector doubleTapGestureDetector = new GestureDetector(new DoubleTapUrlsListener());

    private int commandBufferPos = 0;
    private boolean inputModified;
    private boolean helpMode = false;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        // Debug.startMethodTracing("mukluk");
        setContentView(R.layout.world_connection);

        Bundle extras = getIntent().getExtras();
        world = (World) extras.getParcelable("world");

        tag = "WorldConnectionActivity " + world.getHostAsString();

        layout = (RelativeLayout) findViewById(R.id.world_connection_layout);

        // Begin setting up views
        scrollConsole = (ScrollView) findViewById(R.id.scroll_console);
        scrollConsole.setClickable(false);

        spnAutoSay = (Spinner) findViewById(R.id.spinner_cmd_type);
        spnAutoSay.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if (helpMode)
                {
                    showHelp(spnAutoSay);
                    return true;
                }
                else
                {
                    return false;
                }
            }
        });

        txtConsole = (TextView) findViewById(R.id.console);
        txtConsole.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (helpMode)
                    showHelp(txtConsole);
            }
        });
        txtConsole.setOnLongClickListener(new OnLongClickListener()
        {
            @Override
            public boolean onLongClick(View arg0)
            {
                showAutoscrollCheckbox();
                return true;
            }
        });
        txtConsole.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return doubleTapGestureDetector.onTouchEvent(event);
            }
        });

        txtConsole.setTypeface(Typeface.MONOSPACE);

        txtCommandEntry = (EditText) findViewById(R.id.commandEntry);
        txtCommandEntry.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (helpMode)
                    showHelp(txtCommandEntry);
            }
        });
        txtCommandEntry.setOnKeyListener(new OnKeyListener()
        {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event)
            {
                int action = event.getAction();

                // send message when enter pressed
                if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
                {
                    sendCommandInCommandEntry();

                    return true;
                }
                else if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_UP)
                {
                    if (!inputModified || !verifyHistoryNavigation)
                    {
                        navigateToPreviousCommandInHistory();
                    }
                    else
                    {
                        showHistoryNavigationDialog(false);
                    }

                    return true;
                }
                else if (action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DPAD_DOWN)
                {
                    if (!inputModified || !verifyHistoryNavigation)
                    {
                        navigateToNextCommandInHistory();
                    }
                    else
                    {
                        showHistoryNavigationDialog(true);
                    }

                    return true;
                }
                else if (action == KeyEvent.ACTION_UP && keyCode != KeyEvent.KEYCODE_DPAD_DOWN
                         && keyCode != KeyEvent.KEYCODE_DPAD_UP && keyCode != KeyEvent.KEYCODE_ENTER)
                {
                    if (txtCommandEntry.getText().toString().length() > 0)
                    {
                        inputModified = true;
                    }
                    else
                    {
                        inputModified = false;
                    }

                    return false; // keep processing
                }

                txtCommandEntry.requestFocus();

                return false;
            }
        });
        txtCommandEntry.setOnTouchListener(new OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                return flingGestureDetector.onTouchEvent(event);
            }
        });

        btnEnter = (Button) findViewById(R.id.button_enter);
        btnEnter.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if (!helpMode)
                    sendCommandInCommandEntry();
                else
                    showHelp(btnEnter);
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();

        commandBufferPos = 0;
        inputModified = false;

        // process settings
        processGlobalSettings();
        processWorldSettings();

        // load history before binding to connection
        loadConsoleHistoryFromFile();
        loadCommandHistoryFromFile();
        loadURLHistoryFromFile();

        updateStatus(WorldConnectionService.getWorldConnectionStatus(world.dbID));

        // always try to bind
        bindToOpenConnection(1);
    }

    @Override
    public void onPause()
    {
        super.onPause();

        if (screenOnLock != null)
        {
            screenOnLock.release();
            screenOnLock = null;
        }

        if (alertDialog != null)
            alertDialog.dismiss();
    }

    @Override
    public void onStop()
    {
        super.onStop();

        Editor editPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();

        editPrefs.putInt("selected_auto_say", spnAutoSay.getSelectedItemPosition());
        editPrefs.commit();

        unbindFromConnection();

        writeConsoleHistoryToFile();
        writeCommandHistoryToFile();
        writeURLHistoryToFile();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        // Debug.stopMethodTracing();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.world_connection_menu, menu);

        switch (lastKnownStatus)
        {
            case (WorldConnectionService.STATUS_CONNECTING):
            case (WorldConnectionService.STATUS_DISCONNECTING):
                menu.findItem(R.id.menu_item_connect).setVisible(true);
                menu.findItem(R.id.menu_item_disconnect).setVisible(false);
                break;
            case (WorldConnectionService.STATUS_CONNECTED):
                menu.findItem(R.id.menu_item_connect).setVisible(false);
                menu.findItem(R.id.menu_item_disconnect).setVisible(true);
                break;
            case (WorldConnectionService.STATUS_NOT_CONNECTED):
                menu.findItem(R.id.menu_item_connect).setVisible(true);
                menu.findItem(R.id.menu_item_disconnect).setVisible(false);
                break;
        }

        if (!Utility.isInternetConnectionAvailable(getApplicationContext()))
        {
            Log.w(tag, "Data connection not available!");
            menu.findItem(R.id.menu_item_connect).setVisible(false);
            menu.findItem(R.id.menu_item_disconnect).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId())
        {
            case R.id.menu_item_connect:
                connect();
                return true;
            case R.id.menu_item_disconnect:
                disconnect();
                return true;
            case R.id.menu_item_edit_current_world:
                startEditWorldActivity();
                return true;
            case R.id.menu_item_global_settings:
                startGlobalSettingsActivity();
                return true;
            case R.id.menu_item_help:
                startHelpMode();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    // called when edit world settings activity returns
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (resultCode == RESULT_OK)
        {
            World worldReturned;

            if (requestCode == AddEditWorldActivity.EDIT_WORLD)
            {
                worldReturned = (World) data.getExtras().getParcelable("world");

                WorldDbAdapter worldDbAdapter = new WorldDbAdapter(this);
                worldDbAdapter.open();
                worldDbAdapter.updateWorldInDatabase(worldReturned);
                worldDbAdapter.close();
                worldDbAdapter = null;

                if (lastKnownStatus != WorldConnectionService.STATUS_NOT_CONNECTED
                        && (!worldReturned.host.equals(world.host) || worldReturned.port != world.port))
                {
                    Context context = getApplicationContext();
                    Toast.makeText(context, context.getString(R.string.toast_world_host_or_port_changed),
                                   Toast.LENGTH_LONG).show();
                }

                world = worldReturned;
            }
        }
    }

    private void ProcessMessageFromWorld(WorldMessage wMsg)
    {
        switch (wMsg.type)
        {
            case WorldMessage.MESSAGE_TYPE_TEXT:
                addLineToConsole(wMsg.text);
                addLineToLog(wMsg.text);
                break;
            case WorldMessage.MESSAGE_TYPE_ERROR:
                addLineToConsole(wMsg.text);
            case WorldMessage.MESSAGE_TYPE_STATUS_CHANGE:
                if (wMsg.currentStatus == WorldConnectionService.STATUS_NOT_CONNECTED)
                {
                    unbindFromConnection();
                    addLineToConsole(getString(R.string.console_disconnected) + "\n");
                    Intent intent = new Intent(this, WorldConnectionService.class);
                    intent.putExtra("refresh", true);
                    startService(intent);
                }
                else if (wMsg.currentStatus == WorldConnectionService.STATUS_CONNECTED)
                {
                    addLineToConsole(getString(R.string.console_connected) + "\n");
                    // send any lines from auto-connect

                    for (String cmd : world.postLoginCommands)
                    {
                        sendCommand(cmd);
                    }
                }
                updateStatus(wMsg.currentStatus);
                break;
        }
    }

    private void connect()
    {
        updateStatus(WorldConnectionService.STATUS_CONNECTING);
        Intent intent = new Intent(this, WorldConnectionService.class);
        intent.putExtra("world", world);
        startService(intent);
        bindToOpenConnection(5);
    }

    private void disconnect()
    {
        updateStatus(lastKnownStatus);
        WorldConnectionService.closeConnectionToWorld(world.dbID);
    }

    // get queue information and start reader
    private void bindToOpenConnection(int tries)
    {
        bindChances = tries;

        bindToOpenConnectionDo();
    }

    // Don't call this directly
    private void bindToOpenConnectionDo()
    {
        ArrayList<BlockingQueue<WorldMessage>> queues = WorldConnectionService
                .getBindInformationForConnection(world.dbID);

        // connection may not be opened yet
        if (queues != null)
        {
            messagesFromWorldQueue = queues.get(0);
            messagesToWorldQueue = queues.get(1);
            messagesFromWorldProcessor = new MessagesFromWorldProcessor();
            messagesFromWorldProcessor.start();
        }
        else
        {
            bindChances--;

            if (bindChances > 0)
            {
                // try again in two seconds
                txtConsole.postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        bindToOpenConnectionDo();
                    }
                }, 2000);
            }
            else
            {
                //Log.w(tag, "Couldn't bind to connection. Either not open or other issue.");
                //Toast.makeText(getApplicationContext(), getString(R.string.toast_bind_failure),
                //		Toast.LENGTH_LONG).show();
            }
        }
    }

    private void unbindFromConnection()
    {
        if (messagesFromWorldProcessor != null)
        {
            messagesFromWorldProcessor.stopProcessing = true;
            messagesFromWorldProcessor.interrupt();
            messagesFromWorldProcessor = null;
        }
    }

    private void processGlobalSettings()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        // Appearance

        int bgColor = prefs.getInt("background_color", BackgroundColorPreference.defaultBackgroundColor);
        layout.setBackgroundColor(bgColor);
        scrollConsole.setBackgroundColor(bgColor);
        txtConsole.setBackgroundColor(bgColor);

        txtConsole.setTextColor(prefs.getInt("foreground_color",
                                             ForegroundColorPreference.defaultForegroundColor));

        textSizeInDP = prefs.getInt("text_size_int", TextSizePreference.defaultTextSize);
        txtConsole.setTextSize(TypedValue.COMPLEX_UNIT_DIP, textSizeInDP);

        echoColor = prefs.getInt("echo_color", EchoColorPreference.defaultEchoColor);

        try
        {
            consoleHistory = new ConsoleTextHistory(Integer.parseInt(prefs.getString("screen_buffer_size",
                                                    "100")));
        }
        catch (NumberFormatException ex)
        {
            consoleHistory = new ConsoleTextHistory(100);
        }

        if (prefs.getBoolean("full_screen", false))
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                                 WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        vibrateOnBell = prefs.getBoolean("vibrate_on_bell", false);

        // Commands

        echoCommandsToConsole = prefs.getBoolean("echo_commands", false);
        verifyHistoryNavigation = prefs.getBoolean("verify_history_navigation", true);

        try
        {
            commandHistory = new FixedSizeList<CharSequence>(Integer.parseInt(prefs.getString(
                        "command_buffer_size", "20")));
        }
        catch (NumberFormatException ex)
        {
            commandHistory = new FixedSizeList<CharSequence>(20);
        }

        try
        {
            urlHistory = new FixedSizeList<CharSequence>(Integer.parseInt(prefs.getString("url_buffer_size",
                    "20")));
        }
        catch (NumberFormatException ex)
        {
            urlHistory = new FixedSizeList<CharSequence>(20);
        }

        if (prefs.getBoolean("show_autosay_spinner", false))
        {
            spnAutoSay.setVisibility(Spinner.VISIBLE);
            autoSayEnabled = true;

            spnAutoSay.setSelection(prefs.getInt("selected_auto_say", 0));
        }
        else
        {
            spnAutoSay.setVisibility(Spinner.GONE);
            autoSayEnabled = false;
        }

        if (prefs.getBoolean("show_enter_button", false))
            btnEnter.setVisibility(Button.VISIBLE);
        else
            btnEnter.setVisibility(Button.GONE);

        // Device

        if (screenOnLock != null)
        {
            screenOnLock.release();
            screenOnLock = null;
        }

        if (prefs.getBoolean("keep_screen_awake", false))
        {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            screenOnLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "mukluk_keep_screen_awake");
            screenOnLock.acquire();
        }
    }

    private void processWorldSettings()
    {
        if (world.loggingEnabled)
        {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            {
                logPath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mukluk/");
                logFile = new File(logPath.getAbsolutePath() + "/"
                                   + Utility.stripNonAlphanumericCharacters(world.name) + "_" + world.dbID + ".log");

                loggingEnabled = true;
            }
            else
            {
                Toast.makeText(getApplicationContext(), getString(R.string.toast_logging_failure),
                               Toast.LENGTH_LONG).show();
                loggingEnabled = false;
            }
        }
        else
        {
            loggingEnabled = false;
        }

        if (world.ansiColorEnabled)
            ansiColorEnabled = true;
        else
            ansiColorEnabled = false;

        Charset currentEncoding = WorldConnectionService.getWorldConnectionEncoding(world.dbID);

        if (currentEncoding != null)
        {
            if (!currentEncoding.name().equals(world.encoding.name()))
                Toast.makeText(getApplicationContext(), getString(R.string.toast_encoding_changed),
                               Toast.LENGTH_LONG).show();
        }
    }

    // add a line to console, parsing it for bells and urls
    private void addLineToConsole(CharSequence letters)
    {
        String strLine = letters.toString();

        // vibrate for bell character if that setting is enabled
        if (vibrateOnBell)
        {
            if (strLine.indexOf(bellCharacter) > -1)
            {
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(1000);
                strLine = strLine.replace(bellCharacter, "");
            }
        }

        CharSequence ansiString;

        // txtConsole.append(Utility.stripAnsi(strLine));
        if (letters instanceof SpannableString)
            ansiString = letters;
        else
            ansiString = AnsiUtility.getWithAnsiStyles(strLine, !ansiColorEnabled);

        txtConsole.append(ansiString);

        if (autoScroll)
        {
            txtConsole.post(new Runnable()
            {
                @Override
                public void run()
                {
                    scrollConsole.fullScroll(ScrollView.FOCUS_DOWN);
                    txtCommandEntry.requestFocus();
                }
            });
        }

        consoleHistory.add(ansiString);

        Matcher m = urlPattern.matcher(ansiString);

        while (m.find())
        {
            String url = m.group();

            if (urlHistory.size() == 0 || !(urlHistory.getFirst().equals(url)))
                urlHistory.addFirst(url);
        }
    }

    private void addLineToLog(CharSequence line)
    {
        PrintWriter ps = null;

        if (loggingEnabled)
        {
            try
            {
                if (!logPath.exists())
                    logPath.mkdir();

                try
                {
                    if (!logFile.exists())
                        logFile.createNewFile();
                }
                catch (IOException ex)
                {
                    Log.e(tag,
                          "Exception creating log file " + logFile.getAbsolutePath() + ": "
                          + ex.getMessage());

                    Toast.makeText(getApplicationContext(), getString(R.string.toast_logging_failure),
                                   Toast.LENGTH_LONG).show();
                    loggingEnabled = false;
                }

                ps = new PrintWriter(new FileOutputStream(logFile, true));
                ps.print(line);
                ps.print("\r\n");
                ps.flush();
            }
            catch (FileNotFoundException ex)
            {
                Log.e(tag,
                      "Exception writing to log file: " + logFile.getAbsolutePath() + " - " + ex.toString());

                Toast.makeText(getApplicationContext(), getString(R.string.toast_logging_failure),
                               Toast.LENGTH_LONG).show();
                loggingEnabled = false;
            }
            finally
            {
                try
                {
                    ps.close();
                }
                catch (Exception ex) {}
            }
        }
    }

    private void sendCommandInCommandEntry()
    {
        String cmd = txtCommandEntry.getText().toString();

        if (cmd.length() == 0)
            cmd = "\n";

        if (lastKnownStatus == WorldConnectionService.STATUS_CONNECTED)
        {
            if (autoSayEnabled)
            {
                String selectedVal = spnAutoSay.getSelectedItem().toString();

                if (selectedVal.equals("Say"))
                    cmd = "say " + cmd;
                if (selectedVal.equals("Emote"))
                    cmd = "emote " + cmd;
                else if (selectedVal.equals("Pose"))
                    cmd = "pose " + cmd;
            }

            sendCommand(cmd);

            txtCommandEntry.setText("");

            if (echoCommandsToConsole)
            {
                SpannableString formattedCmd = new SpannableString(cmd);
                formattedCmd.setSpan(new ForegroundColorSpan(echoColor), 0, cmd.length(), 0);
                addLineToConsole(formattedCmd);
                addLineToConsole("\n");
            }

            commandHistory.add(cmd);

            commandBufferPos = 0;

            inputModified = false;
        }

        txtCommandEntry.requestFocus();
    }

    private void sendCommand(String command)
    {
        WorldMessage wMsg = new WorldMessage(WorldMessage.MESSAGE_TYPE_TEXT,
                                             WorldConnectionService.STATUS_CONNECTED, command);
        messagesToWorldQueue.addMessage(wMsg);
    }

    private void updateStatus(int newStatus)
    {
        lastKnownStatus = newStatus;
        setTitle(world.name
                 + " ("
                 + WorldConnectionService.getWorldConnectionStatusDescription(getApplicationContext(),
                         lastKnownStatus) + ")");

        this.invalidateOptionsMenu();
    }

    private void showHistoryNavigationDialog(final boolean next)
    {
        Context context = getApplicationContext();

        alertDialog = new AlertDialog.Builder(this).create();

        alertDialog.setTitle(context.getString(R.string.dialog_title_navigate_history));

        if (!next)
            alertDialog.setMessage(context.getString(R.string.dialog_message_navigate_history_previous));
        else
            alertDialog.setMessage(context.getString(R.string.dialog_message_navigate_history_next));

        alertDialog.setButton(context.getString(R.string.button_okay), new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                if (!next)
                    navigateToPreviousCommandInHistory();
                else
                    navigateToNextCommandInHistory();
            }
        });
        alertDialog.setButton2(context.getString(R.string.button_cancel),
                               new DialogInterface.OnClickListener()
        {
            public void onClick(DialogInterface dialog, int which)
            {
                return;
            }
        });

        alertDialog.show();
    }

    private void navigateToNextCommandInHistory()
    {
        if (commandHistory.size() > 0)
        {
            commandBufferPos++;

            if (commandBufferPos > commandHistory.size() - 1)
                commandBufferPos = 0;

            String historyCommand = commandHistory.get(commandBufferPos).toString();
            String selectedVal = spnAutoSay.getSelectedItem().toString();

            // trim say, etc. from command in history if auto-say
            if (autoSayEnabled)
            {
                if (selectedVal.equals("Say") && historyCommand.toLowerCase().startsWith("say ")
                        && historyCommand.length() > 4)
                    historyCommand = historyCommand.substring(4, historyCommand.length());
                if (selectedVal.equals("Emote") && historyCommand.toLowerCase().startsWith("emote ")
                        && historyCommand.length() > 6)
                    historyCommand = historyCommand.substring(6, historyCommand.length());
                else if (selectedVal.equals("Pose") && historyCommand.toLowerCase().startsWith("pose ")
                         && historyCommand.length() > 5)
                    historyCommand = historyCommand.substring(5, historyCommand.length());
            }

            txtCommandEntry.setText(historyCommand);
            txtCommandEntry.requestFocus();
        }

        inputModified = false;
    }

    private void navigateToPreviousCommandInHistory()
    {
        if (commandHistory.size() > 0)
        {
            commandBufferPos--;

            if (commandBufferPos < 0)
                commandBufferPos = commandHistory.size() - 1;

            String historyCommand = commandHistory.get(commandBufferPos).toString();
            String selectedVal = spnAutoSay.getSelectedItem().toString();

            // trim say, etc. from command in history if auto-say
            if (autoSayEnabled)
            {
                if (selectedVal.equals("Say") && historyCommand.toLowerCase().startsWith("say ")
                        && historyCommand.length() > 4)
                    historyCommand = historyCommand.substring(4, historyCommand.length());
                if (selectedVal.equals("Emote") && historyCommand.toLowerCase().startsWith("emote ")
                        && historyCommand.length() > 6)
                    historyCommand = historyCommand.substring(6, historyCommand.length());
                else if (selectedVal.equals("Pose") && historyCommand.toLowerCase().startsWith("pose ")
                         && historyCommand.length() > 5)
                    historyCommand = historyCommand.substring(5, historyCommand.length());
            }

            txtCommandEntry.setText(historyCommand);
            txtCommandEntry.requestFocus();
        }

        inputModified = false;
    }

    private void loadURLHistoryFromFile()
    {
        BufferedReader br = null;
        String fileName = world.getURLCacheFileName();

        try
        {
            br = new BufferedReader(new InputStreamReader(openFileInput(fileName)));
            String line;

            try
            {
                while ((line = br.readLine()) != null)
                    urlHistory.add(line);
            }
            catch (IOException ex)
            {
                Log.e(tag, "IOException trying to load URL history for world #" + world.dbID);
            }
        }
        catch (FileNotFoundException ex)
        {
            Log.i(tag, "No backup file exists of URL history for world #" + world.dbID);
            return;
        }
        finally
        {
            try
            {
                br.close();
            }
            catch (Exception ex) {}
        }
    }

    private void writeURLHistoryToFile()
    {
        String fileName = world.getURLCacheFileName();

        if (urlHistory.size() > 0)
        {
            PrintWriter pw = null;

            try
            {
                pw = new PrintWriter(openFileOutput(fileName, MODE_PRIVATE));

                for (CharSequence str : urlHistory)
                    pw.println(str);
            }
            catch (FileNotFoundException ex)
            {
                // This shouldn't happen
                Log.e(tag, "FileNotFound exception creating command history file " + fileName);
                return;
            }
            finally
            {
                try
                {
                    pw.close();
                }
                catch (Exception ex) {}
            }
        }
        else
            deleteFile(fileName);
    }

    private void loadCommandHistoryFromFile()
    {
        BufferedReader br = null;
        String fileName = world.getCommandHistoryCacheFileName();

        try
        {
            br = new BufferedReader(new InputStreamReader(openFileInput(fileName)));
            String line;

            try
            {
                while ((line = br.readLine()) != null)
                    commandHistory.add(line);
            }
            catch (IOException ex)
            {
                Log.e(tag, "IOException trying to load command history for world #" + world.dbID);
            }
        }
        catch (FileNotFoundException ex)
        {
            Log.i(tag, "No backup file exists of command history for world #" + world.dbID);
            return;
        }
        finally
        {
            try
            {
                br.close();
            }
            catch (Exception ex) {}
        }
    }

    // Save the command history so the console can be restored
    private void writeCommandHistoryToFile()
    {
        String fileName = world.getCommandHistoryCacheFileName();

        if (commandHistory.size() > 0)
        {
            PrintWriter pw = null;

            try
            {
                pw = new PrintWriter(openFileOutput(fileName, MODE_PRIVATE));

                for (CharSequence str : commandHistory)
                    pw.println(str);
            }
            catch (FileNotFoundException ex)
            {
                // This shouldn't happen
                Log.e(tag, "FileNotFound exception creating command history file " + fileName);
                return;
            }
            finally
            {
                try
                {
                    pw.close();
                }
                catch (Exception ex) {}
            }
        }
        else
            deleteFile(fileName);
    }

    // load the console history from a file and set console text
    private void loadConsoleHistoryFromFile()
    {
        String textFileName = world.getConsoleCacheFileName();
        FileInputStream textInputStream = null;
        FileInputStream styleJacksonJsonStream = null;

        try
        {
            textInputStream = openFileInput(textFileName);
        }
        catch (FileNotFoundException ex)
        {
            Log.i(tag, "No previous text history exists.");
            textInputStream = null;
        }

        if (textInputStream != null)
        {
            if (ansiColorEnabled)
            {
                String styleJacksonJsonFileName = world.getConsoleStyleCacheJacksonJsonFileName();

                try
                {
                    styleJacksonJsonStream = openFileInput(styleJacksonJsonFileName);
                }
                catch (FileNotFoundException ex)
                {
                    Log.i(tag, "No previous console style cache Jackson JSON history exists.");
                    styleJacksonJsonStream = null;
                }
            }

            SpannableStringBuilder allTextAdded = consoleHistory.loadFromFile(textInputStream,
                                                  styleJacksonJsonStream);

            txtConsole.setText(allTextAdded);
            txtConsole.post(new Runnable()
            {
                @Override
                public void run()
                {
                    scrollConsole.fullScroll(ScrollView.FOCUS_DOWN);
                    txtCommandEntry.requestFocus();
                }
            });
        }
    }

    // Save the console history so the console can be restored
    private void writeConsoleHistoryToFile()
    {
        String textFileName = world.getConsoleCacheFileName();
        String stylesJacksonJsonFileName = world.getConsoleStyleCacheJacksonJsonFileName();

        try
        {
            if (!consoleHistory.writeToFiles(openFileOutput(textFileName, MODE_PRIVATE),
                                             openFileOutput(stylesJacksonJsonFileName, MODE_PRIVATE)))
            {

                deleteFile(textFileName);
                deleteFile(stylesJacksonJsonFileName);
            }
        }
        catch (FileNotFoundException ex)
        {
            // This shouldn't happen
            Log.e(tag, "FileNotFoundException while WRITING console text history!");
        }
    }

    private void showAutoscrollCheckbox()
    {
        Context context = getApplicationContext();
        alertDialog = new AlertDialog.Builder(this).create();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.checkbox_dialog,
                                       (ViewGroup) findViewById(R.id.layout_checkbox_dialog_root));
        alertDialog.setView(layout);
        alertDialog.setTitle(getString(R.string.dialog_title_autoscroll));

        final CheckBox chkAutoscrollDisabled = (CheckBox) layout
                                               .findViewById(R.id.checkbox_autoscroll_disabled);
        chkAutoscrollDisabled.setText(getString(R.string.dialog_message_autoscroll));

        if (autoScroll)
            chkAutoscrollDisabled.setChecked(false);
        else
            chkAutoscrollDisabled.setChecked(true);

        alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.button_okay),
                              new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                if (chkAutoscrollDisabled.isChecked())
                    autoScroll = false;
                else
                {
                    autoScroll = true;
                    txtConsole.post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            scrollConsole.fullScroll(ScrollView.FOCUS_DOWN);
                            txtCommandEntry.requestFocus();
                        }
                    });
                }
            }
        });

        alertDialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.string.button_cancel),
                              new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });

        alertDialog.show();
    }

    private void startHelpMode()
    {
        helpMode = !helpMode;

        if (helpMode)
            Toast.makeText(getApplicationContext(), getString(R.string.toast_help_mode_on), Toast.LENGTH_LONG)
            .show();
    }

    private void startEditWorldActivity()
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

    private void showHelp(View v)
    {
        String dialogTitle = null;
        String dialogMessage = null;

        if (v == txtConsole)
        {
            dialogTitle = getString(R.string.help_console_dialog_title);
            dialogMessage = getString(R.string.help_console_dialog_message);
        }
        else if (v == txtCommandEntry)
        {
            dialogTitle = getString(R.string.help_commandEntry_dialog_title);
            dialogMessage = getString(R.string.help_commandEntry_dialog_message);
        }
        else if (v == spnAutoSay)
        {
            dialogTitle = getString(R.string.help_autoSay_dialog_title);
            dialogMessage = getString(R.string.help_autoSay_dialog_message);
        }
        else if (v == btnEnter)
        {
            dialogTitle = getString(R.string.help_enterButton_dialog_title);
            dialogMessage = getString(R.string.help_enterButton_dialog_message);
        }

        if (dialogTitle != null)
        {
            alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setTitle(dialogTitle);
            alertDialog.setMessage(dialogMessage);
            alertDialog.setButton(getString(R.string.button_okay), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int which)
                {
                }
            });
            alertDialog.show();

            helpMode = false;
        }
    }

    private void showUrlDialog()
    {
        Context context = getApplicationContext();

        if (urlHistory.size() == 0)
            Toast.makeText(context, context.getString(R.string.toast_no_urls), Toast.LENGTH_SHORT).show();
        else
        {
            CharSequence[] emptyArray = new CharSequence[0];
            final CharSequence[] urls = urlHistory.toArray(emptyArray);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(context.getString(R.string.dialog_url_title));
            builder.setItems(urls, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    String lcaseUrl = urls[which].toString().toLowerCase().trim();
                    String urlToNavigateTo;

                    if (!lcaseUrl.startsWith("http://") && !lcaseUrl.startsWith("https://"))
                        urlToNavigateTo = "http://" + urls[which].toString().trim();
                    else
                        urlToNavigateTo = httpPattern.matcher(urls[which].toString().trim()).replaceAll("http"); // non-lowercase http crashes app

                    Intent webIntent = new Intent("android.intent.action.VIEW", Uri.parse(urlToNavigateTo));
                    startActivity(webIntent);
                }
            });
            alertDialog = builder.create();
            alertDialog.show();
        }
    }

    // show list of visible urls on double-tap
    class DoubleTapUrlsListener extends SimpleOnGestureListener
    {
        @Override
        public boolean onDoubleTap(MotionEvent e)
        {
            showUrlDialog();

            return true;
        }
    }

    // Navigate to next or previous commands on left or right swipes
    class CommandHistoryFlingListener extends SimpleOnGestureListener
    {
        private static final int SWIPE_THRESHOLD_VELOCITY = 400;

        @Override
        // notes say 1.5 is weird if you don't do this
        public boolean onDown(MotionEvent e)
        {
            return false;
        }

        @Override
        // seems like I'm returning false and true wrong here, but I don't think
        // this works right...
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY)
        {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

            if (prefs.getBoolean("disable_history_navigation", false))
            {
                return true;
            }

            // more horizontal than vertical
            if (Math.abs(e1.getX() - e2.getX()) > Math.abs(e1.getY() - e2.getY()))
            {
                if (e1.getX() - e2.getX() > 0 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    // right -> left fling
                    if (verifyHistoryNavigation && txtCommandEntry.getText().length() != 0)
                        showHistoryNavigationDialog(false);
                    else
                        navigateToPreviousCommandInHistory();

                    return false;
                }
                else if (e2.getX() - e1.getX() > 0 && Math.abs(velocityX) > SWIPE_THRESHOLD_VELOCITY)
                {
                    // left -> right fling
                    if (verifyHistoryNavigation && txtCommandEntry.getText().length() != 0)
                        showHistoryNavigationDialog(true);
                    else
                        navigateToNextCommandInHistory();

                    return false;
                }
            }

            return true;
        }
    }

    // handler passes along world messages
    class MessagesFromWorldHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg)
        {
            Bundle bundle = msg.getData();
            WorldMessage wMsg = (WorldMessage) bundle.getParcelable("worldmessage");
            ProcessMessageFromWorld(wMsg);
        }
    }

    // pass messages received in queue along to activity
    // to stop, set stopProcessing == false and then interrupt thread
    class MessagesFromWorldProcessor extends Thread
    {
        private String tag;

        public volatile boolean stopProcessing = false;

        public MessagesFromWorldProcessor()
        {
            tag = "MessagesFromWorldProcessor (" + world.host + ":" + world.port + ")";
        }

        @Override
        public void run()
        {
            while (!stopProcessing)
            {
                WorldMessage wMsg = messagesFromWorldQueue.getMessage();

                if (wMsg != null)
                {
                    Message msg = new Message();
                    msg.getData().putParcelable("worldmessage", wMsg);

                    try
                    {
                        messenger.send(msg);
                    }
                    catch (RemoteException ex)
                    {
                        Log.e(tag, "Unable to send message to activity! Ending thread.");
                        return;
                    }
                }
            }
        }
    }
}
