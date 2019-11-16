/*
 *
 *  * Android SDK for Matomo
 *  *
 *  * @link https://github.com/matomo-org/matomo-android-sdk
 *  * @license https://github.com/matomo-org/matomo-sdk-android/blob/master/LICENSE BSD-3 Clause
 *
 */

package org.matomo.sdk.tools;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

/**
 * Helps us with Urls.
 */
public class UrlHelper {
    private static final String PARAMETER_SEPARATOR = "&";
    private static final String NAME_VALUE_SEPARATOR = "=";

    // Inspired by https://github.com/android/platform_external_apache-http/blob/master/src/org/apache/http/client/utils/URLEncodedUtils.java
    // Helper due to Apache http deprecation

    public static List<Pair<String, String>> parse(@NonNull final URI uri, @Nullable final String encoding) {
        List<Pair<String, String>> result = Collections.emptyList();
        final String query = uri.getRawQuery();
        if (query != null && query.length() > 0) {
            result = new ArrayList<>();
            parse(result, new Scanner(query), encoding);
        }
        return result;
    }

    public static void parse(@NonNull final List<Pair<String, String>> parameters, @NonNull final Scanner scanner, @Nullable final String encoding) {
        scanner.useDelimiter(PARAMETER_SEPARATOR);
        while (scanner.hasNext()) {
            final String[] nameValue = scanner.next().split(NAME_VALUE_SEPARATOR);
            if (nameValue.length == 0 || nameValue.length > 2)
                throw new IllegalArgumentException("bad parameter");

            final String name = decode(nameValue[0], encoding);
            String value = null;
            if (nameValue.length == 2)
                value = decode(nameValue[1], encoding);
            parameters.add(new Pair<>(name, value));
        }
    }


    private static String decode(@NonNull final String content, @Nullable final String encoding) {
        try {
            return URLDecoder.decode(content, encoding != null ? encoding : "UTF-8");
        } catch (UnsupportedEncodingException problem) {
            throw new IllegalArgumentException(problem);
        }
    }
}
