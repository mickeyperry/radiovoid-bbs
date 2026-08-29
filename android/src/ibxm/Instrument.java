package ibxm;

/* JADX INFO: loaded from: classes.dex */
public class Instrument {
    public String name = "";
    public int numSamples = 1;
    public int vibratoType = 0;
    public int vibratoSweep = 0;
    public int vibratoDepth = 0;
    public int vibratoRate = 0;
    public int volumeFadeOut = 0;
    public Envelope volumeEnvelope = new Envelope();
    public Envelope panningEnvelope = new Envelope();
    public int[] keyToSample = new int[97];
    public Sample[] samples = {new Sample()};

    public void toStringBuffer(StringBuffer stringBuffer) {
        int i;
        stringBuffer.append("Name: " + this.name + '\n');
        if (this.numSamples > 0) {
            if (this.vibratoDepth > 0) {
                stringBuffer.append("Vibrato Type: " + this.vibratoType + '\n');
                stringBuffer.append("Vibrato Sweep: " + this.vibratoSweep + '\n');
                stringBuffer.append("Vibrato Depth: " + this.vibratoDepth + '\n');
                stringBuffer.append("Vibrato Rate: " + this.vibratoRate + '\n');
            }
            if (this.volumeFadeOut > 0) {
                stringBuffer.append("Volume Fade Out: " + this.volumeFadeOut + '\n');
            }
            if (this.volumeEnvelope.enabled) {
                stringBuffer.append("Volume Envelope:\n");
                this.volumeEnvelope.toStringBuffer(stringBuffer, "   ");
            }
            if (this.panningEnvelope.enabled) {
                stringBuffer.append("Panning Envelope:\n");
                this.panningEnvelope.toStringBuffer(stringBuffer, "   ");
            }
            stringBuffer.append("Num Samples: " + this.numSamples + '\n');
            int i2 = 0;
            while (true) {
                i = this.numSamples;
                if (i2 >= i) {
                    break;
                }
                stringBuffer.append("Sample " + i2 + ":\n");
                this.samples[i2].toStringBuffer(stringBuffer, "   ");
                i2++;
            }
            if (i > 1) {
                stringBuffer.append("Key To Sample:\n");
                for (int i3 = 0; i3 < 8; i3++) {
                    stringBuffer.append("   Oct " + i3 + ": ");
                    for (int i4 = 0; i4 < 12; i4++) {
                        stringBuffer.append(this.keyToSample[(i3 * 12) + i4 + 1]);
                        if (i4 < 11) {
                            stringBuffer.append(",");
                        }
                    }
                    stringBuffer.append('\n');
                }
            }
        }
    }
}
