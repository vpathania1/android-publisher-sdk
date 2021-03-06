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

package com.criteo.publisher.cache;

import static com.criteo.publisher.util.AdUnitType.CRITEO_BANNER;
import static com.criteo.publisher.util.AdUnitType.CRITEO_CUSTOM_NATIVE;
import static com.criteo.publisher.util.AdUnitType.CRITEO_INTERSTITIAL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import com.criteo.publisher.model.AdSize;
import com.criteo.publisher.model.CacheAdUnit;
import com.criteo.publisher.model.Slot;
import com.criteo.publisher.util.AdUnitType;
import com.criteo.publisher.util.DeviceUtil;
import java.util.HashMap;
import java.util.Map;

public class SdkCache {

  private final Map<CacheAdUnit, Slot> slotMap;
  private final DeviceUtil deviceUtil;

  public SdkCache(@NonNull DeviceUtil deviceUtil) {
    slotMap = new HashMap<>();
    this.deviceUtil = deviceUtil;
  }

  public void add(@NonNull Slot slot) {
    AdUnitType adUnitType = findAdUnitType(slot);
    CacheAdUnit key = new CacheAdUnit(new AdSize(slot.getWidth(), slot.getHeight())
        , slot.getPlacementId(), adUnitType);
    slotMap.put(key, slot);
  }

  // FIXME: EE-608
  private AdUnitType findAdUnitType(Slot slot) {
    if (slot.isNative()) {
      return CRITEO_CUSTOM_NATIVE;
    }

    if ((deviceUtil.getSizePortrait().getHeight() == slot.getHeight()
        && deviceUtil.getSizePortrait().getWidth() == slot.getWidth())
        || deviceUtil.getSizeLandscape().getHeight() == slot.getHeight()
        && deviceUtil.getSizeLandscape().getWidth() == slot.getWidth()) {
      return CRITEO_INTERSTITIAL;
    }

    return CRITEO_BANNER;
  }

  /**
   * Get the slot corresponding to the given key.
   * <p>
   * If no slot match the given key, then <code>null</code> is returned.
   *
   * @param key of the slot to look for
   * @return found slot or null if not found
   */
  @Nullable
  public Slot peekAdUnit(CacheAdUnit key) {
    return slotMap.get(key);
  }

  public void remove(CacheAdUnit key) {
    slotMap.remove(key);
  }

  @VisibleForTesting
  int getItemCount() {
    return slotMap.size();
  }
}
