/* Copyright (c) 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package com.kostya.webcam;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * A simple class that provides utilities to ease command line parsing.
 */
public class SimpleCommandLineParser {

    private final Map<String, String> argMap;

    /**
     * Initializes the command line parser by parsing the command line
     * args using simple rules.
     * <p/>
     * The arguments are parsed into keys and values and are saved into
     * a HashMap.  Any argument that begins with a '--' or '-' is assumed
     * to be a key.  If the following argument doesn't have a '--'/'-' it
     * is assumed to be a value of the preceding argument.
     */
    public SimpleCommandLineParser(String[] arg) {
        argMap = new HashMap<String, String>();
        for (int i = 0; i < arg.length; i++) {
            String key;
            if (arg[i].startsWith("--")) {
                key = arg[i].substring(2);
            } else if (arg[i].startsWith("-")) {
                key = arg[i].substring(1);
            } else {
                argMap.put(arg[i], null);
                continue;
            }
            String value;
            int index = key.indexOf('=');
            if (index == -1) {
                if (((i + 1) < arg.length) &&
                        (arg[i + 1].charAt(0) != '-')) {
                    argMap.put(key, arg[i + 1]);
                    i++;
                } else {
                    argMap.put(key, null);
                }
            } else {
                value = key.substring(index + 1);
                key = key.substring(0, index);
                argMap.put(key, value);
            }
        }
    }

    public SimpleCommandLineParser(String[] arg, String predict) {
        argMap = new HashMap<String, String>();
        for (String anArg : arg) {
            String[] str = anArg.split(predict, 2);
            if (str.length > 1)
                argMap.put(str[0], str[1]);
        }
    }

    /**
     * Returns the value of the first key found in the map.
     */
    public String getValue(String... keys) {
        for (String key : keys) {
            if (argMap.get(key) != null) {
                return argMap.get(key);
            }
        }
        return null;
    }

    /**
     * Returns true if any of the given keys are present in the map.
     */
    public boolean containsKey(String... keys) {
        Set<String> keySet = argMap.keySet();
        for (String key : keySet) {
            for (String key1 : keys) {
                if (key.equals(key1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public Iterator<String> getKeyIterator() {
        Set<String> keySet = argMap.keySet();
        if (!keySet.isEmpty())
            return keySet.iterator();
        return null;
    }

    public int getSize() {
        return argMap.size();
    }
}
