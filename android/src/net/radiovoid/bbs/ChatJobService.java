package net.radiovoid.bbs;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.net.Uri;
import java.util.function.Consumer;
import org.json.JSONArray;
import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public class ChatJobService extends JobService {
    private static final String CH_CHAT = "rv_chat_v2";
    private static final String CH_CHAT_SILENT = "rv_chat_silent";
    public static final int JOB_ID = 42;
    private static final int NOTIF_ID = 2;

    @Override // android.app.job.JobService
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    /* JADX INFO: renamed from: lambda$onStartJob$0$net-radiovoid-bbs-ChatJobService, reason: not valid java name */
    /* synthetic */ void m1lambda$onStartJob$0$netradiovoidbbsChatJobService(JobParameters jobParameters) {
        jobFinished(jobParameters, false);
    }

    @Override // android.app.job.JobService
    public boolean onStartJob(final JobParameters jobParameters) {
        checkAndNotify(this, new Runnable() { // from class: net.radiovoid.bbs.ChatJobService$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                ChatJobService.this.m1lambda$onStartJob$0$netradiovoidbbsChatJobService(jobParameters);
            }
        });
        return true;
    }

    public static void ensureChannels(Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NotificationManager.class);
        notificationManager.deleteNotificationChannel("rv_chat");
        NotificationChannel notificationChannel = new NotificationChannel(CH_CHAT, "Chat messages", 4);
        notificationChannel.setDescription("New messages in the RadioVoid shoutbox");
        notificationChannel.setSound(Uri.parse("android.resource://" + context.getPackageName() + "/raw/chat_beep"), new AudioAttributes.Builder().setUsage(5).setContentType(4).build());
        notificationChannel.enableVibration(true);
        notificationChannel.setVibrationPattern(new long[]{0, 60, 50, 60});
        notificationManager.createNotificationChannel(notificationChannel);
        notificationManager.createNotificationChannel(new NotificationChannel(CH_CHAT_SILENT, "Chat messages (muted)", 2));
    }

    public static void checkAndNotify(final Context context, final Runnable runnable) {
        final SharedPreferences sharedPreferences = context.getSharedPreferences("rv", 0);
        final String string = sharedPreferences.getString("nick", "anon");
        final long jMax = Math.max(sharedPreferences.getLong("chat_seen", 0L), sharedPreferences.getLong("chat_notified", 0L));
        Net.fetchMessages(new Consumer() { // from class: net.radiovoid.bbs.ChatJobService$$ExternalSyntheticLambda1
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ChatJobService.lambda$checkAndNotify$1(jMax, string, sharedPreferences, context, runnable, (JSONArray) obj);
            }
        }, new Consumer() { // from class: net.radiovoid.bbs.ChatJobService$$ExternalSyntheticLambda2
            @Override // java.util.function.Consumer
            public final void accept(Object obj) {
                ChatJobService.lambda$checkAndNotify$2(runnable, (Exception) obj);
            }
        });
    }

    static /* synthetic */ void lambda$checkAndNotify$1(long j, String str, SharedPreferences sharedPreferences, Context context, Runnable runnable, JSONArray jSONArray) {
        String strOptString = null;
        long j2 = 0;
        int i = 0;
        String strOptString2 = null;
        String replyNick = null;
        String replyBody = null;
        boolean replyIsMention = false;
        try {
            // id -> message map so ">>id" reply refs can be resolved to their target
            java.util.HashMap<Long, JSONObject> byId = new java.util.HashMap<>();
            for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                JSONObject m = jSONArray.optJSONObject(i2);
                if (m != null) {
                    byId.put(m.optLong("id"), m);
                }
            }
            String meLower = str.toLowerCase(java.util.Locale.US);
            for (int i2 = 0; i2 < jSONArray.length(); i2++) {
                JSONObject jSONObjectOptJSONObject = jSONArray.optJSONObject(i2);
                if (jSONObjectOptJSONObject != null) {
                    long jOptLong = jSONObjectOptJSONObject.optLong("id");
                    if (jOptLong > j2) {
                        j2 = jOptLong;
                    }
                    if (jOptLong > j) {
                        if (!jSONObjectOptJSONObject.optString("nick").equals(str)) {
                            i++;
                            String body = jSONObjectOptJSONObject.optString("body", "");
                            if (strOptString2 == null) {
                                strOptString = jSONObjectOptJSONObject.optString("nick", "?");
                                strOptString2 = body;
                            }
                            // is it a reply to me, or an @mention of me?
                            if (replyNick == null && !str.isEmpty()) {
                                boolean isMention = body.toLowerCase(java.util.Locale.US).contains("@" + meLower);
                                boolean isReply = false;
                                long refId = MainActivity.chatRefId(body);
                                if (refId > 0) {
                                    JSONObject ref = byId.get(refId);
                                    isReply = ref != null && ref.optString("nick").equals(str);
                                }
                                if (isReply || isMention) {
                                    replyNick = jSONObjectOptJSONObject.optString("nick", "?");
                                    replyBody = MainActivity.chatStripRef(body);
                                    replyIsMention = !isReply;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            if (runnable != null) {
                runnable.run();
            }
            throw th;
        }
        if (i > 0) {
            sharedPreferences.edit().putLong("chat_notified", j2).apply();
            if (replyNick != null) {
                postDirect(context, sharedPreferences, replyNick, replyBody, replyIsMention);
            } else {
                post(context, sharedPreferences, i, strOptString, strOptString2);
            }
        }
        if (runnable != null) {
            runnable.run();
        }
    }

    private static void postDirect(Context context, SharedPreferences sharedPreferences, String nick, String body, boolean isMention) {
        ensureChannels(context);
        boolean z = sharedPreferences.getBoolean("chat_mute", false);
        PendingIntent activity = PendingIntent.getActivity(context, 7, new Intent(context, (Class<?>) MainActivity.class).putExtra("open_chat", true), 201326592);
        String title = "<" + nick + "> " + (isMention ? "mentioned you" : "replied to you");
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notify(2, new Notification.Builder(context, z ? CH_CHAT_SILENT : CH_CHAT).setSmallIcon(R.drawable.ic_stat).setContentTitle(title).setContentText(body).setStyle(new Notification.BigTextStyle().bigText(body)).setContentIntent(activity).setAutoCancel(true).build());
    }

    static /* synthetic */ void lambda$checkAndNotify$2(Runnable runnable, Exception exc) {
        if (runnable != null) {
            runnable.run();
        }
    }

    private static void post(Context context, SharedPreferences sharedPreferences, int i, String str, String str2) {
        String str3;
        ensureChannels(context);
        boolean z = sharedPreferences.getBoolean("chat_mute", false);
        PendingIntent activity = PendingIntent.getActivity(context, 7, new Intent(context, (Class<?>) MainActivity.class).putExtra("open_chat", true), 201326592);
        Notification.Builder smallIcon = new Notification.Builder(context, z ? CH_CHAT_SILENT : CH_CHAT).setSmallIcon(R.drawable.ic_stat);
        if (i == 1) {
            str3 = "<" + str + "> on RadioVoid";
        } else {
            str3 = i + " new messages on RadioVoid";
        }
        ((NotificationManager) context.getSystemService(NotificationManager.class)).notify(2, smallIcon.setContentTitle(str3).setContentText(str2).setStyle(new Notification.BigTextStyle().bigText(str2)).setContentIntent(activity).setAutoCancel(true).build());
    }
}
