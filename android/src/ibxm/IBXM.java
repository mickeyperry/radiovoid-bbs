package ibxm;

/* JADX INFO: loaded from: classes.dex */
public class IBXM {
    public static final String VERSION = "a76 (c)2026 mumart@gmail.com";
    private int breakSeqPos;
    private Channel[] channels;
    private GlobalVol globalVol;
    private int interpolation;
    private Module module;
    private boolean[] muted;
    private int nextRow;
    private Note note;
    private int plChannel;
    private int plCount;
    private byte[][] playCount;
    private int[] rampBuf;
    private int row;
    private int sampleRate;
    private int seqPos;
    private boolean sequencerEnabled;
    private int speed;
    private int tempo;
    private int tick;

    public IBXM(Module module, int i) {
        this.module = module;
        setSampleRate(i);
        this.interpolation = 1;
        this.rampBuf = new int[128];
        this.playCount = new byte[module.sequenceLength][];
        this.channels = new Channel[module.numChannels];
        this.muted = new boolean[module.numChannels];
        this.globalVol = new GlobalVol();
        this.note = new Note();
        setSequencePos(0);
    }

    public int getSampleRate() {
        return this.sampleRate;
    }

    public void setSampleRate(int i) {
        if (i < 8000 || i > 128000) {
            throw new IllegalArgumentException("Unsupported sampling rate!");
        }
        this.sampleRate = i;
    }

    public int getInterpolation() {
        return this.interpolation;
    }

    public void setInterpolation(int i) {
        this.interpolation = i;
    }

    public int getMixBufferLength() {
        return (calculateTickLen(32, 128000) + 65) * 4;
    }

    public void setMuted(int i, boolean z) {
        if (i < 0) {
            for (int i2 = 0; i2 < this.module.numChannels; i2++) {
                this.muted[i2] = z;
            }
            return;
        }
        if (i < this.module.numChannels) {
            this.muted[i] = z;
        }
    }

    public int getRow() {
        return this.row;
    }

    public int getSequencePos() {
        return this.seqPos;
    }

    public void setSequencePos(int i) {
        if (i >= this.module.sequenceLength) {
            i = 0;
        }
        this.breakSeqPos = i;
        this.nextRow = 0;
        this.tick = 1;
        this.globalVol.volume = this.module.defaultGVol;
        this.speed = this.module.defaultSpeed > 0 ? this.module.defaultSpeed : 6;
        this.tempo = this.module.defaultTempo > 0 ? this.module.defaultTempo : 125;
        this.plChannel = -1;
        this.plCount = -1;
        for (int i2 = 0; i2 < this.playCount.length; i2++) {
            int i3 = this.module.sequence[i2];
            this.playCount[i2] = new byte[i3 < this.module.numPatterns ? this.module.patterns[i3].numRows : 0];
        }
        for (int i4 = 0; i4 < this.module.numChannels; i4++) {
            this.channels[i4] = new Channel(this.module, i4, this.globalVol);
        }
        for (int i5 = 0; i5 < 128; i5++) {
            this.rampBuf[i5] = 0;
        }
        this.sequencerEnabled = true;
        tick();
    }

    public int calculateSongDuration() {
        setSequencePos(0);
        boolean zTick = false;
        int iCalculateTickLen = 0;
        while (!zTick) {
            iCalculateTickLen += calculateTickLen(this.tempo, this.sampleRate);
            zTick = tick();
        }
        setSequencePos(0);
        return iCalculateTickLen;
    }

    public int seek(int i) {
        setSequencePos(0);
        int iCalculateTickLen = calculateTickLen(this.tempo, this.sampleRate);
        int i2 = 0;
        while (i - i2 >= iCalculateTickLen) {
            for (int i3 = 0; i3 < this.module.numChannels; i3++) {
                this.channels[i3].updateSampleIdx(iCalculateTickLen * 2, this.sampleRate * 2);
            }
            i2 += iCalculateTickLen;
            tick();
            iCalculateTickLen = calculateTickLen(this.tempo, this.sampleRate);
        }
        return i2;
    }

    public void seekSequencePos(int i, int i2) {
        setSequencePos(0);
        if (i < 0 || i >= this.module.sequenceLength) {
            i = 0;
        }
        if (i2 >= this.module.patterns[this.module.sequence[i]].numRows) {
            i2 = 0;
        }
        do {
            if (this.seqPos >= i && this.row >= i2) {
                return;
            }
            int iCalculateTickLen = calculateTickLen(this.tempo, this.sampleRate);
            for (int i3 = 0; i3 < this.module.numChannels; i3++) {
                this.channels[i3].updateSampleIdx(iCalculateTickLen * 2, this.sampleRate * 2);
            }
        } while (!tick());
        setSequencePos(i);
    }

    public boolean getSequencerEnabled() {
        return this.sequencerEnabled;
    }

    public void setSequencerEnabled(boolean z) {
        this.sequencerEnabled = z;
    }

    public void trigger(int i, Note note) {
        this.channels[i].row(note);
    }

    public int getAudio(int[] iArr) {
        int iCalculateTickLen = calculateTickLen(this.tempo, this.sampleRate);
        int i = iCalculateTickLen + 65;
        int i2 = i * 4;
        for (int i3 = 0; i3 < i2; i3++) {
            iArr[i3] = 0;
        }
        for (int i4 = 0; i4 < this.module.numChannels; i4++) {
            Channel channel = this.channels[i4];
            if (!this.muted[i4]) {
                channel.resample(iArr, 0, i * 2, this.sampleRate * 2, this.interpolation);
            }
            channel.updateSampleIdx(iCalculateTickLen * 2, this.sampleRate * 2);
        }
        downsample(iArr, iCalculateTickLen + 64);
        volumeRamp(iArr, iCalculateTickLen);
        tick();
        return iCalculateTickLen;
    }

    private int calculateTickLen(int i, int i2) {
        return (i2 * 5) / (i * 2);
    }

    private void volumeRamp(int[] iArr, int i) {
        int i2 = 524288 / this.sampleRate;
        int i3 = 0;
        for (int i4 = 0; i4 < 256; i4 += i2) {
            int i5 = 256 - i4;
            int i6 = iArr[i3] * i4;
            int[] iArr2 = this.rampBuf;
            iArr[i3] = (i6 + (iArr2[i3] * i5)) >> 8;
            int i7 = i3 + 1;
            iArr[i7] = ((iArr[i7] * i4) + (iArr2[i7] * i5)) >> 8;
            i3 += 2;
        }
        System.arraycopy(iArr, i * 2, this.rampBuf, 0, 128);
    }

    private void downsample(int[] iArr, int i) {
        int i2 = i * 2;
        int i3 = 0;
        int i4 = 0;
        while (i3 < i2) {
            int i5 = i4 + 4;
            iArr[i3] = (iArr[i4] >> 2) + (iArr[i4 + 2] >> 1) + (iArr[i5] >> 2);
            iArr[i3 + 1] = (iArr[i4 + 1] >> 2) + (iArr[i4 + 3] >> 1) + (iArr[i4 + 5] >> 2);
            i3 += 2;
            i4 = i5;
        }
    }

    /* JADX WARN: Code duplicated, block: B:10:0x001c A[LOOP:0: B:8:0x0016->B:10:0x001c, LOOP_END] */
    /* JADX WARN: Code duplicated, block: B:7:0x0015  */
    private boolean tick() {
        int i;
        if (this.sequencerEnabled) {
            int i2 = this.tick - 1;
            this.tick = i2;
            if (i2 <= 0) {
                this.tick = this.speed;
                row();
            } else {
                for (i = 0; i < this.module.numChannels; i++) {
                    this.channels[i].tick();
                }
            }
        } else {
            for (i = 0; i < this.module.numChannels; i++) {
                this.channels[i].tick();
            }
        }
        return this.playCount[this.seqPos][this.row] > 1;
    }

    /* JADX WARN: Code duplicated, block: B:101:0x01ba  */
    /* JADX WARN: Code duplicated, block: B:125:0x01c2 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:127:0x01c2 A[SYNTHETIC] */
    /* JADX WARN: Code duplicated, block: B:69:0x0135  */
    /* JADX WARN: Code duplicated, block: B:70:0x0141  */
    /* JADX WARN: Code duplicated, block: B:72:0x0147  */
    /* JADX WARN: Code duplicated, block: B:93:0x0197  */
    /* JADX WARN: Code duplicated, block: B:95:0x019b  */
    /* JADX WARN: Code duplicated, block: B:97:0x019f  */
    /* JADX WARN: Code duplicated, block: B:99:0x01b6  */
    private void row() {
        if (this.nextRow < 0) {
            this.breakSeqPos = this.seqPos + 1;
            this.nextRow = 0;
        }
        int i = this.breakSeqPos;
        if (i >= 0) {
            if (i >= this.module.sequenceLength) {
                this.nextRow = 0;
                this.breakSeqPos = 0;
            }
            while (this.module.sequence[this.breakSeqPos] >= this.module.numPatterns) {
                int i2 = this.breakSeqPos + 1;
                this.breakSeqPos = i2;
                if (i2 >= this.module.sequenceLength) {
                    this.nextRow = 0;
                    this.breakSeqPos = 0;
                }
            }
            this.seqPos = this.breakSeqPos;
            for (int i3 = 0; i3 < this.module.numChannels; i3++) {
                this.channels[i3].plRow = 0;
            }
            this.breakSeqPos = -1;
        }
        Pattern pattern = this.module.patterns[this.module.sequence[this.seqPos]];
        int i4 = this.nextRow;
        this.row = i4;
        if (i4 >= pattern.numRows) {
            this.row = 0;
        }
        byte[] bArr = this.playCount[this.seqPos];
        int i5 = this.row;
        byte b = bArr[i5];
        if (this.plCount < 0 && b < 127) {
            bArr[i5] = (byte) (b + 1);
        }
        int i6 = i5 + 1;
        this.nextRow = i6;
        if (i6 >= pattern.numRows) {
            this.nextRow = -1;
        }
        int i7 = this.row * this.module.numChannels;
        for (int i8 = 0; i8 < this.module.numChannels; i8++) {
            Channel channel = this.channels[i8];
            pattern.getNote(i7 + i8, this.note);
            if (this.note.effect == 14) {
                Note note = this.note;
                note.effect = (note.param >> 4) | 112;
                this.note.param &= 15;
            }
            if (this.note.effect == 147) {
                Note note2 = this.note;
                note2.effect = (note2.param >> 4) | 240;
                this.note.param &= 15;
            }
            if (this.note.effect == 0 && this.note.param > 0) {
                this.note.effect = 138;
            }
            channel.row(this.note);
            int i9 = this.note.effect;
            if (i9 == 11) {
                if (this.plCount < 0) {
                    this.breakSeqPos = this.note.param;
                    this.nextRow = 0;
                }
            } else if (i9 == 13) {
                if (this.plCount >= 0) {
                    if (this.breakSeqPos < 0) {
                        this.breakSeqPos = this.seqPos + 1;
                    }
                    this.nextRow = ((this.note.param >> 4) * 10) + (this.note.param & 15);
                }
            } else if (i9 != 15) {
                if (i9 == 118) {
                    if (this.note.param == 0) {
                        channel.plRow = this.row;
                    }
                    if (channel.plRow >= this.row && this.breakSeqPos < 0) {
                        if (this.plCount < 0) {
                            this.plCount = this.note.param;
                            this.plChannel = i8;
                        }
                        if (this.plChannel == i8) {
                            if (this.plCount == 0) {
                                channel.plRow = this.row + 1;
                            } else {
                                this.nextRow = channel.plRow;
                            }
                            this.plCount--;
                        }
                    }
                } else if (i9 == 126) {
                    int i10 = this.speed;
                    this.tick = i10 + (this.note.param * i10);
                } else if (i9 != 148) {
                    if (i9 == 251) {
                        if (this.note.param == 0) {
                            channel.plRow = this.row;
                        }
                        if (channel.plRow >= this.row) {
                        }
                    } else if (i9 != 254) {
                        switch (i9) {
                            case 129:
                                if (this.note.param > 0) {
                                    int i11 = this.note.param;
                                    this.speed = i11;
                                    this.tick = i11;
                                }
                                break;
                            case 130:
                                if (this.plCount < 0) {
                                    this.breakSeqPos = this.note.param;
                                    this.nextRow = 0;
                                }
                                break;
                            case 131:
                                if (this.plCount >= 0) {
                                    if (this.breakSeqPos < 0) {
                                        this.breakSeqPos = this.seqPos + 1;
                                    }
                                    this.nextRow = ((this.note.param >> 4) * 10) + (this.note.param & 15);
                                }
                                break;
                        }
                    } else {
                        int i12 = this.speed;
                        this.tick = i12 + (this.note.param * i12);
                    }
                } else if (this.note.param > 32) {
                    this.tempo = this.note.param;
                }
            } else if (this.note.param > 0) {
                if (this.note.param < 32) {
                    int i13 = this.note.param;
                    this.speed = i13;
                    this.tick = i13;
                } else {
                    this.tempo = this.note.param;
                }
            }
        }
    }
}
