package com.example.android.notepad;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemSelectedListener;

public class NotesList extends ListActivity {

    private static final String TAG = "NotesList";
    private static final String[] PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_TITLE,
            NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
    };

    // UI元素
    private LinearLayout mSearchContainer;
    private EditText mSearchInput;
    private ImageButton mBtnClearSearch;
    private Spinner mSortSpinner;
    private ImageButton mBtnFilterColor;
    private ImageButton mBtnFilterCategory;
    private ImageButton mBtnSearchTop;
    private ImageButton mBtnSearchBottom;
    private ImageButton mBtnNewNoteTop;
    private ImageButton mBtnNewNoteBottom;
    private ImageButton mBtnDeleteBottom;
    private ImageButton mBtnMoreOptionsTop;

    private SimpleCursorAdapter mAdapter;
    private Handler mHandler = new Handler();
    private String mCurrentSearchQuery = "";
    private String mCurrentSortOrder = NotePad.Notes.DEFAULT_SORT_ORDER;
    private boolean mIsSearchVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notes_list_layout);

        setDefaultKeyMode(DEFAULT_KEYS_SHORTCUT);

        Intent intent = getIntent();
        if (intent.getData() == null) {
            intent.setData(NotePad.Notes.CONTENT_URI);
        }

        getListView().setOnCreateContextMenuListener(this);
        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new MultiChoiceModeListener());

        // 初始化视图
        initViews();

        // 加载笔记数据
        loadNotes();
    }

    private void initViews() {
        // 1. 先查找所有视图组件
        mSearchContainer = (LinearLayout) findViewById(R.id.search_container);
        mSearchInput = (EditText) findViewById(R.id.search_input);
        mBtnClearSearch = (ImageButton) findViewById(R.id.btn_clear_search);
        mSortSpinner = (Spinner) findViewById(R.id.spinner_sort);

        mBtnFilterColor = (ImageButton) findViewById(R.id.btn_filter_color);
        mBtnFilterCategory = (ImageButton) findViewById(R.id.btn_filter_category);
        mBtnSearchTop = (ImageButton) findViewById(R.id.btn_search_top);

        mBtnNewNoteTop = (ImageButton) findViewById(R.id.btn_new_note_top);
        mBtnNewNoteBottom = (ImageButton) findViewById(R.id.btn_new_note_bottom);

        mBtnMoreOptionsTop = (ImageButton) findViewById(R.id.btn_more_options_top);

        // 添加调试日志
        Log.d(TAG, "initViews: mSortSpinner = " + mSortSpinner);
        Log.d(TAG, "initViews: mSearchInput = " + mSearchInput);
        Log.d(TAG, "initViews: mBtnClearSearch = " + mBtnClearSearch);

        // 2. 添加空值检查
        if (mSearchInput != null) {
            setupSearchFunctionality();
        } else {
            Log.e(TAG, "search_input is null, make sure R.layout.notes_list_layout contains EditText with id 'search_input'");
        }

        if (mBtnClearSearch != null) {
            mBtnClearSearch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mSearchInput != null) {
                        mSearchInput.setText("");
                    }
                    mCurrentSearchQuery = "";
                    loadNotes();
                }
            });
        } else {
            Log.e(TAG, "btn_clear_search is null");
        }

        if (mSortSpinner != null) {
            setupSortSpinner();  // 这可能是第147行
        } else {
            Log.e(TAG, "spinner_sort is null, make sure R.layout.notes_list_layout contains Spinner with id 'spinner_sort'");
        }

        // 3. 修复具体的第147行（假设是在设置监听器）
        if (mSearchInput != null) {
            mSearchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                    if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                        performSearch();
                        hideKeyboard();
                        return true;
                    }
                    return false;
                }
            });
        }

        // 4. 添加其他控件的事件监听
        if (mBtnSearchTop != null) {
            mBtnSearchTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSearchVisibility();
                }
            });
        } else {
            Log.e(TAG, "btn_search_top is null");
        }

        if (mBtnNewNoteTop != null) {
            mBtnNewNoteTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewNote();
                }
            });
        } else {
            Log.e(TAG, "btn_new_note_top is null");
        }

        if (mBtnMoreOptionsTop != null) {
            mBtnMoreOptionsTop.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showMoreOptionsDialog("top");
                }
            });
        } else {
            Log.e(TAG, "btn_more_options_top is null");
        }

        if (mBtnSearchBottom != null) {
            mBtnSearchBottom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    toggleSearchVisibility();
                }
            });
        } else {
            Log.e(TAG, "btn_search_bottom is null");
        }

        if (mBtnNewNoteBottom != null) {
            mBtnNewNoteBottom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    createNewNote();
                }
            });
        } else {
            Log.e(TAG, "btn_new_note_bottom is null");
        }

        if (mBtnDeleteBottom != null) {
            mBtnDeleteBottom.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDeleteOptionsDialog("bottom");
                }
            });
        } else {
            Log.e(TAG, "btn_delete_bottom is null");
        }

        if (mBtnFilterColor != null) {
            mBtnFilterColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showColorFilterDialog();
                }
            });
        } else {
            Log.e(TAG, "btn_filter_color is null");
        }

        if (mBtnFilterCategory != null) {
            mBtnFilterCategory.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCategoryFilterDialog();
                }
            });
        } else {
            Log.e(TAG, "btn_filter_category is null");
        }
    }

    private void setupSortSpinner() {
        if (mSortSpinner == null) {
            Log.e(TAG, "setupSortSpinner: mSortSpinner is null!");
            return;
        }

        try {
            // 修复1：检查数组资源是否存在
            int resId = getResources().getIdentifier("sort_options", "array", getPackageName());
            if (resId == 0) {
                Log.e(TAG, "Array resource sort_options not found");
                // 创建一个默认的数组适配器
                String[] defaultSortOptions = {"By Time (Newest)", "By Time (Oldest)", "By Title (A-Z)", "By Title (Z-A)"};
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                        android.R.layout.simple_spinner_item, defaultSortOptions);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSortSpinner.setAdapter(adapter);
            } else {
                ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                        R.array.sort_options, android.R.layout.simple_spinner_item);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                mSortSpinner.setAdapter(adapter);
            }

            // 修复2：确保监听器只在spinner存在时设置
            mSortSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (parent != null && parent.getItemAtPosition(position) != null) {
                        String selectedSort = parent.getItemAtPosition(position).toString();
                        updateSortOrder(selectedSort);
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // 什么都不做
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in setupSortSpinner: " + e.getMessage(), e);
        }
    }

    private void setupSearchFunctionality() {
        if (mSearchInput == null) {
            Log.e(TAG, "setupSearchFunctionality: mSearchInput is null!");
            return;
        }

        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mCurrentSearchQuery = s.toString();
                mHandler.removeCallbacks(mSearchRunnable);
                mHandler.postDelayed(mSearchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void toggleSearchVisibility() {
        if (mSearchContainer == null) {
            Log.e(TAG, "toggleSearchVisibility: mSearchContainer is null!");
            return;
        }

        mIsSearchVisible = !mIsSearchVisible;
        if (mIsSearchVisible) {
            mSearchContainer.setVisibility(View.VISIBLE);
            if (mSearchInput != null) {
                mSearchInput.requestFocus();
                showKeyboard();
            }
        } else {
            mSearchContainer.setVisibility(View.GONE);
            if (mSearchInput != null) {
                mSearchInput.setText("");
            }
            mCurrentSearchQuery = "";
            hideKeyboard();
            loadNotes();
        }
    }

    private void showKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && mSearchInput != null) {
            imm.showSoftInput(mSearchInput, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && mSearchInput != null) {
            imm.hideSoftInputFromWindow(mSearchInput.getWindowToken(), 0);
        }
    }

    private void performSearch() {
        if (mSearchInput != null) {
            String query = mSearchInput.getText().toString().trim();
            mCurrentSearchQuery = query;
            loadNotes();
        }
    }

    private Runnable mSearchRunnable = new Runnable() {
        @Override
        public void run() {
            performSearch();
        }
    };

    private void updateSortOrder(String sortOption) {
        if (sortOption.contains("Newest")) {
            mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";
        } else if (sortOption.contains("Oldest")) {
            mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC";
        } else if (sortOption.contains("A-Z")) {
            mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_TITLE + " ASC";
        } else if (sortOption.contains("Z-A")) {
            mCurrentSortOrder = NotePad.Notes.COLUMN_NAME_TITLE + " DESC";
        } else {
            mCurrentSortOrder = NotePad.Notes.DEFAULT_SORT_ORDER;
        }
        loadNotes();
    }

    private void loadNotes() {
        String selection = null;
        String[] selectionArgs = null;

        if (mCurrentSearchQuery != null && !mCurrentSearchQuery.isEmpty()) {
            selection = NotePad.Notes.COLUMN_NAME_TITLE + " LIKE ?";
            selectionArgs = new String[] { "%" + mCurrentSearchQuery + "%" };
        }

        Cursor cursor = managedQuery(
                getIntent().getData(),
                PROJECTION,
                selection,
                selectionArgs,
                mCurrentSortOrder
        );

        String[] dataColumns = {
                NotePad.Notes.COLUMN_NAME_TITLE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE
        };

        int[] viewIDs = {
                android.R.id.text1,
                R.id.text_timestamp
        };

        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.noteslist_item,
                cursor,
                dataColumns,
                viewIDs
        );

        mAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.text_timestamp) {
                    long timestamp = cursor.getLong(1);
                    String formattedTime = formatTimestamp(timestamp);
                    ((TextView) view).setText(formattedTime);
                    return true;
                }
                return false;
            }
        });

        setListAdapter(mAdapter);
    }

    private String formatTimestamp(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm");
        return sdf.format(new java.util.Date(timestamp));
    }

    private void createNewNote() {
        Intent intent = new Intent(Intent.ACTION_INSERT, getIntent().getData());
        intent.setClassName(this, "com.example.android.notepad.NoteEditor");
        startActivity(intent);
    }

    private void showMoreOptionsDialog(String from) {
        String[] options = from.equals("top") ?
                new String[]{"Settings", "Import Notes", "Export Notes", "Sync"} :
                new String[]{"Delete Selected Notes", "Delete All Notes", "Delete Old Notes (30+ days)"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(from.equals("top") ? "More Options" : "Delete Options");
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (from.equals("top")) {
                    switch (which) {
                        case 0: showSettings(); break;
                        case 1: importNotes(); break;
                        case 2: exportNotes(); break;
                        case 3: syncNotes(); break;
                    }
                } else {
                    switch (which) {
                        case 0: deleteSelectedNotes(); break;
                        case 1: showDeleteAllNotesDialog(); break;
                        case 2: showDeleteOldNotesDialog(); break;
                    }
                }
            }
        });
        builder.show();
    }

    private void showDeleteOptionsDialog(String from) {
        showMoreOptionsDialog(from);
    }

    private void showColorFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Color");

        String[] colors = {"All Notes", "Red", "Green", "Blue", "Yellow", "Purple"};
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(NotesList.this, "Filter by: " + colors[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showCategoryFilterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Filter by Category");

        String[] categories = {"All Categories", "Work", "Personal", "Ideas", "Todo", "Shopping"};
        builder.setItems(categories, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(NotesList.this, "Category: " + categories[which], Toast.LENGTH_SHORT).show();
            }
        });
        builder.show();
    }

    private void showSettings() {
        Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show();
    }

    private void importNotes() {
        Toast.makeText(this, "Import Notes", Toast.LENGTH_SHORT).show();
    }

    private void exportNotes() {
        Toast.makeText(this, "Export Notes", Toast.LENGTH_SHORT).show();
    }

    private void syncNotes() {
        Toast.makeText(this, "Sync", Toast.LENGTH_SHORT).show();
    }

    private void deleteSelectedNotes() {
        if (mAdapter == null || mAdapter.getCount() == 0) {
            Toast.makeText(this, "No notes to delete", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Selected Notes");
        builder.setMessage("Select notes to delete");

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        getListView().clearChoices();

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deletedCount = 0;
                for (int i = 0; i < mAdapter.getCount(); i++) {
                    if (getListView().isItemChecked(i)) {
                        Cursor cursor = (Cursor) mAdapter.getItem(i);
                        if (cursor != null) {
                            long noteId = cursor.getLong(0);
                            Uri uri = ContentUris.withAppendedId(getIntent().getData(), noteId);
                            getContentResolver().delete(uri, null, null);
                            deletedCount++;
                        }
                    }
                }
                Toast.makeText(NotesList.this, deletedCount + " notes deleted", Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        });

        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteAllNotesDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete All Notes");
        builder.setMessage("Are you sure you want to delete ALL notes? This action cannot be undone.");

        builder.setPositiveButton("Delete All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deletedCount = getContentResolver().delete(getIntent().getData(), null, null);
                Toast.makeText(NotesList.this, deletedCount + " notes deleted", Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showDeleteOldNotesDialog() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L);
        String selection = NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " < ?";
        String[] selectionArgs = { String.valueOf(thirtyDaysAgo) };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Old Notes");
        builder.setMessage("Delete notes older than 30 days?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int deletedCount = getContentResolver().delete(getIntent().getData(), selection, selectionArgs);
                Toast.makeText(NotesList.this, deletedCount + " old notes deleted", Toast.LENGTH_SHORT).show();
                loadNotes();
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private class MultiChoiceModeListener implements ListView.MultiChoiceModeListener {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            MenuInflater inflater = mode.getMenuInflater();
            inflater.inflate(R.menu.context_menu_multi_select, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) { return false; }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            if (item.getItemId() == R.id.menu_delete_selected) {
                deleteSelectedNotes();
                mode.finish();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            getListView().clearChoices();
        }

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            int count = getListView().getCheckedItemCount();
            mode.setTitle(count + " selected");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_options_menu, menu);

        Intent intent = new Intent(null, getIntent().getData());
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        MenuItem mPasteItem = menu.findItem(R.id.menu_paste);
        mPasteItem.setEnabled(clipboard != null && clipboard.hasPrimaryClip());

        final boolean haveItems = getListAdapter() != null && getListAdapter().getCount() > 0;
        if (haveItems) {
            long selectedId = getSelectedItemId();
            if (selectedId > 0) {
                Uri uri = ContentUris.withAppendedId(getIntent().getData(), selectedId);
                Intent[] specifics = new Intent[1];
                Intent editIntent = new Intent(Intent.ACTION_EDIT, uri);
                editIntent.setClassName(this, "com.example.android.notepad.NoteEditor");
                specifics[0] = editIntent;
                MenuItem[] items = new MenuItem[1];
                Intent intent = new Intent(null, uri);
                intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                        new ComponentName(this, NotesList.class), null, intent, 0, null);
                if (items[0] != null) {
                    items[0].setShortcut('1', 'e');
                }
            }
        } else {
            menu.removeGroup(Menu.CATEGORY_ALTERNATIVE);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_add) {
            createNewNote();
            return true;
        } else if (id == R.id.menu_paste) {
            Intent pasteIntent = new Intent(Intent.ACTION_PASTE, getIntent().getData());
            pasteIntent.setClassName(this, "com.example.android.notepad.NoteEditor");
            startActivity(pasteIntent);
            return true;
        } else if (id == R.id.menu_search) {
            toggleSearchVisibility();
            return true;
        } else if (id == R.id.menu_delete) {
            showDeleteOptionsDialog("bottom");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfo) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return;
        }

        Cursor cursor = null;
        if (getListAdapter() != null) {
            cursor = (Cursor) getListAdapter().getItem(info.position);
        }

        if (cursor == null) {
            return;
        }

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.list_context_menu, menu);

        menu.add(0, 999, 0, "Delete");
        menu.setHeaderTitle(cursor.getString(1));

        Intent intent = new Intent(null, Uri.withAppendedPath(getIntent().getData(),
                Integer.toString((int) info.id)));
        intent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        menu.addIntentOptions(Menu.CATEGORY_ALTERNATIVE, 0, 0,
                new ComponentName(this, NotesList.class), null, intent, 0, null);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info;
        try {
            info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        } catch (ClassCastException e) {
            Log.e(TAG, "bad menuInfo", e);
            return false;
        }

        Uri noteUri = ContentUris.withAppendedId(getIntent().getData(), info.id);
        int id = item.getItemId();
        if (id == R.id.context_open) {
            Intent editIntent = new Intent(Intent.ACTION_EDIT, noteUri);
            editIntent.setClassName(this, "com.example.android.notepad.NoteEditor");
            startActivity(editIntent);
            return true;
        } else if (id == R.id.context_copy) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newUri(getContentResolver(), "Note", noteUri));
            return true;
        } else if (id == 999) {
            deleteSingleNote(info.position);
            return true;
        }
        return super.onContextItemSelected(item);
    }

    private void deleteSingleNote(final int position) {
        if (mAdapter == null || position < 0 || position >= mAdapter.getCount()) {
            Toast.makeText(this, "Invalid note position", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Note");
        builder.setMessage("Are you sure you want to delete this note?");

        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                if (cursor != null) {
                    long noteId = cursor.getLong(0);
                    Uri uri = ContentUris.withAppendedId(getIntent().getData(), noteId);
                    getContentResolver().delete(uri, null, null);
                    Toast.makeText(NotesList.this, "Note deleted", Toast.LENGTH_SHORT).show();
                    loadNotes();
                }
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Uri uri = ContentUris.withAppendedId(getIntent().getData(), id);
        String action = getIntent().getAction();

        if (Intent.ACTION_PICK.equals(action) || Intent.ACTION_GET_CONTENT.equals(action)) {
            setResult(RESULT_OK, new Intent().setData(uri));
        } else {
            Intent editIntent = new Intent(Intent.ACTION_EDIT, uri);
            editIntent.setClassName(this, "com.example.android.notepad.NoteEditor");
            startActivity(editIntent);
        }
    }
}