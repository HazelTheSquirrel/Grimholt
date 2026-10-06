package dev.grimholt.server.vanilla;

public final class VanillaRaid {
    public enum Status { IDLE, ACTIVE, VICTORY, LOSS }
    private Status status=Status.IDLE; private int wave,totalWaves=5; private float progress;
    public Status status(){return status;} public int wave(){return wave;} public int totalWaves(){return totalWaves;} public float progress(){return progress;}
    public void start(int waves){if(waves<1)throw new IllegalArgumentException();totalWaves=waves;wave=1;progress=0;status=Status.ACTIVE;}
    public void setProgress(float value){if(status!=Status.ACTIVE)return;progress=Math.max(0,Math.min(1,value));if(progress>=1){if(wave>=totalWaves)status=Status.VICTORY;else{wave++;progress=0;}}}
    public void defeat(){if(status==Status.ACTIVE)status=Status.LOSS;}
}
