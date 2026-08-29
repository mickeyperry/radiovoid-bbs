package ibxm;

/* JADX INFO: loaded from: classes.dex */
public class Sample {
    public static final int C2_NTSC = 8363;
    public static final int C2_PAL = 8287;
    private static final int DELAY = 8;
    private static final int FILTER_TAPS = 16;
    public static final int FP_MASK = 32767;
    public static final int FP_ONE = 32768;
    public static final int FP_SHIFT = 15;
    private static final int LOG2_FILTER_TAPS = 4;
    private static final int LOG2_NUM_TABLES = 3;
    private static final int LOG2_TABLE_ACCURACY = 4;
    private static final int NUM_TABLES = 8;
    private static final short[][] SINC_TABLES = calculateSincTables();
    private static final int TABLE_ACCURACY = 16;
    private static final int TABLE_INTERP_MASK = 2047;
    private static final int TABLE_INTERP_ONE = 2048;
    private static final int TABLE_INTERP_SHIFT = 11;
    private short[] sampleData;
    public String name = "";
    public int volume = 0;
    public int panning = -1;
    public int relNote = 0;
    public int fineTune = 0;
    private int loopStart = 0;
    private int loopLength = 0;

    private static short[][] calculateSincTables() {
        short[][] sArr = new short[8][];
        int i = 0;
        while (i < 8) {
            int i2 = i + 1;
            sArr[i] = calculateSincTable(1.0d / ((double) i2));
            i = i2;
        }
        return sArr;
    }

    private static short[] calculateSincTable(double d) {
        short[] sArr = new short[272];
        int i = 0;
        for (int i2 = 0; i2 <= 16; i2++) {
            double d2 = ((((double) i2) / 16.0d) + 7.0d) * 3.141592653589793d;
            double d3 = ((d2 * 2.0d) / 16.0d) + 3.141592653589793d;
            int i3 = 0;
            while (i3 < 16) {
                sArr[i] = (short) Math.round((d2 != 0.0d ? Math.sin(d * d2) / d2 : d) * (((0.35875d - (Math.cos(d3) * 0.48829d)) + (Math.cos(d3 * 2.0d) * 0.14128d)) - (Math.cos(3.0d * d3) * 0.01168d)) * 32767.0d);
                d2 -= 3.141592653589793d;
                d3 -= 0.39269908169872414d;
                i3++;
                i++;
            }
        }
        return sArr;
    }

    public void setSampleData(short[] sArr, int i, int i2, boolean z) {
        int length = sArr.length;
        if (i < 0 || i > length) {
            i = length;
        }
        if (i2 < 0 || i + i2 > length) {
            i2 = length - i;
        }
        int i3 = i + i2;
        int i4 = i + 8;
        short[] sArr2 = new short[i3 + 8 + (z ? i2 : 0) + 16];
        System.arraycopy(sArr, 0, sArr2, 8, i3);
        if (z) {
            int i5 = i4 + i2;
            for (int i6 = 0; i6 < i2; i6++) {
                sArr2[i5 + i6] = sArr2[(i5 - i6) - 1];
            }
            i2 *= 2;
        }
        int i7 = i4 + i2;
        int i8 = i7 + 16;
        while (i7 < i8) {
            sArr2[i7] = sArr2[i7 - i2];
            i7++;
        }
        this.sampleData = sArr2;
        this.loopStart = i4;
        this.loopLength = i2;
    }

    public void resampleNearest(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, int i7) {
        int i8 = this.loopLength;
        int i9 = this.loopStart + i8;
        int iNormaliseSampleIdx = i + 8;
        if (iNormaliseSampleIdx >= i9) {
            iNormaliseSampleIdx = normaliseSampleIdx(iNormaliseSampleIdx);
        }
        short[] sArr = this.sampleData;
        int i10 = i6 << 1;
        int i11 = (i6 + i7) << 1;
        while (i10 < i11) {
            if (iNormaliseSampleIdx >= i9) {
                if (i8 < 2) {
                    return;
                }
                while (iNormaliseSampleIdx >= i9) {
                    iNormaliseSampleIdx -= i8;
                }
            }
            short s = sArr[iNormaliseSampleIdx];
            int i12 = i10 + 1;
            iArr[i10] = iArr[i10] + ((s * i4) >> 15);
            i10 = i12 + 1;
            iArr[i12] = iArr[i12] + ((s * i5) >> 15);
            int i13 = i2 + i3;
            iNormaliseSampleIdx += i13 >> 15;
            i2 = i13 & FP_MASK;
        }
    }

    public void resampleLinear(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, int i7) {
        int i8 = this.loopLength;
        int i9 = this.loopStart + i8;
        int iNormaliseSampleIdx = i + 8;
        if (iNormaliseSampleIdx >= i9) {
            iNormaliseSampleIdx = normaliseSampleIdx(iNormaliseSampleIdx);
        }
        short[] sArr = this.sampleData;
        int i10 = i6 << 1;
        int i11 = (i6 + i7) << 1;
        while (i10 < i11) {
            if (iNormaliseSampleIdx >= i9) {
                if (i8 < 2) {
                    return;
                }
                while (iNormaliseSampleIdx >= i9) {
                    iNormaliseSampleIdx -= i8;
                }
            }
            short s = sArr[iNormaliseSampleIdx];
            int i12 = (((sArr[iNormaliseSampleIdx + 1] - s) * i2) >> 15) + s;
            int i13 = i10 + 1;
            iArr[i10] = iArr[i10] + ((i12 * i4) >> 15);
            i10 = i13 + 1;
            iArr[i13] = iArr[i13] + ((i12 * i5) >> 15);
            int i14 = i2 + i3;
            iNormaliseSampleIdx += i14 >> 15;
            i2 = i14 & FP_MASK;
        }
    }

    public void resampleSinc(int i, int i2, int i3, int i4, int i5, int[] iArr, int i6, int i7) {
        int i8;
        if (i3 > 32768) {
            i8 = (i3 >> 15) - 1;
            if (i8 >= 8) {
                i8 = 7;
            }
        } else {
            i8 = 0;
        }
        short[] sArr = SINC_TABLES[i8];
        int i9 = this.loopLength;
        int i10 = this.loopStart + i9;
        int iNormaliseSampleIdx = i;
        if (iNormaliseSampleIdx >= i10) {
            iNormaliseSampleIdx = normaliseSampleIdx(i);
        }
        short[] sArr2 = this.sampleData;
        int i11 = (i6 + i7) << 1;
        int i12 = i6 << 1;
        int i13 = iNormaliseSampleIdx;
        int i14 = i2;
        while (i12 < i11) {
            if (i13 >= i10) {
                if (i9 < 2) {
                    return;
                }
                while (i13 >= i10) {
                    i13 -= i9;
                }
            }
            int i15 = (i14 >> TABLE_INTERP_SHIFT) << 4;
            int i16 = i15 + 16;
            int i17 = 0;
            int i18 = 0;
            for (int i19 = 0; i19 < 16; i19++) {
                short s = sArr[i15 + i19];
                short s2 = sArr2[i13 + i19];
                i17 += s * s2;
                i18 += sArr[i16 + i19] * s2;
            }
            int i20 = i17 >> 15;
            int i21 = i20 + ((((i18 >> 15) - i20) * (i14 & TABLE_INTERP_MASK)) >> TABLE_INTERP_SHIFT);
            int i22 = i12 + 1;
            iArr[i12] = iArr[i12] + ((i21 * i4) >> 15);
            i12 = i22 + 1;
            iArr[i22] = iArr[i22] + ((i21 * i5) >> 15);
            int i23 = i14 + i3;
            i13 += i23 >> 15;
            i14 = i23 & FP_MASK;
        }
    }

    public int normaliseSampleIdx(int i) {
        int i2 = this.loopStart;
        int i3 = i - i2;
        if (i3 <= 0) {
            return i;
        }
        int i4 = this.loopLength;
        return i4 > 1 ? i2 + (i3 % i4) : i2;
    }

    public boolean looped() {
        return this.loopLength > 1;
    }

    public int getLoopStart() {
        return this.loopStart;
    }

    public int getLoopLength() {
        return this.loopLength;
    }

    public void toStringBuffer(StringBuffer stringBuffer, String str) {
        stringBuffer.append(str + "Name: " + this.name + '\n');
        stringBuffer.append(str + "Volume: " + this.volume + '\n');
        if (this.panning >= 0) {
            stringBuffer.append(str + "Panning: " + this.panning + '\n');
        }
        stringBuffer.append(str + "Relative Note: " + this.relNote + '\n');
        stringBuffer.append(str + "Fine Tune: " + this.fineTune + '\n');
        stringBuffer.append(str + "Loop Start: " + this.loopStart + '\n');
        stringBuffer.append(str + "Loop Length: " + this.loopLength + '\n');
    }
}
