/*
 * Intel License Header Holder
 */
package com.intel.webrtc.base;

import static com.intel.webrtc.base.CheckCondition.RCHECK;

/**
 * Encoding parameters for sending a video track.
 */
public final class VideoEncodingParameters {
    /**
     * Video codec.
     */
    public final VideoCodecParameters codec;
    /**
     * Maximum bitrate for sending a video track.
     * *NOTE* currently setting different bitrates for different video codecs is not supported.
     */
    public static int maxBitrate = 0;

    public VideoEncodingParameters(MediaCodecs.VideoCodec codec) {
        RCHECK(codec);
        this.codec = new VideoCodecParameters(codec);
    }

    public VideoEncodingParameters(VideoCodecParameters videoCodecParameters) {
        RCHECK(videoCodecParameters);
        codec = videoCodecParameters;
    }

    public VideoEncodingParameters(VideoCodecParameters videoCodecParameters, int maxBitrateKbps) {
        RCHECK(videoCodecParameters);
        RCHECK(maxBitrateKbps > 0);
        this.codec = videoCodecParameters;
        maxBitrate = maxBitrateKbps;
    }
}