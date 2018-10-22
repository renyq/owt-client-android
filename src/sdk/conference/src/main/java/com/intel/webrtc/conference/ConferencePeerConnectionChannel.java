/*
 * Intel License Header Holder
 */
package com.intel.webrtc.conference;

import com.intel.webrtc.base.AudioCodecParameters;
import com.intel.webrtc.base.AudioEncodingParameters;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.MediaConstraints;
import com.intel.webrtc.base.PeerConnectionChannel;
import com.intel.webrtc.base.Stream;
import com.intel.webrtc.base.VideoCodecParameters;
import com.intel.webrtc.base.VideoEncodingParameters;

import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.intel.webrtc.base.CheckCondition.DCHECK;

final class ConferencePeerConnectionChannel extends PeerConnectionChannel {
    private boolean remoteSdpSet = false;
    private List<IceCandidate> queuedLocalCandidates;
    Stream stream;
    MuteEventObserver muteEventObserver;

    ConferencePeerConnectionChannel(String key, PeerConnection.RTCConfiguration configuration,
                                    boolean enableVideo, boolean enableAudio,
                                    PeerConnectionChannelObserver observer) {
        super(key, configuration, enableVideo, enableAudio, observer);
        queuedLocalCandidates = new LinkedList<>();
    }

    void publish(LocalStream localStream, PublishOptions options) {
        stream = localStream;
        if (options != null && options.videoEncodingParameters != null
                && options.videoEncodingParameters.size() != 0) {
            videoCodecs = new ArrayList<>();
            for (VideoEncodingParameters param : options.videoEncodingParameters) {
                videoCodecs.add(param.codec.name);
            }
            videoMaxBitrate = VideoEncodingParameters.maxBitrate;
        }
        if (options != null && options.audioEncodingParameters != null
                && options.audioEncodingParameters.size() != 0) {
            audioCodecs = new ArrayList<>();
            for (AudioEncodingParameters param : options.audioEncodingParameters) {
                audioCodecs.add(param.codec.name);
            }
            audioMaxBitrate = AudioEncodingParameters.maxBitrate;
        }
        addStream(GetMediaStream(localStream));
        createOffer();
    }

    void subscribe(RemoteStream remoteStream, SubscribeOptions options) {
        stream = remoteStream;
        if (options != null && options.videoOption != null
                && options.videoOption.codecs.size() != 0) {
            videoCodecs = new ArrayList<>();
            for (VideoCodecParameters param : options.videoOption.codecs) {
                videoCodecs.add(param.name);
            }
        }
        if (options != null && options.audioOption != null
                && options.audioOption.codecs.size() != 0) {
            audioCodecs = new ArrayList<>();
            for (AudioCodecParameters param : options.audioOption.codecs) {
                audioCodecs.add(param.name);
            }
        }
        createOffer();
    }

    protected synchronized void dispose() {
        if (stream instanceof LocalStream) {
            removeStream(GetMediaStream(stream));
        }
        super.dispose();
    }

    MediaStream getMediaStream() {
        return GetMediaStream(stream);
    }

    @Override
    public void onSetSuccess() {
        if (signalingState == PeerConnection.SignalingState.STABLE) {
            remoteSdpSet = true;
            for (IceCandidate iceCandidate : queuedLocalCandidates) {
                observer.onIceCandidate(key, iceCandidate);
            }
            queuedLocalCandidates.clear();

            if (stream instanceof LocalStream) {
                setMaxBitrate(GetMediaStream(stream));
            }
        }
    }

    @Override
    public void onCreateFailure(final String error) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                observer.onError(key, error);
            }
        });
    }

    @Override
    public void onSetFailure(final String error) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                observer.onError(key, error);
            }
        });
    }

    @Override
    public void onSignalingChange(final PeerConnection.SignalingState signalingState) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ConferencePeerConnectionChannel.this.signalingState = signalingState;
            }
        });
    }

    @Override
    public void onIceConnectionChange(final PeerConnection.IceConnectionState iceConnectionState) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (iceConnectionState == PeerConnection.IceConnectionState.CLOSED) {
                    observer.onError(key, "");
                }
            }
        });
    }

    @Override
    public void onIceCandidate(final IceCandidate iceCandidate) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (remoteSdpSet) {
                    observer.onIceCandidate(key, iceCandidate);
                } else {
                    queuedLocalCandidates.add(iceCandidate);
                }
            }
        });
    }

    @Override
    public void onAddStream(final MediaStream mediaStream) {
        DCHECK(stream);
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ((RemoteStream) stream).setMediaStream(mediaStream);
                observer.onAddStream(key, (com.intel.webrtc.base.RemoteStream) stream);
            }
        });
    }

    @Override
    public void onRemoveStream(MediaStream mediaStream) {
        callbackExecutor.execute(new Runnable() {
            @Override
            public void run() {
                ((RemoteStream) stream).onEnded();
            }
        });
    }

    @Override
    public void onRenegotiationNeeded() {

    }
}