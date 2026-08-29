package net.radiovoid.bbs;

import android.graphics.Color;

/* JADX INFO: loaded from: classes.dex */
public final class Theme {
    public static final Theme[] ALL = {new Theme("AMBER", "#ffb000", "#805800", "#ffd866", "#0a0800", "#1a1200", "#ffb000", "#ff6600", "#ffb000", "#ff6600", "#ff3300"), new Theme("GREEN", "#33ff00", "#1a8000", "#88ff66", "#000a00", "#001a00", "#33ff00", "#00ffaa", "#33ff00", "#00ffaa", "#00ff55"), new Theme("ICE", "#00aaff", "#005580", "#66ccff", "#000810", "#001020", "#00aaff", "#ff00ff", "#00aaff", "#ff00ff", "#00ffff"), new Theme("RED", "#ff3333", "#801a1a", "#ff8888", "#0a0000", "#1a0505", "#ff3333", "#ff0066", "#ff3333", "#ff0066", "#ff6600"), new Theme("PURPLE", "#cc66ff", "#663380", "#dd99ff", "#08001a", "#120028", "#cc66ff", "#ff66aa", "#cc66ff", "#ff66aa", "#6666ff"), new Theme("MONO", "#cccccc", "#666666", "#ffffff", "#000000", "#111111", "#888888", "#ffffff", "#cccccc", "#ffffff", "#888888"), new Theme("MATRIX", "#00ff41", "#008f11", "#00ff41", "#000000", "#0d0208", "#003b00", "#00ff41", "#00ff41", "#008f11", "#003b00")};
    public final int accent;
    public final int bg;
    public final int bg2;
    public final int border;
    public final int fg;
    public final int fgBright;
    public final int fgDim;
    public final String name;
    public final int viz1;
    public final int viz2;
    public final int viz3;

    private Theme(String str, String str2, String str3, String str4, String str5, String str6, String str7, String str8, String str9, String str10, String str11) {
        this.name = str;
        this.fg = Color.parseColor(str2);
        this.fgDim = Color.parseColor(str3);
        this.fgBright = Color.parseColor(str4);
        this.bg = Color.parseColor(str5);
        this.bg2 = Color.parseColor(str6);
        this.border = Color.parseColor(str7);
        this.accent = Color.parseColor(str8);
        this.viz1 = Color.parseColor(str9);
        this.viz2 = Color.parseColor(str10);
        this.viz3 = Color.parseColor(str11);
    }
}
