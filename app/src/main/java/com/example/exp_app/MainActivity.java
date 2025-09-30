package com.example.exp_app;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private LinearLayout emptyView;
    private List<Product> productList;

    private static final String PREF_NAME = "expguard_pref";
    private static final String KEY_PRODUCT_LIST = "product_list";

    private RecyclerView.Adapter<RecyclerView.ViewHolder> adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnFilter = findViewById(R.id.btnFilter);
        recyclerView = findViewById(R.id.recyclerView);
        emptyView = findViewById(R.id.emptyView);

        btnFilter.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);
            popupMenu.getMenu().add("유통기한 임박순");
            popupMenu.getMenu().add("가나다순");
            popupMenu.getMenu().add("최신순");
            popupMenu.getMenu().add("오래된순");

            popupMenu.setOnMenuItemClickListener(item -> {
                String selected = item.getTitle().toString();
                btnFilter.setText(selected + " ▼");

                switch (selected) {
                    case "유통기한 임박순":
                        Collections.sort(productList, (a, b) -> a.getExpirationDate().compareTo(b.getExpirationDate()));
                        break;
                    case "가나다순":
                        Collections.sort(productList, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                        break;
                    case "최신순":
                        Collections.sort(productList, (a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
                        break;
                    case "오래된순":
                        Collections.sort(productList, (a, b) -> Long.compare(a.getCreatedAt(), b.getCreatedAt()));
                        break;
                }

                adapter.notifyDataSetChanged();
                return true;
            });

            popupMenu.show();
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        productList = loadProductList();

        if (productList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
        }

        checkAndNotifyExpiringProducts();

        adapter = new RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            @Override
            public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                LinearLayout outerLayout = new LinearLayout(parent.getContext());
                outerLayout.setOrientation(LinearLayout.HORIZONTAL);
                outerLayout.setPadding(16, 16, 16, 16);
                outerLayout.setGravity(Gravity.CENTER_VERTICAL);
                outerLayout.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                View dotView = new View(parent.getContext());
                LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(30, 30);
                dotParams.setMargins(16, 16, 16, 16);
                dotView.setLayoutParams(dotParams);

                LinearLayout textLayout = new LinearLayout(parent.getContext());
                textLayout.setOrientation(LinearLayout.VERTICAL);
                LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                textParams.setMargins(16, 0, 16, 0);
                textLayout.setLayoutParams(textParams);

                TextView name = new TextView(parent.getContext());
                name.setTextSize(18);
                name.setTypeface(null, Typeface.BOLD);

                TextView status = new TextView(parent.getContext());

                textLayout.addView(name);
                textLayout.addView(status);

                Button btnSearch = new Button(parent.getContext());
                btnSearch.setText("분리배출 방법 보기");
                LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                btnSearch.setLayoutParams(btnParams);

                outerLayout.addView(dotView);
                outerLayout.addView(textLayout);
                outerLayout.addView(btnSearch);

                return new RecyclerView.ViewHolder(outerLayout) {
                    TextView nameView = name;
                    TextView statusView = status;
                    View colorDot = dotView;
                    Button searchButton = btnSearch;
                };
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
                Product p = productList.get(position);

                LinearLayout layout = (LinearLayout) holder.itemView;
                View colorDot = layout.getChildAt(0);
                LinearLayout textLayout = (LinearLayout) layout.getChildAt(1);
                Button btn = (Button) layout.getChildAt(2);

                TextView nameView = (TextView) textLayout.getChildAt(0);
                TextView statusView = (TextView) textLayout.getChildAt(1);

                nameView.setText(p.getName());

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                try {
                    Date expDate = sdf.parse(p.getExpirationDate());
                    long diff = expDate.getTime() - System.currentTimeMillis();
                    long daysLeft = diff / (1000 * 60 * 60 * 24);

                    if (daysLeft < 0) {
                        statusView.setText("유통기한 지남");
                        colorDot.setBackgroundColor(Color.BLACK);
                        btn.setVisibility(View.VISIBLE);
                    } else if (daysLeft < 3) {
                        statusView.setText("유통기한: " + p.getExpirationDate() + " / " + daysLeft + "일 남음");
                        colorDot.setBackgroundColor(Color.RED);
                        btn.setVisibility(View.GONE);
                    } else {
                        statusView.setText("유통기한: " + p.getExpirationDate() + " / " + daysLeft + "일 남음");
                        colorDot.setBackgroundColor(Color.GREEN);
                        btn.setVisibility(View.GONE);
                    }
                } catch (ParseException e) {
                    statusView.setText("날짜 오류");
                    colorDot.setBackgroundColor(Color.GRAY);
                    btn.setVisibility(View.GONE);
                }

                btn.setOnClickListener(v -> {
                    String keyword = p.getName() + " 분리배출 방법";
                    String url = "https://www.google.com/search?q=" + Uri.encode(keyword);
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    v.getContext().startActivity(intent);
                });

                layout.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(MainActivity.this)
                            .setTitle("삭제")
                            .setMessage("이 제품을 삭제할까요?")
                            .setPositiveButton("삭제", (dialog, which) -> {
                                productList.remove(position);
                                saveProductList(productList);
                                adapter.notifyDataSetChanged();

                                if (productList.isEmpty()) {
                                    recyclerView.setVisibility(View.GONE);
                                    findViewById(R.id.emptyView).setVisibility(View.VISIBLE);
                                } else {
                                    recyclerView.setVisibility(View.VISIBLE);
                                    findViewById(R.id.emptyView).setVisibility(View.GONE);
                                }

                                Toast.makeText(MainActivity.this, "삭제됨", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                    return true;
                });
            }

            @Override
            public int getItemCount() {
                return productList.size();
            }
        };

        recyclerView.setAdapter(adapter);

        FloatingActionButton fab = findViewById(R.id.fabAdd);
        fab.setOnClickListener(v -> showAddProductDialog());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "알림 권한이 허용되었습니다.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "알림 권한이 거부되었습니다. 일부 기능이 제한됩니다.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkAndNotifyExpiringProducts() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "exp_alert_channel";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId,
                    "유통기한 알림",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            notificationManager.createNotificationChannel(channel);
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < productList.size(); i++) {
            Product p = productList.get(i);
            try {
                Date expDate = sdf.parse(p.getExpirationDate());
                long diff = expDate.getTime() - System.currentTimeMillis();
                long daysLeft = diff / (1000 * 60 * 60 * 24);

                if (daysLeft >= 0 && daysLeft < 3) {
                    String title = "유통기한 임박";
                    String content = p.getName() + "의 유통기한이 " + daysLeft + "일 남았습니다.";

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
                            .setSmallIcon(R.drawable.logo_notification)
                            .setContentTitle(title)
                            .setContentText(content)
                            .setPriority(NotificationCompat.PRIORITY_DEFAULT);

                    notificationManager.notify(i, builder.build());
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }

    private void showAddProductDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("새 제품 추가");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 30, 30, 30);

        EditText inputName = new EditText(this);
        inputName.setHint("제품명");
        layout.addView(inputName);

        final TextView dateDisplay = new TextView(this);
        dateDisplay.setText("유통기한 선택");
        dateDisplay.setTextSize(16);
        dateDisplay.setPadding(20, 40, 20, 40);
        dateDisplay.setBackgroundColor(Color.LTGRAY);
        dateDisplay.setGravity(Gravity.CENTER);
        layout.addView(dateDisplay);

        final Calendar calendar = Calendar.getInstance();
        final String[] selectedDate = {""};

        builder.setView(layout);
        builder.setPositiveButton("추가", null);
        builder.setNegativeButton("취소", null);

        AlertDialog dialog = builder.create();

        dateDisplay.setOnClickListener(v -> {
            int year = calendar.get(Calendar.YEAR);
            int month = calendar.get(Calendar.MONTH);
            int day = calendar.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, y, m, d) -> {
                selectedDate[0] = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d);
                dateDisplay.setText(selectedDate[0]);

                Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (positiveButton != null) {
                    boolean enable = !inputName.getText().toString().trim().isEmpty() && !selectedDate[0].isEmpty();
                    positiveButton.setEnabled(enable);
                }
            }, year, month, day);
            datePickerDialog.show();
        });

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
            inputName.addTextChangedListener(new android.text.TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                    positiveButton.setEnabled(!s.toString().trim().isEmpty() && !selectedDate[0].isEmpty());
                }
                @Override public void afterTextChanged(android.text.Editable s) {}
            });

            positiveButton.setOnClickListener(view -> {
                String name = inputName.getText().toString().trim();
                String date = selectedDate[0];

                if (!name.isEmpty() && !date.isEmpty()) {
                    Product newProduct = new Product(name, date, "");
                    productList.add(newProduct);
                    saveProductList(productList);
                    adapter.notifyDataSetChanged();
                    if (productList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    } else {
                        recyclerView.setVisibility(View.VISIBLE);
                        emptyView.setVisibility(View.GONE);
                    }
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, "제품명과 유통기한을 모두 입력해주세요.", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }


    private void saveProductList(List<Product> list) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        Gson gson = new Gson();
        String json = gson.toJson(list);
        editor.putString(KEY_PRODUCT_LIST, json);
        editor.apply();
    }

    private List<Product> loadProductList() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_PRODUCT_LIST, null);
        if (json != null) {
            Gson gson = new Gson();
            Type type = new TypeToken<ArrayList<Product>>() {}.getType();
            return gson.fromJson(json, type);
        } else {
            return new ArrayList<>();
        }

    }
}