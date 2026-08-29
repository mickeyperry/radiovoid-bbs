package ibxm;

/* JADX INFO: loaded from: classes.dex */
public class Note {
    private static final String b36ToString = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String keyToString = "A-A#B-C-C#D-D#E-F-F#G-G#";
    public int effect;
    public int instrument;
    public int key;
    public int param;
    public int volume;

    public String toString() {
        return new String(toChars(new char[10]));
    }

    public char[] toChars(char[] cArr) {
        keyToChars(this.key, cArr);
        int i = this.instrument;
        cArr[3] = (i <= 15 || i >= 255) ? '-' : b36ToString.charAt((i >> 4) & 15);
        int i2 = this.instrument;
        cArr[4] = (i2 <= 0 || i2 >= 255) ? '-' : b36ToString.charAt(i2 & 15);
        int i3 = this.volume;
        cArr[5] = (i3 <= 15 || i3 >= 255) ? '-' : b36ToString.charAt((i3 >> 4) & 15);
        int i4 = this.volume;
        cArr[6] = (i4 <= 0 || i4 >= 255) ? '-' : b36ToString.charAt(i4 & 15);
        int i5 = this.effect;
        if ((i5 > 0 || this.param > 0) && i5 < 36) {
            cArr[7] = b36ToString.charAt(i5);
        } else if (i5 > 128 && i5 < 159) {
            cArr[7] = (char) ((i5 & 31) + 96);
        } else {
            cArr[7] = '-';
        }
        cArr[8] = (this.effect > 0 || this.param > 0) ? b36ToString.charAt((this.param >> 4) & 15) : '-';
        cArr[9] = (this.effect > 0 || this.param > 0) ? b36ToString.charAt(this.param & 15) : '-';
        return cArr;
    }

    private static void keyToChars(int i, char[] cArr) {
        char c = '-';
        cArr[0] = (i <= 0 || i >= 118) ? '-' : keyToString.charAt(((i + 2) % 12) * 2);
        cArr[1] = (i <= 0 || i >= 118) ? '-' : keyToString.charAt((((i + 2) % 12) * 2) + 1);
        if (i > 0 && i < 118) {
            c = (char) (((i + 2) / 12) + 48);
        }
        cArr[2] = c;
    }
}
