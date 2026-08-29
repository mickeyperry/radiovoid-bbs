package ibxm;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

/* JADX INFO: loaded from: classes.dex */
public class Data {
    private int bufLen;
    private byte[] buffer;
    private InputStream stream;

    public Data(InputStream inputStream) throws IOException {
        this.bufLen = 65536;
        byte[] bArr = new byte[65536];
        this.buffer = bArr;
        this.stream = inputStream;
        readFully(inputStream, bArr, 0, 65536);
    }

    public Data(byte[] bArr) {
        this.bufLen = bArr.length;
        this.buffer = bArr;
    }

    public byte sByte(int i) throws IOException {
        load(i, 1);
        return this.buffer[i];
    }

    public int uByte(int i) throws IOException {
        load(i, 1);
        return this.buffer[i] & 255;
    }

    public int ubeShort(int i) throws IOException {
        load(i, 2);
        byte[] bArr = this.buffer;
        return (bArr[i + 1] & 255) | ((bArr[i] & 255) << 8);
    }

    public int uleShort(int i) throws IOException {
        load(i, 2);
        byte[] bArr = this.buffer;
        return ((bArr[i + 1] & 255) << 8) | (bArr[i] & 255);
    }

    public int uleInt(int i) throws IOException {
        load(i, 4);
        byte[] bArr = this.buffer;
        return ((bArr[i + 3] & 127) << 24) | (bArr[i] & 255) | ((bArr[i + 1] & 255) << 8) | ((bArr[i + 2] & 255) << 16);
    }

    public String strLatin1(int i, int i2) throws IOException {
        load(i, i2);
        char[] cArr = new char[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            int i4 = this.buffer[i + i3] & 255;
            char c = ' ';
            if (i4 >= 32) {
                c = (char) i4;
            }
            cArr[i3] = c;
        }
        return new String(cArr);
    }

    public String strCp850(int i, int i2) throws IOException {
        load(i, i2);
        try {
            char[] charArray = new String(this.buffer, i, i2, "Cp850").toCharArray();
            for (int i3 = 0; i3 < charArray.length; i3++) {
                char c = charArray[i3];
                if (c < ' ') {
                    c = ' ';
                }
                charArray[i3] = c;
            }
            return new String(charArray);
        } catch (UnsupportedEncodingException unused) {
            return strLatin1(i, i2);
        }
    }

    public short[] samS8(int i, int i2) throws IOException {
        load(i, i2);
        short[] sArr = new short[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            sArr[i3] = (short) (this.buffer[i + i3] << 8);
        }
        return sArr;
    }

    public short[] samS8D(int i, int i2) throws IOException {
        load(i, i2);
        short[] sArr = new short[i2];
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            i3 += this.buffer[i + i4];
            sArr[i4] = (short) (i3 << 8);
        }
        return sArr;
    }

    public short[] samU8(int i, int i2) throws IOException {
        load(i, i2);
        short[] sArr = new short[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            sArr[i3] = (short) (((this.buffer[i + i3] & 255) - 128) << 8);
        }
        return sArr;
    }

    public short[] samS16(int i, int i2) throws IOException {
        load(i, i2 * 2);
        short[] sArr = new short[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            byte[] bArr = this.buffer;
            int i4 = (i3 * 2) + i;
            sArr[i3] = (short) ((bArr[i4 + 1] << 8) | (bArr[i4] & 255));
        }
        return sArr;
    }

    public short[] samS16D(int i, int i2) throws IOException {
        load(i, i2 * 2);
        short[] sArr = new short[i2];
        int i3 = 0;
        for (int i4 = 0; i4 < i2; i4++) {
            byte[] bArr = this.buffer;
            int i5 = (i4 * 2) + i;
            i3 += (bArr[i5 + 1] << 8) | (bArr[i5] & 255);
            sArr[i4] = (short) i3;
        }
        return sArr;
    }

    public short[] samU16(int i, int i2) throws IOException {
        load(i, i2 * 2);
        short[] sArr = new short[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            byte[] bArr = this.buffer;
            int i4 = (i3 * 2) + i;
            sArr[i3] = (short) ((((bArr[i4 + 1] & 255) << 8) | (bArr[i4] & 255)) - 32768);
        }
        return sArr;
    }

    private void load(int i, int i2) throws IOException {
        while (true) {
            int i3 = i + i2;
            int i4 = this.bufLen;
            if (i3 <= i4) {
                return;
            }
            int i5 = i4 << 1;
            byte[] bArr = new byte[i5];
            System.arraycopy(this.buffer, 0, bArr, 0, i4);
            InputStream inputStream = this.stream;
            if (inputStream != null) {
                int i6 = this.bufLen;
                readFully(inputStream, bArr, i6, i5 - i6);
            }
            this.bufLen = i5;
            this.buffer = bArr;
        }
    }

    private static void readFully(InputStream inputStream, byte[] bArr, int i, int i2) throws IOException {
        int i3 = i2 + i;
        int i4 = 1;
        while (i4 > 0) {
            i4 = inputStream.read(bArr, i, i3 - i);
            i += i4;
        }
    }
}
