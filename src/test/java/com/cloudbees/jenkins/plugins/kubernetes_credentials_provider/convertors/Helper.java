/*
 * The MIT License
 *
 * Copyright 2018 CloudBees, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.cloudbees.jenkins.plugins.kubernetes_credentials_provider.convertors;

import io.fabric8.kubernetes.api.model.Secret;
import io.fabric8.kubernetes.client.utils.Serialization;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public class Helper {

    /**
     * Same as {@link Serialization#unmarshal(InputStream)} but properly translates {@link Secret#getStringData}.
     * @see <a href="https://v1-7.docs.kubernetes.io/docs/api-reference/v1.7/#secret-v1-core">Secret v1 core reference</a>
     */
    public static Secret unmarshal(InputStream is) {
        Secret secret = Serialization.unmarshal(is, Secret.class);
        Map<String, String> stringData = secret.getStringData();
        if (stringData != null) {
            Map<String, String> mergedData = new LinkedHashMap<>();
            Map<String, String> data = secret.getData();
            if (data != null) {
                // “All keys and values are merged into the data field on write, overwriting any existing values.”
                mergedData.putAll(data);
            }
            for (Map.Entry<String, String> entry : stringData.entrySet()) {
                // From experimentation, strings seem to be interpreted as UTF-8.
                mergedData.put(entry.getKey(), Base64.getEncoder().encodeToString(entry.getValue().getBytes(StandardCharsets.UTF_8)));
            }
            // “It is never output when reading from the API.”
            secret.setStringData(null);
            secret.setData(mergedData);
        }
        return secret;
    }

}
