package com.nyxsky.anydownloader;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Gravity;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebResourceError;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
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
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildShell();
        configureWebView();
        registerNetworkCallback();
        if (state == null || webView.restoreState(state) == null) {
            webView.loadUrl(HOME_URL);
        }
        updateConnectivity();
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(15, 18, 28));

        LinearLayout bar = new LinearLayout(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(14), dp(9), dp(10), dp(9));
        bar.setBackgroundColor(Color.rgb(21, 29, 48));

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setContentDescription(getString(R.string.app_name) + " logo");
        bar.addView(logo, new LinearLayout.LayoutParams(dp(38), dp(38)));

        TextView brand = new TextView(this);
        brand.setText(R.string.app_name);
        brand.setTextColor(Color.WHITE);
        brand.setTextSize(18);
        brand.setTypeface(null, 1);
        brand.setPadding(dp(9), 0, dp(4), 0);
        bar.addView(brand, new LinearLayout.LayoutParams(0, -2, 1));

        Button refresh = actionButton("Refresh");
        refresh.setOnClickListener(v -> {
            updateConnectivity();
            if (isOnline()) webView.reload();
            else showToast("Internet connection required");
        });
        bar.addView(refresh);

        Button share = actionButton("Share");
        share.setOnClickListener(v -> sharePage());
        bar.addView(share);
        root.addView(bar);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(73, 174, 255)));
        progress.setVisibility(View.GONE);
        root.addView(progress, new LinearLayout.LayoutParams(-1, dp(3)));

        status = new TextView(this);
        status.setTextColor(Color.rgb(205, 216, 235));
        status.setTextSize(11);
        status.setPadding(dp(12), dp(7), dp(12), dp(7));
        status.setBackgroundColor(Color.rgb(30, 40, 61));
        status.setOnClickListener(v -> {
            if (isOnline()) webView.reload();
            else showToast("Turn on Wi-Fi or mobile data, then retry");
        });
        root.addView(status);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(15, 18, 28));
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
                updateConnectivity();
                if (isOnline()) status.setText("Online · AnyDownloader service page loaded");
            }

            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) showLoadError();
            }

            @Override public void onReceivedHttpError(WebView view, WebResourceRequest request,
                                                       android.webkit.WebResourceResponse response) {
                if (request.isForMainFrame() && response.getStatusCode() >= 400) {
                    status.setText("Service returned HTTP " + response.getStatusCode() + " · Tap here to retry");
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override public void onDownloadStart(String url, String userAgent, String contentDisposition,
                                                  String mimeType, long contentLength) {
                try {
                    Uri uri = Uri.parse(url);
                    if (!"https".equalsIgnoreCase(uri.getScheme())
                            || !TRUSTED_HOST.equalsIgnoreCase(uri.getHost())) {
                        showToast("Blocked an untrusted download link");
                        return;
                    }

                    String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
                    fileName = safeName(fileName);
                    DownloadManager.Request request = new DownloadManager.Request(uri);
                    request.setTitle(fileName);
                    request.setDescription("Downloading with AnyDownloader");
                    request.setMimeType(mimeType == null ? "application/octet-stream" : mimeType);
                    request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(false);

                    String cookies = CookieManager.getInstance().getCookie(uri.toString());
                    if (cookies != null && !cookies.isEmpty()) request.addRequestHeader("Cookie", cookies);
                    String effectiveAgent = (userAgent == null || userAgent.isEmpty())
                            ? webView.getSettings().getUserAgentString() : userAgent;
                    request.addRequestHeader("User-Agent", effectiveAgent);
                    request.addRequestHeader("Referer", HOME_URL);
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                            "AnyDownloader/" + fileName);

                    DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                    if (manager == null) throw new IllegalStateException("Android Download Manager is unavailable");
                    long downloadId = manager.enqueue(request);
                    if (downloadId > 0) showToast("Download added: " + fileName);
                    else showToast("Android could not queue this download");
                } catch (Exception exception) {
                    showToast("Download failed to start: " + safeMessage(exception));
                }
            }
        });
    }

    private boolean routeUrl(Uri uri) {
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme) && TRUSTED_HOST.equalsIgnoreCase(uri.getHost())) return false;
        if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)
                || "mailto".equalsIgnoreCase(scheme) || "tel".equalsIgnoreCase(scheme)) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
            } catch (Exception exception) {
                showToast("No compatible app is available for this link");
            }
        }
        return true;
    }

    private String safeName(String name) {
        if (name == null || name.trim().isEmpty()) return "media-download";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            boolean unsafe = ch == '/' || ch == '\\' || ch == ':' || ch == '*'
                    || ch == '?' || ch == '"' || ch == '<' || ch == '>' || ch == '|'
                    || Character.isISOControl(ch);
            result.append(unsafe ? '_' : ch);
        }
        String cleaned = result.toString().trim();
        while (cleaned.startsWith(".")) cleaned = cleaned.substring(1);
        if (cleaned.isEmpty()) return "media-download";
        return cleaned.length() > 120 ? cleaned.substring(0, 120) : cleaned;
    }

    private void sharePage() {
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_TEXT, webView.getUrl() == null ? HOME_URL : webView.getUrl());
        startActivity(Intent.createChooser(send, "Share AnyDownloader"));
    }

    private boolean isOnline() {
        ConnectivityManager manager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (manager == null) return false;
        Network active = manager.getActiveNetwork();
        if (active == null) return false;
        NetworkCapabilities caps = manager.getNetworkCapabilities(active);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;
        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override public void onAvailable(Network network) { runOnUiThread(() -> updateConnectivity()); }
            @Override public void onLost(Network network) { runOnUiThread(() -> updateConnectivity()); }
            @Override public void onCapabilitiesChanged(Network network, NetworkCapabilities caps) {
                runOnUiThread(() -> updateConnectivity());
            }
        };
        try {
            connectivityManager.registerNetworkCallback(
                    new NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                    networkCallback);
        } catch (RuntimeException exception) {
            networkCallback = null;
        }
    }

    private void updateConnectivity() {
        if (status == null) return;
        if (isOnline()) status.setText("Online · Service requires an internet connection");
        else status.setText("Offline or no verified internet · Tap here to retry");
    }

    private void showLoadError() {
        if (status != null) status.setText("Could not load the service · Check internet and tap here to retry");
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.trim().isEmpty() ? "Please try again" : message;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        updateConnectivity();
    }

    @Override protected void onPause() {
        if (webView != null) webView.onPause();
        super.onPause();
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
        if (connectivityManager != null && networkCallback != null) {
            try { connectivityManager.unregisterNetworkCallback(networkCallback); }
            catch (IllegalArgumentException ignored) { }
        }
        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
