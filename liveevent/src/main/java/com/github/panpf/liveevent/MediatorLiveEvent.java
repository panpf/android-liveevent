/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.panpf.liveevent;

import android.annotation.SuppressLint;

import androidx.annotation.CallSuper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.internal.SafeIterableMap;

import java.util.Map;

@SuppressLint("RestrictedApi")
public class MediatorLiveEvent<T> extends LiveEvent<T> {
    private SafeIterableMap<LiveEvent<?>, Source<?>> mSources = new SafeIterableMap<>();

    /**
     * Starts to listen the given {@code source} LiveEvent, {@code onChanged} listener will be called
     * when {@code source} value was changed.
     * <p>
     * {@code onChanged} callback will be called only when this {@code MediatorLiveEvent} is active.
     * <p> If the given LiveEvent is already added as a source but with a different Listener,
     * {@link IllegalArgumentException} will be thrown.
     *
     * @param source    the {@code LiveEvent} to listen to
     * @param onChanged The listener that will receive the events
     * @param <S>       The type of data hold by {@code source} LiveEvent
     */
    @MainThread
    public <S> void addSource(@NonNull LiveEvent<S> source, @NonNull Listener<? super S> onChanged) {
        Source<S> e = new Source<>(source, onChanged);
        Source<?> existing = mSources.putIfAbsent(source, e);
        if (existing != null && existing.mListener != onChanged) {
            throw new IllegalArgumentException(
                    "This source was already added with the different listener");
        }
        if (existing != null) {
            return;
        }
        if (hasActiveListeners()) {
            e.plug();
        }
    }

    /**
     * Stops to listen the given {@code LiveEvent}.
     *
     * @param toRemote {@code LiveEvent} to stop to listen
     * @param <S>      the type of data hold by {@code source} LiveEvent
     */
    @MainThread
    public <S> void removeSource(@NonNull LiveEvent<S> toRemote) {
        Source<?> source = mSources.remove(toRemote);
        if (source != null) {
            source.unplug();
        }
    }

    @CallSuper
    @Override
    protected void onActive() {
        for (Map.Entry<LiveEvent<?>, Source<?>> source : mSources) {
            source.getValue().plug();
        }
    }

    @CallSuper
    @Override
    protected void onInactive() {
        for (Map.Entry<LiveEvent<?>, Source<?>> source : mSources) {
            source.getValue().unplug();
        }
    }

    private static class Source<V> implements Listener<V> {
        final LiveEvent<V> mLiveEvent;
        final Listener<? super V> mListener;
        int mVersion = START_VERSION;

        Source(LiveEvent<V> liveEvent, final Listener<? super V> listener) {
            mLiveEvent = liveEvent;
            mListener = listener;
        }

        void plug() {
            mLiveEvent.listenForever(this);
        }

        void unplug() {
            mLiveEvent.removeListener(this);
        }

        @Override
        public void onChanged(@Nullable V v) {
            if (mVersion != mLiveEvent.getVersion()) {
                mVersion = mLiveEvent.getVersion();
                mListener.onChanged(v);
            }
        }
    }
}
