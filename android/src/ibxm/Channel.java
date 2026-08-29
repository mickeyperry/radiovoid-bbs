package ibxm;

/* JADX INFO: loaded from: classes.dex */
public class Channel {
    public static final int LINEAR = 1;
    public static final int NEAREST = 0;
    public static final int SINC = 2;
    private static int[] exp2Table = {Sample.FP_ONE, 32946, 33125, 33305, 33486, 33667, 33850, 34034, 34219, 34405, 34591, 34779, 34968, 35158, 35349, 35541, 35734, 35928, 36123, 36319, 36516, 36715, 36914, 37114, 37316, 37518, 37722, 37927, 38133, 38340, 38548, 38757, 38968, 39180, 39392, 39606, 39821, 40037, 40255, 40473, 40693, 40914, 41136, 41360, 41584, 41810, 42037, 42265, 42495, 42726, 42958, 43191, 43425, 43661, 43898, 44137, 44376, 44617, 44859, 45103, 45348, 45594, 45842, 46091, 46341, 46593, 46846, 47100, 47356, 47613, 47871, 48131, 48393, 48655, 48920, 49185, 49452, 49721, 49991, 50262, 50535, 50810, 51085, 51363, 51642, 51922, 52204, 52488, 52773, 53059, 53347, 53637, 53928, 54221, 54515, 54811, 55109, 55408, 55709, 56012, 56316, 56622, 56929, 57238, 57549, 57861, 58176, 58491, 58809, 59128, 59449, 59772, 60097, 60423, 60751, 61081, 61413, 61746, 62081, 62419, 62757, 63098, 63441, 63785, 64132, 64480, 64830, 65182, 65536};
    private static final short[] sineTable = {0, 24, 49, 74, 97, 120, 141, 161, 180, 197, 212, 224, 235, 244, 250, 253, 255, 253, 250, 244, 235, 224, 212, 197, 180, 161, 141, 120, 97, 74, 49, 24};
    private int ampl;
    private int arpeggioAdd;
    private int arpeggioParam;
    private int autoVibratoCount;
    private int extraFinePortaParam;
    private int fadeOutVol;
    private int finePortaDownParam;
    private int finePortaUpParam;
    private int fineVslideDownParam;
    private int fineVslideUpParam;
    private int freq;
    private int fxCount;
    private GlobalVol globalVol;
    private int globalVslideParam;
    private int id;
    private Instrument instrument;
    private boolean keyOn;
    private Module module;
    private int noteEffect;
    private int noteIns;
    private int noteKey;
    private int noteParam;
    private int noteVol;
    private int offsetParam;
    private int panEnvTick;
    private int pann;
    private int panning;
    private int panningSlideParam;
    private int period;
    public int plRow;
    private int portaDownParam;
    private int portaPeriod;
    private int portaUpParam;
    private int randomSeed;
    private int retrigCount;
    private int retrigTicks;
    private int retrigVolume;
    private Sample sample;
    private int sampleFra;
    private int sampleIdx;
    private int sampleOffset;
    private int tonePortaParam;
    private int tremoloAdd;
    private int tremoloDepth;
    private int tremoloPhase;
    private int tremoloSpeed;
    private int tremoloType;
    private int tremorOffTicks;
    private int tremorOnTicks;
    private int vibratoAdd;
    private int vibratoDepth;
    private int vibratoPhase;
    private int vibratoSpeed;
    private int vibratoType;
    private int volEnvTick;
    private int volume;
    private int vslideParam;

    public Channel(Module module, int i, GlobalVol globalVol) {
        this.module = module;
        this.id = i;
        this.globalVol = globalVol;
        this.panning = module.defaultPanning[i];
        Instrument instrument = new Instrument();
        this.instrument = instrument;
        this.sample = instrument.samples[0];
        this.randomSeed = (i + 1) * 11259375;
    }

    public void resample(int[] iArr, int i, int i2, int i3, int i4) {
        int i5 = this.ampl;
        if (i5 <= 0) {
            return;
        }
        int i6 = this.pann;
        int i7 = ((255 - i6) * i5) >> 8;
        int i8 = (i5 * i6) >> 8;
        int i9 = (this.freq << 12) / (i3 >> 3);
        if (i4 == 0) {
            this.sample.resampleNearest(this.sampleIdx, this.sampleFra, i9, i7, i8, iArr, i, i2);
        } else if (i4 != 2) {
            this.sample.resampleLinear(this.sampleIdx, this.sampleFra, i9, i7, i8, iArr, i, i2);
        } else {
            this.sample.resampleSinc(this.sampleIdx, this.sampleFra, i9, i7, i8, iArr, i, i2);
        }
    }

    public void updateSampleIdx(int i, int i2) {
        int i3 = this.sampleFra + (((this.freq << 12) / (i2 >> 3)) * i);
        this.sampleFra = i3;
        this.sampleIdx = this.sample.normaliseSampleIdx(this.sampleIdx + (i3 >> 15));
        this.sampleFra &= Sample.FP_MASK;
    }

    /* JADX WARN: Code duplicated, block: B:103:0x013a  */
    /* JADX WARN: Code duplicated, block: B:105:0x0140  */
    /* JADX WARN: Code duplicated, block: B:108:0x0148  */
    /* JADX WARN: Code duplicated, block: B:123:0x0189  */
    /* JADX WARN: Code duplicated, block: B:126:0x0190  */
    /* JADX WARN: Code duplicated, block: B:128:0x0196  */
    /* JADX WARN: Code duplicated, block: B:130:0x019a  */
    /* JADX WARN: Code duplicated, block: B:132:0x01a5  */
    /* JADX WARN: Code duplicated, block: B:134:0x01a9  */
    /* JADX WARN: Code duplicated, block: B:135:0x01ad  */
    /* JADX WARN: Code duplicated, block: B:137:0x01b1  */
    /* JADX WARN: Code duplicated, block: B:148:0x01d6  */
    /* JADX WARN: Code duplicated, block: B:150:0x01dc  */
    /* JADX WARN: Code duplicated, block: B:153:0x01e4  */
    /* JADX WARN: Code duplicated, block: B:155:0x01ec  */
    /* JADX WARN: Code duplicated, block: B:157:0x01f2  */
    /* JADX WARN: Code duplicated, block: B:160:0x01fa  */
    /* JADX WARN: Code duplicated, block: B:170:0x0213  */
    /* JADX WARN: Code duplicated, block: B:172:0x0217  */
    /* JADX WARN: Code duplicated, block: B:70:0x00d1  */
    /* JADX WARN: Code duplicated, block: B:72:0x00d5  */
    /* JADX WARN: Code duplicated, block: B:74:0x00df  */
    /* JADX WARN: Code duplicated, block: B:76:0x00e3  */
    /* JADX WARN: Code duplicated, block: B:78:0x00ea  */
    /* JADX WARN: Code duplicated, block: B:80:0x00f0  */
    /* JADX WARN: Code duplicated, block: B:83:0x00f8  */
    /* JADX WARN: Code duplicated, block: B:85:0x0101  */
    /* JADX WARN: Code duplicated, block: B:87:0x0105  */
    /* JADX WARN: Code duplicated, block: B:88:0x0109  */
    /* JADX WARN: Code duplicated, block: B:90:0x010d  */
    /* JADX WARN: Code duplicated, block: B:92:0x0116  */
    /* JADX WARN: Code duplicated, block: B:94:0x011a  */
    public void row(Note note) {
        int i;
        int i2;
        int i3;
        int i4;
        int i5;
        int i6;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        this.noteKey = note.key;
        this.noteIns = note.instrument;
        this.noteVol = note.volume;
        this.noteEffect = note.effect;
        int i13 = note.param;
        this.noteParam = i13;
        this.retrigCount++;
        this.fxCount = 0;
        this.arpeggioAdd = 0;
        this.tremoloAdd = 0;
        this.vibratoAdd = 0;
        int i14 = this.noteEffect;
        if ((i14 != 125 && i14 != 253) || i13 <= 0) {
            trigger();
        }
        int i15 = this.noteEffect;
        if (i15 == 10) {
            i = this.noteParam;
            if (i > 0) {
                this.vslideParam = i;
            }
            volumeSlide();
        } else if (i15 == 12) {
            int i16 = this.noteParam;
            this.volume = i16 < 64 ? i16 & 63 : 64;
        } else if (i15 == 25) {
            int i17 = this.noteParam;
            if (i17 > 0) {
                this.panningSlideParam = i17;
            }
        } else if (i15 == 27) {
            i2 = this.noteParam;
            if ((i2 >> 4) > 0) {
                this.retrigVolume = i2 >> 4;
            }
            if ((i2 & 15) > 0) {
                this.retrigTicks = i2 & 15;
            }
            retrigVolSlide();
        } else if (i15 == 29) {
            i3 = this.noteParam;
            if ((i3 >> 4) > 0) {
                this.tremorOnTicks = i3 >> 4;
            }
            if ((i3 & 15) > 0) {
                this.tremorOffTicks = i3 & 15;
            }
            tremor();
        } else if (i15 == 33) {
            int i18 = this.noteParam;
            if (i18 > 0) {
                this.extraFinePortaParam = i18;
            }
            int i19 = this.extraFinePortaParam;
            int i20 = i19 & 240;
            if (i20 == 16) {
                portamentoUp((i19 & 15) | 224);
            } else if (i20 == 32) {
                portamentoDown((i19 & 15) | 224);
            }
        } else if (i15 == 116) {
            i4 = this.noteParam;
            if (i4 < 8) {
                this.vibratoType = i4;
            }
        } else if (i15 == 119) {
            i5 = this.noteParam;
            if (i5 < 8) {
                this.tremoloType = i5;
            }
        } else if (i15 == 248) {
            this.panning = this.noteParam * 17;
        } else if (i15 == 252) {
            if (this.noteParam <= 0) {
                this.volume = 0;
            }
        } else if (i15 == 16) {
            GlobalVol globalVol = this.globalVol;
            int i21 = this.noteParam;
            globalVol.volume = i21 < 64 ? i21 & 63 : 64;
        } else if (i15 == 17) {
            int i22 = this.noteParam;
            if (i22 > 0) {
                this.globalVslideParam = i22;
            }
        } else if (i15 == 20) {
            this.keyOn = false;
        } else if (i15 == 21) {
            int i23 = this.noteParam & 255;
            this.panEnvTick = i23;
            this.volEnvTick = i23;
        } else if (i15 == 113) {
            int i24 = this.noteParam;
            if (i24 > 0) {
                this.finePortaUpParam = i24;
            }
            portamentoUp((this.finePortaUpParam & 15) | 240);
        } else if (i15 == 114) {
            int i25 = this.noteParam;
            if (i25 > 0) {
                this.finePortaDownParam = i25;
            }
            portamentoDown((this.finePortaDownParam & 15) | 240);
        } else if (i15 == 145) {
            i2 = this.noteParam;
            if ((i2 >> 4) > 0) {
                this.retrigVolume = i2 >> 4;
            }
            if ((i2 & 15) > 0) {
                this.retrigTicks = i2 & 15;
            }
            retrigVolSlide();
        } else if (i15 == 146) {
            i6 = this.noteParam;
            if ((i6 >> 4) > 0) {
                this.tremoloSpeed = i6 >> 4;
            }
            if ((i6 & 15) > 0) {
                this.tremoloDepth = i6 & 15;
            }
            tremolo();
        } else if (i15 == 149) {
            int i26 = this.noteParam;
            if ((i26 >> 4) > 0) {
                this.vibratoSpeed = i26 >> 4;
            }
            if ((i26 & 15) > 0) {
                this.vibratoDepth = i26 & 15;
            }
            vibrato(true);
        } else if (i15 == 150) {
            GlobalVol globalVol2 = this.globalVol;
            int i27 = this.noteParam;
            globalVol2.volume = i27 < 64 ? i27 & 63 : 64;
        } else if (i15 == 243) {
            i4 = this.noteParam;
            if (i4 < 8) {
                this.vibratoType = i4;
            }
        } else if (i15 != 244) {
            switch (i15) {
                case LINEAR /* 1 */:
                    i7 = this.noteParam;
                    if (i7 > 0) {
                        this.portaUpParam = i7;
                    }
                    portamentoUp(this.portaUpParam);
                    break;
                case SINC /* 2 */:
                    i8 = this.noteParam;
                    if (i8 > 0) {
                        this.portaDownParam = i8;
                    }
                    portamentoDown(this.portaDownParam);
                    break;
                case 3:
                    i9 = this.noteParam;
                    if (i9 > 0) {
                        this.tonePortaParam = i9;
                    }
                    break;
                case 4:
                    i10 = this.noteParam;
                    if ((i10 >> 4) > 0) {
                        this.vibratoSpeed = i10 >> 4;
                    }
                    if ((i10 & 15) > 0) {
                        this.vibratoDepth = i10 & 15;
                    }
                    vibrato(false);
                    break;
                case 5:
                    i11 = this.noteParam;
                    if (i11 > 0) {
                        this.vslideParam = i11;
                    }
                    volumeSlide();
                    break;
                case 6:
                    i12 = this.noteParam;
                    if (i12 > 0) {
                        this.vslideParam = i12;
                    }
                    vibrato(false);
                    volumeSlide();
                    break;
                case 7:
                    i6 = this.noteParam;
                    if ((i6 >> 4) > 0) {
                        this.tremoloSpeed = i6 >> 4;
                    }
                    if ((i6 & 15) > 0) {
                        this.tremoloDepth = i6 & 15;
                    }
                    tremolo();
                    break;
                case 8:
                    this.panning = this.noteParam & 255;
                    break;
                default:
                    switch (i15) {
                        case 122:
                            int i28 = this.noteParam;
                            if (i28 > 0) {
                                this.fineVslideUpParam = i28;
                            }
                            int i29 = this.volume + this.fineVslideUpParam;
                            this.volume = i29;
                            if (i29 > 64) {
                                this.volume = 64;
                            }
                            break;
                        case 123:
                            int i30 = this.noteParam;
                            if (i30 > 0) {
                                this.fineVslideDownParam = i30;
                            }
                            int i31 = this.volume - this.fineVslideDownParam;
                            this.volume = i31;
                            if (i31 < 0) {
                                this.volume = 0;
                            }
                            break;
                        case 124:
                            if (this.noteParam <= 0) {
                                this.volume = 0;
                            }
                            break;
                        default:
                            switch (i15) {
                                case 132:
                                    i = this.noteParam;
                                    if (i > 0) {
                                        this.vslideParam = i;
                                    }
                                    volumeSlide();
                                    break;
                                case 133:
                                    i8 = this.noteParam;
                                    if (i8 > 0) {
                                        this.portaDownParam = i8;
                                    }
                                    portamentoDown(this.portaDownParam);
                                    break;
                                case 134:
                                    i7 = this.noteParam;
                                    if (i7 > 0) {
                                        this.portaUpParam = i7;
                                    }
                                    portamentoUp(this.portaUpParam);
                                    break;
                                case 135:
                                    i9 = this.noteParam;
                                    if (i9 > 0) {
                                        this.tonePortaParam = i9;
                                    }
                                    break;
                                case 136:
                                    i10 = this.noteParam;
                                    if ((i10 >> 4) > 0) {
                                        this.vibratoSpeed = i10 >> 4;
                                    }
                                    if ((i10 & 15) > 0) {
                                        this.vibratoDepth = i10 & 15;
                                    }
                                    vibrato(false);
                                    break;
                                case 137:
                                    i3 = this.noteParam;
                                    if ((i3 >> 4) > 0) {
                                        this.tremorOnTicks = i3 >> 4;
                                    }
                                    if ((i3 & 15) > 0) {
                                        this.tremorOffTicks = i3 & 15;
                                    }
                                    tremor();
                                    break;
                                case 138:
                                    int i32 = this.noteParam;
                                    if (i32 > 0) {
                                        this.arpeggioParam = i32;
                                    }
                                    break;
                                case 139:
                                    i12 = this.noteParam;
                                    if (i12 > 0) {
                                        this.vslideParam = i12;
                                    }
                                    vibrato(false);
                                    volumeSlide();
                                    break;
                                case 140:
                                    i11 = this.noteParam;
                                    if (i11 > 0) {
                                        this.vslideParam = i11;
                                    }
                                    volumeSlide();
                                    break;
                            }
                            break;
                    }
                    break;
            }
        } else {
            i5 = this.noteParam;
            if (i5 < 8) {
                this.tremoloType = i5;
            }
        }
        autoVibrato();
        calculateFrequency();
        calculateAmplitude();
        updateEnvelopes();
    }

    /* JADX WARN: Code duplicated, block: B:102:0x0181  */
    /* JADX WARN: Code duplicated, block: B:71:0x00d2  */
    /* JADX WARN: Code duplicated, block: B:72:0x00e1  */
    /* JADX WARN: Code duplicated, block: B:73:0x00e9  */
    /* JADX WARN: Code duplicated, block: B:74:0x00f5  */
    /* JADX WARN: Code duplicated, block: B:75:0x00fa  */
    /* JADX WARN: Code duplicated, block: B:76:0x0101  */
    /* JADX WARN: Code duplicated, block: B:77:0x0108  */
    /* JADX WARN: Code duplicated, block: B:78:0x0114  */
    /* JADX WARN: Code duplicated, block: B:80:0x011a  */
    /* JADX WARN: Code duplicated, block: B:81:0x011e  */
    /* JADX WARN: Code duplicated, block: B:83:0x0124  */
    /* JADX WARN: Code duplicated, block: B:88:0x013f  */
    /* JADX WARN: Code duplicated, block: B:89:0x0143  */
    public void tick() {
        this.vibratoAdd = 0;
        int i = this.fxCount + 1;
        this.fxCount = i;
        this.retrigCount++;
        if (this.noteEffect != 125 || i > this.noteParam) {
            int i2 = this.noteVol;
            int i3 = i2 & 240;
            if (i3 == 96) {
                int i4 = this.volume - (i2 & 15);
                this.volume = i4;
                if (i4 < 0) {
                    this.volume = 0;
                }
            } else if (i3 == 112) {
                int i5 = this.volume + (i2 & 15);
                this.volume = i5;
                if (i5 > 64) {
                    this.volume = 64;
                }
            } else if (i3 == 176) {
                this.vibratoPhase += this.vibratoSpeed;
                vibrato(false);
            } else if (i3 == 208) {
                int i6 = this.panning - (i2 & 15);
                this.panning = i6;
                if (i6 < 0) {
                    this.panning = 0;
                }
            } else if (i3 == 224) {
                int i7 = this.panning + (i2 & 15);
                this.panning = i7;
                if (i7 > 255) {
                    this.panning = 255;
                }
            } else if (i3 == 240) {
                tonePortamento();
            }
        }
        int i8 = this.noteEffect;
        if (i8 == 10) {
            volumeSlide();
        } else if (i8 == 17) {
            GlobalVol globalVol = this.globalVol;
            int i9 = globalVol.volume;
            int i10 = this.globalVslideParam;
            globalVol.volume = i9 + ((i10 >> 4) - (i10 & 15));
            if (this.globalVol.volume < 0) {
                this.globalVol.volume = 0;
            }
            if (this.globalVol.volume > 64) {
                this.globalVol.volume = 64;
            }
        } else if (i8 == 25) {
            int i11 = this.panning;
            int i12 = this.panningSlideParam;
            int i13 = i11 + ((i12 >> 4) - (i12 & 15));
            this.panning = i13;
            if (i13 < 0) {
                this.panning = 0;
            }
            if (this.panning > 255) {
                this.panning = 255;
            }
        } else if (i8 == 27) {
            retrigVolSlide();
        } else if (i8 == 29) {
            tremor();
        } else if (i8 != 121) {
            if (i8 == 149) {
                this.vibratoPhase += this.vibratoSpeed;
                vibrato(true);
            } else if (i8 == 124) {
                if (this.noteParam == this.fxCount) {
                    this.volume = 0;
                }
            } else if (i8 == 125) {
                if (this.noteParam == this.fxCount) {
                    trigger();
                }
            } else if (i8 == 145) {
                retrigVolSlide();
            } else if (i8 == 146) {
                this.tremoloPhase += this.tremoloSpeed;
                tremolo();
            } else if (i8 != 252) {
                if (i8 != 253) {
                    switch (i8) {
                        case LINEAR /* 1 */:
                            portamentoUp(this.portaUpParam);
                            break;
                        case SINC /* 2 */:
                            portamentoDown(this.portaDownParam);
                            break;
                        case 3:
                            tonePortamento();
                            break;
                        case 4:
                            this.vibratoPhase += this.vibratoSpeed;
                            vibrato(false);
                            break;
                        case 5:
                            tonePortamento();
                            volumeSlide();
                            break;
                        case 6:
                            this.vibratoPhase += this.vibratoSpeed;
                            vibrato(false);
                            volumeSlide();
                            break;
                        case 7:
                            this.tremoloPhase += this.tremoloSpeed;
                            tremolo();
                            break;
                        default:
                            switch (i8) {
                                case 132:
                                    volumeSlide();
                                    break;
                                case 133:
                                    portamentoDown(this.portaDownParam);
                                    break;
                                case 134:
                                    portamentoUp(this.portaUpParam);
                                    break;
                                case 135:
                                    tonePortamento();
                                    break;
                                case 136:
                                    this.vibratoPhase += this.vibratoSpeed;
                                    vibrato(false);
                                    break;
                                case 137:
                                    tremor();
                                    break;
                                case 138:
                                    if (this.fxCount > 2) {
                                        this.fxCount = 0;
                                    }
                                    int i14 = this.fxCount;
                                    if (i14 == 0) {
                                        this.arpeggioAdd = 0;
                                    }
                                    if (i14 == 1) {
                                        this.arpeggioAdd = this.arpeggioParam >> 4;
                                    }
                                    if (i14 == 2) {
                                        this.arpeggioAdd = this.arpeggioParam & 15;
                                    }
                                    break;
                                case 139:
                                    this.vibratoPhase += this.vibratoSpeed;
                                    vibrato(false);
                                    volumeSlide();
                                    break;
                                case 140:
                                    tonePortamento();
                                    volumeSlide();
                                    break;
                            }
                            break;
                    }
                } else if (this.noteParam == this.fxCount) {
                    trigger();
                }
            } else if (this.noteParam == this.fxCount) {
                this.volume = 0;
            }
        } else if (this.fxCount >= this.noteParam) {
            this.fxCount = 0;
            this.sampleFra = 0;
            this.sampleIdx = 0;
        }
        autoVibrato();
        calculateFrequency();
        calculateAmplitude();
        updateEnvelopes();
    }

    private void updateEnvelopes() {
        if (this.instrument.volumeEnvelope.enabled) {
            if (!this.keyOn) {
                int i = this.fadeOutVol - this.instrument.volumeFadeOut;
                this.fadeOutVol = i;
                if (i < 0) {
                    this.fadeOutVol = 0;
                }
            }
            this.volEnvTick = this.instrument.volumeEnvelope.nextTick(this.volEnvTick, this.keyOn);
        }
        if (this.instrument.panningEnvelope.enabled) {
            this.panEnvTick = this.instrument.panningEnvelope.nextTick(this.panEnvTick, this.keyOn);
        }
    }

    private void autoVibrato() {
        int i = this.instrument.vibratoDepth & 127;
        if (i > 0) {
            int i2 = this.instrument.vibratoSweep & 127;
            int i3 = this.instrument.vibratoRate & 127;
            int i4 = this.instrument.vibratoType;
            int i5 = this.autoVibratoCount;
            if (i5 < i2) {
                i = (i * i5) / i2;
            }
            this.vibratoAdd += (waveform((i5 * i3) >> 2, i4 + 4) * i) >> 8;
            this.autoVibratoCount++;
        }
    }

    private void volumeSlide() {
        int i = this.vslideParam;
        int i2 = i >> 4;
        int i3 = i & 15;
        if (i3 != 15 || i2 <= 0) {
            if (i2 == 15 && i3 > 0) {
                if (this.fxCount == 0) {
                    this.volume -= i3;
                }
            } else if (this.fxCount > 0 || this.module.fastVolSlides) {
                this.volume += i2 - i3;
            }
        } else if (this.fxCount == 0) {
            this.volume += i2;
        }
        if (this.volume > 64) {
            this.volume = 64;
        }
        if (this.volume < 0) {
            this.volume = 0;
        }
    }

    private void portamentoUp(int i) {
        int i2 = i & 240;
        if (i2 != 224) {
            if (i2 == 240) {
                if (this.fxCount == 0) {
                    this.period -= (i & 15) << 2;
                }
            } else if (this.fxCount > 0) {
                this.period -= i << 2;
            }
        } else if (this.fxCount == 0) {
            this.period -= i & 15;
        }
        if (this.period < 0) {
            this.period = 0;
        }
    }

    private void portamentoDown(int i) {
        int i2 = this.period;
        if (i2 > 0) {
            int i3 = i & 240;
            if (i3 != 224) {
                if (i3 == 240) {
                    if (this.fxCount == 0) {
                        this.period = i2 + ((i & 15) << 2);
                    }
                } else if (this.fxCount > 0) {
                    this.period = i2 + (i << 2);
                }
            } else if (this.fxCount == 0) {
                this.period = i2 + (i & 15);
            }
            if (this.period > 65535) {
                this.period = 65535;
            }
        }
    }

    private void tonePortamento() {
        int i = this.period;
        if (i > 0) {
            int i2 = this.portaPeriod;
            if (i < i2) {
                int i3 = i + (this.tonePortaParam << 2);
                this.period = i3;
                if (i3 > i2) {
                    this.period = i2;
                    return;
                }
                return;
            }
            int i4 = i - (this.tonePortaParam << 2);
            this.period = i4;
            if (i4 < i2) {
                this.period = i2;
            }
        }
    }

    private void vibrato(boolean z) {
        this.vibratoAdd = (waveform(this.vibratoPhase, this.vibratoType & 3) * this.vibratoDepth) >> (z ? 7 : 5);
    }

    private void tremolo() {
        this.tremoloAdd = (waveform(this.tremoloPhase, this.tremoloType & 3) * this.tremoloDepth) >> 6;
    }

    private int waveform(int i, int i2) {
        int i3;
        switch (i2) {
            case LINEAR /* 1 */:
            case 7:
                return 255 - (((i + 32) & 63) << 3);
            case SINC /* 2 */:
            case 5:
                return (i & 32) <= 0 ? -255 : 255;
            case 3:
            case 8:
                int i4 = this.randomSeed;
                this.randomSeed = ((i4 * 65) + 17) & 536870911;
                i3 = (i4 >> 20) - 255;
                break;
            case 4:
            default:
                short s = sineTable[i & 31];
                i3 = s;
                if ((i & 32) > 0) {
                    return -s;
                }
                break;
            case 6:
                return (((i + 32) & 63) << 3) - 255;
        }
        return i3;
    }

    private void tremor() {
        int i = this.retrigCount;
        int i2 = this.tremorOnTicks;
        if (i >= i2) {
            this.tremoloAdd = -64;
        }
        if (i >= i2 + this.tremorOffTicks) {
            this.retrigCount = 0;
            this.tremoloAdd = 0;
        }
    }

    private void retrigVolSlide() {
        if (this.retrigCount >= this.retrigTicks) {
            this.sampleFra = 0;
            this.sampleIdx = 0;
            this.retrigCount = 0;
            switch (this.retrigVolume) {
                case LINEAR /* 1 */:
                    this.volume--;
                    break;
                case SINC /* 2 */:
                    this.volume -= 2;
                    break;
                case 3:
                    this.volume -= 4;
                    break;
                case 4:
                    this.volume -= 8;
                    break;
                case 5:
                    this.volume -= 16;
                    break;
                case 6:
                    this.volume = (this.volume * 2) / 3;
                    break;
                case 7:
                    this.volume >>= 1;
                    break;
                case 9:
                    this.volume++;
                    break;
                case 10:
                    this.volume += 2;
                    break;
                case 11:
                    this.volume += 4;
                    break;
                case 12:
                    this.volume += 8;
                    break;
                case 13:
                    this.volume += 16;
                    break;
                case 14:
                    this.volume = (this.volume * 3) / 2;
                    break;
                case Sample.FP_SHIFT /* 15 */:
                    this.volume <<= 1;
                    break;
            }
            if (this.volume < 0) {
                this.volume = 0;
            }
            if (this.volume > 64) {
                this.volume = 64;
            }
        }
    }

    private void calculateFrequency() {
        int i = this.period + this.vibratoAdd;
        if (this.module.linearPeriods) {
            int i2 = i - (this.arpeggioAdd << 6);
            if (i2 < 28 || i2 > 7680) {
                i2 = 7680;
            }
            this.freq = ((this.module.c2Rate >> 4) * exp2(((4608 - i2) << 15) / 768)) >> 11;
            return;
        }
        if (i > 29021) {
            i = 29021;
        }
        int iExp2 = (i << 15) / exp2((this.arpeggioAdd << 15) / 12);
        this.freq = (this.module.c2Rate * 1712) / (iExp2 >= 28 ? iExp2 : 29021);
    }

    private void calculateAmplitude() {
        int iCalculateAmpl = this.keyOn ? 64 : 0;
        if (this.instrument.volumeEnvelope.enabled) {
            iCalculateAmpl = this.instrument.volumeEnvelope.calculateAmpl(this.volEnvTick);
        }
        int i = this.volume + this.tremoloAdd;
        int i2 = i <= 64 ? i : 64;
        this.ampl = ((((((((i2 >= 0 ? i2 : 0) * this.module.gain) * Sample.FP_ONE) >> 13) * this.fadeOutVol) >> 15) * this.globalVol.volume) * iCalculateAmpl) >> 12;
        int iCalculateAmpl2 = this.instrument.panningEnvelope.enabled ? this.instrument.panningEnvelope.calculateAmpl(this.panEnvTick) : 32;
        int i3 = this.panning;
        this.pann = i3 + (((i3 < 128 ? i3 : 255 - i3) * (iCalculateAmpl2 - 32)) >> 5);
    }

    private void trigger() {
        int i;
        int i2 = this.noteIns;
        if (i2 > 0 && i2 <= this.module.numInstruments) {
            Instrument instrument = this.module.instruments[this.noteIns];
            this.instrument = instrument;
            Sample[] sampleArr = instrument.samples;
            int[] iArr = this.instrument.keyToSample;
            int i3 = this.noteKey;
            if (i3 >= 97) {
                i3 = 0;
            }
            Sample sample = sampleArr[iArr[i3]];
            this.volume = sample.volume >= 64 ? 64 : sample.volume & 63;
            if (sample.panning >= 0) {
                this.panning = sample.panning & 255;
            }
            if (this.period > 0 && sample.looped()) {
                this.sample = sample;
            }
            this.panEnvTick = 0;
            this.volEnvTick = 0;
            this.sampleOffset = 0;
            this.fadeOutVol = Sample.FP_ONE;
            this.keyOn = true;
        }
        int i4 = this.noteEffect;
        if (i4 == 9 || i4 == 143) {
            int i5 = this.noteParam;
            if (i5 > 0) {
                this.offsetParam = i5;
            }
            this.sampleOffset = this.offsetParam << 8;
        }
        int i6 = this.noteVol;
        if (i6 >= 16 && i6 < 96) {
            this.volume = i6 < 80 ? i6 - 16 : 64;
        }
        int i7 = i6 & 240;
        if (i7 == 128) {
            int i8 = this.volume - (i6 & 15);
            this.volume = i8;
            if (i8 < 0) {
                this.volume = 0;
            }
        } else if (i7 == 144) {
            int i9 = this.volume + (i6 & 15);
            this.volume = i9;
            if (i9 > 64) {
                this.volume = 64;
            }
        } else if (i7 != 160) {
            if (i7 == 176) {
                if ((i6 & 15) > 0) {
                    this.vibratoDepth = i6 & 15;
                }
                vibrato(false);
            } else if (i7 == 192) {
                this.panning = (i6 & 15) * 17;
            } else if (i7 == 240 && (i6 & 15) > 0) {
                this.tonePortaParam = i6 & 15;
            }
        } else if ((i6 & 15) > 0) {
            this.vibratoSpeed = i6 & 15;
        }
        int i10 = this.noteKey;
        if (i10 > 0) {
            if (i10 > 96) {
                this.keyOn = false;
                return;
            }
            boolean z = (this.noteVol & 240) == 240 || (i = this.noteEffect) == 3 || i == 5 || i == 135 || i == 140;
            if (!z) {
                this.sample = this.instrument.samples[this.instrument.keyToSample[this.noteKey]];
            }
            int i11 = this.sample.fineTune;
            int i12 = this.noteEffect;
            if (i12 == 117 || i12 == 242) {
                i11 = ((this.noteParam & 15) << 4) - 128;
            }
            int i13 = this.noteKey + this.sample.relNote;
            if (i13 < 1) {
                i13 = 1;
            }
            if (i13 > 120) {
                i13 = 120;
            }
            int i14 = (i13 << 6) + (i11 >> 1);
            if (this.module.linearPeriods) {
                this.portaPeriod = 7744 - i14;
            } else {
                this.portaPeriod = (exp2((i14 << 15) / (-768)) * 29021) >> 15;
            }
            if (z) {
                return;
            }
            this.period = this.portaPeriod;
            this.sampleIdx = this.sampleOffset;
            this.sampleFra = 0;
            if (this.vibratoType < 4) {
                this.vibratoPhase = 0;
            }
            if (this.tremoloType < 4) {
                this.tremoloPhase = 0;
            }
            this.autoVibratoCount = 0;
            this.retrigCount = 0;
        }
    }

    public static int exp2(int i) {
        int i2 = (i & Sample.FP_MASK) >> 8;
        int[] iArr = exp2Table;
        int i3 = iArr[i2];
        return (((((iArr[i2 + 1] - i3) * (i & 255)) >> 8) + i3) << 15) >> (15 - (i >> 15));
    }

    public static int log2(int i) {
        int i2 = 524288;
        for (int i3 = 524288; i3 > 0; i3 >>= 1) {
            int i4 = i2 - i3;
            if (exp2(i4) >= i) {
                i2 = i4;
            }
        }
        return i2;
    }
}
