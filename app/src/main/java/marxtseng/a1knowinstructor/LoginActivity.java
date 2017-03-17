package marxtseng.a1knowinstructor;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class LoginActivity extends AppCompatActivity {

    private String TAG = "LoginActivity";
    private String LOGIN_URL = "https://auth.ischool.com.tw/oauth/authorize.php?client_id=a266592ce660b43f980d49b4079d53ed&response_type=code&state=mobile&redirect_uri=http://1know.net/oauth/ischool&scope=User.Mail,User.BasicInfo";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        checkCookie();
    }

    private void checkCookie() {
        if (!getSharedPreferences("COOKIE_INFO", 0).getBoolean("HAS_COOKIE", false)) {
            login();
        } else
            startup();
    }

    private void login() {
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (url.equalsIgnoreCase("http://1know.net/mobile/welcome")) {
                    String cookie = CookieManager.getInstance().getCookie("http://1know.net");

                    getSharedPreferences("COOKIE_INFO", 0).edit().putBoolean("HAS_COOKIE", true).apply();
                    getSharedPreferences("COOKIE_INFO", 0).edit().putString("COOKIE", cookie).apply();

                    startup();
                }
            }
        });
        webView.loadUrl(LOGIN_URL);
    }

    private void startup() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setAction(getIntent().getAction());
        intent.setType(getIntent().getType());
        intent.putExtra(Intent.EXTRA_TEXT, getIntent().getStringExtra(Intent.EXTRA_TEXT));
        startActivity(intent);
        finish();
    }
}

