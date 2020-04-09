package ro.atm.corden.model.video;

import androidx.annotation.NonNull;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

/***/
public class VideoInfo implements Serializable {
    @Expose(serialize = false)
    private boolean isSeekable;
    @Expose(serialize = false)
    private long seekableInit;
    @Expose(serialize = false)
    private long seekableEnd;
    @Expose(serialize = false)
    private long duration;

    protected VideoInfo() {
    }

    public VideoInfo(boolean isSeekable,
                     long seekableInit,
                     long seekableEnd,
                     long duration) {
        this.isSeekable = isSeekable;
        this.seekableInit = seekableInit;
        this.seekableEnd = seekableEnd;
        this.duration = duration;
    }

    public boolean getIsSeekable() {
        return this.isSeekable;
    }

    public void setIsSeekable(boolean isSeekable) {
        this.isSeekable = isSeekable;
    }

    public long getSeekableInit() {
        return this.seekableInit;
    }

    public void setSeekableInit(long seekableInit) {
        this.seekableInit = seekableInit;
    }

    public long getSeekableEnd() {
        return this.seekableEnd;
    }

    public void setSeekableEnd(long seekableEnd) {
        this.seekableEnd = seekableEnd;
    }

    public long getDuration() {
        return this.duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Is seekable: ").append(isSeekable).append("\n");
        stringBuilder.append("Seekabe init: ").append(seekableInit).append("\n");
        stringBuilder.append("Seekable end: ").append(seekableEnd).append("\n");
        stringBuilder.append("Duration: ").append(duration).append("\n");
        return stringBuilder.toString();
    }
}
