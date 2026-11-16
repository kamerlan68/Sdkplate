package com.extratools.manager

import android.app.*
import android.os.Bundle
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.Typeface
import android.view.*
import android.widget.*
import androidx.documentfile.provider.DocumentFile
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipFile
import java.util.Locale

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("et_settings", MODE_PRIVATE) }
    private var dark = true
    private var lang = "ru"
    private var rootUri: Uri? = null
    private var currentDir: DocumentFile? = null
    private var currentPath = ""
    private var selectedApk: Uri? = null
    private var zipTemp: File? = null
    private var zipPath = ""
    private var zipRoot = ""
    private var pendingOutput: String? = null
    private var pendingSource: Uri? = null
    private var pendingOperation: String? = null
    private lateinit var base: LinearLayout
    private lateinit var toolbar: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var scroll: ScrollView
    private val selected = linkedSetOf<Uri>()

    private val bg get() = if (dark) 0xFF202020.toInt() else 0xFFF4F4F4.toInt()
    private val panel get() = if (dark) 0xFF303030.toInt() else 0xFFFFFFFF.toInt()
    private val fg get() = if (dark) 0xFFF2F2F2.toInt() else 0xFF171717.toInt()
    private val muted get() = if (dark) 0xFFAAAAAA.toInt() else 0xFF666666.toInt()
    private val line get() = if (dark) 0xFF484848.toInt() else 0xFFDDDDDD.toInt()

    private fun tr(key: String): String {
        val ru = mapOf(
            "home" to "Главная", "files" to "Файловый менеджер", "extractor" to "APK Extractor",
            "explorer" to "APK Explorer", "scanner" to "File Scanner", "settings" to "Настройки",
            "recent" to "Последние файлы", "tools" to "Инструменты", "info" to "Информация",
            "about" to "О приложении", "version" to "Версия: 1.0.0", "choose_folder" to "Выбрать папку",
            "open_apk" to "Открыть APK", "installed" to "Установленные приложения",
            "extract" to "Извлечь APK", "extract_contents" to "Извлечь содержимое",
            "scan" to "Начать проверку", "no_suspicion" to "Без явных подозрений",
            "attention" to "Требует внимания", "suspicious" to "Подозрительный файл",
            "language" to "Язык", "theme" to "Тема", "light" to "Светлая", "dark" to "Тёмная",
            "russian" to "Русский", "english" to "English", "chinese" to "中文",
            "name" to "Имя", "size" to "Размер", "date" to "Дата", "sort" to "Сортировка",
            "search" to "Поиск", "new_folder" to "Новая папка", "rename" to "Переименовать",
            "delete" to "Удалить", "copy" to "Копировать", "move" to "Переместить",
            "cancel" to "Отмена", "save" to "Сохранить", "back" to "Назад",
            "select_root" to "Сначала выберите папку хранилища",
            "empty" to "Папка пуста", "permission" to "Выберите папку, к которой приложению разрешён доступ.",
            "hash" to "SHA-256", "properties" to "Свойства", "no_recent" to "Недавно открытых файлов пока нет",
            "installed_none" to "Не удалось получить список приложений",
            "scanner_note" to "Локальная эвристическая проверка. Это не антивирус и не гарантия безопасности.",
            "done" to "Готово", "error" to "Ошибка", "select_apk" to "Выберите APK-файл",
            "select_output" to "Выберите папку для сохранения", "extracting" to "Извлечение…",
            "folder" to "Папка", "file" to "Файл", "root" to "Хранилище"
        )
        val en = mapOf(
            "home" to "Home", "files" to "File Manager", "extractor" to "APK Extractor",
            "explorer" to "APK Explorer", "scanner" to "File Scanner", "settings" to "Settings",
            "recent" to "Recent files", "tools" to "Tools", "info" to "Information",
            "about" to "About", "version" to "Version: 1.0.0", "choose_folder" to "Choose folder",
            "open_apk" to "Open APK", "installed" to "Installed apps", "extract" to "Extract APK",
            "extract_contents" to "Extract contents", "scan" to "Start scan",
            "no_suspicion" to "No obvious issues", "attention" to "Needs attention",
            "suspicious" to "Suspicious file", "language" to "Language", "theme" to "Theme",
            "light" to "Light", "dark" to "Dark", "russian" to "Русский", "english" to "English",
            "chinese" to "中文", "name" to "Name", "size" to "Size", "date" to "Date",
            "sort" to "Sort", "search" to "Search", "new_folder" to "New folder",
            "rename" to "Rename", "delete" to "Delete", "copy" to "Copy", "move" to "Move",
            "cancel" to "Cancel", "save" to "Save", "back" to "Back",
            "select_root" to "Choose a storage folder first", "empty" to "Folder is empty",
            "permission" to "Choose a folder the app is allowed to access.",
            "hash" to "SHA-256", "properties" to "Properties",
            "no_recent" to "No recently opened files", "installed_none" to "Could not list installed apps",
            "scanner_note" to "Local heuristic scan. This is not an antivirus or a safety guarantee.",
            "done" to "Done", "error" to "Error", "select_apk" to "Select an APK file",
            "select_output" to "Choose an output folder", "extracting" to "Extracting…",
            "folder" to "Folder", "file" to "File", "root" to "Storage"
        )
        val zh = mapOf(
            "home" to "主页", "files" to "文件管理器", "extractor" to "APK 提取器",
            "explorer" to "APK 浏览器", "scanner" to "文件扫描", "settings" to "设置",
            "recent" to "最近文件", "tools" to "工具", "info" to "信息", "about" to "关于",
            "version" to "版本：1.0.0", "choose_folder" to "选择文件夹", "open_apk" to "打开 APK",
            "installed" to "已安装应用", "extract" to "提取 APK", "extract_contents" to "解压内容",
            "scan" to "开始扫描", "no_suspicion" to "未发现明显问题", "attention" to "需要注意",
            "suspicious" to "可疑文件", "language" to "语言", "theme" to "主题",
            "light" to "浅色", "dark" to "深色", "russian" to "Русский", "english" to "English",
            "chinese" to "中文", "name" to "名称", "size" to "大小", "date" to "日期",
            "sort" to "排序", "search" to "搜索", "new_folder" to "新建文件夹", "rename" to "重命名",
            "delete" to "删除", "copy" to "复制", "move" to "移动", "cancel" to "取消",
            "save" to "保存", "back" to "返回", "select_root" to "请先选择存储文件夹",
            "empty" to "文件夹为空", "permission" to "选择应用有权访问的文件夹。",
            "hash" to "SHA-256", "properties" to "属性", "no_recent" to "暂无最近打开的文件",
            "installed_none" to "无法获取应用列表",
            "scanner_note" to "本地启发式检查，不是杀毒软件，也不保证安全。",
            "done" to "完成", "error" to "错误", "select_apk" to "选择 APK 文件",
            "select_output" to "选择保存文件夹", "extracting" to "正在提取…",
            "folder" to "文件夹", "file" to "文件", "root" to "存储"
        )
        return when (lang) { "en" -> en[key] ?: key; "zh" -> zh[key] ?: key; else -> ru[key] ?: key }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dark = prefs.getBoolean("dark", true)
        lang = prefs.getString("lang", "ru") ?: "ru"
        rootUri = prefs.getString("tree", null)?.let(Uri::parse)
        rootUri?.let { currentDir = DocumentFile.fromTreeUri(this, it); currentPath = "/" }
        renderHome()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun text(s: String, size: Float = 16f, color: Int = fg, bold: Boolean = false): TextView =
        TextView(this).apply { text = s; textSize = size; setTextColor(color); if (bold) setTypeface(null, Typeface.BOLD); setPadding(dp(12), dp(9), dp(12), dp(9)) }
    private fun button(s: String, action: () -> Unit): TextView = text(s, 15f, fg, true).apply {
        setBackgroundColor(panel); isClickable = true; setOnClickListener { action() }
    }
    private fun row(parent: LinearLayout, child: View, pad: Int = 2) {
        parent.addView(child, LinearLayout.LayoutParams(-1, -2).apply { setMargins(dp(pad), dp(1), dp(pad), dp(1)) })
    }
    private fun init(title: String, menu: Boolean = true) {
        base = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(0xFF303030.toInt()) }
        val titleView = text(title, 19f, 0xFFFFFFFF.toInt(), true)
        toolbar.addView(titleView, LinearLayout.LayoutParams(0, dp(56), 1f))
        if (menu) toolbar.addView(button("⋮") { popupMenu() })
        base.addView(toolbar)
        scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(8), dp(8), dp(8), dp(16)) }
        scroll.addView(content)
        base.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(base)
    }
    private fun heading(s: String) { row(content, text(s, 17f, muted, true).apply { setPadding(dp(8), dp(16), dp(8), dp(8)) }) }
    private fun item(icon: String, title: String, subtitle: String = "", action: () -> Unit) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setBackgroundColor(panel); setPadding(dp(5), dp(4), dp(5), dp(4)) }
        box.addView(text(icon, 23f, fg, true), LinearLayout.LayoutParams(dp(54), dp(58)))
        val labels = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL }
        labels.addView(text(title, 16f, fg, true).apply { setPadding(dp(6), dp(3), dp(4), 0) })
        if (subtitle.isNotEmpty()) labels.addView(text(subtitle, 12f, muted).apply { setPadding(dp(6), dp(2), dp(4), dp(4)) })
        box.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
        box.addView(text("›", 26f, muted))
        box.setOnClickListener { action() }
        row(content, box)
        row(content, View(this).apply { setBackgroundColor(line) })
    }
    private fun popupMenu() {
        val p = PopupMenu(this, toolbar.getChildAt(toolbar.childCount - 1))
        listOf(tr("home"), tr("files"), tr("extractor"), tr("explorer"), tr("scanner"), tr("settings")).forEachIndexed { i, s -> p.menu.add(0, i, i, s) }
        p.setOnMenuItemClickListener {
            when (it.itemId) { 0 -> renderHome(); 1 -> renderFiles(); 2 -> renderExtractor(); 3 -> chooseApk("explore"); 4 -> chooseApk("scan"); 5 -> renderSettings() }
            true
        }
        p.show()
    }
    private fun renderHome() {
        init("▣   ExtraTools")
        heading(tr("recent"))
        val recent = prefs.getString("recent", "")!!.split("|").filter { it.isNotBlank() }.take(5)
        if (recent.isEmpty()) row(content, text(tr("no_recent"), 14f, muted))
        else recent.forEach { item("▤", it) { chooseApk("explore") } }
        heading(tr("tools"))
        item("▰", tr("files"), tr("files")) { renderFiles() }
        item("▣", tr("extractor"), tr("extract")) { renderExtractor() }
        item("⌕", tr("explorer"), "APK / ZIP") { chooseApk("explore") }
        item("✓", tr("scanner"), tr("scan")) { chooseApk("scan") }
        heading(tr("info"))
        item("ⓘ", tr("about"), tr("version")) { showInfo() }
        toolbar.addView(button("⚙") { renderSettings() })
        toolbar.addView(button("▣") { renderFiles() })
    }
    private fun showInfo() = AlertDialog.Builder(this).setTitle("ExtraTools").setMessage("ExtraTools 1.0.0\\nKotlin Android application").setPositiveButton("OK", null).show()

    private val pickTree = 100
    private val pickApk = 101
    private val pickOut = 102
    private var actionAfterPick = "explore"
    private fun selectTree() {
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), pickTree)
    }
    private fun renderFiles() {
        init(currentPath.ifBlank { tr("files") })
        if (currentDir == null) {
            row(content, text(tr("permission"), 14f, muted))
            row(content, button(tr("choose_folder")) { selectTree() })
            return
        }
        toolbar.addView(button("⌕") { searchFiles() })
        toolbar.addView(button("+") { newFolder() })
        toolbar.addView(button("↕") { sortDialog() })
        row(content, text(currentPath, 13f, muted))
        row(content, button("‹  ${tr("back")} ") { 
            val parent = currentDir?.parentFile
            if (parent != null) { currentDir = parent; currentPath = currentPath.substringBeforeLast("/", ""); renderFiles() }
            else renderHome()
        })
        val children = currentDir?.listFiles()?.toMutableList() ?: mutableListOf()
        if (children.isEmpty()) row(content, text(tr("empty"), 14f, muted))
        children.forEach { f ->
            val isDir = f.isDirectory
            val label = (if (isDir) "▰  " else "▤  ") + (f.name ?: "(unnamed)")
            val sub = if (isDir) tr("folder") else "${f.length()} B"
            val v = text("$label\n$sub", 15f, fg, isDir).apply {
                setBackgroundColor(panel); setPadding(dp(12), dp(10), dp(8), dp(10))
                setOnClickListener {
                    if (isDir) { currentDir = f; currentPath += "/${f.name}"; renderFiles() }
                    else showFileActions(f)
                }
                setOnLongClickListener { showFileActions(f); true }
            }
            row(content, v)
        }
    }
    private fun showFileActions(f: DocumentFile) {
        val options = arrayOf("Просмотр", tr("properties"), tr("rename"), tr("delete"), tr("hash"), tr("copy"), tr("move"))
        AlertDialog.Builder(this).setTitle(f.name).setItems(options) { _, which ->
            when (which) {
                0 -> previewDocument(f)
                1 -> AlertDialog.Builder(this).setMessage("${f.name}\\n${f.type}\\n${f.length()} B\\n${f.uri}").setPositiveButton("OK", null).show()
                2 -> inputDialog(tr("rename"), f.name ?: "") { f.renameTo(it); renderFiles() }
                3 -> AlertDialog.Builder(this).setMessage("${tr("delete")}?").setNegativeButton(tr("cancel"), null).setPositiveButton(tr("delete")) { _, _ -> f.delete(); renderFiles() }.show()
                4 -> { val h = hashUri(f.uri); AlertDialog.Builder(this).setTitle(tr("hash")).setMessage(h).setPositiveButton("OK", null).show() }
                5 -> { pendingSource = f.uri; pendingOperation = "copy"; selectOutput() }
                6 -> { pendingSource = f.uri; pendingOperation = "move"; selectOutput() }
            }
        }.show()
    }
    private fun previewDocument(file: DocumentFile) {
        try {
            val name = file.name ?: "file"
            val ext = name.substringAfterLast(".", "").lowercase()
            val textLike = ext in setOf("txt", "xml", "json", "yaml", "yml", "cfg", "ini", "properties", "log", "md", "html", "js", "kt", "java", "smali")
            if (!textLike) {
                AlertDialog.Builder(this).setTitle(name)
                    .setMessage("${file.type}\\n${file.length()} B\\n${tr("hash")}: ${hashUri(file.uri)}")
                    .setPositiveButton("OK", null).show()
                return
            }
            val bytes = contentResolver.openInputStream(file.uri)!!.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(8192); var total = 0
                while (total < 512 * 1024) {
                    val n = input.read(buffer, 0, minOf(buffer.size, 512 * 1024 - total))
                    if (n <= 0) break
                    out.write(buffer, 0, n); total += n
                }
                out.toByteArray()
            }
            val view = TextView(this).apply {
                text = String(bytes, Charsets.UTF_8); textSize = 12f; setTextColor(fg)
                typeface = Typeface.MONOSPACE; setPadding(dp(12), dp(12), dp(12), dp(12))
            }
            AlertDialog.Builder(this).setTitle(name)
                .setView(ScrollView(this).apply { addView(view) })
                .setPositiveButton("OK", null).show()
        } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
    }

    private fun newFolder() = inputDialog(tr("new_folder"), "") { name ->
        if (name.isNotBlank()) currentDir?.createDirectory(name)
        renderFiles()
    }
    private fun inputDialog(title: String, initial: String, done: (String) -> Unit) {
        val edit = EditText(this).apply { setText(initial); setTextColor(fg) }
        AlertDialog.Builder(this).setTitle(title).setView(edit).setNegativeButton(tr("cancel"), null)
            .setPositiveButton(tr("save")) { _, _ -> done(edit.text.toString()) }.show()
    }
    private fun searchFiles() = inputDialog(tr("search"), "") { q ->
        val all = mutableListOf<DocumentFile>()
        fun walk(d: DocumentFile) { d.listFiles().forEach { if (it.isDirectory) walk(it) else if ((it.name ?: "").contains(q, true)) all.add(it) } }
        currentDir?.let { walk(it) }
        init(tr("search"))
        all.forEach { f -> item("▤", f.name ?: "", "${f.length()} B") { showFileActions(f) } }
    }
    private fun sortDialog() {
        val opts = arrayOf(tr("name"), tr("size"), tr("date"))
        AlertDialog.Builder(this).setTitle(tr("sort")).setItems(opts) { _, which ->
            val files = currentDir?.listFiles()?.toList().orEmpty()
            val sorted = when(which) { 1 -> files.sortedByDescending { it.length() }; 2 -> files.sortedByDescending { it.lastModified() }; else -> files.sortedBy { it.name?.lowercase(Locale.ROOT) } }
            init(currentPath)
            sorted.forEach { f -> item(if (f.isDirectory) "▰" else "▤", f.name ?: "", "${f.length()} B") { if (f.isDirectory) { currentDir=f; currentPath += "/${f.name}"; renderFiles() } else showFileActions(f) } }
        }.show()
    }

    private fun renderExtractor() {
        init(tr("extractor"))
        row(content, text(tr("installed"), 17f, fg, true))
        val pm = packageManager
        val apps = try { pm.getInstalledApplications(PackageManager.GET_META_DATA).sortedBy { pm.getApplicationLabel(it).toString().lowercase() } } catch (_: Exception) { emptyList() }
        if (apps.isEmpty()) row(content, text(tr("installed_none"), 14f, muted))
        apps.forEach { app ->
            val label = pm.getApplicationLabel(app).toString()
            val pkg = app.packageName
            item("▣", label, pkg) {
                val source = app.sourceDir
                AlertDialog.Builder(this).setTitle(label)
                    .setItems(arrayOf(tr("extract"), tr("properties"))) { _, which ->
                        if (which == 0) {
                            pendingOutput = source.substringAfterLast("/")
                            selectOutput()
                        } else AlertDialog.Builder(this).setMessage("$pkg\\n$source").setPositiveButton("OK", null).show()
                    }.show()
                selectedApk = Uri.fromFile(File(source))
            }
        }
    }
    private fun selectOutput() { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), pickOut) }

    private fun copyDocument(source: DocumentFile, destination: DocumentFile, deleteAfter: Boolean) {
        if (source.isDirectory) {
            val dir = destination.createDirectory(source.name ?: "folder") ?: throw IOException("Cannot create folder")
            source.listFiles().forEach { copyDocument(it, dir, false) }
            if (deleteAfter) source.delete()
        } else {
            val target = destination.createFile(source.type ?: "application/octet-stream", source.name ?: "file")
                ?: throw IOException("Cannot create destination file")
            contentResolver.openInputStream(source.uri)!!.use { input ->
                contentResolver.openOutputStream(target.uri)!!.use { output -> input.copyTo(output) }
            }
            if (deleteAfter) source.delete()
        }
    }

    private fun chooseApk(action: String) {
        actionAfterPick = action
        val i = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("application/vnd.android.package-archive", "application/zip", "application/octet-stream"))
        }
        startActivityForResult(i, pickApk)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data?.data == null) return
        val uri = data.data!!
        when (requestCode) {
            pickTree -> {
                contentResolver.takePersistableUriPermission(uri, data.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
                rootUri = uri; prefs.edit().putString("tree", uri.toString()).apply()
                currentDir = DocumentFile.fromTreeUri(this, uri); currentPath = "/"; renderFiles()
            }
            pickApk -> {
                selectedApk = uri
                if (actionAfterPick == "explore") openZip(uri) else runScan(uri)
            }
            pickOut -> {
                val out = DocumentFile.fromTreeUri(this, uri)
                val src = selectedApk
                if (pendingSource != null && out != null) {
                    try {
                        val source = DocumentFile.fromSingleUri(this, pendingSource!!)
                        if (source != null) copyDocument(source, out, pendingOperation == "move")
                        pendingSource = null; pendingOperation = null
                        toast(tr("done")); renderFiles()
                    } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
                } else if (src != null && out != null) {
                    val name = pendingOutput ?: "application.apk"
                    try {
                        val target = out.createFile("application/vnd.android.package-archive", name)
                        if (target != null) contentResolver.openInputStream(src)?.use { input -> contentResolver.openOutputStream(target.uri)?.use { output -> input.copyTo(output) } }
                        toast(tr("done"))
                    } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
                    renderExtractor()
                }
            }
        }
    }

    private fun copyUriToTemp(uri: Uri): File {
        val file = File(cacheDir, "inspect_${System.currentTimeMillis()}.zip")
        contentResolver.openInputStream(uri)!!.use { input -> FileOutputStream(file).use { output -> input.copyTo(output) } }
        return file
    }
    private fun openZip(uri: Uri) {
        try {
            zipTemp?.delete()
            zipTemp = copyUriToTemp(uri)
            zipPath = ""
            zipRoot = uriName(uri)
            prefs.edit().putString("recent", (zipRoot + "|" + prefs.getString("recent", "")).take(500)).apply()
            showZipFolder()
        } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
    }
    private fun showZipFolder() {
        val archive = zipTemp ?: return
        val zf = ZipFile(archive)
        init("APK Explorer  /  $zipRoot")
        toolbar.addView(button("‹") { if (zipPath.isNotEmpty()) { zipPath = zipPath.substringBeforeLast("/", ""); showZipFolder() } else renderHome() })
        val prefix = if (zipPath.isEmpty()) "" else "$zipPath/"
        val entries = zf.entries().toList().filter { it.name.startsWith(prefix) && it.name != prefix }
        val direct = linkedMapOf<String, Boolean>()
        entries.forEach { e ->
            val rest = e.name.removePrefix(prefix)
            val first = rest.substringBefore("/")
            if (first.isNotEmpty()) direct[first] = rest.contains("/") || e.isDirectory
        }
        if (direct.isEmpty()) row(content, text(tr("empty"), 14f, muted))
        direct.forEach { (name, isDir) ->
            item(if (isDir) "▰" else "▤", name, if (isDir) tr("folder") else "APK entry") {
                if (isDir) { zipPath = if (zipPath.isEmpty()) name else "$zipPath/$name"; showZipFolder() }
                else previewZipEntry("$prefix$name")
            }
        }
        zf.close()
        row(content, button(tr("extract_contents")) { extractZipContents() })
    }
    private fun previewZipEntry(entry: String) {
        try {
            val archive = zipTemp ?: return
            val zf = ZipFile(archive)
            val ze = zf.getEntry(entry) ?: return
            val stream = zf.getInputStream(ze)
            val bytes = stream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                var total = 0
                while (total < 1024 * 1024) {
                    val count = input.read(buffer, 0, minOf(buffer.size, 1024 * 1024 - total))
                    if (count <= 0) break
                    output.write(buffer, 0, count); total += count
                }
                output.toByteArray()
            }
            zf.close()
            val isText = entry.endsWith(".xml", true) || entry.endsWith(".txt", true) || entry.endsWith(".json", true) || entry.endsWith(".yaml", true) || entry.endsWith(".yml", true) || entry.endsWith(".properties", true) || entry.endsWith(".cfg", true)
            val message = if (isText) String(bytes, Charsets.UTF_8) else "Binary file\\n${bytes.size} bytes previewed.\\nSHA-256: ${sha(bytes)}"
            val tv = TextView(this).apply { text = message; textSize = 13f; setTextColor(fg); setPadding(dp(12), dp(12), dp(12), dp(12)) }
            AlertDialog.Builder(this).setTitle(entry).setView(ScrollView(this).apply { addView(tv) }).setPositiveButton("OK", null).show()
        } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
    }
    private fun extractZipContents() {
        if (rootUri == null) { toast(tr("select_root")); selectTree(); return }
        val dest = currentDir ?: DocumentFile.fromTreeUri(this, rootUri!!)
        try {
            val archive = zipTemp ?: return
            val zf = ZipFile(archive)
            val basePath = zipPath
            zf.entries().asSequence().filter { it.name.startsWith(if (basePath.isEmpty()) "" else "$basePath/") }.forEach { e ->
                val relative = if (basePath.isEmpty()) e.name else e.name.removePrefix("$basePath/")
                if (relative.isBlank()) return@forEach
                val parts = relative.split("/").filter { it.isNotBlank() }
                var parent = dest
                parts.dropLast(1).forEach { part -> parent = parent.findFile(part) ?: parent.createDirectory(part) ?: parent }
                if (!e.isDirectory && parts.isNotEmpty()) {
                    val f = parent.createFile("application/octet-stream", parts.last())
                    if (f != null) zf.getInputStream(e).use { input -> contentResolver.openOutputStream(f.uri)?.use { output -> input.copyTo(output) } }
                }
            }
            zf.close(); toast(tr("done"))
        } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
    }

    private fun runScan(uri: Uri) {
        val name = uriName(uri)
        val findings = mutableListOf<String>()
        var score = 0
        try {
            val temp = copyUriToTemp(uri)
            val zf = try { ZipFile(temp) } catch (_: Exception) { null }
            if (zf == null) {
                findings.add("File is not a readable ZIP/APK archive")
                score += 1
            } else {
                val names = zf.entries().asSequence().map { it.name }.toList()
                val suspicious = listOf("classes.dex", "AndroidManifest.xml")
                if (!names.containsAll(suspicious)) { findings.add("APK structure is incomplete or not a standard APK"); score++ }
                val manifest = zf.getEntry("AndroidManifest.xml")
                if (manifest == null) { findings.add("AndroidManifest.xml not found"); score++ }
                val dex = names.filter { it.endsWith(".dex") }
                if (dex.size > 5) { findings.add("Unusually many DEX files: ${dex.size}"); score++ }
                if (names.any { it.contains("../") || it.startsWith("/") }) { findings.add("Archive contains path traversal-like entry names"); score += 2 }
                val suspiciousNames = names.filter { it.contains("payload", true) || it.contains("exploit", true) || it.contains("inject", true) }
                if (suspiciousNames.isNotEmpty()) { findings.add("Suspicious-looking entry names: ${suspiciousNames.take(5).joinToString()}"); score++ }
                val allStrings = names.joinToString(" ").lowercase()
                if (listOf("su", "magisk", "frida", "xposed").any { allStrings.contains(it) }) {
                    findings.add("Entry names contain terms sometimes associated with instrumentation/root tools"); score++
                }
                zf.close()
            }
            val status = when { score >= 3 -> tr("suspicious"); score > 0 -> tr("attention"); else -> tr("no_suspicion") }
            val report = "$name\\n\\n$status\\n\\n${if (findings.isEmpty()) "No heuristic indicators found." else findings.joinToString("\\n• ", "• ")}\\n\\n${tr("scanner_note")}"
            init(tr("scanner"))
            row(content, text(name, 17f, fg, true))
            row(content, text(status, 18f, if (score >= 3) 0xFFCC6666.toInt() else fg, true))
            row(content, text(report, 14f, muted))
            row(content, button(tr("hash")) { AlertDialog.Builder(this).setMessage(hashUri(uri)).setPositiveButton("OK", null).show() })
        } catch (e: Exception) { toast("${tr("error")}: ${e.message}") }
    }

    private fun hashUri(uri: Uri): String {
        val digest = MessageDigest.getInstance("SHA-256")
        contentResolver.openInputStream(uri)!!.use { input ->
            val buf = ByteArray(8192)
            while (true) { val n = input.read(buf); if (n <= 0) break; digest.update(buf, 0, n) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
    private fun sha(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    private fun uriName(uri: Uri): String {
        var name = "file"
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) name = it.getString(0) ?: name
        }
        return name
    }
    private fun renderSettings() {
        init(tr("settings"))
        heading(tr("language"))
        listOf("ru" to tr("russian"), "en" to tr("english"), "zh" to tr("chinese")).forEach { (code, label) ->
            item(if (lang == code) "◉" else "○", label) { lang = code; prefs.edit().putString("lang", lang).apply(); renderSettings() }
        }
        heading(tr("theme"))
        item(if (dark) "◉" else "○", tr("dark")) { dark = true; prefs.edit().putBoolean("dark", true).apply(); renderSettings() }
        item(if (!dark) "◉" else "○", tr("light")) { dark = false; prefs.edit().putBoolean("dark", false).apply(); renderSettings() }
        row(content, button(tr("back")) { renderHome() })
    }
    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    override fun onBackPressed() {
        if (zipPath.isNotEmpty()) { zipPath = zipPath.substringBeforeLast("/", ""); showZipFolder() }
        else if (currentDir != null && currentPath.isNotEmpty() && currentPath != "/") {
            currentDir = currentDir?.parentFile; currentPath = currentPath.substringBeforeLast("/", ""); renderFiles()
        } else renderHome()
    }
}
