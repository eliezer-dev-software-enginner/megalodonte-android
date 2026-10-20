package megalodonte.android.bridge;

import android.app.DatePickerDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.function.Consumer;
import megalodonte.base.components.Component;
import megalodonte.base.state.State;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** Android-native controls for the data and media JSONC component families. */
final class AndroidJsoncControl extends Component {
    private final String type;
    private final JSONObject props;
    private final State<String> data;
    private final State<String> value;
    private final State<Integer> selected;
    private final State<String> selectedItems;
    private final String event;
    private final String doubleClickEvent;
    private final Consumer<String> actions;

    AndroidJsoncControl(String type, JSONObject props, State<String> data, State<String> value,
                        State<Integer> selected, State<String> selectedItems, String event,
                        String doubleClickEvent, Consumer<String> actions) {
        this.type = type;
        this.props = props;
        this.data = data;
        this.value = value;
        this.selected = selected;
        this.selectedItems = selectedItems;
        this.event = event;
        this.doubleClickEvent = doubleClickEvent;
        this.actions = actions;
    }

    View create(Context context, List<Runnable> cleanup) {
        return switch (type) {
            case "Select" -> select(context, cleanup);
            case "DatePicker" -> datePicker(context, cleanup);
            case "Image", "ImagePreview" -> image(context, cleanup);
            case "ImageSequence" -> imageSequence(context, cleanup);
            case "SoundSequence" -> list(context, cleanup, true);
            case "DataList" -> list(context, cleanup, false);
            case "DiffView" -> diff(context, cleanup);
            case "Table" -> table(context, cleanup);
            case "BarChart", "PieChart" -> chart(context, cleanup);
            default -> throw new IllegalArgumentException("Unsupported Android JSONC control: " + type);
        };
    }

    private View select(Context context, List<Runnable> cleanup) {
        Spinner spinner = new Spinner(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinner.setAdapter(adapter);
        List<String> values = new ArrayList<>();
        boolean[] updating = {false};
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (!updating[0] && value != null && position < values.size()) value.set(values.get(position));
            }
        });
        if (data != null) cleanup.add(data.observe(json -> {
            JSONArray items = array(json);
            updating[0] = true;
            values.clear(); adapter.clear();
            for (int i = 0; i < items.length(); i++) {
                Object entry = items.opt(i);
                JSONObject option = entry instanceof JSONObject object ? object : null;
                values.add(option == null ? String.valueOf(entry) : option.optString("value"));
                adapter.add(option == null ? String.valueOf(entry) : option.optString("label"));
            }
            adapter.notifyDataSetChanged();
            int index = values.indexOf(value == null ? "" : value.get());
            if (index >= 0) spinner.setSelection(index);
            updating[0] = false;
        })::close);
        if (value != null) cleanup.add(value.observe(current -> {
            int index = values.indexOf(current);
            if (index >= 0 && spinner.getSelectedItemPosition() != index) {
                updating[0] = true; spinner.setSelection(index); updating[0] = false;
            }
        })::close);
        cleanup.add(() -> spinner.setOnItemSelectedListener(null));
        return spinner;
    }

    private View datePicker(Context context, List<Runnable> cleanup) {
        EditText field = new EditText(context);
        field.setFocusable(false);
        field.setHint("AAAA-MM-DD");
        if (value != null) cleanup.add(value.observe(field::setText)::close);
        field.setOnClickListener(ignored -> {
            LocalDate initial;
            try { initial = LocalDate.parse(value == null ? "" : value.get()); }
            catch (RuntimeException error) { initial = LocalDate.now(); }
            new DatePickerDialog(context, (picker, year, month, day) -> {
                String iso = LocalDate.of(year, month + 1, day).toString();
                if (value != null) value.set(iso); else field.setText(iso);
            }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
        });
        cleanup.add(() -> field.setOnClickListener(null));
        return field;
    }

    private View image(Context context, List<Runnable> cleanup) {
        ImageView image = new ImageView(context);
        image.setContentDescription(props.optString("description", "Image"));
        image.setAdjustViewBounds(true);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setLayoutParams(new ViewGroup.LayoutParams(dp(context, props.optInt("width", 160)),
                dp(context, props.optInt("height", 120))));
        if (data == null) setImage(image, props.optString("source", ""));
        else cleanup.add(data.observe(source -> setImage(image, source))::close);
        return image;
    }

    private View imageSequence(Context context, List<Runnable> cleanup) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        LinearLayout strip = new LinearLayout(context);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        scroll.addView(strip);
        if (data != null) cleanup.add(data.observe(json -> {
            strip.removeAllViews();
            JSONArray items = array(json);
            for (int i = 0; i < items.length(); i++) {
                final int index = i;
                Object entry = items.opt(i);
                JSONObject item = entry instanceof JSONObject object ? object : null;
                String source = item == null ? String.valueOf(entry) : item.optString("source");
                ImageView image = new ImageView(context);
                image.setAdjustViewBounds(true);
                image.setScaleType(ImageView.ScaleType.CENTER_CROP);
                image.setContentDescription(item == null ? source : item.optString("label", source)
                        + (item == null ? "" : ", " + item.optDouble("duration") + " segundos"));
                image.setLayoutParams(new LinearLayout.LayoutParams(dp(context, 96), dp(context, 96)));
                setImage(image, source);
                image.setOnClickListener(ignored -> choose(index));
                strip.addView(image);
            }
        })::close);
        return scroll;
    }

    private View list(Context context, List<Runnable> cleanup, boolean sounds) {
        ListView list = new ListView(context);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, new ArrayList<>());
        list.setAdapter(adapter);
        if (data != null) cleanup.add(data.observe(json -> {
            JSONArray items = array(json);
            adapter.clear();
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                String label = item == null ? String.valueOf(items.opt(i)) : item.optString("label", item.optString("title", ""));
                if (sounds && item != null)
                    label += "\n" + item.optDouble("start") + "s · " + item.optDouble("duration")
                            + "s · volume " + item.optDouble("volume", 1);
                if (!sounds && item != null) {
                    if (!item.optString("detail").isBlank()) label += "\n" + item.optString("detail");
                    if (!item.optString("badge").isBlank()) label += " · " + item.optString("badge");
                }
                adapter.add(label);
            }
            adapter.notifyDataSetChanged();
        })::close);
        list.setOnItemClickListener((parent, view, position, id) -> choose(position));
        if (selected != null) cleanup.add(selected.observe(index -> {
            if (index != null && index >= 0) list.setItemChecked(index, true);
        })::close);
        cleanup.add(() -> list.setOnItemClickListener(null));
        return list;
    }

    private View diff(Context context, List<Runnable> cleanup) {
        TextView text = new TextView(context);
        text.setTypeface(Typeface.MONOSPACE);
        text.setTextIsSelectable(true);
        text.setPadding(dp(context, 12), dp(context, 12), dp(context, 12), dp(context, 12));
        if (data != null) cleanup.add(data.observe(text::setText)::close);
        return text;
    }

    private View table(Context context, List<Runnable> cleanup) {
        HorizontalScrollView scroll = new HorizontalScrollView(context);
        LinearLayout rows = new LinearLayout(context);
        rows.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(rows);
        JSONArray columns = props.optJSONArray("columns");
        if (columns == null) throw new IllegalArgumentException("Table requires props.columns");
        rows.addView(tableRow(context, columns, null));
        Map<String, CheckBox> checks = new HashMap<>();
        boolean[] updating = {false};
        long[] lastClick = {0};
        int[] lastIndex = {-1};
        if (data != null) cleanup.add(data.observe(json -> {
            rows.removeViews(1, Math.max(0, rows.getChildCount() - 1));
            checks.clear();
            JSONArray items = array(json);
            for (int i = 0; i < items.length(); i++) {
                final int index = i;
                JSONObject item = items.optJSONObject(i);
                if (item == null) throw new IllegalArgumentException("Table rows must be objects");
                LinearLayout row = (LinearLayout) tableRow(context, columns, item);
                String serialized = item.toString();
                if (props.optBoolean("multipleSelection") && selectedItems != null) {
                    CheckBox check = new CheckBox(context);
                    updating[0] = true;
                    check.setChecked(contains(array(selectedItems.get()), serialized));
                    updating[0] = false;
                    check.setOnCheckedChangeListener((button, checked) -> {
                        if (updating[0]) return;
                        JSONArray current = array(selectedItems.get());
                        JSONArray next = new JSONArray();
                        for (int j = 0; j < current.length(); j++) {
                            Object entry = current.opt(j);
                            if (entry != null && !serialized.equals(entry.toString())) next.put(entry);
                        }
                        if (checked) next.put(item);
                        selectedItems.set(next.toString());
                        if (!event.isBlank()) actions.accept(event);
                    });
                    row.addView(check, 0);
                    checks.put(serialized, check);
                }
                row.setOnClickListener(ignored -> {
                    if (value != null) value.set(serialized);
                    choose(index);
                    long now = android.os.SystemClock.uptimeMillis();
                    if (lastIndex[0] == index && now - lastClick[0] < 350 && !doubleClickEvent.isBlank())
                        actions.accept(doubleClickEvent);
                    lastIndex[0] = index;
                    lastClick[0] = now;
                });
                rows.addView(row);
            }
        })::close);
        if (selectedItems != null) cleanup.add(selectedItems.observe(json -> {
            JSONArray current = array(json);
            updating[0] = true;
            checks.forEach((serialized, check) -> check.setChecked(contains(current, serialized)));
            updating[0] = false;
        })::close);
        return scroll;
    }

    private static boolean contains(JSONArray array, String value) {
        for (int i = 0; i < array.length(); i++) {
            Object entry = array.opt(i);
            if (entry != null && value.equals(entry.toString())) return true;
        }
        return false;
    }

    private static View tableRow(Context context, JSONArray columns, JSONObject item) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int c = 0; c < columns.length(); c++) {
            JSONObject column = columns.optJSONObject(c);
            if (column == null) throw new IllegalArgumentException("Table column must be an object");
            if (item != null && column.has("kind")) {
                ImageView image = new ImageView(context);
                image.setContentDescription(column.optString("title"));
                image.setScaleType(ImageView.ScaleType.FIT_CENTER);
                setImage(image, item.optString(column.optString("field")));
                row.addView(image, new LinearLayout.LayoutParams(dp(context, 120),
                        dp(context, column.optInt("size", 40))));
                continue;
            }
            TextView cell = new TextView(context);
            String value = item == null ? column.optString("title") : item.optString(column.optString("field"));
            cell.setText(value);
            cell.setPadding(dp(context, 8), dp(context, 8), dp(context, 8), dp(context, 8));
            if (item == null) cell.setTypeface(Typeface.DEFAULT_BOLD);
            row.addView(cell, new LinearLayout.LayoutParams(dp(context, 120), -2));
        }
        return row;
    }

    private View chart(Context context, List<Runnable> cleanup) {
        ChartView chart = new ChartView(context, type.equals("PieChart"), props.optString("title", ""));
        chart.setLayoutParams(new ViewGroup.LayoutParams(dp(context, props.optInt("width", 280)),
                dp(context, props.optInt("height", 220))));
        if (data != null) cleanup.add(data.observe(json -> chart.setItems(array(json)))::close);
        return chart;
    }

    private void choose(int index) {
        if (selected != null) selected.set(index);
        if (!event.isBlank()) actions.accept(event);
    }

    private static JSONArray array(String json) {
        try { return new JSONArray(json == null || json.isBlank() ? "[]" : json); }
        catch (JSONException error) { throw new IllegalArgumentException("JSONC items must be a JSON array", error); }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static void setImage(ImageView image, String source) {
        if (source == null || source.isBlank()) { image.setImageDrawable(null); return; }
        try {
            Bitmap bitmap;
            if (source.startsWith("file:")) bitmap = BitmapFactory.decodeFile(Uri.parse(source).getPath());
            else {
                bitmap = null;
                try (InputStream input = image.getContext().getAssets().open(source.replaceFirst("^/", ""))) {
                    bitmap = BitmapFactory.decodeStream(input);
                } catch (java.io.IOException missingAsset) {
                    if (source.startsWith("/")) bitmap = BitmapFactory.decodeFile(source);
                    else throw missingAsset;
                }
            }
            if (bitmap == null) throw new IllegalArgumentException("Image not found: " + source);
            image.setImageBitmap(bitmap);
        } catch (java.io.IOException error) { throw new IllegalArgumentException("Image not found: " + source, error); }
    }

    private static final class ChartView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final boolean pie;
        private final String title;
        private JSONArray items = new JSONArray();
        ChartView(Context context, boolean pie, String title) { super(context); this.pie = pie; this.title = title; }
        void setItems(JSONArray value) { items = Objects.requireNonNull(value); invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            paint.setColor(Color.DKGRAY); paint.setTextSize(36);
            canvas.drawText(title, 12, 42, paint);
            double total = 0;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item != null) total += Math.max(0, item.optDouble("value"));
            }
            if (total <= 0) return;
            float start = -90;
            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.optJSONObject(i);
                if (item == null) continue;
                float fraction = (float) (Math.max(0, item.optDouble("value")) / total);
                paint.setColor(Color.HSVToColor(new float[]{(i * 67) % 360, .7f, .85f}));
                if (pie) {
                    float diameter = Math.min(getWidth() - 24, getHeight() - 64);
                    canvas.drawArc(12, 56, 12 + diameter, 56 + diameter, start, fraction * 360, true, paint);
                    start += fraction * 360;
                } else {
                    float width = (getWidth() - 24f) / items.length();
                    float height = (getHeight() - 72) * fraction;
                    canvas.drawRect(12 + i * width, getHeight() - height - 12, 12 + (i + 1) * width - 6, getHeight() - 12, paint);
                }
            }
        }
    }
}
