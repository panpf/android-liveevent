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

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

/**
 * Transformation methods for {@link LiveEvent}.
 * <p>
 * These methods permit functional composition and delegation of {@link LiveEvent} instances. The
 * transformations are calculated lazily, and will run only when the returned {@link LiveEvent} is
 * observed. Lifecycle behavior is propagated from the input {@code source} {@link LiveEvent} to the
 * returned one.
 */
public class LiveEventTransformations {

    private LiveEventTransformations() {
    }

    /**
     * Returns a {@code LiveEvent} mapped from the input {@code source} {@code LiveEvent} by applying
     * {@code mapFunction} to each value set on {@code source}.
     * <p>
     * This method is analogous to {@link io.reactivex.Observable#map}.
     * <p>
     * {@code transform} will be executed on the main thread.
     * <p>
     * Here is an example mapping a simple {@code User} struct in a {@code LiveEvent} to a
     * {@code LiveEvent} containing their full name as a {@code String}.
     *
     * <pre>
     * LiveEvent<User> userLiveEvent = ...;
     * LiveEvent<String> userFullNameLiveEvent =
     *     Transformations.map(
     *         userLiveEvent,
     *         user -> user.firstName + user.lastName);
     * });
     * </pre>
     *
     * @param source      the {@code LiveEvent} to map from
     * @param mapFunction a function to apply to each value set on {@code source} in order to set
     *                    it
     *                    on the output {@code LiveEvent}
     * @param <X>         the generic type parameter of {@code source}
     * @param <Y>         the generic type parameter of the returned {@code LiveEvent}
     * @return a LiveEvent mapped from {@code source} to type {@code <Y>} by applying
     * {@code mapFunction} to each value set.
     */
    @SuppressWarnings("JavadocReference")
    @MainThread
    public static <X, Y> LiveEvent<Y> map(
            @NonNull LiveEvent<X> source,
            @NonNull final Function<X, Y> mapFunction) {
        final MediatorLiveEvent<Y> result = new MediatorLiveEvent<>();
        result.addSource(source, new Listener<X>() {
            @Override
            public void onChanged(@Nullable X x) {
                result.setValue(mapFunction.apply(x));
            }
        });
        return result;
    }

    /**
     * Returns a {@code LiveEvent} mapped from the input {@code source} {@code LiveEvent} by applying
     * {@code switchMapFunction} to each value set on {@code source}.
     * <p>
     * The returned {@code LiveEvent} delegates to the most recent {@code LiveEvent} created by
     * calling {@code switchMapFunction} with the most recent value set to {@code source}, without
     * changing the reference. In this way, {@code switchMapFunction} can change the 'backing'
     * {@code LiveEvent} transparently to any observer registered to the {@code LiveEvent} returned
     * by {@code switchMap()}.
     * <p>
     * Note that when the backing {@code LiveEvent} is switched, no further values from the older
     * {@code LiveEvent} will be set to the output {@code LiveEvent}. In this way, the method is
     * analogous to {@link io.reactivex.Observable#switchMap}.
     * <p>
     * {@code switchMapFunction} will be executed on the main thread.
     * <p>
     * Here is an example class that holds a typed-in name of a user
     * {@code String} (such as from an {@code EditText}) in a {@link MutableLiveEvent} and
     * returns a {@code LiveEvent} containing a List of {@code User} objects for users that have
     * that name. It populates that {@code LiveEvent} by requerying a repository-pattern object
     * each time the typed name changes.
     * <p>
     * This {@code ViewModel} would permit the observing UI to update "live" as the user ID text
     * changes.
     *
     * <pre>
     * class UserViewModel extends AndroidViewModel {
     *     LiveEvent<String> nameQueryLiveEvent = ...
     *
     *     LiveEvent<List<String>> getUsersWithNameLiveEvent() {
     *         return Transformations.switchMap(
     *             nameQueryLiveEvent,
     *                 name -> myDataSource.getUsersWithNameLiveEvent(name));
     *     }
     *
     *     void setNameQuery(String name) {
     *         this.nameQueryLiveEvent.setValue(name);
     *     }
     * }
     * </pre>
     *
     * @param source            the {@code LiveEvent} to map from
     * @param switchMapFunction a function to apply to each value set on {@code source} to create a
     *                          new delegate {@code LiveEvent} for the returned one
     * @param <X>               the generic type parameter of {@code source}
     * @param <Y>               the generic type parameter of the returned {@code LiveEvent}
     * @return a LiveEvent mapped from {@code source} to type {@code <Y>} by delegating
     * to the LiveEvent returned by applying {@code switchMapFunction} to each
     * value set
     */
    @SuppressWarnings("JavadocReference")
    @MainThread
    public static <X, Y> LiveEvent<Y> switchMap(
            @NonNull LiveEvent<X> source,
            @NonNull final Function<X, LiveEvent<Y>> switchMapFunction) {
        final MediatorLiveEvent<Y> result = new MediatorLiveEvent<>();
        result.addSource(source, new Listener<X>() {
            LiveEvent<Y> mSource;

            @Override
            public void onChanged(@Nullable X x) {
                LiveEvent<Y> newLiveEvent = switchMapFunction.apply(x);
                if (mSource == newLiveEvent) {
                    return;
                }
                if (mSource != null) {
                    result.removeSource(mSource);
                }
                mSource = newLiveEvent;
                if (mSource != null) {
                    result.addSource(mSource, new Listener<Y>() {
                        @Override
                        public void onChanged(@Nullable Y y) {
                            result.setValue(y);
                        }
                    });
                }
            }
        });
        return result;
    }
}
