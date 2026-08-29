package net.radiovoid.bbs;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class Net {
    private static final String ADMIN_EMAIL = "sysop@radiovoid.bbs";
    private static final String BASE = "https://uhcxcnhsujsawgzfaeae.supabase.co";
    private static final String KEY = "sb_publishable_oRCO4ISFS51ebU3-rpyxew_U_ktpJSs";
    private static volatile String accessToken;
    private static SharedPreferences authPrefs;
    private static volatile String refreshToken;
    private static volatile long tokenExpiry;
    private static final ExecutorService IO = Executors.newCachedThreadPool();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    public static void runIo(Runnable runnable) {
        IO.execute(runnable);
    }

    public static void onMain(Runnable runnable) {
        MAIN.post(runnable);
    }

    public static void initAuth(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences("rv_auth", 0);
        authPrefs = sharedPreferences;
        accessToken = sharedPreferences.getString("at", null);
        refreshToken = authPrefs.getString("rt", null);
        tokenExpiry = authPrefs.getLong("exp", 0L);
    }

    public static boolean isAdmin() {
        return refreshToken != null;
    }

    private static void saveAuth() {
        SharedPreferences sharedPreferences = authPrefs;
        if (sharedPreferences == null) {
            return;
        }
        sharedPreferences.edit().putString("at", accessToken).putString("rt", refreshToken).putLong("exp", tokenExpiry).apply();
    }

    public static void login(final String str, final Consumer<Boolean> consumer) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$login$1(str, consumer);
            }
        });
    }

    static /* synthetic */ void lambda$login$1(String str, final Consumer consumer) {
        boolean ok = false;
        try {
            applyTokens(new JSONObject(post("https://uhcxcnhsujsawgzfaeae.supabase.co/auth/v1/token?grant_type=password", new JSONObject().put("email", ADMIN_EMAIL).put("password", str).toString(), null, true)));
            if (accessToken != null) {
                ok = true;
            }
        } catch (Exception unused) {
        }
        final boolean z = ok;
        MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                consumer.accept(Boolean.valueOf(z));
            }
        });
    }

    public static void logout(final Runnable runnable) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda12
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$logout$2(runnable);
            }
        });
    }

    static /* synthetic */ void lambda$logout$2(Runnable runnable) {
        try {
            if (accessToken != null) {
                post("https://uhcxcnhsujsawgzfaeae.supabase.co/auth/v1/logout", "{}", accessToken, false);
            }
        } catch (Exception unused) {
        }
        accessToken = null;
        refreshToken = null;
        tokenExpiry = 0L;
        saveAuth();
        MAIN.post(runnable);
    }

    private static void applyTokens(JSONObject jSONObject) {
        accessToken = jSONObject.optString("access_token", null);
        refreshToken = jSONObject.optString("refresh_token", null);
        tokenExpiry = (System.currentTimeMillis() + (jSONObject.optLong("expires_in", 3600L) * 1000)) - 60000;
        saveAuth();
    }

    private static String adminToken() throws Exception {
        if (refreshToken == null) {
            throw new RuntimeException("not sysop");
        }
        if (accessToken == null || System.currentTimeMillis() > tokenExpiry) {
            applyTokens(new JSONObject(post("https://uhcxcnhsujsawgzfaeae.supabase.co/auth/v1/token?grant_type=refresh_token", new JSONObject().put("refresh_token", refreshToken).toString(), null, true)));
            if (accessToken == null) {
                throw new RuntimeException("refresh failed");
            }
        }
        return accessToken;
    }

    public static void fetchTracks(final Consumer<List<Track>> consumer, final Consumer<Exception> consumer2) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$fetchTracks$5(consumer, consumer2);
            }
        });
    }

    static /* synthetic */ void lambda$fetchTracks$5(final Consumer consumer, final Consumer consumer2) {
        try {
            JSONArray jSONArray = new JSONArray(get("https://uhcxcnhsujsawgzfaeae.supabase.co/rest/v1/tracks?select=*&order=created_at.desc"));
            final ArrayList arrayList = new ArrayList();
            for (int i = 0; i < jSONArray.length(); i++) {
                arrayList.add(new Track(jSONArray.getJSONObject(i)));
            }
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda2
                @Override // java.lang.Runnable
                public final void run() {
                    consumer.accept(arrayList);
                }
            });
        } catch (Exception e) {
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda3
                @Override // java.lang.Runnable
                public final void run() {
                    consumer2.accept(e);
                }
            });
        }
    }

    public static void deleteTrack(final Track track, final Runnable runnable, final Consumer<Exception> consumer) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$deleteTrack$7(track, runnable, consumer);
            }
        });
    }

    static /* synthetic */ void lambda$deleteTrack$7(Track track, Runnable runnable, final Consumer consumer) {
        try {
            String strAdminToken = adminToken();
            int iLastIndexOf = track.url.lastIndexOf(47);
            if (iLastIndexOf > 0) {
                try {
                    request("DELETE", "https://uhcxcnhsujsawgzfaeae.supabase.co/storage/v1/object/tracks/" + URLEncoder.encode(track.url.substring(iLastIndexOf + 1), "UTF-8").replace("+", "%20"), null, strAdminToken);
                } catch (Exception unused) {
                }
            }
            request("DELETE", "https://uhcxcnhsujsawgzfaeae.supabase.co/rest/v1/tracks?id=eq." + track.id, null, strAdminToken);
            MAIN.post(runnable);
        } catch (Exception e) {
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda5
                @Override // java.lang.Runnable
                public final void run() {
                    consumer.accept(e);
                }
            });
        }
    }

    public static void fetchMessages(final Consumer<JSONArray> consumer, final Consumer<Exception> consumer2) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda16
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$fetchMessages$10(consumer, consumer2);
            }
        });
    }

    static /* synthetic */ void lambda$fetchMessages$10(final Consumer consumer, final Consumer consumer2) {
        try {
            final JSONArray jSONArray = new JSONArray(get("https://uhcxcnhsujsawgzfaeae.supabase.co/rest/v1/messages?select=*&order=id.desc&limit=60"));
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda17
                @Override // java.lang.Runnable
                public final void run() {
                    consumer.accept(jSONArray);
                }
            });
        } catch (Exception e) {
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda18
                @Override // java.lang.Runnable
                public final void run() {
                    consumer2.accept(e);
                }
            });
        }
    }

    public static void sendMessage(final String str, final String str2, final Runnable runnable, final Consumer<Exception> consumer) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$sendMessage$12(str, str2, runnable, consumer);
            }
        });
    }

    static /* synthetic */ void lambda$sendMessage$12(String str, String str2, Runnable runnable, final Consumer consumer) {
        try {
            post("https://uhcxcnhsujsawgzfaeae.supabase.co/rest/v1/messages", new JSONObject().put("nick", str).put("body", str2).toString(), null, false);
            MAIN.post(runnable);
        } catch (Exception e) {
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda15
                @Override // java.lang.Runnable
                public final void run() {
                    consumer.accept(e);
                }
            });
        }
    }

    public static void deleteMessage(final long j, final Runnable runnable, final Consumer<Exception> consumer) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda14
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$deleteMessage$14(j, runnable, consumer);
            }
        });
    }

    static /* synthetic */ void lambda$deleteMessage$14(long j, Runnable runnable, final Consumer consumer) {
        try {
            request("DELETE", "https://uhcxcnhsujsawgzfaeae.supabase.co/rest/v1/messages?id=eq." + j, null, adminToken());
            MAIN.post(runnable);
        } catch (Exception e) {
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    consumer.accept(e);
                }
            });
        }
    }

    public static void clearChat(final Runnable runnable, final Consumer<Exception> consumer) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$clearChat$16(runnable, consumer);
            }
        });
    }

    static /* synthetic */ void lambda$clearChat$16(Runnable runnable, final Consumer consumer) {
        try {
            request("DELETE", "https://uhcxcnhsujsawgzfaeae.supabase.co/rest/v1/messages?id=gt.0", null, adminToken());
            MAIN.post(runnable);
        } catch (Exception e) {
            MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda8
                @Override // java.lang.Runnable
                public final void run() {
                    consumer.accept(e);
                }
            });
        }
    }

    public static File cacheFile(Context context, Track track) {
        return new File(context.getCacheDir(), "trk_" + track.id + "." + track.ext());
    }

    public static File cached(Context context, Track track) {
        File fileCacheFile = cacheFile(context, track);
        if (!fileCacheFile.exists() || fileCacheFile.length() <= 0) {
            return null;
        }
        return fileCacheFile;
    }

    public static byte[] downloadFileBlocking(Context context, Track track) throws Exception {
        File fileCacheFile = cacheFile(context, track);
        if (fileCacheFile.exists() && fileCacheFile.length() > 0) {
            return readAll(new FileInputStream(fileCacheFile));
        }
        HttpURLConnection httpURLConnectionOpen = open(track.url, null);
        int responseCode = httpURLConnectionOpen.getResponseCode();
        if (responseCode >= 400) {
            throw new RuntimeException("HTTP " + responseCode);
        }
        byte[] all = readAll(httpURLConnectionOpen.getInputStream());
        File file = new File(fileCacheFile.getPath() + ".part");
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            fileOutputStream.write(all);
            fileOutputStream.close();
            file.renameTo(fileCacheFile);
            return all;
        } catch (Throwable th) {
            try {
                fileOutputStream.close();
            } catch (Throwable th2) {
                th.addSuppressed(th2);
            }
            throw th;
        }
    }

    public static void shorten(final String str, final Consumer<String> consumer) {
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                Net.lambda$shorten$18(str, consumer);
            }
        });
    }

    static /* synthetic */ void lambda$shorten$18(final String str, final Consumer consumer) {
        String result = str;
        try {
            HttpURLConnection httpURLConnection = (HttpURLConnection) new URL("https://tinyurl.com/api-create.php?url=" + URLEncoder.encode(str, "UTF-8")).openConnection();
            httpURLConnection.setConnectTimeout(5000);
            httpURLConnection.setReadTimeout(5000);
            String strTrim = new String(readAll(httpURLConnection.getInputStream()), StandardCharsets.UTF_8).trim();
            if (strTrim.startsWith("http")) {
                result = strTrim;
            }
        } catch (Exception unused) {
        }
        final String out = result;
        MAIN.post(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                consumer.accept(out);
            }
        });
    }

    public static void prefetch(final Context context, final Track track) {
        if (track == null || cached(context, track) != null) {
            return;
        }
        IO.execute(new Runnable() { // from class: net.radiovoid.bbs.Net$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                try {
                    Net.downloadFileBlocking(context, track);
                } catch (Exception ignored) {
                }
            }
        });
    }

    private static HttpURLConnection open(String str, String str2) throws Exception {
        HttpURLConnection httpURLConnection = (HttpURLConnection) new URL(str).openConnection();
        httpURLConnection.setConnectTimeout(15000);
        httpURLConnection.setReadTimeout(60000);
        httpURLConnection.setRequestProperty("apikey", KEY);
        StringBuilder sb = new StringBuilder("Bearer ");
        if (str2 == null) {
            str2 = KEY;
        }
        sb.append(str2);
        httpURLConnection.setRequestProperty("Authorization", sb.toString());
        return httpURLConnection;
    }

    private static String get(String str) throws Exception {
        HttpURLConnection httpURLConnectionOpen = open(str, null);
        int responseCode = httpURLConnectionOpen.getResponseCode();
        if (responseCode >= 400) {
            throw new RuntimeException("HTTP " + responseCode);
        }
        return new String(readAll(httpURLConnectionOpen.getInputStream()), StandardCharsets.UTF_8);
    }

    private static String post(String str, String str2, String str3, boolean z) throws Exception {
        return request("POST", str, str2, str3);
    }

    private static String request(String str, String str2, String str3, String str4) throws Exception {
        HttpURLConnection httpURLConnectionOpen = open(str2, str4);
        httpURLConnectionOpen.setRequestMethod(str);
        httpURLConnectionOpen.setRequestProperty("Content-Type", "application/json");
        if (str.equals("POST")) {
            httpURLConnectionOpen.setRequestProperty("Prefer", "return=minimal");
        }
        if (str3 != null) {
            httpURLConnectionOpen.setDoOutput(true);
            OutputStream outputStream = httpURLConnectionOpen.getOutputStream();
            try {
                outputStream.write(str3.getBytes(StandardCharsets.UTF_8));
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (Throwable th) {
                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Throwable th2) {
                        th.addSuppressed(th2);
                    }
                }
                throw th;
            }
        }
        int responseCode = httpURLConnectionOpen.getResponseCode();
        InputStream errorStream = responseCode >= 400 ? httpURLConnectionOpen.getErrorStream() : httpURLConnectionOpen.getInputStream();
        String str5 = errorStream != null ? new String(readAll(errorStream), StandardCharsets.UTF_8) : "";
        if (responseCode < 400) {
            return str5;
        }
        throw new RuntimeException("HTTP " + responseCode + " " + str5);
    }

    private static byte[] readAll(InputStream inputStream) throws Exception {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] bArr = new byte[16384];
        while (true) {
            int i = inputStream.read(bArr);
            if (i <= 0) {
                inputStream.close();
                return byteArrayOutputStream.toByteArray();
            }
            byteArrayOutputStream.write(bArr, 0, i);
        }
    }
}
