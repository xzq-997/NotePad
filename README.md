# MyNotePad - Android记事本应用

<p align="center">
  <b>一个功能完善、界面美观的Android记事本应用</b><br>
  <i>简洁 · 高效 · 实用</i>
</p>

---

## 📱 项目概述
**MyNotePad** 是一款功能完善的Android记事本应用，集成了笔记管理、智能搜索、主题切换、分类管理等核心功能，采用原生Android开发，为用户提供高效便捷的笔记记录和管理体验。

---

## 📋 目录
1. [项目设计思路](#1-项目设计思路)
2. [项目功能实现](#2-项目功能实现)
3. [关键技术细节](#3-关键技术细节)
4. [项目亮点和扩展方向](#4-项目亮点和扩展方向)
---

## 一. 项目设计思路

### 🎯 核心目标
打造一个**轻量化、功能实用、界面美观**的记事本应用，重点解决用户日常记录的便捷性和管理效率问题。

### 📊 用户需求分析
- **快速记录需求**：简化操作流程，支持快速创建和编辑笔记
- **高效管理需求**：提供搜索、分类、排序功能，便于大量笔记管理
- **个性化需求**：支持界面主题定制，提升用户体验
- **数据安全需求**：确保笔记数据的本地存储安全

### 🛠️ 技术选型
| 技术组件 | 选型理由 | 优势 |
|---------|---------|------|
| **ContentProvider** | 数据共享与封装 | 安全访问、标准接口 |
| **CursorAdapter** | 数据绑定 | 高效列表展示、自动更新 |
| **Material Design** | 设计规范 | 统一界面、良好体验 |

---

## 二. 项目功能实现

### ⏰ 时间戳功能
**功能描述**：每条笔记自动记录创建和修改时间，支持多种时间显示格式（相对时间、完整时间）

 * 在列表中显示时间信息

      
        Note note = getItem(position);
        if (note != null) {
            TextView tvTitle = convertView.findViewById(android.R.id.text1);
            TextView tvTime = convertView.findViewById(R.id.text_timestamp);
            TextView tvContent = convertView.findViewById(R.id.text_content);
            
            tvTitle.setText(note.getTitle());
            tvTime.setText(note.getRelativeModifyTime());
            
            // 内容预览
            String content = note.getContent();
            if (content.length() > 50) {
                content = content.substring(0, 50) + "...";
            }
            tvContent.setText(content);
            
            // 根据时间显示不同的颜色
            long now = System.currentTimeMillis();
            long diff = now - note.getModifyTime();
            
            if (diff < 3600000) { // 1小时内
                tvTime.setTextColor(ContextCompat.getColor(context, R.color.colorRecent));
            } else if (diff < 86400000) { // 1天内
                tvTime.setTextColor(ContextCompat.getColor(context, R.color.colorToday));
            } else {
                tvTime.setTextColor(ContextCompat.getColor(context, R.color.colorOlder));
            }
        }
        
        return convertView;

**实现方案**：

    
    public static String getRelativeTime(long timestamp) {
    long now = System.currentTimeMillis();
    long diff = now - timestamp;
    if (diff < 60000) return "刚刚";
    if (diff < 3600000) return (diff/60000) + "分钟前";
    if (diff < 86400000) return (diff/3600000) + "小时前";
    if (diff < 604800000) {
        long days = diff/86400000;
        if (days == 1) return "昨天";
        if (days == 2) return "前天";
        return days + "天前";
    }
    return new SimpleDateFormat("MM-dd HH:mm").format(new Date(timestamp));
    
**使用场景**：
- ⏱️ 笔记详情显示完整时间（yyyy-MM-dd HH:mm:ss）
<img width="510" height="981" alt="ebe76ff86e55ad5fbbeb71362f4c5f0f" src="https://github.com/user-attachments/assets/fd2e8261-4b95-43bd-a864-a1adea17f3b7" />

## 🔍 搜索功能

**功能描述**：支持标题和内容关键字实时搜索，提升笔记查找效率

**功能特性**：
- ✅ 实时搜索，输入即显示结果
- ✅ 支持标题和内容双重搜索
- ✅ 自动清理，返回完整列表

### 搜索适配器实现
实现实时搜索：

```java
/**
 * 支持搜索的笔记适配器
 * 实现实时搜索功能
 */
public class NoteSearchAdapter extends BaseAdapter implements Filterable {
    private static final String TAG = "NoteSearchAdapter";
    
    private Context context;
    private List<Note> originalData;
    private List<Note> filteredData;
    private ItemFilter filter = new ItemFilter();
    
    public NoteSearchAdapter(Context context, List<Note> data) {
        this.context = context;
        this.originalData = new ArrayList<>(data);
        this.filteredData = new ArrayList<>(data);
    }
    
    public int getCount() {
        return filteredData.size();
    }
    
    public Note getItem(int position) {
        return filteredData.get(position);
    }
    
    public long getItemId(int position) {
        return position;
    }
    
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context)
                .inflate(R.layout.noteslist_item, parent, false);
            holder = new ViewHolder();
            holder.title = convertView.findViewById(android.R.id.text1);
            holder.time = convertView.findViewById(R.id.text_timestamp);
            holder.content = convertView.findViewById(R.id.text_content);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Note note = getItem(position);
        holder.title.setText(highlightSearchText(note.getTitle()));
        holder.time.setText(DateUtil.getRelativeTime(note.getModifyTime()));
        holder.content.setText(highlightSearchText(note.getContent()));
        
        return convertView;
    }
    
 
    
    /**
     * 自定义过滤器
     */
    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();
            List<Note> filteredList = new ArrayList<>();
            
            if (constraint == null || constraint.length() == 0) {
                // 如果没有过滤条件，返回全部数据
                filteredList.addAll(originalData);
            } else {
                String filterPattern = constraint.toString().toLowerCase(Locale.getDefault()).trim();
                currentFilter = filterPattern;
                
                for (Note note : originalData) {
                    // 搜索标题和内容
                    if (note.getTitle().toLowerCase(Locale.getDefault()).contains(filterPattern) ||
                        note.getContent().toLowerCase(Locale.getDefault()).contains(filterPattern)) {
                        filteredList.add(note);
                    }
                }
            }
            
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }
        
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            filteredData = (List<Note>) results.values;
            
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }
    
    static class ViewHolder {
        TextView title;
        TextView time;
        TextView content;
    }
}
```

### Activity中搜索功能集成
```java
/**
 * 搜索Activity
 * 实现实时搜索功能
 */
public class SearchActivity extends AppCompatActivity {
    private static final String TAG = "SearchActivity";
    
    private EditText mSearchEditText;
    private ImageButton mBtnClearSearch;
    private ListView mListView;
    private NoteSearchAdapter mAdapter;
    private List<Note> mAllNotes = new ArrayList<>();
    private Handler mHandler = new Handler();
    private Runnable mSearchRunnable;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        
        initViews();
        loadAllNotes();
        setupSearch();
    }
    
    private void initViews() {
        mSearchEditText = findViewById(R.id.et_search);
        mBtnClearSearch = findViewById(R.id.btn_clear_search);
        mListView = findViewById(R.id.lv_search_results);
        
        // 清除按钮点击事件
        mBtnClearSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSearchEditText.setText("");
                mBtnClearSearch.setVisibility(View.GONE);
                performSearch(""); // 清空搜索
            }
        });
        
        // 搜索结果点击事件
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Note note = mAdapter.getItem(position);
                if (note != null) {
                    openNoteDetail(note);
                }
            }
        });
    }
    
    private void loadAllNotes() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<Note> notes = DatabaseHelper.getInstance(SearchActivity.this).getAllNotes();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mAllNotes.clear();
                        mAllNotes.addAll(notes);
                        mAdapter = new NoteSearchAdapter(SearchActivity.this, mAllNotes);
                        mListView.setAdapter(mAdapter);
                    }
                });
            }
        }).start();
    }
    
    private void setupSearch() {
        // 搜索框文本变化监听
        mSearchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 显示/隐藏清除按钮
                mBtnClearSearch.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                
                // 延迟搜索，避免频繁查询
                mHandler.removeCallbacks(mSearchRunnable);
                mSearchRunnable = new Runnable() {
                    @Override
                    public void run() {
                        performSearch(s.toString());
                    }
                };
                mHandler.postDelayed(mSearchRunnable, 500); // 500ms防抖
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        // 键盘搜索按钮监听
        mSearchEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    performSearch(v.getText().toString());
                    hideKeyboard();
                    return true;
                }
                return false;
            }
        });
    }
    
    private void performSearch(String query) {
        if (mAdapter != null) {
            mAdapter.getFilter().filter(query);
            
            // 更新搜索结果数量显示
            int count = mAdapter.getCount();
            updateSearchResultCount(count);
            
            // 显示无结果提示
            if (count == 0 && !TextUtils.isEmpty(query)) {
                showNoResultsMessage(query);
            }
        }
    }
    
    private void updateSearchResultCount(int count) {
        TextView resultCount = findViewById(R.id.tv_result_count);
        if (resultCount != null) {
            String text = "找到 " + count + " 条结果";
            resultCount.setText(text);
            resultCount.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
        }
    }
    
    private void showNoResultsMessage(String query) {
        TextView emptyView = (TextView) findViewById(R.id.tv_empty_view);
        if (emptyView != null) {
            emptyView.setText("未找到包含 \"" + query + "\" 的笔记");
            emptyView.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, 
                "未找到包含 \"" + query + "\" 的笔记", 
                Toast.LENGTH_SHORT).show();
        }
    }
    
    private void openNoteDetail(Note note) {
        Intent intent = new Intent(this, NoteDetailActivity.class);
        intent.putExtra("note_id", note.getId());
        startActivity(intent);
    }
    
    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && mSearchEditText != null) {
            imm.hideSoftInputFromWindow(mSearchEditText.getWindowToken(), 0);
        }
    }
}
```



### 搜索布局文件
```xml
<!-- res/layout/activity_search.xml -->
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:background="@color/white">
    
    <!-- 搜索栏 -->
    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="horizontal"
        android:padding="12dp"
        android:background="@color/colorPrimary"
        android:elevation="4dp">
        
        <ImageButton
            android:id="@+id/btn_back"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_arrow_back"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/back"
            android:tint="@android:color/white"/>
        
        <EditText
            android:id="@+id/et_search"
            android:layout_width="0dp"
            android:layout_height="wrap_content"
            android:layout_weight="1"
            android:layout_marginStart="8dp"
            android:layout_marginEnd="8dp"
            android:hint="@string/search_hint"
            android:singleLine="true"
            android:imeOptions="actionSearch"
            android:inputType="text"
            android:maxLines="1"
            android:padding="12dp"
            android:background="@drawable/bg_search_edittext"
            android:textColor="@android:color/white"
            android:textColorHint="@color/white_60"/>
        
        <ImageButton
            android:id="@+id/btn_clear_search"
            android:layout_width="48dp"
            android:layout_height="48dp"
            android:src="@drawable/ic_close"
            android:background="?attr/selectableItemBackgroundBorderless"
            android:contentDescription="@string/clear"
            android:tint="@android:color/white"
            android:visibility="gone"/>
            
    </LinearLayout>
```



<img width="513" height="987" alt="dc5496b66a3a7a838918cd205ec20ceb" src="https://github.com/user-attachments/assets/a0d910c6-1060-40a9-ae72-54087effa53b" />



## 📁 分类与排序功能

**功能描述**：支持笔记的智能分类管理和多种排序方式，提升笔记管理效率

**功能特性**：
- ✅ 灵活分类管理 - 支持添加、删除、重命名分类
- ✅ 智能分类筛选 - 按分类快速筛选笔记
- ✅ 多维度排序 - 支持时间、标题、修改时间等多种排序方式
- ✅ 分类颜色标识 - 不同分类使用不同颜色标识
- ✅ 统计功能 - 显示每个分类的笔记数量
- ✅ 持久化存储 - 分类设置自动保存

### 分类筛选实现
**功能描述**：支持为笔记添加分类标签，并按分类筛选笔记

**实现代码**：
```java
/**
 * 分类管理器
 * 负责笔记的分类和筛选
 */
public class CategoryManager {
    private static CategoryManager instance;
    private Context context;
    private SharedPreferences prefs;
    
    private static final String PREFS_CATEGORIES = "note_categories";
    private static final String DEFAULT_CATEGORIES = "工作,生活,学习,想法,旅行,个人";
    
    public static synchronized CategoryManager getInstance(Context context) {
        if (instance == null) {
            instance = new CategoryManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private CategoryManager(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREFS_CATEGORIES, Context.MODE_PRIVATE);
    }
    
    /**
     * 获取所有分类
     */
    public List<String> getAllCategories() {
        String categoriesStr = prefs.getString("categories", DEFAULT_CATEGORIES);
        String[] categories = categoriesStr.split(",");
        List<String> categoryList = new ArrayList<>();
        
        // 添加"全部"选项
        categoryList.add("全部");
        for (String category : categories) {
            if (!category.trim().isEmpty()) {
                categoryList.add(category.trim());
            }
        }
        
        return categoryList;
    }
    
    /**
     * 添加新分类
     */
    public boolean addCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        
        String trimmedCategory = category.trim();
        List<String> categories = getAllCategories();
        
        // 检查是否已存在
        for (String c : categories) {
            if (c.equals(trimmedCategory)) {
                return false;
            }
        }
        
        // 保存新分类
        categories.add(trimmedCategory);
        saveCategories(categories);
        return true;
    }
    
    /**
     * 删除分类
     */
    public boolean removeCategory(String category) {
        List<String> categories = getAllCategories();
        boolean removed = categories.remove(category);
        
        if (removed) {
            saveCategories(categories);
            
            // 将该分类下的笔记移到"未分类"
            moveNotesToDefault(category);
        }
        
        return removed;
    }
    
    /**
     * 重命名分类
     */
    public boolean renameCategory(String oldName, String newName) {
        List<String> categories = getAllCategories();
        int index = categories.indexOf(oldName);
        
        if (index != -1) {
            categories.set(index, newName);
            saveCategories(categories);
            
            // 更新笔记的分类
            updateNotesCategory(oldName, newName);
            return true;
        }
        
        return false;
    }
    
    /**
     * 按分类筛选笔记
     */
    public List<Note> filterByCategory(String category, List<Note> allNotes) {
        if ("全部".equals(category)) {
            return allNotes;
        }
        
        List<Note> filteredNotes = new ArrayList<>();
        for (Note note : allNotes) {
            if (category.equals(note.getCategory())) {
                filteredNotes.add(note);
            }
        }
        return filteredNotes;
    }
    
    /**
     * 获取分类颜色
     */
    public int getCategoryColor(String category) {
        int[] colors = context.getResources().getIntArray(R.array.category_colors);
        List<String> categories = getAllCategories();
        
        int index = categories.indexOf(category);
        if (index >= 0 && index < colors.length) {
            return colors[index % colors.length];
        }
        
        return Color.GRAY;
    }
    
    /**
     * 获取分类统计
     */
    public Map<String, Integer> getCategoryStats(List<Note> allNotes) {
        Map<String, Integer> stats = new HashMap<>();
        
        for (Note note : allNotes) {
            String category = note.getCategory();
            if (category == null || category.isEmpty()) {
                category = "未分类";
            }
            
            stats.put(category, stats.getOrDefault(category, 0) + 1);
        }
        
        return stats;
    }
    
    private void saveCategories(List<String> categories) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < categories.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(categories.get(i));
        }
        prefs.edit().putString("categories", sb.toString()).apply();
    }
    
    private void moveNotesToDefault(String oldCategory) {
        DatabaseHelper.getInstance(context)
            .updateNotesCategory(oldCategory, "未分类");
    }
    
    private void updateNotesCategory(String oldName, String newName) {
        DatabaseHelper.getInstance(context)
            .updateNotesCategory(oldName, newName);
    }
}
```

### 排序功能实现
**🔄 支持按时间、字母、颜色排序功能**

**实现代码**：
```java
/**
 * 排序管理器
 * 支持多种排序方式
 */
public class SortManager {
    public enum SortBy {
        TIME_DESC,    // 时间降序（最新）
        TIME_ASC,     // 时间升序（最旧）
        TITLE_ASC,    // 标题A-Z
        TITLE_DESC,   // 标题Z-A
        MODIFY_DESC,  // 修改时间降序
        MODIFY_ASC    // 修改时间升序
    }
    
    private static SortManager instance;
    private Context context;
    private SortBy currentSortBy = SortBy.MODIFY_DESC;
    
    public static synchronized SortManager getInstance(Context context) {
        if (instance == null) {
            instance = new SortManager(context.getApplicationContext());
        }
        return instance;
    }
    
    private SortManager(Context context) {
        this.context = context;
        loadSortPreference();
    }
    
    /**
     * 设置排序方式
     */
    public void setSortBy(SortBy sortBy) {
        this.currentSortBy = sortBy;
        saveSortPreference();
    }
    
    /**
     * 获取当前排序方式
     */
    public SortBy getCurrentSortBy() {
        return currentSortBy;
    }
    
    /**
     * 获取排序SQL语句
     */
    public String getSortOrder() {
        switch (currentSortBy) {
            case TIME_DESC:
                return NotePad.Notes.COLUMN_NAME_CREATE_DATE + " DESC";
            case TIME_ASC:
                return NotePad.Notes.COLUMN_NAME_CREATE_DATE + " ASC";
            case TITLE_ASC:
                return NotePad.Notes.COLUMN_NAME_TITLE + " ASC";
            case TITLE_DESC:
                return NotePad.Notes.COLUMN_NAME_TITLE + " DESC";
            case MODIFY_DESC:
                return NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " DESC";
            case MODIFY_ASC:
            default:
                return NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " ASC";
        }
    }
    
    /**
     * 对列表进行排序
     */
    public void sortNotes(List<Note> notes) {
        Collections.sort(notes, (note1, note2) -> {
            switch (currentSortBy) {
                case TIME_DESC:
                    return Long.compare(note2.getCreateTime(), note1.getCreateTime());
                case TIME_ASC:
                    return Long.compare(note1.getCreateTime(), note2.getCreateTime());
                case TITLE_ASC:
                    return note1.getTitle().compareToIgnoreCase(note2.getTitle());
                case TITLE_DESC:
                    return note2.getTitle().compareToIgnoreCase(note1.getTitle());
                case MODIFY_DESC:
                    return Long.compare(note2.getModifyTime(), note1.getModifyTime());
                case MODIFY_ASC:
                default:
                    return Long.compare(note1.getModifyTime(), note2.getModifyTime());
            }
        });
    }
    
    /**
     * 显示排序对话框
     */
    public void showSortDialog(Activity activity, OnSortSelectedListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("排序方式");
        
        String[] sortOptions = {
            "按时间（最新）",
            "按时间（最早）", 
            "按标题（A-Z）",
            "按标题（Z-A）",
            "按修改时间（最新）",
            "按修改时间（最早）"
        };
        
        int selectedIndex = 4; // 默认按修改时间最新
        switch (currentSortBy) {
            case TIME_DESC: selectedIndex = 0; break;
            case TIME_ASC: selectedIndex = 1; break;
            case TITLE_ASC: selectedIndex = 2; break;
            case TITLE_DESC: selectedIndex = 3; break;
            case MODIFY_DESC: selectedIndex = 4; break;
            case MODIFY_ASC: selectedIndex = 5; break;
        }
        
        builder.setSingleChoiceItems(sortOptions, selectedIndex, (dialog, which) -> {
            SortBy newSortBy = SortBy.MODIFY_DESC;
            switch (which) {
                case 0: newSortBy = SortBy.TIME_DESC; break;
                case 1: newSortBy = SortBy.TIME_ASC; break;
                case 2: newSortBy = SortBy.TITLE_ASC; break;
                case 3: newSortBy = SortBy.TITLE_DESC; break;
                case 4: newSortBy = SortBy.MODIFY_DESC; break;
                case 5: newSortBy = SortBy.MODIFY_ASC; break;
            }
            
            setSortBy(newSortBy);
            if (listener != null) {
                listener.onSortSelected(newSortBy);
            }
            dialog.dismiss();
        });
        
        builder.setNegativeButton("取消", null);
        builder.show();
    }
    
    public interface OnSortSelectedListener {
        void onSortSelected(SortBy sortBy);
    }
    
    private void loadSortPreference() {
        SharedPreferences prefs = context.getSharedPreferences("sort_prefs", Context.MODE_PRIVATE);
        int sortIndex = prefs.getInt("sort_index", 4);
        
        switch (sortIndex) {
            case 0: currentSortBy = SortBy.TIME_DESC; break;
            case 1: currentSortBy = SortBy.TIME_ASC; break;
            case 2: currentSortBy = SortBy.TITLE_ASC; break;
            case 3: currentSortBy = SortBy.TITLE_DESC; break;
            case 4: currentSortBy = SortBy.MODIFY_DESC; break;
            case 5: currentSortBy = SortBy.MODIFY_ASC; break;
        }
    }
    
    private void saveSortPreference() {
        SharedPreferences prefs = context.getSharedPreferences("sort_prefs", Context.MODE_PRIVATE);
        int sortIndex = 4;
        switch (currentSortBy) {
            case TIME_DESC: sortIndex = 0; break;
            case TIME_ASC: sortIndex = 1; break;
            case TITLE_ASC: sortIndex = 2; break;
            case TITLE_DESC: sortIndex = 3; break;
            case MODIFY_DESC: sortIndex = 4; break;
            case MODIFY_ASC: sortIndex = 5; break;
        }
        prefs.edit().putInt("sort_index", sortIndex).apply();
    }
}
```

  <img width="515" height="975" alt="e32e69f2ff3b899e81855ab809d0c3a9" src="https://github.com/user-attachments/assets/24cfd850-b513-44fa-9701-e0b934d82628" />
**分类选项**：
- 📁 工作
- 🏠 生活
- 📚 学习
- ✈️ 旅行
- 💡 想法
- 🛍️ 购物
- 📦 等等
<img width="513" height="986" alt="c1b9f471e11561d82af0c9148f264022" src="https://github.com/user-attachments/assets/0db7bb8e-c69e-481d-97bc-a5fa3efbe19b" />
**分类选项（颜色）**：
<img width="495" height="987" alt="c86740b5dbe4a252b9b414d4802cd8de" src="https://github.com/user-attachments/assets/b0122f38-9924-4d2e-97fc-23dc4bf59742" />

## 三. 关键技术细节

## 🏗️ 数据库设计

### 数据库查询实现
```java
// 笔记内容提供者中的查询方法实现
@Override
public Cursor query(Uri uri, String[] projection, String selection,
                    String[] selectionArgs, String sortOrder) {
    SQLiteDatabase db = mOpenHelper.getReadableDatabase();
    SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
    qb.setTables(NotePad.Notes.TABLE_NAME);
    
    Cursor c = qb.query(db, projection, selection, selectionArgs,
                        null, null, sortOrder);
    c.setNotificationUri(getContext().getContentResolver(), uri);
    return c;
}
```

### 内存管理
```java
public class WeakReferenceHandler extends Handler {
    private final WeakReference<Activity> mActivity;
    
    public WeakReferenceHandler(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }
    
    @Override
    public void handleMessage(Message msg) {
        Activity activity = mActivity.get();
        if (activity != null && !activity.isFinishing()) {
            // 处理消息逻辑...
        }
    }
}
```

### 完整的数据库操作类示例
```java
public class NoteDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "notes.db";
    private static final int DATABASE_VERSION = 1;
    
    public static final String TABLE_NOTES = "notes";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TITLE = "title";
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CREATED = "created_date";
    public static final String COLUMN_MODIFIED = "modified_date";
    public static final String COLUMN_CATEGORY = "category";
    
    public NoteDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_NOTES_TABLE = "CREATE TABLE " + TABLE_NOTES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TITLE + " TEXT NOT NULL,"
                + COLUMN_CONTENT + " TEXT,"
                + COLUMN_CREATED + " INTEGER NOT NULL,"
                + COLUMN_MODIFIED + " INTEGER NOT NULL,"
                + COLUMN_CATEGORY + " TEXT DEFAULT '未分类'"
                + ")";
        db.execSQL(CREATE_NOTES_TABLE);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NOTES);
        onCreate(db);
    }
}
```

## 四. 项目亮点和扩展方向

### ✨ 项目亮点
| 亮点 | 描述 | 优势 |
|------|------|------|
| 🎯 **功能完备** | 完整的CRUD操作 | 覆盖笔记应用所有基础功能 |
| ⚡ **性能高效** | 优化的数据库查询 | 快速响应，流畅体验 |
| 🔄 **实时同步** | 搜索、排序实时更新 | 即时反馈，操作顺滑 |

### 🚀 扩展方向
#### 短期规划
1. **🔐 笔记加密**
   - AES加密存储敏感笔记
   - 指纹/面部识别解锁
   - 密码保护功能

2. **☁️ 云同步功能**
   - 多设备数据同步
   - 自动备份和恢复
   - 版本冲突解决

3. **📤 导出功能**
   - 导出为PDF/Word/Text
   - 批量导出功能
   - 分享到其他应用

#### 中期规划
1. **📝 富文本编辑**
   - Markdown支持
   - 图片插入和编辑
   - 表格和列表支持
   - 代码高亮功能

2. **🧠 智能功能**
   - 自动标签生成
   - 智能分类建议
   - 内容摘要生成
   - 搜索历史分析

3. **👥 协作功能**
   - 多人协作编辑
   - 评论和批注
   - 版本历史查看
   - 权限管理

#### 长期规划
1. **🤖 AI增强**
   - 智能内容推荐
   - 写作助手
   - 语音转文字
   - 图片文字识别

2. **🌐 跨平台支持**
   - Web版本
   - 桌面客户端
   - 浏览器插件
   - 移动端优化

3. **🔄 生态系统**
   - 插件系统
   - API开放
   - 第三方集成
   - 模板市场

