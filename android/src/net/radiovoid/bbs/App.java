package net.radiovoid.bbs;

import android.app.Application;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

/* JADX INFO: loaded from: classes.dex */
public class App extends Application {
    @Override // android.app.Application
    public void onCreate() {
        super.onCreate();
        try {
            File[] fileArrListFiles = getCacheDir().listFiles();
            if (fileArrListFiles != null) {
                for (File file : fileArrListFiles) {
                    if (file.getName().startsWith("trk_0.") || file.getName().endsWith(".part")) {
                        file.delete();
                    }
                }
            }
        } catch (Exception unused) {
        }
        final Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() { // from class: net.radiovoid.bbs.App$$ExternalSyntheticLambda0
            @Override // java.lang.Thread.UncaughtExceptionHandler
            public final void uncaughtException(Thread thread, Throwable th) {
                App.this.m0lambda$onCreate$0$netradiovoidbbsApp(defaultUncaughtExceptionHandler, thread, th);
            }
        });
    }

    /* JADX INFO: renamed from: lambda$onCreate$0$net-radiovoid-bbs-App, reason: not valid java name */
    /* synthetic */ void m0lambda$onCreate$0$netradiovoidbbsApp(Thread.UncaughtExceptionHandler uncaughtExceptionHandler, Thread thread, Throwable th) {
        try {
            StringWriter stringWriter = new StringWriter();
            th.printStackTrace(new PrintWriter(stringWriter));
            getSharedPreferences("rv", 0).edit().putString("last_crash", new Date() + "  thread:" + thread.getName() + "\n\n" + stringWriter).commit();
        } catch (Throwable unused) {
        }
        if (uncaughtExceptionHandler != null) {
            uncaughtExceptionHandler.uncaughtException(thread, th);
        }
    }
}
