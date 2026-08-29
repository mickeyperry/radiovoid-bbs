package net.radiovoid.bbs;

import org.json.JSONObject;

/* JADX INFO: loaded from: classes.dex */
public final class Track {
    public final String artist;
    public final int bpm;
    public final String createdAt;
    public final double duration;
    public final String filename;
    public final String id;
    public final String title;
    public final String url;
    public final int year;

    public boolean isPlayable() {
        return true;
    }

    public Track(JSONObject jSONObject) {
        this.id = jSONObject.optString("id", "");
        this.title = jSONObject.optString("title", "Untitled");
        this.artist = jSONObject.optString("artist", "Unknown");
        this.url = jSONObject.optString("url", "");
        this.filename = jSONObject.optString("filename", "");
        this.duration = jSONObject.optDouble("duration", 0.0d);
        this.year = jSONObject.optInt("year", 0);
        this.bpm = jSONObject.optInt("bpm", 0);
        this.createdAt = jSONObject.optString("created_at", "");
    }

    public String ext() {
        String str = this.url.isEmpty() ? this.filename : this.url;
        int iLastIndexOf = str.lastIndexOf(46);
        return iLastIndexOf < 0 ? "" : str.substring(iLastIndexOf + 1).toLowerCase();
    }

    public boolean isModule() {
        String strExt = ext();
        return strExt.equals("mod") || strExt.equals("s3m") || strExt.equals("xm") || strExt.equals("it");
    }
}
