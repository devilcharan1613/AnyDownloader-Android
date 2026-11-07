package com.nyxsky.anydownloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceError;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String HOME_URL = "https://anydownloader-g9r8.onrender.com/";
    private static final String TRUSTED_HOST = "anydownloader-g9r8.onrender.com";

    private WebView webView;
    private ProgressBar progress;
    private TextView status;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildShell();
        configureWebView();
        if (state == null) webView.loadUrl(HOME_URL);
        else if (webView.restoreState(state) == null) webView.loadUrl(HOME_URL);
        updateConnectivity();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(18, 18, 24));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(16), dp(10), dp(12), dp(10));
        bar.setBackgroundColor(Color.rgb(29, 27, 36));

        TextView brand = new TextView(this);
        brand.setText("⬇  AnyDownloader");
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(19);
        brand.setTypeface(null, 1);
        bar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        Button refresh = actionButton("Refresh");
        refresh.setOnClickListener(v -> webView.reload());
        bar.addView(refresh);
        Button share = actionButton("Share");
        share.setOnClickListener(v -> sharePage());
        bar.addView(share);
        root.addView(bar);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        status = new TextView(this);
        status.setTextColor(Color.LTGRAY);
        status.setTextSize(11);
        status.setPadding(dp(12), dp(4), dp(12), dp(4));
        status.setBackgroundColor(Color.rgb(38, 36, 46));
        root.addView(status);

        webView = new WebView(this);
        root.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private Button actionButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(11);
        button.setAllCaps(false);
        button.setTextColor(Color.WHITE);
        return button;
    }

    private void configureWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        CookieManager.getInstance().setAcceptCookie(true);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onProgressChanged(WebView view, int value) {
                progress.setProgress(value);
                progress.setVisibility(value >= 100 ? View.GONE : View.VISIBLE);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return routeUrl(request.getUrl());
            }

            @SuppressWarnings("deprecation")
            @Override public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return routeUrl(Uri.parse(url));
            }

            @Override public void onPageFinished(WebView view, String url) {
                status.setText(isOnline()
                    ? "Connected · Processing is handled by the AnyDownloader service"
                    : "Offline · Connect to the internet to use downloader");
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    status.setText("Unable to load service · Check your connection and retry");
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                                  String mimeType, long contentLength) {
                Uri uri = Uri.parse(url);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                        || !TRUSTED_HOST.equalsIgnoreCase(uri.getHost())) {
                    Toast.makeText(MainActivity.this, "Blocked an untrusted download link", Toast.LENGTH_LONG).show();
                    return;
                }
                try {
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setTitle("AnyDownloader file");
                    request.setDescription("Saving downloaded media");
                    request.setMimeType(mimeType == null ? "application/octet-stream" : mimeType);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(false);

                    String cookies = CookieManager.getInstance().getCookie(uri.toString());
                    if (cookies != null && !cookies.isEmpty()) {
                        request.addRequestHeader("Cookie", cookies);
                    }
                    request.addRequestHeader("User-Agent", webView.getSettings().getUserAgentString());
                    request.addRequestHeader("Referer", HOME_URL);
                    request.setDestinationInExternalPublicDir(
                        Environment.DIRECTORY_DOWNLOADS,
                        "AnyDownloader/" + safeName(uri.getLastPathSegment())
                    );

                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (manager == null) throw new IllegalStateException("Android Download Manager is unavailable");
                    manager.enqueue(request);
                    Toast.makeText(MainActivity.this, "Download added to Android Downloads", Toast.LENGTH_LONG).show();
                } catch (Exception exception) {
                    Toast.makeText(MainActivity.this,
                        "Could not start file download: " + exception.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private boolean routeUrl(Uri uri) {
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme) && TRUSTED_HOST.equalsIgnoreCase(uri.getHost())) return false;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception exception) {
                Toast.makeText(this, "No browser available to open this link", Toast.LENGTH_SHORT).show();
            }
        }
        return true;
    }

    private String safeName(String name) {
        if (name == null || name.trim().isEmpty()) return "media-download";
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void sharePage() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, webView.getUrl() == null ? HOME_URL : webView.getUrl());
        startActivity(Intent.createChooser(send, "Share AnyDownloader"));
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null || manager.getActiveNetwork() == null) return false;
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(manager.getActiveNetwork());
        return capabilities != null && (
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }

    private void updateConnectivity() {
        status.setText(isOnline() ? "Online · Ready" : "Offline · Internet required");
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (webView != null) webView.saveState(out);
    }

    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        if (webView != null) {
            webView.stopLoading();
            webView.destroy();
        }
        super.onDestroy();
    }
}