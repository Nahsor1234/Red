package com.example.jeecommandcenter;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "jee_command_center";
    private static final String TASKS = "tasks_json";
    private static final String TARGET_DATE = "target_date";
    private static final String DAILY_TARGET = "daily_target_minutes";
    private static final int BG = Color.rgb(11, 13, 16);
    private static final int SURFACE = Color.rgb(18, 22, 28);
    private static final int SURFACE_HIGH = Color.rgb(24, 29, 36);
    private static final int TEXT = Color.rgb(242, 246, 250);
    private static final int MUTED = Color.rgb(169, 180, 192);
    private static final int PRIMARY = Color.rgb(125, 211, 252);
    private static final int SECONDARY = Color.rgb(167, 243, 208);
    private static final int SUCCESS = Color.rgb(101, 214, 162);
    private static final int WARNING = Color.rgb(244, 201, 93);
    private static final int ERROR = Color.rgb(255, 142, 142);

    private SharedPreferences prefs;
    private LinearLayout content;
    private BottomNavigationView nav;
    private long activeSessionStart = 0L;
    private String activeSessionLabel = "Study session";
    private final Handler timerHandler = new Handler();
    private Runnable timerRunnable;

    private final String[] physics = {
            "Units & Dimensions","Vectors","Kinematics","Laws of Motion","Work, Energy & Power",
            "System of Particles & COM","Rotational Motion","Gravitation","Properties of Matter",
            "Thermal Physics","Thermodynamics","Kinetic Theory","Oscillations","Waves","Electrostatics"
    };
    private final String[] chemistry = {
            "Some Basic Concepts of Chemistry","Atomic Structure","Periodic Classification",
            "Chemical Bonding","States of Matter","Thermodynamics","Equilibrium","Redox Reactions",
            "Organic Chemistry Basics","Hydrocarbons","Solutions","Electrochemistry","Chemical Kinetics",
            "Coordination Compounds","d- and f-Block Elements"
    };
    private final String[] maths = {
            "Basic Mathematics","Quadratic Equations","Sequences & Series","Binomial Theorem",
            "Permutations & Combinations","Straight Lines","Circles","Complex Numbers",
            "Matrices & Determinants","Limits & Continuity","Differentiation","Application of Derivatives",
            "Integral Calculus","Probability","Vectors & 3D Geometry"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        seedSyllabus();
        buildShell();
        showHome();
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(18), dp(12), dp(18), dp(92));
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(-1, -1);
        root.addView(content, cp);

        nav = new BottomNavigationView(this);
        nav.setBackgroundColor(Color.rgb(15, 18, 23));
        nav.setItemIconTintList(android.content.res.ColorStateList.valueOf(TEXT));
        nav.setItemTextColor(android.content.res.ColorStateList.valueOf(MUTED));
        nav.inflateMenu(com.example.jeecommandcenter.R.menu.bottom_nav);
        nav.setSelectedItemId(com.example.jeecommandcenter.R.id.nav_home);

        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1, dp(72), Gravity.BOTTOM);
        np.setMargins(dp(8), 0, dp(8), dp(6));
        root.addView(nav, np);

        nav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) showHome();
            else if (id == R.id.nav_syllabus) showSyllabus("Physics");
            else if (id == R.id.nav_tasks) showTasks();
            else if (id == R.id.nav_progress) showProgress();
            return true;
        });

        setContentView(root);
    }

    private void showHome() {
        content.removeAllViews();
        addHeader("JEE COMMAND CENTER", "Your preparation, organized.");
        addCountdownCard();
        addDailyCard();
        addDoNowCard();
        addTodayTasks();
        addActionButton("＋  Add Task", v -> showAddTaskDialog(), PRIMARY);
    }

    private void addHeader(String title, String subtitle) {
        TextView t = text(title, 26, TEXT, Typeface.BOLD);
        content.addView(t, marginParams(-2, 0, 0, 2));
        TextView s = text(subtitle, 13, MUTED, Typeface.NORMAL);
        content.addView(s, marginParams(-2, 0, 0, 14));
    }

    private void addCountdownCard() {
        MaterialCardView card = card(SURFACE_HIGH);
        LinearLayout box = vertical();
        box.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView small = text("JEE 2027", 13, PRIMARY, Typeface.BOLD);
        box.addView(small);
        TextView big = text(countdownText(), 30, TEXT, Typeface.BOLD);
        big.setPadding(0, dp(5), 0, 0);
        box.addView(big);
        TextView sub = text(targetSubtitle(), 12, MUTED, Typeface.NORMAL);
        box.addView(sub);

        MaterialButton set = button(prefs.getLong(TARGET_DATE, 0) == 0 ? "Set target date" : "Change target date");
        set.setOnClickListener(v -> showTargetDateDialog());
        box.addView(set, marginParams(-1, 14, 0, 0));

        card.addView(box);
        content.addView(card, marginParams(-1, 0, 0, 12));
    }

    private void addDailyCard() {
        MaterialCardView card = card(SURFACE);
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(16), dp(16), dp(16));

        CircularProgressIndicator ring = new CircularProgressIndicator(this);
        ring.setIndicatorColor(PRIMARY);
        ring.setTrackColor(Color.rgb(42, 52, 63));
        ring.setProgress((int)Math.round(dailyProgress() * 100));
        ring.setMax(100);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(dp(74), dp(74));
        row.addView(ring, rp);

        LinearLayout info = vertical();
        info.setPadding(dp(14), 0, 0, 0);
        TextView h = text("TODAY", 13, PRIMARY, Typeface.BOLD);
        info.addView(h);
        TextView time = text(formatMinutes(todayStudySeconds() / 60) + " / " + formatMinutes(dailyTargetMinutes()) , 23, TEXT, Typeface.BOLD);
        info.addView(time);
        TextView details = text("Study target", 12, MUTED, Typeface.NORMAL);
        info.addView(details);

        MaterialButton edit = button("Edit target");
        edit.setOnClickListener(v -> showDailyTargetDialog());
        info.addView(edit, marginParams(-2, 8, 0, 0));
        row.addView(info, new LinearLayout.LayoutParams(0, -2, 1));

        card.addView(row);
        content.addView(card, marginParams(-1, 0, 0, 12));
    }

    private void addDoNowCard() {
        Task best = bestOpenTask();
        MaterialCardView card = card(Color.rgb(18, 34, 43));
        LinearLayout box = vertical();
        box.setPadding(dp(18), dp(18), dp(18), dp(18));

        TextView label = text("⚡  DO THIS NOW", 13, PRIMARY, Typeface.BOLD);
        box.addView(label);

        String title = best == null ? firstIncompleteChapter() : best.title;
        TextView task = text(title, 23, TEXT, Typeface.BOLD);
        task.setPadding(0, dp(7), 0, 0);
        box.addView(task);

        String why = best == null ? "Start with an incomplete chapter." : best.priorityName() + " priority · " + best.minutes + " min";
        box.addView(text(why, 12, MUTED, Typeface.NORMAL));

        MaterialButton start = button("Start study");
        start.setOnClickListener(v -> startStudyDialog(title));
        box.addView(start, marginParams(-1, 12, 0, 0));
        card.addView(box);
        content.addView(card, marginParams(-1, 0, 0, 12));
    }

    private void addTodayTasks() {
        TextView title = text("TODAY'S TASKS", 13, MUTED, Typeface.BOLD);
        content.addView(title, marginParams(-2, 4, 0, 6));
        List<Task> list = loadTasks();
        int count = 0;
        for (Task task : list) {
            if (!task.completed) {
                content.addView(taskRow(task), marginParams(-1, 0, 0, 6));
                count++;
                if (count >= 4) break;
            }
        }
        if (count == 0) {
            TextView empty = text("No open tasks. Add one when you are ready.", 13, MUTED, Typeface.NORMAL);
            content.addView(empty, marginParams(-2, 0, 0, 10));
        }
    }

    private View taskRow(Task task) {
        MaterialCardView card = card(SURFACE);
        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(10), dp(7), dp(10), dp(7));

        CheckBox cb = new CheckBox(this);
        cb.setButtonTintList(android.content.res.ColorStateList.valueOf(PRIMARY));
        cb.setChecked(task.completed);
        cb.setOnClickListener(v -> {
            task.completed = cb.isChecked();
            saveTasks(loadTasks());
            showHome();
        });
        row.addView(cb);

        LinearLayout mid = vertical();
        TextView title = text(task.title, 15, TEXT, Typeface.BOLD);
        mid.addView(title);
        mid.addView(text(task.priorityName() + " priority · " + task.minutes + " min", 11, MUTED, Typeface.NORMAL));
        row.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));

        MaterialButton start = button("Start");
        start.setMinWidth(dp(0));
        start.setOnClickListener(v -> startStudyDialog(task.title));
        row.addView(start, new LinearLayout.LayoutParams(dp(76), dp(44)));

        card.addView(row);
        return card;
    }

    private void showSyllabus(String subject) {
        content.removeAllViews();
        addHeader("SYLLABUS", "Track real progress, not just checkboxes.");

        HorizontalScrollView hsv = new HorizontalScrollView(this);
        LinearLayout chips = horizontal();
        chips.setPadding(0, 0, 0, dp(8));
        for (String s : new String[]{"Physics","Chemistry","Mathematics"}) {
            MaterialButton b = button(s);
            if (s.equals(subject)) b.setBackgroundColor(Color.rgb(18, 52, 71));
            b.setOnClickListener(v -> showSyllabus(s));
            chips.addView(b, marginParams(-2, 0, 8, 0));
        }
        hsv.addView(chips);
        content.addView(hsv, marginParams(-1, 0, 0, 4));

        String[] chapters = subject.equals("Physics") ? physics : subject.equals("Chemistry") ? chemistry : maths;
        float overall = 0f;
        for (String chapter : chapters) overall += chapterProgress(subject, chapter);
        overall = chapters.length == 0 ? 0 : overall / chapters.length;

        MaterialCardView summary = card(SURFACE_HIGH);
        LinearLayout sb = vertical();
        sb.setPadding(dp(16), dp(14), dp(16), dp(14));
        TextView p = text(subject + "  " + Math.round(overall) + "%", 20, TEXT, Typeface.BOLD);
        sb.addView(p);
        LinearProgressIndicator prog = new LinearProgressIndicator(this);
        prog.setMax(100);
        prog.setProgress((int)Math.round(overall));
        prog.setIndicatorColor(PRIMARY);
        prog.setTrackColor(Color.rgb(42,52,63));
        sb.addView(prog, marginParams(-1, 9, 0, 0));
        summary.addView(sb);
        content.addView(summary, marginParams(-1, 0, 0, 12));

        for (String chapter : chapters) {
            content.addView(chapterRow(subject, chapter), marginParams(-1, 0, 0, 7));
        }

        addActionButton("＋  Add custom chapter", v -> showAddChapterDialog(subject), SECONDARY);
    }

    private View chapterRow(String subject, String chapter) {
        MaterialCardView card = card(SURFACE);
        card.setClickable(true);
        card.setOnClickListener(v -> showChapterProgressDialog(subject, chapter));

        LinearLayout box = vertical();
        box.setPadding(dp(15), dp(13), dp(15), dp(13));
        LinearLayout top = horizontal();
        TextView name = text(chapter, 15, TEXT, Typeface.BOLD);
        top.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
        TextView value = text(Math.round(chapterProgress(subject, chapter)) + "%", 14, PRIMARY, Typeface.BOLD);
        top.addView(value);
        box.addView(top);

        LinearProgressIndicator bar = new LinearProgressIndicator(this);
        bar.setMax(100);
        bar.setProgress((int)Math.round(chapterProgress(subject, chapter)));
        bar.setIndicatorColor(PRIMARY);
        bar.setTrackColor(Color.rgb(42,52,63));
        box.addView(bar, marginParams(-1, 8, 0, 0));
        box.addView(text("Tap to set progress", 10, MUTED, Typeface.NORMAL), marginParams(-2, 5, 0, 0));
        card.addView(box);
        return card;
    }

    private void showTasks() {
        content.removeAllViews();
        addHeader("TASKS", "Capture work, then execute it.");

        MaterialButton add = button("＋  Add task");
        add.setOnClickListener(v -> showAddTaskDialog());
        content.addView(add, marginParams(-1, 0, 0, 10));

        for (Task task : loadTasks()) {
            content.addView(taskRow(task), marginParams(-1, 0, 0, 7));
        }
    }

    private void showProgress() {
        content.removeAllViews();
        addHeader("PROGRESS", "A small, useful view of your real activity.");

        addMetricCard("Study today", formatMinutes(todayStudySeconds()/60), "Target: " + formatMinutes(dailyTargetMinutes()));
        addMetricCard("Study this week", formatMinutes(weekStudyMinutes()), "Across completed sessions");
        addMetricCard("Open tasks", String.valueOf(openTaskCount()), "Finish tasks from the Tasks tab");
        addMetricCard("Syllabus", Math.round(overallSyllabus()) + "%", "Average chapter progress");

        TextView sub = text("SUBJECT PROGRESS", 13, MUTED, Typeface.BOLD);
        content.addView(sub, marginParams(-2, 8, 0, 8));
        addSubjectProgress("Physics", physics);
        addSubjectProgress("Chemistry", chemistry);
        addSubjectProgress("Mathematics", maths);
    }

    private void addMetricCard(String label, String value, String sub) {
        MaterialCardView card = card(SURFACE);
        LinearLayout b = vertical();
        b.setPadding(dp(16), dp(14), dp(16), dp(14));
        b.addView(text(label.toUpperCase(Locale.US), 11, MUTED, Typeface.BOLD));
        b.addView(text(value, 26, TEXT, Typeface.BOLD));
        b.addView(text(sub, 11, MUTED, Typeface.NORMAL));
        card.addView(b);
        content.addView(card, marginParams(-1, 0, 0, 8));
    }

    private void addSubjectProgress(String subject, String[] chapters) {
        float sum = 0;
        for (String c : chapters) sum += chapterProgress(subject, c);
        float p = chapters.length == 0 ? 0 : sum / chapters.length;

        LinearLayout row = horizontal();
        row.setGravity(Gravity.CENTER_VERTICAL);
        TextView t = text(subject, 14, TEXT, Typeface.BOLD);
        row.addView(t, new LinearLayout.LayoutParams(dp(108), -2));
        LinearProgressIndicator bar = new LinearProgressIndicator(this);
        bar.setMax(100);
        bar.setProgress((int)Math.round(p));
        bar.setIndicatorColor(PRIMARY);
        bar.setTrackColor(Color.rgb(42,52,63));
        row.addView(bar, new LinearLayout.LayoutParams(0, dp(8), 1));
        row.addView(text("  " + Math.round(p) + "%", 13, MUTED, Typeface.NORMAL));
        content.addView(row, marginParams(-1, 0, 0, 13));
    }

    private void showAddTaskDialog() {
        LinearLayout body = vertical();
        body.setPadding(dp(4), dp(4), dp(4), 0);

        EditText title = edit("Task title");
        body.addView(title, marginParams(-1, 0, 0, 10));

        EditText minutes = edit("Estimated minutes");
        minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        body.addView(minutes, marginParams(-1, 0, 0, 10));

        TextView pLabel = text("Priority", 12, MUTED, Typeface.BOLD);
        body.addView(pLabel, marginParams(-1, 2, 0, 5));
        LinearLayout choices = horizontal();
        final int[] selected = {2};
        for (int i = 1; i <= 3; i++) {
            int value = i;
            MaterialButton b = button(i == 1 ? "Low" : i == 2 ? "Medium" : "High");
            b.setOnClickListener(v -> selected[0] = value);
            choices.addView(b, marginParams(0, 0, 6, 0));
        }
        body.addView(choices);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Add task")
                .setView(body)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String titleText = title.getText().toString().trim();
                    if (titleText.isEmpty()) return;
                    int mins = safeInt(minutes.getText().toString(), 30);
                    List<Task> tasks = loadTasks();
                    tasks.add(new Task(titleText, mins, selected[0], false));
                    saveTasks(tasks);
                    showTasks();
                }).show();
    }

    private void showTargetDateDialog() {
        Calendar now = Calendar.getInstance();
        long existing = prefs.getLong(TARGET_DATE, 0L);
        if (existing > 0) now.setTimeInMillis(existing);
        DatePickerDialog picker = new DatePickerDialog(this, (view, year, month, day) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, day, 0, 0, 0);
            prefs.edit().putLong(TARGET_DATE, selected.getTimeInMillis()).apply();
            showHome();
        }, now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
        picker.show();
    }

    private void showDailyTargetDialog() {
        EditText input = edit("Minutes per day");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(dailyTargetMinutes()));
        new MaterialAlertDialogBuilder(this)
                .setTitle("Daily study target")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    int mins = Math.max(15, safeInt(input.getText().toString(), 300));
                    prefs.edit().putInt(DAILY_TARGET, mins).apply();
                    showHome();
                }).show();
    }

    private void showChapterProgressDialog(String subject, String chapter) {
        EditText input = edit("Progress 0–100");
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setText(String.valueOf(Math.round(chapterProgress(subject, chapter))));

        new MaterialAlertDialogBuilder(this)
                .setTitle(chapter)
                .setMessage("Set the chapter progress. V1 uses this as the chapter completion measure.")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    int p = Math.min(100, Math.max(0, safeInt(input.getText().toString(), 0)));
                    prefs.edit().putInt(chapterKey(subject, chapter), p).apply();
                    showSyllabus(subject);
                }).show();
    }

    private void showAddChapterDialog(String subject) {
        EditText input = edit("Chapter name");
        new MaterialAlertDialogBuilder(this)
                .setTitle("Add custom chapter")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;
                    String key = "custom_" + subject;
                    String old = prefs.getString(key, "");
                    String updated = old.isEmpty() ? name : old + "|" + name;
                    prefs.edit().putString(key, updated).putInt(chapterKey(subject, name), 0).apply();
                    showSyllabus(subject);
                }).show();
    }

    private void startStudyDialog(String label) {
        activeSessionLabel = label;
        LinearLayout body = vertical();
        body.setPadding(dp(4), dp(4), dp(4), 0);
        TextView timer = text("00:00", 42, TEXT, Typeface.BOLD);
        timer.setGravity(Gravity.CENTER);
        body.addView(timer, marginParams(-1, 8, 0, 16));

        TextView hint = text(label, 14, MUTED, Typeface.NORMAL);
        hint.setGravity(Gravity.CENTER);
        body.addView(hint, marginParams(-1, 0, 0, 16));

        final MaterialButton start = button("Start");
        final MaterialButton finish = button("Finish");
        finish.setEnabled(false);
        body.addView(start);
        body.addView(finish, marginParams(-1, 8, 0, 0));

        final android.app.Dialog[] dialog = new android.app.Dialog[1];
        dialog[0] = new MaterialAlertDialogBuilder(this)
                .setTitle("Study session")
                .setView(body)
                .setNegativeButton("Close", null)
                .create();

        start.setOnClickListener(v -> {
            activeSessionStart = System.currentTimeMillis();
            start.setEnabled(false);
            finish.setEnabled(true);
            timerRunnable = new Runnable() {
                @Override public void run() {
                    long elapsed = System.currentTimeMillis() - activeSessionStart;
                    timer.setText(formatClock(elapsed));
                    timerHandler.postDelayed(this, 500);
                }
            };
            timerHandler.post(timerRunnable);
        });

        finish.setOnClickListener(v -> {
            long elapsed = activeSessionStart == 0 ? 0 : System.currentTimeMillis() - activeSessionStart;
            timerHandler.removeCallbacksAndMessages(null);
            addStudySeconds(elapsed / 1000L);
            activeSessionStart = 0L;
            dialog[0].dismiss();
            showHome();
        });

        dialog[0].setOnDismissListener(v -> timerHandler.removeCallbacksAndMessages(null));
        dialog[0].show();
    }

    private void seedSyllabus() {
        if (prefs.getBoolean("seeded", false)) return;
        SharedPreferences.Editor e = prefs.edit();
        for (String c : physics) e.putInt(chapterKey("Physics", c), 0);
        for (String c : chemistry) e.putInt(chapterKey("Chemistry", c), 0);
        for (String c : maths) e.putInt(chapterKey("Mathematics", c), 0);
        e.putInt(DAILY_TARGET, 300);
        e.putBoolean("seeded", true);
        e.apply();
    }

    private String[] withCustom(String subject, String[] base) {
        String extra = prefs.getString("custom_" + subject, "");
        if (extra == null || extra.trim().isEmpty()) return base;
        String[] additions = extra.split("\\|");
        String[] result = new String[base.length + additions.length];
        System.arraycopy(base, 0, result, 0, base.length);
        System.arraycopy(additions, 0, result, base.length, additions.length);
        return result;
    }

    private List<Task> loadTasks() {
        List<Task> list = new ArrayList<>();
        String raw = prefs.getString(TASKS, "[]");
        try {
            JSONArray arr = new JSONArray(raw);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Task(
                        o.optString("title"),
                        o.optInt("minutes", 30),
                        o.optInt("priority", 2),
                        o.optBoolean("completed", false)
                ));
            }
        } catch (Exception ignored) {}
        return list;
    }

    private void saveTasks(List<Task> tasks) {
        JSONArray arr = new JSONArray();
        for (Task t : tasks) {
            try {
                JSONObject o = new JSONObject();
                o.put("title", t.title);
                o.put("minutes", t.minutes);
                o.put("priority", t.priority);
                o.put("completed", t.completed);
                arr.put(o);
            } catch (Exception ignored) {}
        }
        prefs.edit().putString(TASKS, arr.toString()).apply();
    }

    private Task bestOpenTask() {
        Task best = null;
        for (Task t : loadTasks()) {
            if (!t.completed && (best == null || t.priority > best.priority)) best = t;
        }
        return best;
    }

    private String firstIncompleteChapter() {
        for (String c : withCustom("Physics", physics)) if (chapterProgress("Physics", c) < 100) return c;
        for (String c : withCustom("Chemistry", chemistry)) if (chapterProgress("Chemistry", c) < 100) return c;
        for (String c : withCustom("Mathematics", maths)) if (chapterProgress("Mathematics", c) < 100) return c;
        return "All chapters complete 🎯";
    }

    private int openTaskCount() {
        int n = 0;
        for (Task t : loadTasks()) if (!t.completed) n++;
        return n;
    }

    private float overallSyllabus() {
        return (subjectProgress("Physics", physics) + subjectProgress("Chemistry", chemistry) + subjectProgress("Mathematics", maths)) / 3f;
    }

    private float subjectProgress(String subject, String[] base) {
        String[] chapters = withCustom(subject, base);
        float sum = 0;
        for (String c : chapters) sum += chapterProgress(subject, c);
        return chapters.length == 0 ? 0 : sum / chapters.length;
    }

    private float chapterProgress(String subject, String chapter) {
        return prefs.getInt(chapterKey(subject, chapter), 0);
    }

    private String chapterKey(String subject, String chapter) {
        return "chapter_" + subject + "_" + chapter.replaceAll("[^A-Za-z0-9]", "_");
    }

    private long todayStudySeconds() {
        return prefs.getLong("study_" + dayKey(0), 0L);
    }

    private long weekStudyMinutes() {
        long total = 0;
        for (int i = 0; i < 7; i++) total += prefs.getLong("study_" + dayKey(-i), 0L);
        return total / 60;
    }

    private void addStudySeconds(long seconds) {
        if (seconds <= 0) return;
        String key = "study_" + dayKey(0);
        prefs.edit().putLong(key, prefs.getLong(key, 0L) + seconds).apply();
    }

    private double dailyProgress() {
        return Math.min(1.0, todayStudySeconds() / (double)(dailyTargetMinutes() * 60L));
    }

    private int dailyTargetMinutes() {
        return prefs.getInt(DAILY_TARGET, 300);
    }

    private String countdownText() {
        long target = prefs.getLong(TARGET_DATE, 0L);
        if (target == 0L) return "TARGET NOT SET";
        long diff = target - System.currentTimeMillis();
        long days = Math.max(0, diff / 86400000L);
        return days + " DAYS";
    }

    private String targetSubtitle() {
        long target = prefs.getLong(TARGET_DATE, 0L);
        if (target == 0L) return "Set your own target date; the app will count down to it.";
        return "Target • " + new SimpleDateFormat("dd MMM yyyy", Locale.US).format(new Date(target));
    }

    private String dayKey(int offset) {
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, offset);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
    }

    private String formatMinutes(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        if (h == 0) return m + "m";
        return h + "h " + m + "m";
    }

    private String formatClock(long millis) {
        long sec = millis / 1000;
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s);
    }

    private int safeInt(String s, int fallback) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return fallback; }
    }

    private TextView text(String value, float size, int color, int style) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(Typeface.DEFAULT, style));
        return t;
    }

    private EditText edit(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(TEXT);
        e.setHintTextColor(MUTED);
        e.setSingleLine(false);
        e.setBackgroundColor(Color.TRANSPARENT);
        e.setPadding(dp(4), dp(10), dp(4), dp(10));
        return e;
    }

    private MaterialButton button(String label) {
        MaterialButton b = new MaterialButton(this);
        b.setText(label);
        b.setTextSize(12);
        b.setTextColor(TEXT);
        b.setAllCaps(false);
        return b;
    }

    private void addActionButton(String label, View.OnClickListener listener, int color) {
        MaterialButton b = button(label);
        b.setOnClickListener(listener);
        b.setTextColor(BG);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        content.addView(b, marginParams(-1, 4, 0, 16));
    }

    private MaterialCardView card(int color) {
        MaterialCardView c = new MaterialCardView(this);
        c.setCardBackgroundColor(color);
        c.setStrokeColor(Color.rgb(42, 52, 63));
        c.setStrokeWidth(dp(1));
        c.setRadius(dp(18));
        c.setUseCompatPadding(false);
        return c;
    }

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private LinearLayout horizontal() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.HORIZONTAL);
        return l;
    }

    private LinearLayout.LayoutParams marginParams(int w, int top, int bottom, int end) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w == 0 ? 0 : w, -2);
        p.setMargins(0, dp(top), dp(end), dp(bottom));
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class Task {
        String title;
        int minutes;
        int priority;
        boolean completed;

        Task(String title, int minutes, int priority, boolean completed) {
            this.title = title;
            this.minutes = minutes;
            this.priority = priority;
            this.completed = completed;
        }

        String priorityName() {
            return priority == 3 ? "High" : priority == 2 ? "Medium" : "Low";
        }
    }
}
