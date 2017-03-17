package marxtseng.a1knowinstructor;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.threeten.bp.Duration;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private String LOGOUT_URL = "https://auth.ischool.com.tw/logout.php";
    private String SERVICE_URL = "http://1know.net/private";

    private String COOKIE;

    private boolean mIsError;
    private boolean mIsSave;
    private boolean mMenuItem = true;

    private MenuItem mSaveMenuItem;
    private MenuItem mPublishMenuItem;

    private JSONObject mCourse;
    private JSONObject mTopic;
    private JSONObject mUnit = new JSONObject();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        checkCookie();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_menu, menu);

        mSaveMenuItem = menu.getItem(0);
        mPublishMenuItem = menu.getItem(1);

        if (!mMenuItem) {
            mSaveMenuItem.setVisible(false);
            mPublishMenuItem.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_save_unit)
            save();
        else if (id == R.id.action_publish_course)
            publish();
        else if (id == R.id.action_logout)
            logout();

        return super.onOptionsItemSelected(item);
    }

    private void checkCookie() {
        if (!getSharedPreferences("COOKIE_INFO", 0).getBoolean("HAS_COOKIE", false)) {
            login();
        } else
            startup();
    }

    private void login() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setAction(getIntent().getAction());
        intent.setType(getIntent().getType());
        intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
        startActivity(intent);
        finish();
    }

    private void logout() {
        WebView webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.equalsIgnoreCase("https://auth.ischool.com.tw/logout.php")) {
                    getSharedPreferences("COOKIE_INFO", 0).edit().clear().commit();
                    finish();
                }
            }
        });
        webView.loadUrl(LOGOUT_URL);
    }

    private void startup() {
        COOKIE = getSharedPreferences("COOKIE_INFO", 0).getString("COOKIE", "");

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type.equals("text/plain")) {
            String url = intent.getStringExtra(Intent.EXTRA_TEXT);

            Matcher matcherYoutube = Pattern.compile("^.*?youtu(?:\\.be|be\\.com)\\/(?:watch\\?[^#]*v=|embed\\/)?([a-z0-9_\\-]+)", Pattern.CASE_INSENSITIVE).matcher(url);
            Matcher matcherVimeo = Pattern.compile("^.*(vimeo\\.com\\/)((channels\\/[A-z]+\\/)|(groups\\/[A-z]+\\/videos\\/))?([0-9]+)", Pattern.CASE_INSENSITIVE).matcher(url);
            Uri uri = Uri.parse(url);

            if (matcherYoutube.matches())
                toYoutube(url, matcherYoutube.group(1));
            else if (matcherVimeo.matches())
                toVimeo(url, matcherVimeo.group(5));
            else if (uri != null)
                toWeb(url);
            else
                mIsError = true;

            if (!mIsError)
                getCourses();
            else
                toastError();
        } else {
            findViewById(R.id.contentPanel).setVisibility(View.GONE);
            findViewById(R.id.errorTextView).setVisibility(View.VISIBLE);

            mMenuItem = false;
        }
    }

    private void toastError() {
        Toast.makeText(this, R.string.unsupported_shared_content, Toast.LENGTH_SHORT).show();
        findViewById(R.id.contentPanel).setVisibility(View.GONE);
        findViewById(R.id.errorTextView).setVisibility(View.VISIBLE);

        setMenuItem(false);
    }

    private void setMenuItem(boolean enbaled) {
        if (mSaveMenuItem != null) {
            mSaveMenuItem.setEnabled(enbaled);
            mSaveMenuItem.setVisible(enbaled);
        }

        if (mPublishMenuItem != null) {
            mPublishMenuItem.setEnabled(enbaled);
            mPublishMenuItem.setVisible(enbaled);
        }
    }

    private void toYoutube(final String url, final String videoId) {
        String apiUrl = TextUtils.join("", new String[]{"https://www.googleapis.com/youtube/v3/videos?id=", videoId, "&part=snippet,contentDetails&fields=items(id,snippet(title,description),contentDetails(duration))&key=AIzaSyCNnpxFYGsummFFWqRJnXt3IOWtA4XMI1M"});

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject entry = response.getJSONArray("items").getJSONObject(0);
                    long duration = Duration.parse(entry.getJSONObject("contentDetails").getString("duration")).getSeconds();

                    JSONObject content = new JSONObject();
                    content.put("duration", duration);
                    content.put("min", 0);
                    content.put("max", duration);
                    content.put("floor", 0);
                    content.put("ceil", duration);

                    JSONObject completion = new JSONObject();
                    completion.put("type", "automatic");
                    completion.put("percentage", 98);

                    mUnit.put("name", entry.getJSONObject("snippet").getString("title"));
                    mUnit.put("unit_type", "video");
                    mUnit.put("content_url", url);
                    mUnit.put("content_time", duration);
                    mUnit.put("content", content.toString());
                    mUnit.put("completion", completion.toString());
                    mUnit.put("description", entry.getJSONObject("snippet").getString("description"));
                    mUnit.put("logo", TextUtils.join("", new String[]{"http://i.ytimg.com/vi/", videoId, "/mqdefault.jpg"}));
                    mUnit.put("optional", false);

                    render();
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                    setMenuItem(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.getMessage());

                Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                setMenuItem(false);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void toVimeo(final String url, final String videoId) {
        String apiUrl = TextUtils.join("", new String[]{"http://vimeo.com/api/oembed.json?url=https://vimeo.com/", videoId});

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, apiUrl, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    long duration = response.getLong("duration");

                    JSONObject content = new JSONObject();
                    content.put("duration", duration);
                    content.put("min", 0);
                    content.put("max", duration);
                    content.put("floor", 0);
                    content.put("ceil", duration);

                    JSONObject completion = new JSONObject();
                    completion.put("type", "automatic");
                    completion.put("percentage", 98);

                    mUnit.put("name", response.getString("title"));
                    mUnit.put("unit_type", "video");
                    mUnit.put("content_url", url);
                    mUnit.put("content_time", duration);
                    mUnit.put("content", content.toString());
                    mUnit.put("completion", completion.toString());
                    mUnit.put("description", response.getString("description"));
                    mUnit.put("logo", response.getString("thumbnail_url"));
                    mUnit.put("optional", false);

                    render();
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                    setMenuItem(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.getMessage());

                Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                setMenuItem(false);
            }
        });

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonObjectRequest);
    }

    private void toWeb(final String url) {
        String apiUrl = TextUtils.join("", new String[]{SERVICE_URL, "/utility/parseURL"});

        try {
            JSONObject request = new JSONObject();
            request.put("url", url);

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiUrl, request, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    try {
                        JSONObject content = new JSONObject();
                        content.put("sandbox", true);
                        content.put("x_frame_options", !response.isNull("x_frame_options"));

                        JSONObject completion = new JSONObject();
                        completion.put("type", "automatic");
                        completion.put("time", 1);

                        mUnit.put("name", response.getString("title"));
                        mUnit.put("unit_type", "web");
                        mUnit.put("content_url", url);
                        mUnit.put("content_time", 1);
                        mUnit.put("content", content.toString());
                        mUnit.put("completion", completion.toString());
                        mUnit.put("description", "");
                        mUnit.put("logo", "");
                        mUnit.put("optional", false);

                        render();
                    } catch (JSONException e) {
                        Log.d(TAG, e.getMessage());

                        Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                        setMenuItem(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                    setMenuItem(false);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cookie", COOKIE);
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.d(TAG, "Web Api JSONObject : " + e.toString());

            Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
            setMenuItem(false);
        }
    }

    private void render() {
        try {
            EditText nameView = (EditText) findViewById(R.id.name);
            EditText descView = (EditText) findViewById(R.id.description);

            nameView.setText(mUnit.getString("name"));
            descView.setText(mUnit.getString("description"));
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private void getCourses() {
        String url = TextUtils.join("", new String[]{SERVICE_URL, "/course?ctype=created"});

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray response) {
                try {
                    final JSONArray courses = response;

                    String[] items = new String[courses.length()];
                    for (int i = 0; i < items.length; i++)
                        items[i] = courses.getJSONObject(i).getString("name");

                    Spinner spinner = (Spinner) findViewById(R.id.course);

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_dropdown_title, items);
                    adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinner.setAdapter(adapter);
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            try {
                                mCourse = courses.getJSONObject(position);
                                getTopics();
                            } catch (JSONException e) {
                                Log.d(TAG, e.getMessage());
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {

                        }
                    });
                } catch (JSONException e) {
                    Log.d(TAG, e.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                    setMenuItem(false);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d(TAG, error.getMessage());

                Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                setMenuItem(false);
            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
                headers.put("Cookie", COOKIE);
                return headers;
            }
        };

        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(jsonArrayRequest);
    }

    private void getTopics() {
        try {
            String url = TextUtils.join("", new String[]{SERVICE_URL, "/course/", mCourse.getString("uqid"), "/chapter?ctype=created"});

            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        final JSONArray topics = response;

                        String[] items = new String[topics.length()];
                        for (int i = 0; i < items.length; i++)
                            items[i] = topics.getJSONObject(i).getString("name");

                        Spinner spinner = (Spinner) findViewById(R.id.topic);

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_dropdown_title, items);
                        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                        spinner.setAdapter(adapter);
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                try {
                                    mTopic = topics.getJSONObject(position);
                                    getUnits();
                                } catch (JSONException e) {
                                    Log.d(TAG, e.getMessage());
                                }
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                    } catch (JSONException e) {
                        Log.d(TAG, e.getMessage());

                        Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                        setMenuItem(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                    setMenuItem(false);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cookie", COOKIE);
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(jsonArrayRequest);
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());

            Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
            setMenuItem(false);
        }
    }

    private void getUnits() {
        try {
            String url = TextUtils.join("", new String[]{SERVICE_URL, "/course/chapter/", mTopic.getString("uqid"), "/unit?ctype=created"});

            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray response) {
                    try {
                        mTopic.put("units", response);
                    } catch (JSONException e) {
                        Log.d(TAG, e.getMessage());

                        Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                        setMenuItem(false);
                    }
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
                    setMenuItem(false);
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cookie", COOKIE);
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(jsonArrayRequest);
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());

            Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
            setMenuItem(false);
        }
    }

    private void save() {
        if (mIsError) {
            toastError();
            return;
        }

        if (mIsSave)
            return;

        try {
            String url = TextUtils.join("", new String[]{SERVICE_URL, "/course/chapter/", mTopic.getString("uqid"), "/unit"});

            JSONObject request = new JSONObject();
            request.put("name", mUnit.getString("name"));
            request.put("priority", mTopic.getJSONArray("units").length()+1);
            request.put("description", mUnit.getString("description"));
            request.put("unit_type", mUnit.getString("unit_type"));
            request.put("content_url", mUnit.getString("content_url"));
            request.put("content_time", mUnit.getLong("content_time"));
            request.put("content", mUnit.getString("content"));
            request.put("completion", mUnit.getString("completion"));
            request.put("logo", mUnit.getString("logo"));
            request.put("optional", mUnit.getBoolean("optional"));

            JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, request, new Response.Listener<JSONObject>() {
                @Override
                public void onResponse(JSONObject response) {
                    if (response.isNull("error")) {
                        Toast.makeText(getApplicationContext(), R.string.unit_saved, Toast.LENGTH_SHORT).show();

                        mIsSave = true;
                        mSaveMenuItem.setEnabled(false);
                    }
                    else
                        Toast.makeText(getApplicationContext(), R.string.saved_data_error_please_try_again_later, Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Toast.makeText(getApplicationContext(), R.string.saved_data_error_please_try_again_later, Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cookie", COOKIE);
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(jsonObjectRequest);
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());

            Toast.makeText(getApplicationContext(), R.string.saved_data_error_please_try_again_later, Toast.LENGTH_SHORT).show();
        }
    }

    private void publish() {
        if (mIsError) {
            toastError();
            return;
        }

        if (mCourse == null) {
            Toast.makeText(getApplicationContext(), R.string.error_fetching_data_please_try_again_later, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            String url = TextUtils.join("", new String[]{SERVICE_URL, "/course/", mCourse.getString("uqid"), "/publish"});

            StringRequest stringRequest = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    if (response.equalsIgnoreCase("done!"))
                        Toast.makeText(getApplicationContext(), R.string.course_published, Toast.LENGTH_SHORT).show();
                    else
                        Toast.makeText(getApplicationContext(), R.string.saved_data_error_please_try_again_later, Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d(TAG, error.getMessage());

                    Toast.makeText(getApplicationContext(), R.string.saved_data_error_please_try_again_later, Toast.LENGTH_SHORT).show();
                }
            }) {
                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Cookie", COOKIE);
                    return headers;
                }
            };

            RequestQueue requestQueue = Volley.newRequestQueue(this);
            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            Log.d(TAG, e.getMessage());

            Toast.makeText(getApplicationContext(), R.string.saved_data_error_please_try_again_later, Toast.LENGTH_SHORT).show();
        }
    }
}
