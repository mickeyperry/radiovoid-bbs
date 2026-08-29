package ibxm;

/* JADX INFO: loaded from: classes.dex */
public class Envelope {
    public boolean enabled = false;
    public boolean sustain = false;
    public boolean looped = false;
    public int sustainTick = 0;
    public int loopStartTick = 0;
    public int loopEndTick = 0;
    public int numPoints = 1;
    public int[] pointsTick = new int[1];
    public int[] pointsAmpl = new int[1];

    public int nextTick(int i, boolean z) {
        int i2;
        int i3 = i + 1;
        if (this.looped && i3 >= this.loopEndTick) {
            i3 = this.loopStartTick;
        }
        return (this.sustain && z && i3 >= (i2 = this.sustainTick)) ? i2 : i3;
    }

    public int calculateAmpl(int i) {
        int[] iArr = this.pointsAmpl;
        int i2 = this.numPoints;
        int i3 = iArr[i2 - 1];
        if (i >= this.pointsTick[i2 - 1]) {
            return i3;
        }
        int i4 = 0;
        for (int i5 = 1; i5 < this.numPoints; i5++) {
            if (this.pointsTick[i5] <= i) {
                i4 = i5;
            }
        }
        int[] iArr2 = this.pointsAmpl;
        int i6 = iArr2[i4];
        int[] iArr3 = this.pointsTick;
        int i7 = i4 + 1;
        int i8 = iArr3[i7];
        int i9 = iArr3[i4];
        int i10 = i8 - i9;
        return i10 > 0 ? i6 + (((((iArr2[i7] - i6) << 24) / i10) * (i - i9)) >> 24) : i6;
    }

    public void toStringBuffer(StringBuffer stringBuffer, String str) {
        if (this.sustain) {
            stringBuffer.append(str + "Sustain Tick: " + this.sustainTick + '\n');
        }
        if (this.looped) {
            stringBuffer.append(str + "Loop Start Tick: " + this.loopStartTick + '\n');
            stringBuffer.append(str + "Loop End Tick: " + this.loopEndTick + '\n');
        }
        stringBuffer.append(str + "Points:");
        for (int i = 0; i < this.numPoints; i++) {
            if (i % 3 == 0) {
                stringBuffer.append("\n" + str + str);
            }
            stringBuffer.append("(" + this.pointsTick[i] + ", " + this.pointsAmpl[i] + "), ");
        }
        stringBuffer.append('\n');
    }
}
