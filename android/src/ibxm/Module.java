package ibxm;

import java.io.IOException;
import java.io.InputStream;

/* JADX INFO: loaded from: classes.dex */
public class Module {
    public int c2Rate;
    public int defaultGVol;
    public int[] defaultPanning;
    public int defaultSpeed;
    public int defaultTempo;
    public boolean fastVolSlides;
    public int gain;
    public Instrument[] instruments;
    public boolean linearPeriods;
    public int numChannels;
    public int numInstruments;
    public int numPatterns;
    public Pattern[] patterns;
    public int restartPos;
    public int[] sequence;
    public int sequenceLength;
    public String songName;

    public Module() {
        this.songName = "Blank";
        this.numChannels = 4;
        this.numInstruments = 1;
        this.numPatterns = 1;
        this.sequenceLength = 1;
        this.restartPos = 0;
        this.defaultGVol = 64;
        this.defaultSpeed = 6;
        this.defaultTempo = 125;
        this.c2Rate = Sample.C2_PAL;
        this.gain = 64;
        this.linearPeriods = false;
        this.fastVolSlides = false;
        this.defaultPanning = new int[]{51, 204, 204, 51};
        this.sequence = new int[]{0};
        this.patterns = new Pattern[]{new Pattern(4, 64)};
        this.instruments = new Instrument[]{new Instrument(), new Instrument()};
    }

    public Module(Data data) throws IOException {
        this.songName = "Blank";
        this.numChannels = 4;
        this.numInstruments = 1;
        this.numPatterns = 1;
        this.sequenceLength = 1;
        this.restartPos = 0;
        this.defaultGVol = 64;
        this.defaultSpeed = 6;
        this.defaultTempo = 125;
        this.c2Rate = Sample.C2_PAL;
        this.gain = 64;
        this.linearPeriods = false;
        this.fastVolSlides = false;
        this.defaultPanning = new int[]{51, 204, 204, 51};
        this.sequence = new int[]{0};
        this.patterns = new Pattern[]{new Pattern(4, 64)};
        this.instruments = new Instrument[]{new Instrument(), new Instrument()};
        if (data.strLatin1(0, 17).equals("Extended Module: ")) {
            loadXM(data);
        } else if (data.strLatin1(44, 4).equals("SCRM")) {
            loadS3M(data);
        } else {
            loadMod(data);
        }
        if (this.numChannels < 1 || this.numPatterns < 1 || this.sequenceLength < 1) {
            throw new IllegalArgumentException("Invalid module file!");
        }
    }

    public Module(InputStream inputStream) throws IOException {
        this(new Data(inputStream));
    }

    public Module(byte[] bArr) throws IOException {
        this(new Data(bArr));
    }

    private void loadMod(Data data) throws IOException {
        int i;
        this.songName = data.strLatin1(0, 20);
        this.sequenceLength = data.uByte(950) & 127;
        int iUByte = data.uByte(951) & 127;
        this.restartPos = iUByte;
        if (iUByte >= this.sequenceLength) {
            this.restartPos = 0;
        }
        this.sequence = new int[128];
        for (int i2 = 0; i2 < 128; i2++) {
            int iUByte2 = data.uByte(i2 + 952) & 127;
            this.sequence[i2] = iUByte2;
            if (iUByte2 >= this.numPatterns) {
                this.numPatterns = iUByte2 + 1;
            }
        }
        int iUbeShort = data.ubeShort(1082);
        int i3 = 64;
        if (iUbeShort == 17224) {
            int iUByte3 = (data.uByte(1080) - 48) * 10;
            this.numChannels = iUByte3;
            this.numChannels = iUByte3 + (data.uByte(1081) - 48);
            this.c2Rate = Sample.C2_NTSC;
            this.gain = 32;
        } else if (iUbeShort == 18510) {
            this.numChannels = data.uByte(1080) - 48;
            this.c2Rate = Sample.C2_NTSC;
            this.gain = 32;
        } else if (iUbeShort == 19233 || iUbeShort == 19246 || iUbeShort == 21556) {
            this.numChannels = 4;
            this.c2Rate = Sample.C2_PAL;
            this.gain = 64;
        } else {
            throw new IllegalArgumentException("MOD Format not recognised!");
        }
        this.defaultGVol = 64;
        this.defaultSpeed = 6;
        this.defaultTempo = 125;
        this.defaultPanning = new int[this.numChannels];
        int i4 = 0;
        while (true) {
            if (i4 >= this.numChannels) {
                break;
            }
            int[] iArr = this.defaultPanning;
            iArr[i4] = 51;
            int i5 = i4 & 3;
            if (i5 == 1 || i5 == 2) {
                iArr[i4] = 204;
            }
            i4++;
        }
        this.patterns = new Pattern[this.numPatterns];
        int i6 = 1084;
        int i7 = 0;
        while (i7 < this.numPatterns) {
            Pattern[] patternArr = this.patterns;
            Pattern pattern = new Pattern(this.numChannels, i3);
            patternArr[i7] = pattern;
            for (int i8 = 0; i8 < pattern.data.length; i8 += 5) {
                int iUByte4 = (data.uByte(i6 + 1) | ((data.uByte(i6) & 15) << 8)) * 4;
                if (iUByte4 >= 112 && iUByte4 <= 6848) {
                    int iLog2 = Channel.log2((iUByte4 << 15) / 29021) * (-12);
                    pattern.data[i8] = (byte) ((iLog2 + (iLog2 & 16384)) >> 15);
                }
                int i9 = i6 + 2;
                pattern.data[i8 + 1] = (byte) (((data.uByte(i9) & 240) >> 4) | (data.uByte(i6) & 16));
                int iUByte5 = data.uByte(i9) & 15;
                int iUByte6 = data.uByte(i6 + 3);
                if (iUByte6 == 0 && (iUByte5 < 3 || iUByte5 == 10)) {
                    iUByte5 = 0;
                }
                if (iUByte6 == 0 && (iUByte5 == 5 || iUByte5 == 6)) {
                    iUByte5 -= 2;
                }
                if (iUByte5 == 8) {
                    if (this.numChannels == 4) {
                        iUByte5 = 0;
                        iUByte6 = 0;
                    } else {
                        iUByte6 = iUByte6 > 128 ? 128 : (iUByte6 * 255) >> 7;
                    }
                }
                pattern.data[i8 + 3] = (byte) iUByte5;
                pattern.data[i8 + 4] = (byte) iUByte6;
                i6 += 4;
            }
            i7++;
            i3 = 64;
        }
        this.numInstruments = 31;
        Instrument[] instrumentArr = new Instrument[31 + 1];
        this.instruments = instrumentArr;
        instrumentArr[0] = new Instrument();
        for (i = 1; i <= this.numInstruments; i++) {
            Instrument[] instrumentArr2 = this.instruments;
            Instrument instrument = new Instrument();
            instrumentArr2[i] = instrument;
            Sample sample = instrument.samples[0];
            int i10 = i * 30;
            instrument.name = data.strLatin1(i10 - 10, 22);
            int iUbeShort2 = data.ubeShort(i10 + 12) * 2;
            int iUByte7 = (data.uByte(i10 + 14) & 15) << 4;
            if (iUByte7 >= 128) {
                iUByte7 -= 256;
            }
            sample.fineTune = iUByte7;
            int iUByte8 = data.uByte(i10 + 15) & 127;
            if (iUByte8 > 64) {
                iUByte8 = 64;
            }
            sample.volume = iUByte8;
            sample.panning = -1;
            int iUbeShort3 = data.ubeShort(i10 + 16) * 2;
            int iUbeShort4 = data.ubeShort(i10 + 18) * 2;
            if (iUbeShort3 + iUbeShort4 > iUbeShort2) {
                int i11 = iUbeShort3 / 2;
                if (i11 + iUbeShort4 <= iUbeShort2) {
                    iUbeShort3 = i11;
                } else {
                    iUbeShort4 = iUbeShort2 - iUbeShort3;
                }
            }
            if (iUbeShort4 < 4) {
                iUbeShort4 = 0;
                iUbeShort3 = iUbeShort2;
            }
            sample.setSampleData(data.samS8(i6, iUbeShort2), iUbeShort3, iUbeShort4, false);
            i6 += iUbeShort2;
        }
    }

    /* JADX WARN: Code duplicated, block: B:108:0x0280  */
    /* JADX WARN: Code duplicated, block: B:116:0x029a  */
    /* JADX WARN: Code duplicated, block: B:117:0x029d  */
    /* JADX WARN: Code duplicated, block: B:120:0x02a9  */
    /* JADX WARN: Code duplicated, block: B:121:0x02d1  */
    private void loadS3M(Data data) throws IOException {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6 = 0;
        int i7;
        int iUByte;
        int iUByte2;
        int i8;
        boolean z;
        int i9;
        int i10;
        int i11 = 0;
        int i12 = 28;
        this.songName = data.strCp850(0, 28);
        this.sequenceLength = data.uleShort(32);
        this.numInstruments = data.uleShort(34);
        this.numPatterns = data.uleShort(36);
        int i13 = 1;
        this.fastVolSlides = (data.uleShort(38) & 64) == 64 || data.uleShort(40) == 4864;
        boolean z2 = data.uleShort(42) == 1;
        if (data.uleInt(44) != 1297236819) {
            throw new IllegalArgumentException("Not an S3M file!");
        }
        this.defaultGVol = data.uByte(48);
        this.defaultSpeed = data.uByte(49);
        this.defaultTempo = data.uByte(50);
        this.c2Rate = Sample.C2_NTSC;
        this.gain = data.uByte(51) & 127;
        boolean z3 = (data.uByte(51) & 128) == 128;
        boolean z4 = data.uByte(53) == 252;
        this.numChannels = 0;
        int[] iArr = new int[32];
        int i14 = 0;
        while (true) {
            i = -1;
            if (i14 >= 32) {
                break;
            }
            iArr[i14] = -1;
            if (data.uByte(i14 + 64) < 16) {
                int i15 = this.numChannels;
                this.numChannels = i15 + 1;
                iArr[i14] = i15;
            }
            i14++;
        }
        int i16 = this.sequenceLength;
        int i17 = i16 + 96;
        this.sequence = new int[i16];
        for (int i18 = 0; i18 < this.sequenceLength; i18++) {
            this.sequence[i18] = data.uByte(i18 + 96);
        }
        while (true) {
            int i19 = this.sequenceLength;
            if (i19 <= 0 || this.sequence[i19 - 1] < this.numPatterns) {
                break;
            } else {
                this.sequenceLength = i19 - 1;
            }
        }
        Instrument[] instrumentArr = new Instrument[this.numInstruments + 1];
        this.instruments = instrumentArr;
        instrumentArr[0] = new Instrument();
        int i20 = 1;
        while (i20 <= this.numInstruments) {
            Instrument[] instrumentArr2 = this.instruments;
            Instrument instrument = new Instrument();
            instrumentArr2[i20] = instrument;
            Sample sample = instrument.samples[i11];
            int iUleShort = data.uleShort(i17) << 4;
            int i21 = i17 + 2;
            instrument.name = data.strCp850(iUleShort + 48, i12);
            if (data.uByte(iUleShort) == i13 && data.uleShort(iUleShort + 76) == 17235) {
                int iUByte3 = (data.uByte(iUleShort + 13) << 20) + (data.uleShort(iUleShort + 14) << 4);
                int iUleInt = data.uleInt(iUleShort + 16);
                int iUleInt2 = data.uleInt(iUleShort + 20);
                int iUleInt3 = data.uleInt(iUleShort + 24) - iUleInt2;
                sample.volume = data.uByte(iUleShort + 28);
                sample.panning = i;
                boolean z5 = data.uByte(iUleShort + 30) != 0;
                int i22 = iUleShort + 31;
                z = z4;
                i9 = i21;
                boolean z6 = (data.uByte(i22) & 1) == 1;
                if (iUleInt2 + iUleInt3 > iUleInt) {
                    iUleInt3 = iUleInt - iUleInt2;
                }
                if (iUleInt3 < 1 || !z6) {
                    iUleInt2 = iUleInt;
                    iUleInt3 = 0;
                }
                data.uByte(i22);
                boolean z7 = (data.uByte(i22) & 4) == 4;
                if (z5) {
                    throw new IllegalArgumentException("Packed samples not supported!");
                }
                int iLog2 = (Channel.log2(data.uleInt(iUleShort + 32)) - Channel.log2(this.c2Rate)) * 12;
                sample.relNote = iLog2 >> 15;
                sample.fineTune = (iLog2 & Sample.FP_MASK) >> 8;
                if (!z7) {
                    i10 = 0;
                    if (z2) {
                        sample.setSampleData(data.samS8(iUByte3, iUleInt), iUleInt2, iUleInt3, false);
                    } else {
                        sample.setSampleData(data.samU8(iUByte3, iUleInt), iUleInt2, iUleInt3, false);
                    }
                } else if (z2) {
                    i10 = 0;
                    sample.setSampleData(data.samS16(iUByte3, iUleInt), iUleInt2, iUleInt3, false);
                } else {
                    i10 = 0;
                    sample.setSampleData(data.samU16(iUByte3, iUleInt), iUleInt2, iUleInt3, false);
                }
            } else {
                z = z4;
                i9 = i21;
                i10 = 0;
            }
            i20++;
            i11 = i10;
            i17 = i9;
            z4 = z;
            i12 = 28;
            i13 = 1;
            i = -1;
        }
        int i23 = i11;
        boolean z8 = z4;
        this.patterns = new Pattern[this.numPatterns];
        int i24 = i23;
        while (i24 < this.numPatterns) {
            Pattern[] patternArr = this.patterns;
            int i25 = 64;
            Pattern pattern = new Pattern(this.numChannels, 64);
            patternArr[i24] = pattern;
            int iUleShort2 = (data.uleShort(i17) << 4) + 2;
            int i26 = i23;
            while (i26 < i25) {
                int i27 = iUleShort2 + 1;
                int iUByte4 = data.uByte(iUleShort2);
                if (iUByte4 == 0) {
                    i26++;
                    iUleShort2 = i27;
                    i25 = 64;
                } else {
                    if ((iUByte4 & 32) == 32) {
                        int i28 = i27 + 1;
                        int iUByte5 = data.uByte(i27);
                        int i29 = i28 + 1;
                        int iUByte6 = data.uByte(i28);
                        if (iUByte5 < 254) {
                            iUByte5 = ((iUByte5 >> 4) * 12) + (iUByte5 & 15) + 1;
                        }
                        if (iUByte5 == 255) {
                            i27 = i29;
                            i5 = iUByte6;
                            i4 = i23;
                        } else {
                            i4 = iUByte5;
                            i27 = i29;
                            i5 = iUByte6;
                        }
                    } else {
                        i4 = i23;
                        i5 = i4;
                    }
                    if ((iUByte4 & 64) == 64) {
                        int i30 = i27 + 1;
                        int iUByte7 = (data.uByte(i27) & 127) + 16;
                        if (iUByte7 > 80) {
                            i27 = i30;
                        } else {
                            i6 = iUByte7;
                            i27 = i30;
                        }
                        if ((iUByte4 & 128) == 128) {
                            int i31 = i27 + 1;
                            iUByte = data.uByte(i27);
                            i7 = i31 + 1;
                            iUByte2 = data.uByte(i31);
                            if (iUByte >= 1 || iUByte >= 64) {
                                iUByte = 0;
                                iUByte2 = 0;
                            }
                            if (iUByte > 0) {
                                iUByte += 128;
                            }
                        } else {
                            i7 = i27;
                            iUByte = 0;
                            iUByte2 = 0;
                        }
                        i8 = iArr[iUByte4 & 31];
                        if (i8 >= 0) {
                            int i32 = ((this.numChannels * i26) + i8) * 5;
                            pattern.data[i32] = (byte) i4;
                            pattern.data[i32 + 1] = (byte) i5;
                            pattern.data[i32 + 2] = (byte) i6;
                            pattern.data[i32 + 3] = (byte) iUByte;
                            pattern.data[i32 + 4] = (byte) iUByte2;
                        }
                        iUleShort2 = i7;
                        i25 = 64;
                        i23 = 0;
                    }
                    i6 = i23;
                    if ((iUByte4 & 128) == 128) {
                        int i33 = i27 + 1;
                        iUByte = data.uByte(i27);
                        i7 = i33 + 1;
                        iUByte2 = data.uByte(i33);
                        if (iUByte >= 1) {
                        }
                        iUByte = 0;
                        iUByte2 = 0;
                        if (iUByte > 0) {
                            iUByte += 128;
                        }
                    } else {
                        i7 = i27;
                        iUByte = 0;
                        iUByte2 = 0;
                    }
                    i8 = iArr[iUByte4 & 31];
                    if (i8 >= 0) {
                        int i34 = ((this.numChannels * i26) + i8) * 5;
                        pattern.data[i34] = (byte) i4;
                        pattern.data[i34 + 1] = (byte) i5;
                        pattern.data[i34 + 2] = (byte) i6;
                        pattern.data[i34 + 3] = (byte) iUByte;
                        pattern.data[i34 + 4] = (byte) iUByte2;
                    }
                    iUleShort2 = i7;
                    i25 = 64;
                    i23 = 0;
                }
            }
            i17 += 2;
            i24++;
            i23 = 0;
        }
        this.defaultPanning = new int[this.numChannels];
        int i35 = 0;
        int i36 = 32;
        while (i35 < i36) {
            if (iArr[i35] < 0) {
                i3 = 32;
            } else {
                if (z3) {
                    i2 = data.uByte(i35 + 64) < 8 ? 3 : 12;
                } else {
                    i2 = 7;
                }
                if (z8) {
                    int iUByte8 = data.uByte(i17 + i35);
                    i3 = 32;
                    if ((iUByte8 & 32) == 32) {
                        i2 = iUByte8 & 15;
                    }
                } else {
                    i3 = 32;
                }
                this.defaultPanning[iArr[i35]] = i2 * 17;
            }
            i35++;
            i36 = i3;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    private void loadXM(Data data) throws IOException {
        int i;
        byte bSByte;
        byte bSByte2;
        byte bSByte3;
        byte bSByte4;
        byte bSByte5;
        Module module = this;
        if (data.uleShort(58) != 260) {
            throw new IllegalArgumentException("XM format version must be 0x0104!");
        }
        module.songName = data.strCp850(17, 20);
        boolean zStartsWith = data.strLatin1(38, 20).startsWith("DigiBooster Pro");
        int iUleInt = data.uleInt(60) + 60;
        module.sequenceLength = data.uleShort(64);
        module.restartPos = data.uleShort(66);
        module.numChannels = data.uleShort(68);
        module.numPatterns = data.uleShort(70);
        module.numInstruments = data.uleShort(72);
        boolean z = true;
        int i2 = 0;
        module.linearPeriods = (data.uleShort(74) & 1) > 0;
        module.defaultGVol = 64;
        module.defaultSpeed = data.uleShort(76);
        module.defaultTempo = data.uleShort(78);
        module.c2Rate = Sample.C2_NTSC;
        module.gain = 64;
        module.defaultPanning = new int[module.numChannels];
        for (int i3 = 0; i3 < module.numChannels; i3++) {
            module.defaultPanning[i3] = 128;
        }
        module.sequence = new int[module.sequenceLength];
        for (int i4 = 0; i4 < module.sequenceLength; i4++) {
            int iUByte = data.uByte(i4 + 80);
            int[] iArr = module.sequence;
            if (iUByte >= module.numPatterns) {
                iUByte = 0;
            }
            iArr[i4] = iUByte;
        }
        module.patterns = new Pattern[module.numPatterns];
        int i5 = 0;
        while (i5 < module.numPatterns) {
            if (data.uByte(iUleInt + 4) != 0) {
                throw new IllegalArgumentException("Unknown pattern packing type!");
            }
            int iUleShort = data.uleShort(iUleInt + 5);
            if (iUleShort < 1) {
                iUleShort = 1;
            }
            int i6 = module.numChannels;
            int i7 = iUleShort * i6;
            Pattern[] patternArr = module.patterns;
            Pattern pattern = new Pattern(i6, iUleShort);
            patternArr[i5] = pattern;
            int iUleShort2 = data.uleShort(iUleInt + 7);
            int iUleInt2 = iUleInt + data.uleInt(iUleInt);
            int i8 = iUleInt2 + iUleShort2;
            if (iUleShort2 > 0) {
                int i9 = 0;
                int i10 = 0;
                while (i9 < i7) {
                    int iUByte2 = data.uByte(iUleInt2);
                    if ((iUByte2 & 128) == 0) {
                        iUByte2 = 31;
                    } else {
                        iUleInt2++;
                    }
                    if ((iUByte2 & 1) > 0) {
                        i = iUleInt2 + 1;
                        bSByte = data.sByte(iUleInt2);
                    } else {
                        i = iUleInt2;
                        bSByte = 0;
                    }
                    int i11 = i10 + 1;
                    pattern.data[i10] = bSByte;
                    if ((iUByte2 & 2) > 0) {
                        bSByte2 = data.sByte(i);
                        i++;
                    } else {
                        bSByte2 = 0;
                    }
                    int i12 = i11 + 1;
                    pattern.data[i11] = bSByte2;
                    if ((iUByte2 & 4) > 0) {
                        bSByte3 = data.sByte(i);
                        i++;
                    } else {
                        bSByte3 = 0;
                    }
                    int i13 = i12 + 1;
                    pattern.data[i12] = bSByte3;
                    if ((iUByte2 & 8) > 0) {
                        bSByte4 = data.sByte(i);
                        i++;
                    } else {
                        bSByte4 = 0;
                    }
                    if ((iUByte2 & 16) > 0) {
                        iUleInt2 = i + 1;
                        bSByte5 = data.sByte(i);
                    } else {
                        bSByte5 = 0;
                        iUleInt2 = i;
                    }
                    if (bSByte4 >= 64) {
                        bSByte4 = 0;
                        bSByte5 = 0;
                    }
                    int i14 = i13 + 1;
                    pattern.data[i13] = bSByte4;
                    pattern.data[i14] = bSByte5;
                    i9++;
                    i10 = i14 + 1;
                }
            }
            i5++;
            iUleInt = i8;
        }
        Instrument[] instrumentArr = new Instrument[module.numInstruments + 1];
        module.instruments = instrumentArr;
        instrumentArr[0] = new Instrument();
        int i15 = 1;
        while (i15 <= module.numInstruments) {
            Instrument[] instrumentArr2 = module.instruments;
            Instrument instrument = new Instrument();
            instrumentArr2[i15] = instrument;
            instrument.name = data.strCp850(iUleInt + 4, 22);
            int iUleShort3 = data.uleShort(iUleInt + 27);
            if (iUleShort3 > 0) {
                instrument.numSamples = iUleShort3;
                instrument.samples = new Sample[iUleShort3];
                int i16 = i2;
                while (i16 < 96) {
                    int iUByte3 = data.uByte(iUleInt + 33 + i16);
                    int[] iArr2 = instrument.keyToSample;
                    i16++;
                    if (iUByte3 >= iUleShort3) {
                        iUByte3 = i2;
                    }
                    iArr2[i16] = iUByte3;
                }
                Envelope envelope = new Envelope();
                instrument.volumeEnvelope = envelope;
                envelope.pointsTick = new int[16];
                envelope.pointsAmpl = new int[16];
                int i17 = i2;
                int iUleShort4 = i17;
                while (i17 < 12) {
                    int i18 = iUleInt + 129 + (i17 * 4);
                    if (!zStartsWith) {
                        iUleShort4 = i2;
                    }
                    iUleShort4 += data.uleShort(i18);
                    envelope.pointsTick[i17] = iUleShort4;
                    envelope.pointsAmpl[i17] = data.uleShort(i18 + 2);
                    i17++;
                }
                Envelope envelope2 = new Envelope();
                instrument.panningEnvelope = envelope2;
                envelope2.pointsTick = new int[16];
                envelope2.pointsAmpl = new int[16];
                int i19 = i2;
                int iUleShort5 = i19;
                while (i19 < 12) {
                    int i20 = iUleInt + 177 + (i19 * 4);
                    if (!zStartsWith) {
                        iUleShort5 = i2;
                    }
                    iUleShort5 += data.uleShort(i20);
                    envelope2.pointsTick[i19] = iUleShort5;
                    envelope2.pointsAmpl[i19] = data.uleShort(i20 + 2);
                    i19++;
                }
                envelope.numPoints = data.uByte(iUleInt + 225);
                if (envelope.numPoints > 12) {
                    envelope.numPoints = i2;
                }
                envelope2.numPoints = data.uByte(iUleInt + 226);
                if (envelope2.numPoints > 12) {
                    envelope2.numPoints = i2;
                }
                envelope.sustainTick = envelope.pointsTick[data.uByte(iUleInt + 227) & 15];
                envelope.loopStartTick = envelope.pointsTick[data.uByte(iUleInt + 228) & 15];
                envelope.loopEndTick = envelope.pointsTick[data.uByte(iUleInt + 229) & 15];
                envelope2.sustainTick = envelope2.pointsTick[data.uByte(iUleInt + 230) & 15];
                envelope2.loopStartTick = envelope2.pointsTick[data.uByte(iUleInt + 231) & 15];
                envelope2.loopEndTick = envelope2.pointsTick[data.uByte(iUleInt + 232) & 15];
                envelope.enabled = envelope.numPoints > 0 && (data.uByte(iUleInt + 233) & 1) > 0;
                int i21 = iUleInt + 233;
                envelope.sustain = (data.uByte(i21) & 2) > 0;
                envelope.looped = (data.uByte(i21) & 4) > 0;
                envelope2.enabled = envelope2.numPoints > 0 && (data.uByte(iUleInt + 234) & 1) > 0;
                int i22 = iUleInt + 234;
                envelope2.sustain = (data.uByte(i22) & 2) > 0;
                envelope2.looped = (data.uByte(i22) & 4) > 0;
                instrument.vibratoType = data.uByte(iUleInt + 235);
                instrument.vibratoSweep = data.uByte(iUleInt + 236);
                instrument.vibratoDepth = data.uByte(iUleInt + 237);
                instrument.vibratoRate = data.uByte(iUleInt + 238);
                instrument.volumeFadeOut = data.uleShort(iUleInt + 239);
            }
            int iUleInt3 = iUleInt + data.uleInt(iUleInt);
            int i23 = i2;
            int i24 = iUleInt3;
            iUleInt = (iUleShort3 * 40) + iUleInt3;
            boolean z2 = z;
            while (i23 < iUleShort3) {
                Sample[] sampleArr = instrument.samples;
                Sample sample = new Sample();
                sampleArr[i23] = sample;
                int iUleInt4 = data.uleInt(i24);
                int iUleInt5 = data.uleInt(i24 + 4);
                int iUleInt6 = data.uleInt(i24 + 8);
                sample.volume = data.sByte(i24 + 12);
                sample.fineTune = data.sByte(i24 + 13);
                int i25 = i24 + 14;
                int i26 = (data.uByte(i25) & 3) > 0 ? z2 ? 1 : 0 : i2;
                boolean z3 = z2;
                if ((data.uByte(i25) & 2) <= 0) {
                    z3 = false;
                }
                int i27 = (data.uByte(i25) & 16) > 0 ? 1 : i2;
                sample.panning = data.uByte(i24 + 15);
                sample.relNote = data.sByte(i24 + 16);
                sample.name = data.strCp850(i24 + 18, 22);
                i24 += 40;
                if (i26 == 0 || iUleInt5 + iUleInt6 > iUleInt4) {
                    iUleInt5 = iUleInt4;
                    iUleInt6 = 0;
                }
                if (i27 != 0) {
                    sample.setSampleData(data.samS16D(iUleInt, iUleInt4 >> 1), iUleInt5 >> 1, iUleInt6 >> 1, z3);
                } else {
                    sample.setSampleData(data.samS8D(iUleInt, iUleInt4), iUleInt5, iUleInt6, z3);
                }
                iUleInt += iUleInt4;
                i23++;
                z2 = true;
                i2 = 0;
            }
            i15++;
            module = this;
            z = true;
            i2 = 0;
        }
    }

    public void toStringBuffer(StringBuffer stringBuffer) {
        stringBuffer.append("Song Name: " + this.songName + "\nNum Channels: " + this.numChannels + "\nNum Instruments: " + this.numInstruments + "\nNum Patterns: " + this.numPatterns + "\nSequence Length: " + this.sequenceLength + "\nRestart Pos: " + this.restartPos + "\nDefault Speed: " + this.defaultSpeed + "\nDefault Tempo: " + this.defaultTempo + "\nLinear Periods: " + this.linearPeriods + '\n');
        stringBuffer.append("Sequence: ");
        for (int i = 0; i < this.sequence.length; i++) {
            stringBuffer.append(this.sequence[i] + ", ");
        }
        stringBuffer.append('\n');
        for (int i2 = 0; i2 < this.patterns.length; i2++) {
            stringBuffer.append("Pattern " + i2 + ":\n");
            this.patterns[i2].toStringBuffer(stringBuffer);
        }
        for (int i3 = 1; i3 < this.instruments.length; i3++) {
            stringBuffer.append("Instrument " + i3 + ":\n");
            this.instruments[i3].toStringBuffer(stringBuffer);
        }
    }
}
