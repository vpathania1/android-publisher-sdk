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

package com.criteo.publisher.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.criteo.publisher.logging.Logger;
import com.criteo.publisher.logging.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtil {

  @NonNull
  private static final Logger logger = LoggerFactory.getLogger(ReflectionUtil.class);

  @Nullable
  public static Object callMethodOnObject(
      @NonNull Object object,
      @NonNull String methodName,
      @NonNull Object... params
  ) {
    try {
      int len = params.length;
      Class<?>[] classes = new Class[len];
      for (int i = 0; i < len; i++) {
        classes[i] = params[i].getClass();
      }
      Method method = object.getClass().getMethod(methodName, classes);
      return method.invoke(object, params);
    } catch (NullPointerException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
      logger.debug("Failed to call " + methodName, e);
      return null;
    }
  }

}
