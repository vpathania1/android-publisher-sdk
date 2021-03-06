/*
 *    Copyright 2020 Criteo
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.criteo.publisher;

import static android.content.ContentValues.TAG;

import android.util.Log;
import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.criteo.publisher.bid.BidLifecycleListener;
import com.criteo.publisher.cache.SdkCache;
import com.criteo.publisher.csm.MetricSendingQueueConsumer;
import com.criteo.publisher.model.AdUnit;
import com.criteo.publisher.model.AdUnitMapper;
import com.criteo.publisher.model.CacheAdUnit;
import com.criteo.publisher.model.CdbRequest;
import com.criteo.publisher.model.CdbResponse;
import com.criteo.publisher.model.Config;
import com.criteo.publisher.model.Slot;
import com.criteo.publisher.network.BidRequestSender;
import com.criteo.publisher.util.ApplicationStoppedListener;
import com.criteo.publisher.util.CdbCallListener;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class BidManager implements ApplicationStoppedListener {

  /**
   * Default TTL (15 minutes in seconds) overridden on immediate bids (CPM > 0, TTL = 0).
   */
  private static final int DEFAULT_TTL_IN_SECONDS = 15 * 60;

  @NonNull
  @GuardedBy("cacheLock")
  private final SdkCache cache;
  private final Object cacheLock = new Object();

  private final AtomicLong cdbTimeToNextCall = new AtomicLong(0);

  @NonNull
  private final Config config;

  @NonNull
  private final Clock clock;

  @NonNull
  private final AdUnitMapper adUnitMapper;

  @NonNull
  private final BidRequestSender bidRequestSender;

  @NonNull
  private final BidLifecycleListener bidLifecycleListener;

  @NonNull
  private final MetricSendingQueueConsumer metricSendingQueueConsumer;

  BidManager(
      @NonNull SdkCache sdkCache,
      @NonNull Config config,
      @NonNull Clock clock,
      @NonNull AdUnitMapper adUnitMapper,
      @NonNull BidRequestSender bidRequestSender,
      @NonNull BidLifecycleListener bidLifecycleListener,
      @NonNull MetricSendingQueueConsumer metricSendingQueueConsumer
  ) {
    this.cache = sdkCache;
    this.config = config;
    this.clock = clock;
    this.adUnitMapper = adUnitMapper;
    this.bidRequestSender = bidRequestSender;
    this.bidLifecycleListener = bidLifecycleListener;
    this.metricSendingQueueConsumer = metricSendingQueueConsumer;
  }

  /**
   * load data for next time
   */
  private void fetch(CacheAdUnit cacheAdUnit) {
    if (cdbTimeToNextCall.get() <= clock.getCurrentTimeInMillis()) {
      sendBidRequest(Collections.singletonList(cacheAdUnit));
    }
  }

  private void sendBidRequest(List<CacheAdUnit> prefetchCacheAdUnits) {
    if (killSwitchEngaged()) {
      return;
    }

    bidRequestSender.sendBidRequest(prefetchCacheAdUnits, new CdbListener());
    metricSendingQueueConsumer.sendMetricBatch();
  }

  /**
   * Returns the last fetched bid a fetch a new one for the next invocation.
   * <p>
   * A <code>null</code> value could be returned. This means that there is no valid bid for the
   * given {@link AdUnit}. And caller should not try to display anything.
   * <code>null</code> may be returned in case of
   * <ul>
   *   <li>The kill switch is engaged. See {@link Config#isKillSwitchEnabled()}</li>
   *   <li>The given {@link AdUnit} is not valid. See {@link AdUnitMapper} for validity definition</li>
   *   <li>There is no last fetch bid or last is consumed</li>
   *   <li>Last fetch bid correspond to a no-bid (CPM = 0 and TTL = 0)</li>
   *   <li>Last fetch bid is a not-expired silence (CPM = 0 and TTL > 0)</li>
   *   <li>Last fetch bid is expired</li>
   * </ul>
   * <p>
   * Asynchronously, a new bid is fetch to CDB to get a new proposition. Hence if this method
   * returns a bid, it is consumed, and you have to wait for the new proposition to get a result
   * again. Meanwhile, you'll only get a <code>null</code> value.
   * There may be some case when a new bid is not fetch:
   * <ul>
   *   <li>The kill switch is engaged</li>
   *   <li>The given {@link AdUnit} is not valid</li>
   *   <li>Last fetch bid is a not-expired silence</li>
   *   <li>There is already an async call to CDB for the given {@link AdUnit}</li>
   * </ul>
   *
   * @param adUnit Declaration of ad unit to get a bid from
   * @return a valid bid that may be displayed or <code>null</code> that should be ignored
   */
  @Nullable
  public Slot getBidForAdUnitAndPrefetch(@Nullable AdUnit adUnit) {
    if (killSwitchEngaged()) {
      return null;
    }
    CacheAdUnit cacheAdUnit = adUnitMapper.map(adUnit);
    if (cacheAdUnit == null) {
      Log.e(TAG, "Valid AdUnit is required.");
      return null;
    }

    synchronized (cacheLock) {
      Slot peekSlot = cache.peekAdUnit(cacheAdUnit);
      if (peekSlot == null) {
        // If no matching bid response is found
        fetch(cacheAdUnit);
        return null;
      }

      double cpm = (peekSlot.getCpmAsNumber() == null ? 0.0 : peekSlot.getCpmAsNumber());
      long ttl = peekSlot.getTtl();

      boolean isNotExpired = !peekSlot.isExpired(clock);
      boolean isValidBid = (cpm > 0) && (ttl > 0);
      boolean isSilentBid = (cpm == 0) && (ttl > 0);

      if (isSilentBid && isNotExpired) {
        return null;
      }

      bidLifecycleListener.onBidConsumed(cacheAdUnit, peekSlot);
      cache.remove(cacheAdUnit);
      fetch(cacheAdUnit);

      if (isValidBid && isNotExpired) {
        return peekSlot;
      }

      return null;
    }
  }


  @VisibleForTesting
  void setCacheAdUnits(@NonNull List<Slot> slots) {
    long instant = clock.getCurrentTimeInMillis();

    synchronized (cacheLock) {
      for (Slot slot : slots) {
        if (slot.isValid()) {
          boolean isImmediateBid = slot.getCpmAsNumber() > 0 && slot.getTtl() == 0;
          if (isImmediateBid) {
            slot.setTtl(DEFAULT_TTL_IN_SECONDS);
          }

          slot.setTimeOfDownload(instant);
          cache.add(slot);
        }
      }
    }
  }

  @VisibleForTesting
  void setTimeToNextCall(int seconds) {
    if (seconds > 0) {
      this.cdbTimeToNextCall.set(clock.getCurrentTimeInMillis() + seconds * 1000);
    }
  }

  @Override
  public void onApplicationStopped() {
    bidRequestSender.cancelAllPendingTasks();
  }

  /**
   * This method is called back after the "useragent" is fetched
   *
   * @param adUnits list of ad units to prefetch
   */
  public void prefetch(@NonNull List<AdUnit> adUnits) {
    List<List<CacheAdUnit>> requestedAdUnitsChunks = adUnitMapper.mapToChunks(adUnits);

    bidRequestSender.sendRemoteConfigRequest(config);

    for (List<CacheAdUnit> requestedAdUnits : requestedAdUnitsChunks) {
      sendBidRequest(requestedAdUnits);
    }
  }

  private boolean killSwitchEngaged() {
    return config.isKillSwitchEnabled();
  }

  private class CdbListener implements CdbCallListener {

    @Override
    public void onCdbRequest(@NonNull CdbRequest request) {
      bidLifecycleListener.onCdbCallStarted(request);
    }

    @Override
    public void onCdbResponse(@NonNull CdbRequest request, @NonNull CdbResponse response) {
      setCacheAdUnits(response.getSlots());
      setTimeToNextCall(response.getTimeToNextCall());
      bidLifecycleListener.onCdbCallFinished(request, response);
    }

    @Override
    public void onCdbError(@NonNull CdbRequest request, @NonNull Exception exception) {
      bidLifecycleListener.onCdbCallFailed(request, exception);
    }
  }
}
