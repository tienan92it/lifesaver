/*
 * Copyright 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.textuality.lifesaver2;

import android.content.Context;

public class ColumnsFactory {
    public static Columns calls(Context context) {
        String[] names = { "type", "numbertype", "number", "numberlabel",
                "date", "duration" };
        Class<?> types[] = { Integer.TYPE, Integer.TYPE, String.class,
                String.class, Long.TYPE, Long.TYPE };
        return new Columns(context, names, types, "number", "date");
    }

    public static Columns messages(Context context) {
        String[] names = { "person", "status", "address", "read", "subject",
                "body", "service_center", "date", "type" };
        Class<?> types[] = { Integer.TYPE, Integer.TYPE, String.class,
                String.class, String.class, String.class, String.class,
                Long.TYPE, Integer.TYPE };
        return new Columns(context, names, types, "address", "date");
    }
}
