/*
 * Copyright (c) 2014-2016, Eren Okka
 * Copyright (c) 2016, Paul Miller
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package com.dgtlrepublic.model.test;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import com.dgtlrepublic.model.utility.DataJsonConverter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parsing unit tests.
 *
 * @author Paul Miller
 */
public class DataTest {
    @Test
    public void validateParsingResults() throws Exception {
        List<Map> testCases = new ObjectMapper().readValue(new File(DataTest.class.getResource("/test-cases.json")
                                                                            .getPath()),
                                                           new TypeReference<List<Map>>() { });
        System.out.println(String.format("Loaded %s test cases.", testCases.size()));
        long start = System.nanoTime();
        for (Map testCase : testCases) { verify(testCase); }
        System.out.println(String.format("Tests took: %s(ms)",
                                         TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
    }

    @SuppressWarnings("unchecked")
    private static void verify(Map entry) throws Exception {
        String fileName = (String) entry.getOrDefault("file_name", "");
        boolean ignore = (Boolean) entry.getOrDefault("ignore", false);
        int id = (Integer) entry.getOrDefault("id", -1);
        HashMap<String, Object> testCases = (HashMap<String, Object>) entry.getOrDefault("results",
                                                                                         new HashMap<String, Object>());
        if (ignore || StringUtils.isBlank(fileName) || testCases.size() == 0) {
            System.out.println(String.format("Ignoring [%s] : { id: %s | results: %s | explicit: %s }",
                                             fileName,
                                             id,
                                             testCases.size(),
                                             ignore));
            return;
        }

        System.out.println("Parsing: " + fileName);
        HashMap<String, Object> parseResults = (HashMap<String, Object>) DataJsonConverter.toTestCaseMap(fileName)
                .getOrDefault("results", null);

        for (Entry<String, Object> testCase : testCases.entrySet()) {
            Object elValue = parseResults.get(testCase.getKey());
            if (elValue == null) {
                throw new Exception(String.format("%n[%s] Missing Element: %s [%s]",
                                                  fileName,
                                                  testCase.getKey(),
                                                  testCase.getValue()));
            } else if (elValue instanceof String && !elValue.equals(testCase.getValue())) {
                throw new Exception(String.format("%n[%s] Incorrect Value:(%s) [%s] { required: [%s] } ",
                                                  fileName,
                                                  testCase.getKey(),
                                                  elValue,
                                                  testCase.getValue()));
            } else if (elValue instanceof List && !((List) elValue).containsAll((Collection<?>) testCase.getValue())) {
                throw new Exception(String.format("%n[%s] Incorrect List Values:(%s) [%s] { required: [%s] } ",
                                                  fileName,
                                                  testCase.getKey(),
                                                  elValue,
                                                  testCase.getValue()));
            }
        }
    }
}
